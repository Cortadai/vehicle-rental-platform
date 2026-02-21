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

## Constraint: RabbitMQConfig location

`RabbitMQConfig` SHALL live in `reservation-infrastructure` at `com.vehiclerental.reservation.infrastructure.config`. It is infrastructure configuration, not an output adapter — the package reflects this distinction. Only Reservation Service beans are declared in this change — other services will declare their own beans when implemented.
