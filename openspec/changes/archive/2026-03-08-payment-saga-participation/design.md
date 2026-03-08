## Context

Payment Service ya tiene toda la infraestructura de messaging (change #16): OutboxPaymentDomainEventPublisher, RabbitMQConfig con event queues, scanning annotations, y RabbitMQ en ITs. Tambien tiene los use cases `ProcessPaymentUseCase` y `RefundPaymentUseCase` con comandos que ya incluyen `reservationId` para correlacion SAGA.

Lo unico que falta es la **recepcion de comandos por RabbitMQ**: 2 listeners que escuchen en las command queues (ya definidas en `definitions.json` desde change #17) y deleguen a los use cases existentes. Es el mismo patron que `CustomerValidationListener` (change #18) y `FleetConfirmationListener`/`FleetReleaseListener` (change #19).

## Goals / Non-Goals

**Goals:**
- Payment Service puede recibir comandos de cobro y reembolso via RabbitMQ
- Las command queues estan declaradas como Spring beans en RabbitMQConfig
- Los listeners delegan a los use cases existentes sin modificarlos
- ITs verifican la recepcion y procesamiento de comandos

**Non-Goals:**
- Modificar use cases o comandos existentes (ya tienen los campos necesarios)
- Implementar el orquestador SAGA (change #21)
- Anadir nuevos domain events (los 3 existentes cubren el flujo)
- Crear command queues en definitions.json (ya existen desde change #17)

## Decisions

### Decision 1: Reusar use cases existentes, no crear nuevos

**Elegido**: Los listeners invocan `ProcessPaymentUseCase` y `RefundPaymentUseCase` directamente.

**Rationale**: A diferencia de Customer y Fleet que necesitaron use cases nuevos (ValidateCustomerForReservation, ConfirmFleetAvailability), Payment ya fue disenado con los comandos correctos desde el change #14. `ProcessPaymentCommand(reservationId, customerId, amount, currency)` y `RefundPaymentCommand(reservationId)` ya llevan correlacion SAGA.

**Alternativa descartada**: Crear `ProcessPaymentForReservationUseCase` — seria un wrapper innecesario sobre el mismo comando y la misma logica.

### Decision 2: Listeners parsean raw Message (patron establecido)

**Elegido**: `@RabbitListener` recibe `org.springframework.amqp.core.Message`, parsea JSON con ObjectMapper, construye el command record.

**Rationale**: Es el patron exacto de `CustomerValidationListener`, `FleetConfirmationListener`, y `FleetReleaseListener`. Consistencia > cleverness.

**Alternativa descartada**: Auto-deserializacion con `@Payload` — no la usamos en ningun otro servicio.

### Decision 3: Command queues como Spring beans en RabbitMQConfig

**Elegido**: Declarar `payment.process.command.queue` y `payment.refund.command.queue` como beans `Queue` con DLQ routing, junto con sus bindings a `payment.exchange`.

**Rationale**: Las queues existen en definitions.json (para docker) pero no como Spring beans. Declararlas da consistencia con Customer y Fleet que declaran sus command queues como beans. Ademas, Spring AMQP las crea automaticamente si no existen (util en tests).

## Risks / Trade-offs

**[Risk] ProcessPaymentUseCase usa PaymentGateway (simulado)** → No es un riesgo real. El gateway ya esta mockeado en la implementacion actual. Cuando el listener invoque el use case, el flujo es identico al de REST.

**[Risk] RefundPaymentCommand solo lleva reservationId** → El use case busca el Payment por reservationId internamente. Esto funciona porque hay un unique constraint en reservationId. Sin riesgo.

**[Tradeoff] No hay retry especifico en listeners** → Usamos el retry global de Spring AMQP configurado en application.yml (3 attempts, exponential backoff). Si falla 3 veces, va al DLQ. Suficiente para este stage.
