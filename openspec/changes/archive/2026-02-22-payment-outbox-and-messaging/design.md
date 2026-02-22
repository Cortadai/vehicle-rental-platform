## Context

Payment Service tiene 4 módulos funcionales (domain, application, infrastructure, container) con 13 integration tests pasando. Los eventos de dominio (`PaymentCompletedEvent`, `PaymentFailedEvent`, `PaymentRefundedEvent`) se "publican" via un logger no-op (`PaymentDomainEventPublisherAdapter`) — no llegan a RabbitMQ.

La infraestructura de messaging ya existe desde `reservation-outbox-and-messaging` (ciclo #12):
- `common-messaging` module con OutboxEvent, OutboxPublisher (polling 500ms), OutboxCleanupScheduler, MessageConverterConfig
- Docker Compose con PostgreSQL + RabbitMQ
- `definitions.json` con topología pre-declarada (payment.exchange, payment.completed.queue, payment.failed.queue, payment.dlq)
- `init-schemas.sql` con schema `payment` + `payment_user`

Este change solo conecta Payment al bus existente. No hay código nuevo en common-messaging, Docker Compose, ni root POM.

## Goals / Non-Goals

**Goals:**
- Reemplazar el logger no-op por un OutboxPaymentDomainEventPublisher que persiste eventos en la tabla outbox_events dentro de la misma transacción que el aggregate
- Declarar la topología RabbitMQ de Payment como beans Spring (validación idempotente de lo pre-declarado en definitions.json)
- Añadir la queue faltante `payment.refunded.queue` a definitions.json
- Verificar atomicidad (payment + outbox en misma TX) y publishing (scheduler → RabbitMQ) con integration tests

**Non-Goals:**
- Modificar common-messaging (ya es genérico y funciona)
- Crear consumidores de mensajes (eso es responsabilidad de la SAGA)
- Modificar domain o application layers (los output ports ya existen)
- Añadir Docker Compose changes (ya tiene RabbitMQ)
- Crear outbox para Customer o Fleet (changes separados, misma mecánica)

## Decisions

### Decision 1: Reemplazar PaymentDomainEventPublisherAdapter, no mantener ambos

**Elegido**: Eliminar `PaymentDomainEventPublisherAdapter` y crear `OutboxPaymentDomainEventPublisher` en su lugar.

**Alternativa rechazada**: Mantener ambos con un `@Profile`. Añade complejidad innecesaria — el logger no-op no tiene valor una vez que existe el outbox. En tests el outbox funciona igual (Testcontainers PostgreSQL + RabbitMQ).

**Consecuencia**: BeanConfiguration no necesita cambios. El viejo adapter era `@Component`, el nuevo también es `@Component` implementando la misma interfaz `PaymentDomainEventPublisher`. Spring auto-detecta el reemplazo.

### Decision 2: 3 routing keys para 3 eventos de dominio

Payment tiene 3 eventos de dominio:
- `PaymentCompletedEvent` → routing key `payment.completed`
- `PaymentFailedEvent` → routing key `payment.failed`
- `PaymentRefundedEvent` → routing key `payment.refunded`

Reservation tiene 2 (created, cancelled). Payment tiene 1 más porque el flujo de pagos tiene 3 estados terminales (completed, failed, refunded) que son relevantes para la SAGA.

**Consecuencia en RabbitMQConfig**: 3 queues principales (completed, failed, refunded) + 1 DLQ compartida. Reservation tiene 1 queue + 1 DLQ.

**Consecuencia en definitions.json**: Falta `payment.refunded.queue` y su binding + DLQ binding. Hay que añadirlos.

### Decision 3: Derivación de routing key por convención — mismo patrón que Reservation

```java
// PaymentCompletedEvent → payment.completed
String eventName = simpleName.replace("Payment", "").replace("Event", "").toLowerCase();
return "payment." + eventName;
```

Mismo patrón que Reservation (`ReservationCreatedEvent` → `reservation.created`). La convención `{service}.{eventType}` se mantiene uniforme.

### Decision 4: Extracción de aggregateId — pattern matching sobre 3 tipos

Payment tiene 3 tipos de evento, todos con `paymentId()`. El `extractAggregateId` usa pattern matching:
```java
if (event instanceof PaymentCompletedEvent e) return e.paymentId().value().toString();
if (event instanceof PaymentFailedEvent e) return e.paymentId().value().toString();
if (event instanceof PaymentRefundedEvent e) return e.paymentId().value().toString();
throw new IllegalArgumentException("Unknown domain event type: " + event.getClass().getSimpleName());
```

**Alternativa considerada**: Interfaz `PaymentDomainEvent` con método `paymentId()`. Correcta pero requiere modificar el domain module, que no es goal de este change. Los 3 eventos ya están definidos como records independientes.

### Decision 5: ITs existentes necesitan RabbitMQ container — lección del ciclo 12

Una vez que `common-messaging` está en el classpath, `OutboxPublisher` necesita `RabbitTemplate`, que necesita conexión RabbitMQ. **Todos** los `@SpringBootTest` ITs deben declarar un `RabbitMQContainer`.

Los 3 ITs existentes (`PaymentRepositoryAdapterIT`, `PaymentControllerIT`, `PaymentServiceApplicationIT`) necesitan añadir:
```java
@Container
@ServiceConnection
static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.13-management-alpine");
```

**Alternativa**: Extraer un `BaseIT` con los containers. No lo hacemos ahora para mantener el scope mínimo — 5 ITs con la misma declaración es tolerable.

### Decision 6: Flyway V2 migration — misma tabla outbox_events que Reservation

La tabla `outbox_events` es idéntica en todos los servicios — es genérica por diseño (aggregate_type, aggregate_id, payload como TEXT). Cada servicio tiene su propia copia de la tabla en su propio schema/database.

La migración `V2__create_outbox_events_table.sql` es textualmente idéntica a la de Reservation.

### Decision 7: RabbitMQConfig con DLQ compartida

Igual que Reservation: una sola DLQ (`payment.dlq`) recibe mensajes muertos de las 3 queues. Las 3 queues principales configuran `x-dead-letter-exchange: dlx.exchange` y `x-dead-letter-routing-key: payment.{eventType}.dlq`.

El `dlx.exchange` ya está declarado globalmente (shared entre todos los servicios). Cada servicio re-declara el bean como idempotente — RabbitMQ no duplica exchanges.

## Risks / Trade-offs

- **[Riesgo] Todos los ITs existentes se rompen sin RabbitMQ container** → Mitigación: añadir `@Container @ServiceConnection RabbitMQContainer` a cada IT antes de cualquier otra modificación.
- **[Trade-off] 3 queues vs 1 queue con filtrado** → 3 queues es más explícito y permite consumers independientes por tipo de evento. La SAGA necesitará distinguir completed vs failed para decidir si compensar.
- **[Riesgo] definitions.json no tiene payment.refunded.queue** → Mitigación: añadirlo en este change. Sin él, el RabbitMQConfig declarará la queue pero no habrá binding pre-existente en Docker.

## Open Questions

Ninguna — el patrón está consolidado desde Reservation y las decisiones son mecánicas.
