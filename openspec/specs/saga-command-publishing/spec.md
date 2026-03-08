## ADDED Requirements

### Requirement: SagaCommandPublisher output port

SagaCommandPublisher SHALL be an interface in `reservation-application/port/output/` with a single method: `publish(String exchange, String routingKey, String payload)`. It SHALL have no Spring or JPA annotations.

#### Scenario: Port defines publish method

- **WHEN** SagaCommandPublisher interface is inspected
- **THEN** it SHALL declare `void publish(String exchange, String routingKey, String payload)`
- **AND** it SHALL have no dependencies on Spring or JPA

### Requirement: OutboxSagaCommandPublisher implementation

OutboxSagaCommandPublisher SHALL be a `@Component` in `reservation-infrastructure/adapter/output/messaging/saga/` that implements SagaCommandPublisher. It SHALL create an OutboxEvent with aggregateType "SAGA", persist it via OutboxEventRepository, and let the existing OutboxPublisher scheduler handle delivery to RabbitMQ.

#### Scenario: Command published via Outbox

- **WHEN** `publish("customer.exchange", "customer.validate.command", jsonPayload)` is called
- **THEN** an OutboxEvent SHALL be saved with aggregateType "SAGA", exchange "customer.exchange", routingKey "customer.validate.command", payload equal to jsonPayload, and status PENDING

#### Scenario: Aggregate ID derived from payload

- **WHEN** a command is published with payload containing a reservationId
- **THEN** the OutboxEvent aggregateId SHALL be extracted from the payload's reservationId field

#### Scenario: Event type reflects command

- **WHEN** a command is published with routing key "payment.process.command"
- **THEN** the OutboxEvent eventType SHALL be "SAGA_COMMAND" (generic, since the routing key already identifies the specific command)

### Requirement: Commands delivered by existing OutboxPublisher

OutboxSagaCommandPublisher SHALL NOT publish directly to RabbitMQ. The existing OutboxPublisher scheduler (polling every 500ms in common-messaging) SHALL pick up the SAGA command OutboxEvents and deliver them, exactly as it does for domain events.

#### Scenario: No direct RabbitMQ interaction

- **WHEN** OutboxSagaCommandPublisher is inspected
- **THEN** it SHALL NOT inject or use RabbitTemplate
- **AND** it SHALL only depend on OutboxEventRepository

### Requirement: Steps use SagaCommandPublisher for all commands

All SagaStep implementations SHALL use SagaCommandPublisher to send commands. They SHALL serialize their command payload to JSON using ObjectMapper, then call `sagaCommandPublisher.publish(exchange, routingKey, jsonPayload)`.

#### Scenario: Steps do not access OutboxEventRepository

- **WHEN** CustomerValidationStep, PaymentStep, or FleetConfirmationStep is inspected
- **THEN** none SHALL depend on OutboxEventRepository or any JPA type
- **AND** all SHALL depend only on SagaCommandPublisher and ObjectMapper
