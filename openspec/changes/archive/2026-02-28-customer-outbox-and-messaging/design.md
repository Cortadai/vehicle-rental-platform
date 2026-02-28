## Context

Customer Service tiene 4 modulos hexagonales funcionales (domain 58 tests, application 17 tests, infrastructure 0 tests directos, container 11 ITs) con un logger no-op (`CustomerDomainEventPublisherAdapter`) como event publisher. Los 4 eventos de dominio existentes se loguean y se pierden.

La infraestructura de messaging ya existe desde `reservation-outbox-and-messaging` (ciclo #12) y fue replicada en `payment-outbox-and-messaging` (ciclo #16):
- `common-messaging` module con OutboxEvent, OutboxPublisher (polling 500ms), OutboxCleanupScheduler, MessageConverterConfig
- Docker Compose con PostgreSQL + RabbitMQ
- `definitions.json` con topologia pre-declarada incluyendo customer.exchange, customer.validated.queue, customer.rejected.queue, customer.validate.command.queue, customer.dlq (las command queues se añadieron en `pre-saga-alignment`)

Este change es unico porque Customer no es solo emisor de eventos (como Reservation/Payment hasta ahora) — es el primer **participante SAGA**. Ademas de enchufar el outbox, necesita recibir comandos via `@RabbitListener` y responder con eventos SAGA.

### Flujo actual (sin cambios)
```
CustomerApplicationService.execute()              // @Transactional
  → Customer.create(...)                           // aggregate acumula eventos
  → customerRepository.save(customer)              // JPA persist
  → eventPublisher.publish(customer.getDomainEvents())  // ← ESTE adapter cambia
  → customer.clearDomainEvents()
```

### Flujo nuevo: validacion SAGA (novedad)
```
RabbitMQ → CustomerValidationListener.handle()     // @RabbitListener
  → validateCustomerUseCase.execute(command)        // @Transactional
    → customerRepository.findById(customerId)       // lectura
    → evaluar si existe y esta activo
    → eventPublisher.publish(List.of(responseEvent)) // outbox write en misma TX
```

## Goals / Non-Goals

**Goals:**
- Reemplazar el logger no-op por un OutboxCustomerDomainEventPublisher que persiste los 6 eventos (4 lifecycle + 2 SAGA) en la tabla outbox_events
- Declarar la topologia RabbitMQ de Customer como beans Spring — incluyendo la command queue de entrada
- Implementar el primer `@RabbitListener` de la plataforma: recibir comandos de validacion y responder via outbox
- Añadir 2 nuevos domain events SAGA (CustomerValidatedEvent, CustomerRejectedEvent) con reservationId para correlacion
- Establecer el patron de error handling para listeners: business failures → evento de respuesta, infrastructure failures → Spring Retry → DLQ
- Verificar atomicidad y publishing con integration tests

**Non-Goals:**
- Modificar common-messaging (ya es generico y funciona)
- Modificar domain o application layers existentes (los 5 use cases actuales no cambian)
- Crear el orquestador SAGA (eso es reservation-saga-orchestration)
- Crear outbox para Fleet (change separado, misma mecanica)
- Implementar idempotencia de consumidor (no hay consumidores de los eventos de Customer todavia)
- Logging estructurado / MDC (change post-SAGA)

## Decisions

### Decision 1: Reemplazar CustomerDomainEventPublisherAdapter, no mantener ambos

**Elegido**: Eliminar `CustomerDomainEventPublisherAdapter` y crear `OutboxCustomerDomainEventPublisher` en su lugar.

**Alternativa rechazada**: Mantener ambos con `@Profile`. Complejidad innecesaria — el logger no-op no tiene valor una vez que existe el outbox.

**Consecuencia**: El viejo adapter era `@Component`, el nuevo tambien es `@Component` implementando la misma interfaz. Spring auto-detecta el reemplazo. `BeanConfiguration` no necesita cambios para este punto (si necesita cambios por el nuevo use case — ver Decision 6).

### Decision 2: Publicar los 6 eventos — 4 lifecycle + 2 SAGA

El OutboxCustomerDomainEventPublisher maneja 6 tipos de evento:

| Evento | Routing Key | Queue bindada | Consumidor actual |
|--------|-------------|---------------|-------------------|
| CustomerCreatedEvent | customer.created | (ninguna) | Ninguno |
| CustomerSuspendedEvent | customer.suspended | (ninguna) | Ninguno |
| CustomerActivatedEvent | customer.activated | (ninguna) | Ninguno |
| CustomerDeletedEvent | customer.deleted | (ninguna) | Ninguno |
| CustomerValidatedEvent | customer.validated | customer.validated.queue | Futuro orquestador SAGA |
| CustomerRejectedEvent | customer.rejected | customer.rejected.queue | Futuro orquestador SAGA |

**Por que publicar los 4 lifecycle sin consumidores**: El publisher implementa `CustomerDomainEventPublisher`, la misma interfaz que usan los 5 use cases existentes. Si el publisher solo manejara 2 eventos, los otros 4 provocarian `IllegalArgumentException` en `extractAggregateId()` en runtime. Los mensajes sin queue bindada se descartan silenciosamente por RabbitMQ. El overhead (unas filas en outbox_events limpiadas por el scheduler) es negligible.

**Alternativa rechazada**: Publicar solo 2 SAGA events y log+ignore los 4 lifecycle. Añade logica condicional que oscurece bugs reales (un evento nuevo que se olvida manejar seria silenciosamente ignorado en vez de fallar ruidosamente).

### Decision 3: Derivacion de routing key por convencion — auto-derivacion sin special cases

```java
String eventName = simpleName.replace("Customer", "").replace("Event", "").toLowerCase();
return "customer." + eventName;
```

Mapping para los 6 eventos:
- CustomerCreatedEvent → `customer.created`
- CustomerSuspendedEvent → `customer.suspended`
- CustomerActivatedEvent → `customer.activated`
- CustomerDeletedEvent → `customer.deleted`
- CustomerValidatedEvent → `customer.validated`
- CustomerRejectedEvent → `customer.rejected`

Todos auto-derivan correctamente. El naming `CustomerRejectedEvent` (en vez de `CustomerValidationFailedEvent`) fue elegido especificamente para que la auto-derivacion produzca `customer.rejected`, que coincide con la queue `customer.rejected.queue` y el routing key `customer.rejected` en `definitions.json`. Es consistente con el par `FleetConfirmedEvent`/`FleetRejectedEvent` que Fleet usara.

### Decision 4: Extraccion de aggregateId — 6 instanceof checks, todos con customerId()

```java
if (event instanceof CustomerCreatedEvent e) return e.customerId().value().toString();
if (event instanceof CustomerSuspendedEvent e) return e.customerId().value().toString();
// ... 4 mas, mismo patron
throw new IllegalArgumentException("Unknown domain event type");
```

Los 6 eventos tienen `customerId()`. Las 6 ramas son identicas excepto por el tipo. Es el mismo patron que Payment (3 checks) y Reservation (2 checks).

**Alternativa considerada**: Interfaz `CustomerDomainEventMarker` con metodo `customerId()`. Correcta pero requiere modificar las 4 clases de eventos existentes en customer-domain. No vale la pena para un POC — 6 instanceof es tolerable.

### Decision 5: RabbitMQConfig con command queue — diferencia vs Payment/Reservation

Payment y Reservation solo declaran event queues salientes (completed, failed, refunded / created). Customer es el primer participante SAGA y necesita declarar tambien la queue de comando entrante:

```
customer.exchange (TopicExchange, durable)
  ├── customer.validated.queue     → routing key "customer.validated"      (evento saliente)
  ├── customer.rejected.queue      → routing key "customer.rejected"       (evento saliente)
  └── customer.validate.command.queue → routing key "customer.validate.command" (comando entrante)
      └── DLQ → dlx.exchange → customer.dlq (compartida por las 3 queues)
```

**Por que declarar la command queue en RabbitMQConfig**: El `@RabbitListener(queues = "customer.validate.command.queue")` necesita que la queue exista como bean Spring. Sin la declaracion, Spring no puede resolver el nombre de queue a un bean y el listener falla al arrancar. Aunque `definitions.json` pre-crea la queue en RabbitMQ, la declaracion en Java es necesaria para el wiring de Spring.

**Total beans: ~15** (2 exchanges + 4 queues + 3 bindings + 3 DLQ bindings = 12, mas los beans de binding).

### Decision 6: ValidateCustomerForReservationUseCase — read con side-effect via outbox

**Implementacion**: `CustomerApplicationService` añade `ValidateCustomerForReservationUseCase` a su lista de `implements` (pasa de 5 a 6 interfaces). Es consistente con el patron del proyecto — un solo Application Service por bounded context que implementa todos los input ports: Payment implementa 3, Reservation 2, Fleet 5, Customer 6. Para un POC con 6 metodos de orquestacion simples es manejable; si en el futuro el servicio crece, se puede separar en dos Application Services sin romper los ports.

Este use case es arquitecturalmente unico en la plataforma:

1. **Es una lectura con side-effect**: Lee el customer, evalua su estado, emite un evento de respuesta. No modifica el aggregate.
2. **Los eventos NO se registran en el aggregate**: A diferencia de `Customer.create()` que llama `registerDomainEvent()`, este use case crea los eventos directamente y los publica via `eventPublisher.publish(List.of(event))`.
3. **`@Transactional` (no readOnly)**: Aunque la operacion sobre Customer es de lectura, el outbox write necesita una transaccion de escritura.
4. **No llama `customerRepository.save()`**: El aggregate no cambia de estado.
5. **No llama `customer.clearDomainEvents()`**: No hay eventos registrados en el aggregate.

**ValidateCustomerCommand**: Lleva `customerId` (String) + `reservationId` (String). Ambos como primitivos, siguiendo el patron de los commands existentes.

**reservationId como UUID raw (no typed ID)**: El customer-domain no debe depender de reservation-domain. `java.util.UUID` es suficiente para correlacion. Los eventos `CustomerValidatedEvent` y `CustomerRejectedEvent` tambien llevan `reservationId` como `UUID`.

**Logica de validacion**:
- Customer not found → `CustomerRejectedEvent` con failureMessages `["Customer not found: {id}"]`
- Customer exists pero no ACTIVE → `CustomerRejectedEvent` con failureMessages `["Customer is not active, current status: {status}"]`
- Customer exists y ACTIVE → `CustomerValidatedEvent`

**Alternativa rechazada**: Poner la logica de validacion en el listener directamente (sin use case). Viola la arquitectura hexagonal — la logica de negocio debe vivir en la capa de aplicacion, no en un adaptador de infraestructura.

### Decision 7: CustomerValidationListener — primer @RabbitListener, patron de error handling

**Package**: `adapter.input.messaging` (nuevo, paralelo a `adapter.input.rest`). Establece la convencion de paquetes para todos los listeners futuros.

**Tipo de mensaje**: Recibe `Message` (Spring AMQP raw) y parsea JSON con `ObjectMapper`. No usa deserializacion automatica de tipos porque el orquestador SAGA (futuro) enviara comandos como JSON plano sin headers `__TypeId__`.

**Contrato del mensaje entrante**: JSON con campos `customerId` y `reservationId`:
```json
{"customerId": "uuid-string", "reservationId": "uuid-string"}
```

**Error handling — dos categorias**:
1. **Business failures** (customer not found, not active): El use case emite `CustomerRejectedEvent` y retorna normalmente. El listener completa sin excepcion. El mensaje se ackea.
2. **Infrastructure failures** (DB down, JSON parse error, outbox write failure): Excepcion burbujea desde el listener. Spring Retry reintenta (3 attempts, backoff exponencial configurado en application.yml). Si agota retries, el mensaje va a `customer.dlq` via la configuracion DLQ de la queue.

**Configuracion Spring Retry** (en application.yml):
```yaml
spring.rabbitmq.listener.simple.retry:
  enabled: true
  initial-interval: 1000
  max-attempts: 3
  multiplier: 2.0
```

**Alternativa rechazada**: `@Retryable` a nivel de metodo. Spring AMQP tiene su propio mecanismo de retry integrado que es mas idiomatico y se configura declarativamente.

### Decision 8: ITs existentes necesitan RabbitMQ container — leccion del ciclo 12

Identico a Payment: una vez que `common-messaging` esta en el classpath, `OutboxPublisher` requiere `RabbitTemplate`. Los 3 ITs existentes (`CustomerServiceApplicationIT`, `CustomerControllerIT`, `CustomerRepositoryAdapterIT`) necesitan `@Container @ServiceConnection RabbitMQContainer`.

**Alternativa diferida**: Extraer un `BaseIT` con containers compartidos. Con 5 ITs (3 existentes + 2 nuevos) la repeticion es tolerable. Se puede hacer en un change de mejoras post-SAGA.

### Decision 9: Flyway V2 — misma tabla outbox_events que Reservation y Payment

La migracion `V2__create_outbox_events_table.sql` es textualmente identica a la de Reservation y Payment. Cada servicio tiene su propia copia en su propio database.

## Risks / Trade-offs

- **[Riesgo] Todos los ITs existentes se rompen sin RabbitMQ container** → Mitigacion: añadir `@Container @ServiceConnection RabbitMQContainer` a cada IT como primera tarea de la fase de tests.
- **[Riesgo] Contrato del mensaje de comando no esta tipado** → El listener parsea JSON esperando `customerId` y `reservationId`. Si el orquestador SAGA envia un formato diferente, falla en runtime. Mitigacion: documentar el contrato aqui y verificar cuando se implemente el orquestador.
- **[Trade-off] 6 instanceof checks en extractAggregateId** → Verboso pero seguro. Un evento nuevo que no se maneje falla ruidosamente con IllegalArgumentException en vez de ser silenciosamente ignorado.
- **[Trade-off] 4 eventos lifecycle sin consumidores van al bus** → Overhead negligible (outbox rows limpiadas a los 7 dias, mensajes descartados por RabbitMQ). Futuro-compatible si se añaden queues para esos eventos.
- **[Trade-off] Spring Retry en vez de retry manual** → Menos control granular pero mas idiomatico. Suficiente para un POC. Si se necesita retry selectivo por tipo de excepcion, se puede añadir un `RetryTemplate` custom.

## Open Questions

Ninguna — el patron de outbox esta consolidado desde los ciclos 12 y 16, y las decisiones de SAGA participation se resolvieron durante la revision de la proposal (naming Validated/Rejected, reservationId en command/eventos, error handling).
