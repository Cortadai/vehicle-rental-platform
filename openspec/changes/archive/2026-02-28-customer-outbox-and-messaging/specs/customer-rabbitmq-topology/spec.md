## ADDED Requirements

### Requirement: Customer exchange declaration

RabbitMQConfig SHALL declare a durable `TopicExchange` named `customer.exchange`.

#### Scenario: Exchange bean exists

- **WHEN** the Spring application context is loaded
- **THEN** a `TopicExchange` bean with name `customer.exchange` SHALL be available
- **AND** it SHALL be durable

### Requirement: Event queues for SAGA responses with per-queue DLQ routing

RabbitMQConfig SHALL declare two durable queues for SAGA response events: `customer.validated.queue` and `customer.rejected.queue`. Each SHALL have its own DLQ routing key for traceability, following the same pattern as Payment Service.

#### Scenario: Validated event queue exists with DLX

- **WHEN** the Spring application context is loaded
- **THEN** a durable queue named `customer.validated.queue` SHALL be available
- **AND** it SHALL have argument `x-dead-letter-exchange` set to `dlx.exchange`
- **AND** it SHALL have argument `x-dead-letter-routing-key` set to `customer.validated.dlq`

#### Scenario: Rejected event queue exists with DLX

- **WHEN** the Spring application context is loaded
- **THEN** a durable queue named `customer.rejected.queue` SHALL be available
- **AND** it SHALL have argument `x-dead-letter-exchange` set to `dlx.exchange`
- **AND** it SHALL have argument `x-dead-letter-routing-key` set to `customer.rejected.dlq`

### Requirement: Command queue for SAGA validation requests with per-queue DLQ routing

RabbitMQConfig SHALL declare a durable queue named `customer.validate.command.queue` with its own DLQ routing key. This is the first command queue declared as a Spring bean in the platform.

#### Scenario: Command queue exists with DLX

- **WHEN** the Spring application context is loaded
- **THEN** a durable queue named `customer.validate.command.queue` SHALL be available
- **AND** it SHALL have argument `x-dead-letter-exchange` set to `dlx.exchange`
- **AND** it SHALL have argument `x-dead-letter-routing-key` set to `customer.validate.command.dlq`

### Requirement: Dead letter queue and DLX exchange

RabbitMQConfig SHALL declare a durable queue named `customer.dlq` and a `DirectExchange` named `dlx.exchange` for dead-lettering failed messages. The single `customer.dlq` queue receives dead letters from all 3 operational queues, but each uses a distinct routing key for traceability.

#### Scenario: DLQ and DLX exchange exist

- **WHEN** the Spring application context is loaded
- **THEN** a durable queue named `customer.dlq` SHALL be available
- **AND** a `DirectExchange` named `dlx.exchange` SHALL be available

### Requirement: Event queue bindings

RabbitMQConfig SHALL bind `customer.validated.queue` to `customer.exchange` with routing key `customer.validated`, and `customer.rejected.queue` to `customer.exchange` with routing key `customer.rejected`.

#### Scenario: Validated queue binding

- **WHEN** a message is published to `customer.exchange` with routing key `customer.validated`
- **THEN** it SHALL be routed to `customer.validated.queue`

#### Scenario: Rejected queue binding

- **WHEN** a message is published to `customer.exchange` with routing key `customer.rejected`
- **THEN** it SHALL be routed to `customer.rejected.queue`

### Requirement: Command queue binding

RabbitMQConfig SHALL bind `customer.validate.command.queue` to `customer.exchange` with routing key `customer.validate.command`.

#### Scenario: Command queue binding

- **WHEN** a message is published to `customer.exchange` with routing key `customer.validate.command`
- **THEN** it SHALL be routed to `customer.validate.command.queue`

### Requirement: Per-queue DLQ bindings to shared DLQ

RabbitMQConfig SHALL bind `customer.dlq` to `dlx.exchange` three times, once per operational queue's DLQ routing key. This preserves traceability of which queue originated each dead-lettered message, consistent with Payment Service's pattern and `definitions.json`.

#### Scenario: DLQ binding for validated queue

- **WHEN** a message in `customer.validated.queue` is dead-lettered
- **THEN** it SHALL be routed to `customer.dlq` via `dlx.exchange` with routing key `customer.validated.dlq`

#### Scenario: DLQ binding for rejected queue

- **WHEN** a message in `customer.rejected.queue` is dead-lettered
- **THEN** it SHALL be routed to `customer.dlq` via `dlx.exchange` with routing key `customer.rejected.dlq`

#### Scenario: DLQ binding for command queue

- **WHEN** a message in `customer.validate.command.queue` is dead-lettered
- **THEN** it SHALL be routed to `customer.dlq` via `dlx.exchange` with routing key `customer.validate.command.dlq`

### Requirement: RabbitMQConfig location and annotation

RabbitMQConfig SHALL be a `@Configuration` class in `com.vehiclerental.customer.infrastructure.config`.

#### Scenario: Configuration class is properly annotated

- **WHEN** RabbitMQConfig is inspected
- **THEN** it SHALL be annotated with `@Configuration`
