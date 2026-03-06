## Context

Fleet Service tiene 4 módulos hexagonales funcionales (domain 51 tests, application 17 tests, infrastructure 0 tests directos, container 3 ITs) con un logger no-op (`FleetDomainEventPublisherAdapter`) como event publisher. Los 4 eventos de dominio existentes se loguean y se pierden.

La infraestructura de messaging ya existe desde `reservation-outbox-and-messaging` (ciclo #12) y fue replicada en `payment-outbox-and-messaging` (ciclo #16) y `customer-outbox-and-messaging` (ciclo #18):
- `common-messaging` module con OutboxEvent, OutboxPublisher (polling 500ms), OutboxCleanupScheduler, MessageConverterConfig
- Docker Compose con PostgreSQL + RabbitMQ
- `definitions.json` con topología pre-declarada incluyendo fleet.exchange, fleet.confirmed.queue, fleet.rejected.queue, fleet.confirm.command.queue, fleet.dlq (la command queue de confirm se añadió en `pre-saga-alignment`; la de release NO existe y debe añadirse)

Fleet es único entre los 4 servicios porque participa en **dos** flujos SAGA:
1. **Confirmación** (happy path): recibe comando → verifica disponibilidad → responde confirmed/rejected
2. **Compensación** (unhappy path): recibe comando de release → libera el vehículo → responde released

Customer solo tiene confirmación (validate). Payment tiene confirmación (process) + compensación (refund) pero la compensación de Payment se implementará en `reservation-saga-orchestration` (es el orquestador quien envía el comando de refund). Fleet necesita ambas queues ahora para estar listo.

### Flujo actual (sin cambios)
```
FleetApplicationService.execute(RegisterVehicleCommand)  // @Transactional
  → Vehicle.create(...)                                   // aggregate acumula eventos
  → vehicleRepository.save(vehicle)                       // JPA persist
  → eventPublisher.publish(vehicle.getDomainEvents())     // ← ESTE adapter cambia
  → vehicle.clearDomainEvents()
```

### Flujo nuevo: confirmación SAGA
```
RabbitMQ → FleetConfirmationListener.handle()             // @RabbitListener
  → confirmFleetAvailabilityUseCase.execute(command)       // @Transactional
    → vehicleRepository.findById(vehicleId)                // lectura
    → evaluar si existe y está ACTIVE
    → eventPublisher.publish(List.of(responseEvent))       // outbox write en misma TX
```

### Flujo nuevo: compensación SAGA
```
RabbitMQ → FleetReleaseListener.handle()                   // @RabbitListener
  → releaseFleetReservationUseCase.execute(command)         // @Transactional
    → vehicleRepository.findById(vehicleId)                 // lectura
    → (no-op sobre aggregate — sin modelo de reservas)
    → eventPublisher.publish(List.of(releasedEvent))        // outbox write en misma TX
```

## Goals / Non-Goals

**Goals:**
- Reemplazar el logger no-op por un OutboxFleetDomainEventPublisher que persiste los 7 eventos (4 lifecycle + 3 SAGA) en la tabla outbox_events
- Declarar la topología RabbitMQ de Fleet como beans Spring — incluyendo las 2 command queues de entrada
- Implementar 2 `@RabbitListener`: uno para confirmación, otro para compensación
- Añadir 3 nuevos domain events SAGA (FleetConfirmedEvent, FleetRejectedEvent, FleetReleasedEvent) con reservationId para correlación
- Añadir `fleet.release.command.queue` a definitions.json (la de confirm ya existe)
- Verificar atomicidad y publishing con integration tests

**Non-Goals:**
- Modificar common-messaging (ya es genérico y funciona)
- Modificar domain o application layers existentes (los 5 use cases actuales no cambian)
- Crear el orquestador SAGA (eso es reservation-saga-orchestration)
- Implementar disponibilidad por fechas / modelo de vehicle_reservations (change post-SAGA)
- Implementar idempotencia de consumidor (no hay consumidores de los eventos de Fleet todavía)
- Logging estructurado / MDC (change post-SAGA)

## Decisions

### Decision 1: Reemplazar FleetDomainEventPublisherAdapter, no mantener ambos

**Elegido**: Eliminar `FleetDomainEventPublisherAdapter` y crear `OutboxFleetDomainEventPublisher` en su lugar.

**Alternativa rechazada**: Mantener ambos con `@Profile`. Complejidad innecesaria — el logger no-op no tiene valor una vez que existe el outbox.

**Consecuencia**: El viejo adapter era `@Component`, el nuevo también es `@Component` implementando la misma interfaz. Spring auto-detecta el reemplazo. `BeanConfiguration` no necesita cambios para este punto (sí necesita cambios por los nuevos use cases — ver Decision 7).

### Decision 2: Publicar los 7 eventos — 4 lifecycle + 3 SAGA

El OutboxFleetDomainEventPublisher maneja 7 tipos de evento:

| Evento | Routing Key | Queue bindada | Consumidor actual |
|--------|-------------|---------------|-------------------|
| VehicleRegisteredEvent | fleet.registered | (ninguna) | Ninguno |
| VehicleSentToMaintenanceEvent | fleet.senttomaintenance | (ninguna) | Ninguno |
| VehicleActivatedEvent | fleet.activated | (ninguna) | Ninguno |
| VehicleRetiredEvent | fleet.retired | (ninguna) | Ninguno |
| FleetConfirmedEvent | fleet.confirmed | fleet.confirmed.queue | Futuro orquestador SAGA |
| FleetRejectedEvent | fleet.rejected | fleet.rejected.queue | Futuro orquestador SAGA |
| FleetReleasedEvent | fleet.released | (ninguna por ahora) | Futuro orquestador SAGA |

**Por qué publicar los 4 lifecycle sin consumidores**: Mismo razonamiento que Customer (Decision 2 del ciclo #18). El publisher implementa `FleetDomainEventPublisher`, la misma interfaz que usan los 5 use cases existentes. Si el publisher solo manejara 3 eventos SAGA, los otros 4 provocarían `IllegalArgumentException` en runtime. Los mensajes sin queue bindada se descartan silenciosamente por RabbitMQ.

**FleetReleasedEvent sin queue bindada**: No hay `fleet.released.queue` en definitions.json ni en RabbitMQConfig. El orquestador SAGA consumirá este evento cuando se implemente — en ese momento se añadirá la queue al exchange de Fleet. Por ahora el evento se publica al exchange y RabbitMQ lo descarta.

### Decision 3: Derivación de routing key — approach mixto por naming inconsistente

Fleet tiene un problema que Customer y Payment no tenían: los eventos de lifecycle se llaman `Vehicle*Event` (no `Fleet*Event`), mientras que los eventos SAGA se llaman `Fleet*Event`. La auto-derivación con un solo `replace()` no funciona:

```
// Customer: uniforme — todos "Customer*Event"
CustomerCreatedEvent → replace("Customer","").replace("Event","") → "created" → "customer.created" ✓

// Fleet: inconsistente
VehicleRegisteredEvent → replace("Fleet","").replace("Event","") → "VehicleRegistered" → "fleet.vehicleregistered" ✗
FleetConfirmedEvent → replace("Fleet","").replace("Event","") → "Confirmed" → "fleet.confirmed" ✓
```

**Elegido**: Derivación explícita con Map estático. Es más verbose que la convención de Customer/Payment pero es correcto y legible:

```java
private static final Map<Class<? extends DomainEvent>, String> ROUTING_KEYS = Map.of(
    VehicleRegisteredEvent.class, "fleet.registered",
    VehicleSentToMaintenanceEvent.class, "fleet.senttomaintenance",
    VehicleActivatedEvent.class, "fleet.activated",
    VehicleRetiredEvent.class, "fleet.retired",
    FleetConfirmedEvent.class, "fleet.confirmed",
    FleetRejectedEvent.class, "fleet.rejected",
    FleetReleasedEvent.class, "fleet.released"
);
```

**Alternativa rechazada**: Doble `replace()` con "Vehicle" y "Fleet". Funciona pero es frágil — si se añade un evento con otro prefijo, falla silenciosamente. El Map explícito falla ruidosamente con `IllegalArgumentException` si un evento no está registrado.

**Alternativa rechazada**: Renombrar los 4 lifecycle events a `Fleet*Event` (e.g. `FleetRegisteredEvent`). Cambiaría la API de dominio existente y rompe tests. No vale la pena para un POC.

### Decision 4: Extracción de aggregateId — 7 instanceof checks con vehicleId() en todos

Los 7 eventos tienen `vehicleId()` (los 4 lifecycle ya lo tienen; los 3 SAGA nuevos lo llevarán). Las 7 ramas son idénticas excepto por el tipo. Mismo patrón que Customer (6 checks) y Payment (3 checks).

```java
if (event instanceof VehicleRegisteredEvent e) return e.vehicleId().value().toString();
if (event instanceof VehicleSentToMaintenanceEvent e) return e.vehicleId().value().toString();
// ... 5 más, mismo patrón
throw new IllegalArgumentException("Unknown domain event type");
```

### Decision 5: RabbitMQConfig con 2 command queues — diferencia vs Customer

Customer declara 1 command queue (validate). Fleet declara 2 command queues (confirm + release):

```
fleet.exchange (TopicExchange, durable)
  ├── fleet.confirmed.queue          → routing key "fleet.confirmed"          (evento saliente)
  ├── fleet.rejected.queue           → routing key "fleet.rejected"           (evento saliente)
  ├── fleet.confirm.command.queue    → routing key "fleet.confirm.command"    (comando entrante)
  ├── fleet.release.command.queue    → routing key "fleet.release.command"    (comando compensación)
  └── DLQ → dlx.exchange → fleet.dlq (compartida por las 5 queues)
```

**Total beans: ~19** (2 exchanges + 5 queues + 5 bindings normales + 5 bindings DLQ + 2 listeners ≈ 19 beans).

### Decision 6: Dos listeners separados — FleetConfirmationListener + FleetReleaseListener

**Elegido**: Un listener por command queue. `FleetConfirmationListener` consume de `fleet.confirm.command.queue`, `FleetReleaseListener` consume de `fleet.release.command.queue`.

**Alternativa rechazada**: Un solo listener con switch en el tipo de mensaje. Mezcla responsabilidades — el contrato del mensaje de confirm (vehicleId + reservationId + pickupDate + returnDate) es diferente del de release (vehicleId + reservationId). Dos listeners son más claros y permiten evolucionar independientemente.

**Package**: `adapter.input.messaging` (mismo que Customer).

**Tipo de mensaje**: Ambos reciben `Message` (Spring AMQP raw) y parsean JSON con `ObjectMapper`. No usa deserialización automática — consistente con Customer.

**Contrato del mensaje de confirm**:
```json
{"vehicleId": "uuid", "reservationId": "uuid", "pickupDate": "2025-02-01", "returnDate": "2025-02-04"}
```

**Contrato del mensaje de release**:
```json
{"vehicleId": "uuid", "reservationId": "uuid"}
```

### Decision 7: Dos use cases SAGA — ConfirmFleetAvailabilityUseCase + ReleaseFleetReservationUseCase

`FleetApplicationService` añade ambos a su lista de `implements` (pasa de 5 a 7 interfaces). Es consistente con el patrón del proyecto — un solo Application Service por bounded context.

**ConfirmFleetAvailabilityUseCase** — lectura con side-effect (mismo patrón que Customer):
1. Lee el vehicle por vehicleId
2. Evalúa si existe y está ACTIVE
3. Publica `FleetConfirmedEvent` o `FleetRejectedEvent` vía eventPublisher
4. `@Transactional` (no readOnly) por el outbox write
5. No llama `save()` ni `clearDomainEvents()` — el aggregate no muta

**Lógica de confirmación**:
- Vehicle not found → `FleetRejectedEvent` con failureMessages `["Vehicle not found: {id}"]`
- Vehicle exists pero no ACTIVE → `FleetRejectedEvent` con failureMessages `["Vehicle is not available, current status: {status}"]`
- Vehicle exists y ACTIVE → `FleetConfirmedEvent`

**ConfirmFleetAvailabilityCommand**: Lleva `vehicleId` (String) + `reservationId` (String) + `pickupDate` (String) + `returnDate` (String). Fechas como String ISO-8601 para simplicidad en el command — se parsean si se necesitan en el futuro. Por ahora no se usan en la lógica.

**ReleaseFleetReservationUseCase** — compensación (no-op sobre aggregate):
1. Lee el vehicle por vehicleId (verificación de existencia)
2. Publica `FleetReleasedEvent` — confirma al orquestador que la liberación se completó
3. `@Transactional` (no readOnly) por el outbox write
4. No llama `save()` ni `clearDomainEvents()` — el aggregate no muta
5. Si el vehicle no existe, publica `FleetReleasedEvent` igualmente — la compensación debe ser idempotente y nunca fallar por business reasons

**ReleaseFleetReservationCommand**: Lleva `vehicleId` (String) + `reservationId` (String). Sin fechas — no son necesarias para liberar.

**reservationId como UUID raw**: Mismo razonamiento que Customer — el fleet-domain no debe depender de reservation-domain. `java.util.UUID` es suficiente para correlación.

### Decision 8: FleetReleasedEvent como evento propio — no reusar FleetRejectedEvent

**Elegido**: `FleetReleasedEvent` es un record separado con campos (eventId, occurredOn, vehicleId, reservationId). Sin failureMessages — la liberación siempre es exitosa.

**Alternativa rechazada**: Reusar `FleetRejectedEvent` para la compensación. Semánticamente distinto: rejected = "no pude confirmar la disponibilidad", released = "liberé lo que tenía reservado". Mezclarlos dificulta el routing y la lógica del orquestador.

**Routing key**: `fleet.released`. No tiene queue bindada por ahora — cuando se implemente el orquestador, se añadirá.

### Decision 9: Simplificación de disponibilidad — solo existencia + status ACTIVE

**Elegido**: `ConfirmFleetAvailabilityUseCase` solo verifica que el vehículo exista y esté en status ACTIVE. No verifica solapamiento de fechas.

**Alternativa rechazada para este change**: Modelo de `vehicle_reservations` con tabla, JPA entity, repository, y query de solapamiento. Añade complejidad significativa (nueva tabla, nueva migración, nuevo entity, nuevo repository, lógica de rango de fechas) que no es necesaria para el objetivo del change (conectar Fleet al bus).

**Consecuencia**: Las fechas (`pickupDate`, `returnDate`) viajan en el `ConfirmFleetAvailabilityCommand` pero no se usan en la lógica. Cuando se implemente el modelo de disponibilidad por fechas (change post-SAGA), el contrato del mensaje no cambia.

### Decision 10: ITs existentes necesitan RabbitMQ container — lección del ciclo #12

Idéntico a Customer y Payment: una vez que `common-messaging` está en el classpath, `OutboxPublisher` requiere `RabbitTemplate`. Los 3 ITs existentes (`FleetServiceApplicationIT`, `VehicleControllerIT`, `VehicleRepositoryAdapterIT`) necesitan `@Container @ServiceConnection RabbitMQContainer`.

### Decision 11: definitions.json — añadir fleet.release.command.queue

La queue `fleet.confirm.command.queue` ya existe desde `pre-saga-alignment`. La queue `fleet.release.command.queue` no — debe añadirse con el mismo patrón:

```json
{
  "name": "fleet.release.command.queue",
  "durable": true,
  "auto_delete": false,
  "arguments": {
    "x-dead-letter-exchange": "dlx.exchange",
    "x-dead-letter-routing-key": "fleet.release.command.dlq"
  }
}
```

Bindings:
- `fleet.exchange` → `fleet.release.command.queue` con routing key `fleet.release.command`
- `dlx.exchange` → `fleet.dlq` con routing key `fleet.release.command.dlq`

### Decision 12: Flyway V2 — misma tabla outbox_events

La migración `V2__create_outbox_events_table.sql` es textualmente idéntica a la de Customer, Payment y Reservation. Cada servicio tiene su propia copia en su propia base de datos.

## Risks / Trade-offs

- **[Riesgo] Todos los ITs existentes se rompen sin RabbitMQ container** → Mitigación: añadir `@Container @ServiceConnection RabbitMQContainer` a cada IT como primera tarea de la fase de tests.
- **[Riesgo] Contrato del mensaje de comando no está tipado** → Los listeners parsean JSON esperando campos específicos. Si el orquestador SAGA envía un formato diferente, falla en runtime. Mitigación: documentar contratos en el design (arriba) y verificar cuando se implemente el orquestador.
- **[Trade-off] Routing keys explícitas con Map vs auto-derivación** → El Map es más verboso que el patrón de Customer/Payment, pero es necesario por el naming mixto (Vehicle* + Fleet*). Falla ruidosamente si un evento nuevo no se registra.
- **[Trade-off] 4 eventos lifecycle sin consumidores van al bus** → Overhead negligible (outbox rows limpiadas a los 7 días, mensajes descartados por RabbitMQ). Futuro-compatible.
- **[Trade-off] FleetReleasedEvent sin queue bindada** → El evento se publica al exchange y RabbitMQ lo descarta. Cuando se implemente el orquestador, se añade la queue. No hay pérdida de datos porque el orquestador no existe aún.
- **[Trade-off] ReleaseFleetReservationUseCase es no-op sobre el aggregate** → Sin modelo de vehicle_reservations, no hay nada que "liberar" en el aggregate. El use case existe para: (a) emitir el evento de respuesta que el orquestador necesita, (b) tener la estructura lista para cuando se implemente la lógica real.
- **[Trade-off] Compensación siempre exitosa** → `ReleaseFleetReservationUseCase` publica `FleetReleasedEvent` incluso si el vehicle no existe. En SAGA, las compensaciones deben ser idempotentes y nunca fallar por business reasons — si el recurso no existe, no hay nada que compensar.

## Open Questions

Ninguna — el patrón de outbox está consolidado desde los ciclos #12, #16 y #18. Las decisiones específicas de Fleet (routing keys, 2 listeners, compensación) se resolvieron durante el análisis de la proposal.
