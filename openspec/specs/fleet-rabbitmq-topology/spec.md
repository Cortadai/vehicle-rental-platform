## ADDED Requirements

### Requirement: Fleet exchange declaration

RabbitMQConfig SHALL declare a durable `TopicExchange` named `fleet.exchange`.

#### Scenario: Exchange bean exists

- **WHEN** the Spring application context is loaded
- **THEN** a `TopicExchange` bean with name `fleet.exchange` SHALL be available
- **AND** it SHALL be durable

### Requirement: Event queues for SAGA responses with per-queue DLQ routing

RabbitMQConfig SHALL declare two durable queues for SAGA response events: `fleet.confirmed.queue` and `fleet.rejected.queue`. Each SHALL have its own DLQ routing key for traceability.

#### Scenario: Confirmed event queue exists with DLX

- **WHEN** the Spring application context is loaded
- **THEN** a durable queue named `fleet.confirmed.queue` SHALL be available
- **AND** it SHALL have argument `x-dead-letter-exchange` set to `dlx.exchange`
- **AND** it SHALL have argument `x-dead-letter-routing-key` set to `fleet.confirmed.dlq`

#### Scenario: Rejected event queue exists with DLX

- **WHEN** the Spring application context is loaded
- **THEN** a durable queue named `fleet.rejected.queue` SHALL be available
- **AND** it SHALL have argument `x-dead-letter-exchange` set to `dlx.exchange`
- **AND** it SHALL have argument `x-dead-letter-routing-key` set to `fleet.rejected.dlq`

### Requirement: Command queues for SAGA requests with per-queue DLQ routing

RabbitMQConfig SHALL declare two durable command queues: `fleet.confirm.command.queue` for availability confirmation and `fleet.release.command.queue` for compensation. Each SHALL have its own DLQ routing key.

#### Scenario: Confirm command queue exists with DLX

- **WHEN** the Spring application context is loaded
- **THEN** a durable queue named `fleet.confirm.command.queue` SHALL be available
- **AND** it SHALL have argument `x-dead-letter-exchange` set to `dlx.exchange`
- **AND** it SHALL have argument `x-dead-letter-routing-key` set to `fleet.confirm.command.dlq`

#### Scenario: Release command queue exists with DLX

- **WHEN** the Spring application context is loaded
- **THEN** a durable queue named `fleet.release.command.queue` SHALL be available
- **AND** it SHALL have argument `x-dead-letter-exchange` set to `dlx.exchange`
- **AND** it SHALL have argument `x-dead-letter-routing-key` set to `fleet.release.command.dlq`

### Requirement: Dead letter queue and DLX exchange

RabbitMQConfig SHALL declare a durable queue named `fleet.dlq` and a `DirectExchange` named `dlx.exchange` for dead-lettering failed messages. The single `fleet.dlq` queue receives dead letters from all 5 operational queues, but each uses a distinct routing key for traceability.

#### Scenario: DLQ and DLX exchange exist

- **WHEN** the Spring application context is loaded
- **THEN** a durable queue named `fleet.dlq` SHALL be available
- **AND** a `DirectExchange` named `dlx.exchange` SHALL be available

### Requirement: Event queue bindings

RabbitMQConfig SHALL bind `fleet.confirmed.queue` to `fleet.exchange` with routing key `fleet.confirmed`, and `fleet.rejected.queue` to `fleet.exchange` with routing key `fleet.rejected`.

#### Scenario: Confirmed queue binding

- **WHEN** a message is published to `fleet.exchange` with routing key `fleet.confirmed`
- **THEN** it SHALL be routed to `fleet.confirmed.queue`

#### Scenario: Rejected queue binding

- **WHEN** a message is published to `fleet.exchange` with routing key `fleet.rejected`
- **THEN** it SHALL be routed to `fleet.rejected.queue`

### Requirement: Command queue bindings

RabbitMQConfig SHALL bind `fleet.confirm.command.queue` to `fleet.exchange` with routing key `fleet.confirm.command`, and `fleet.release.command.queue` to `fleet.exchange` with routing key `fleet.release.command`.

#### Scenario: Confirm command queue binding

- **WHEN** a message is published to `fleet.exchange` with routing key `fleet.confirm.command`
- **THEN** it SHALL be routed to `fleet.confirm.command.queue`

#### Scenario: Release command queue binding

- **WHEN** a message is published to `fleet.exchange` with routing key `fleet.release.command`
- **THEN** it SHALL be routed to `fleet.release.command.queue`

### Requirement: Per-queue DLQ bindings to shared DLQ

RabbitMQConfig SHALL bind `fleet.dlq` to `dlx.exchange` five times, once per operational queue's DLQ routing key.

#### Scenario: DLQ binding for confirmed queue

- **WHEN** a message in `fleet.confirmed.queue` is dead-lettered
- **THEN** it SHALL be routed to `fleet.dlq` via `dlx.exchange` with routing key `fleet.confirmed.dlq`

#### Scenario: DLQ binding for rejected queue

- **WHEN** a message in `fleet.rejected.queue` is dead-lettered
- **THEN** it SHALL be routed to `fleet.dlq` via `dlx.exchange` with routing key `fleet.rejected.dlq`

#### Scenario: DLQ binding for confirm command queue

- **WHEN** a message in `fleet.confirm.command.queue` is dead-lettered
- **THEN** it SHALL be routed to `fleet.dlq` via `dlx.exchange` with routing key `fleet.confirm.command.dlq`

#### Scenario: DLQ binding for release command queue

- **WHEN** a message in `fleet.release.command.queue` is dead-lettered
- **THEN** it SHALL be routed to `fleet.dlq` via `dlx.exchange` with routing key `fleet.release.command.dlq`

### Requirement: RabbitMQConfig location and annotation

RabbitMQConfig SHALL be a `@Configuration` class in `com.vehiclerental.fleet.infrastructure.config`.

#### Scenario: Configuration class is properly annotated

- **WHEN** RabbitMQConfig is inspected
- **THEN** it SHALL be annotated with `@Configuration`
