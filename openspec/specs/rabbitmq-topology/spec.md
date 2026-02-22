# rabbitmq-topology Specification

## Purpose

RabbitMQ exchange, queue, binding, and dead-letter queue topology for Reservation Service. Pre-loaded via `definitions.json` for the full platform and declared as Spring beans for the Reservation Service scope.

## ADDED Requirements

### Requirement: Reservation exchange declared as TopicExchange

The Reservation Service SHALL declare a durable `TopicExchange` named `reservation.exchange`.

#### Scenario: Exchange bean exists

- **WHEN** the Spring application context loads `RabbitMQConfig`
- **THEN** a `TopicExchange` bean SHALL be registered with name `reservation.exchange` and `durable = true`

### Requirement: Reservation created queue with DLQ routing

A durable queue `reservation.created.queue` SHALL be declared with dead-letter routing to the DLX exchange.

#### Scenario: Queue with DLQ arguments

- **WHEN** the Spring application context loads `RabbitMQConfig`
- **THEN** a `Queue` bean `reservation.created.queue` SHALL be registered with `durable = true`, `x-dead-letter-exchange` set to `dlx.exchange`, and `x-dead-letter-routing-key` set to `reservation.created.dlq`

### Requirement: DLX exchange and reservation DLQ

A durable `DirectExchange` named `dlx.exchange` SHALL be declared, along with a durable `reservation.dlq` queue.

#### Scenario: DLX exchange bean exists

- **WHEN** the Spring application context loads `RabbitMQConfig`
- **THEN** a `DirectExchange` bean SHALL be registered with name `dlx.exchange` and `durable = true`

#### Scenario: Reservation DLQ exists

- **WHEN** the Spring application context loads `RabbitMQConfig`
- **THEN** a `Queue` bean `reservation.dlq` SHALL be registered with `durable = true`

### Requirement: Bindings connect queues to exchanges

Bindings SHALL route messages from exchanges to their respective queues using routing keys.

#### Scenario: Reservation created binding

- **WHEN** the Spring application context loads `RabbitMQConfig`
- **THEN** a `Binding` SHALL exist routing `reservation.created.queue` to `reservation.exchange` with routing key `reservation.created`

#### Scenario: DLQ binding

- **WHEN** the Spring application context loads `RabbitMQConfig`
- **THEN** a `Binding` SHALL exist routing `reservation.dlq` to `dlx.exchange` with routing key `reservation.created.dlq`

### Requirement: RabbitMQ definitions.json pre-loads full platform topology

The `docker/rabbitmq/definitions.json` file SHALL define the complete RabbitMQ topology for all four services, loaded at container startup.

#### Scenario: All service exchanges declared

- **WHEN** RabbitMQ starts with `definitions.json` loaded
- **THEN** the following durable topic exchanges SHALL exist: `reservation.exchange`, `customer.exchange`, `payment.exchange`, `fleet.exchange`

#### Scenario: DLX exchange declared

- **WHEN** RabbitMQ starts with `definitions.json` loaded
- **THEN** a durable direct exchange `dlx.exchange` SHALL exist

#### Scenario: All service queues declared

- **WHEN** RabbitMQ starts with `definitions.json` loaded
- **THEN** the following durable queues SHALL exist with DLQ routing to `dlx.exchange`: `reservation.created.queue`, `customer.validated.queue`, `customer.rejected.queue`, `payment.completed.queue`, `payment.failed.queue`, `fleet.confirmed.queue`, `fleet.rejected.queue`

#### Scenario: All DLQ queues declared

- **WHEN** RabbitMQ starts with `definitions.json` loaded
- **THEN** the following durable DLQ queues SHALL exist: `reservation.dlq`, `customer.dlq`, `payment.dlq`, `fleet.dlq`

#### Scenario: Bindings route by event type

- **WHEN** RabbitMQ starts with `definitions.json` loaded
- **THEN** each queue SHALL be bound to its service exchange with routing key `{service}.{event-type}` (e.g., `reservation.created.queue` bound to `reservation.exchange` with key `reservation.created`)

### Requirement: RabbitMQ connection configuration

The Reservation Service SHALL configure RabbitMQ connection properties in `application.yml`.

#### Scenario: Connection properties with defaults

- **WHEN** `application.yml` is loaded
- **THEN** it SHALL configure `spring.rabbitmq.host` (default `localhost`), `spring.rabbitmq.port` (default `5672`), `spring.rabbitmq.username` (default `guest`), `spring.rabbitmq.password` (default `guest`), all overridable via environment variables

### Requirement: RabbitMQ configuration loaded from file

The RabbitMQ container SHALL load its topology from a definitions file at startup.

#### Scenario: rabbitmq.conf references definitions.json

- **WHEN** `docker/rabbitmq/rabbitmq.conf` is inspected
- **THEN** it SHALL contain `load_definitions = /etc/rabbitmq/definitions.json`

### Requirement: Command queues for SAGA orchestration

The `docker/rabbitmq/definitions.json` SHALL declare 4 command queues for SAGA orchestration. Each command queue is bound to the receiver service's exchange (not the orchestrator's exchange), following the convention that each service owns its exchange.

#### Scenario: customer.validate.command.queue exists in definitions.json

- **WHEN** `definitions.json` is inspected
- **THEN** it SHALL declare a durable queue `"customer.validate.command.queue"`
- **AND** it SHALL have `x-dead-letter-exchange` set to `"dlx.exchange"`
- **AND** it SHALL have `x-dead-letter-routing-key` set to `"customer.validate.command.dlq"`

#### Scenario: payment.process.command.queue exists in definitions.json

- **WHEN** `definitions.json` is inspected
- **THEN** it SHALL declare a durable queue `"payment.process.command.queue"`
- **AND** it SHALL have `x-dead-letter-exchange` set to `"dlx.exchange"`
- **AND** it SHALL have `x-dead-letter-routing-key` set to `"payment.process.command.dlq"`

#### Scenario: payment.refund.command.queue exists in definitions.json

- **WHEN** `definitions.json` is inspected
- **THEN** it SHALL declare a durable queue `"payment.refund.command.queue"`
- **AND** it SHALL have `x-dead-letter-exchange` set to `"dlx.exchange"`
- **AND** it SHALL have `x-dead-letter-routing-key` set to `"payment.refund.command.dlq"`

#### Scenario: fleet.confirm.command.queue exists in definitions.json

- **WHEN** `definitions.json` is inspected
- **THEN** it SHALL declare a durable queue `"fleet.confirm.command.queue"`
- **AND** it SHALL have `x-dead-letter-exchange` set to `"dlx.exchange"`
- **AND** it SHALL have `x-dead-letter-routing-key` set to `"fleet.confirm.command.dlq"`

### Requirement: Command queue bindings use receiver's exchange

Each command queue SHALL be bound to the exchange of the service that will consume the command, with routing key `{service}.{action}.command`.

#### Scenario: customer.validate.command.queue binding

- **WHEN** `definitions.json` is inspected
- **THEN** it SHALL declare a binding from `"customer.exchange"` to `"customer.validate.command.queue"` with routing key `"customer.validate.command"`

#### Scenario: payment.process.command.queue binding

- **WHEN** `definitions.json` is inspected
- **THEN** it SHALL declare a binding from `"payment.exchange"` to `"payment.process.command.queue"` with routing key `"payment.process.command"`

#### Scenario: payment.refund.command.queue binding

- **WHEN** `definitions.json` is inspected
- **THEN** it SHALL declare a binding from `"payment.exchange"` to `"payment.refund.command.queue"` with routing key `"payment.refund.command"`

#### Scenario: fleet.confirm.command.queue binding

- **WHEN** `definitions.json` is inspected
- **THEN** it SHALL declare a binding from `"fleet.exchange"` to `"fleet.confirm.command.queue"` with routing key `"fleet.confirm.command"`

### Requirement: Command queue DLQ bindings route to receiver's DLQ

Each command queue's dead letters SHALL be routed to the DLQ of the receiver service via `dlx.exchange`.

#### Scenario: customer.validate.command DLQ binding

- **WHEN** `definitions.json` is inspected
- **THEN** it SHALL declare a binding from `"dlx.exchange"` to `"customer.dlq"` with routing key `"customer.validate.command.dlq"`

#### Scenario: payment.process.command DLQ binding

- **WHEN** `definitions.json` is inspected
- **THEN** it SHALL declare a binding from `"dlx.exchange"` to `"payment.dlq"` with routing key `"payment.process.command.dlq"`

#### Scenario: payment.refund.command DLQ binding

- **WHEN** `definitions.json` is inspected
- **THEN** it SHALL declare a binding from `"dlx.exchange"` to `"payment.dlq"` with routing key `"payment.refund.command.dlq"`

#### Scenario: fleet.confirm.command DLQ binding

- **WHEN** `definitions.json` is inspected
- **THEN** it SHALL declare a binding from `"dlx.exchange"` to `"fleet.dlq"` with routing key `"fleet.confirm.command.dlq"`

## Constraint: RabbitMQConfig location

`RabbitMQConfig` SHALL live in `reservation-infrastructure` at `com.vehiclerental.reservation.infrastructure.config`. It is infrastructure configuration, not an output adapter — the package reflects this distinction. Only Reservation Service beans are declared in this change — other services will declare their own beans when implemented.
