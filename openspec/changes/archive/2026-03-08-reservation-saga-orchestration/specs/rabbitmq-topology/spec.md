## ADDED Requirements

### Requirement: Response queue declarations in Reservation RabbitMQConfig

Reservation Service's RabbitMQConfig SHALL declare 7 response queues as Spring beans. Each queue SHALL be durable with dead-letter routing to `dlx.exchange`. These queues already exist in `definitions.json` — the Spring bean declarations allow `@RabbitListener` to reference them.

#### Scenario: customer.validated.queue declared

- **WHEN** the Spring application context loads RabbitMQConfig
- **THEN** a `Queue` bean `customer.validated.queue` SHALL be registered with `durable = true`, `x-dead-letter-exchange` set to `dlx.exchange`, and `x-dead-letter-routing-key` set to `customer.validated.dlq`

#### Scenario: customer.rejected.queue declared

- **WHEN** the Spring application context loads RabbitMQConfig
- **THEN** a `Queue` bean `customer.rejected.queue` SHALL be registered with `durable = true`, `x-dead-letter-exchange` set to `dlx.exchange`, and `x-dead-letter-routing-key` set to `customer.rejected.dlq`

#### Scenario: payment.completed.queue declared

- **WHEN** the Spring application context loads RabbitMQConfig
- **THEN** a `Queue` bean `payment.completed.queue` SHALL be registered with `durable = true`, `x-dead-letter-exchange` set to `dlx.exchange`, and `x-dead-letter-routing-key` set to `payment.completed.dlq`

#### Scenario: payment.failed.queue declared

- **WHEN** the Spring application context loads RabbitMQConfig
- **THEN** a `Queue` bean `payment.failed.queue` SHALL be registered with `durable = true`, `x-dead-letter-exchange` set to `dlx.exchange`, and `x-dead-letter-routing-key` set to `payment.failed.dlq`

#### Scenario: payment.refunded.queue declared

- **WHEN** the Spring application context loads RabbitMQConfig
- **THEN** a `Queue` bean `payment.refunded.queue` SHALL be registered with `durable = true`, `x-dead-letter-exchange` set to `dlx.exchange`, and `x-dead-letter-routing-key` set to `payment.refunded.dlq`

#### Scenario: fleet.confirmed.queue declared

- **WHEN** the Spring application context loads RabbitMQConfig
- **THEN** a `Queue` bean `fleet.confirmed.queue` SHALL be registered with `durable = true`, `x-dead-letter-exchange` set to `dlx.exchange`, and `x-dead-letter-routing-key` set to `fleet.confirmed.dlq`

#### Scenario: fleet.rejected.queue declared

- **WHEN** the Spring application context loads RabbitMQConfig
- **THEN** a `Queue` bean `fleet.rejected.queue` SHALL be registered with `durable = true`, `x-dead-letter-exchange` set to `dlx.exchange`, and `x-dead-letter-routing-key` set to `fleet.rejected.dlq`

### Requirement: Response queue bindings to participant exchanges

Each response queue SHALL be bound to the participant service's exchange (not reservation.exchange). The orchestrator consumes from queues that are fed by participant exchanges.

#### Scenario: customer.validated.queue binding

- **WHEN** RabbitMQConfig is loaded
- **THEN** a `Binding` SHALL exist routing `customer.validated.queue` to `customer.exchange` with routing key `customer.validated`

#### Scenario: customer.rejected.queue binding

- **WHEN** RabbitMQConfig is loaded
- **THEN** a `Binding` SHALL exist routing `customer.rejected.queue` to `customer.exchange` with routing key `customer.rejected`

#### Scenario: payment.completed.queue binding

- **WHEN** RabbitMQConfig is loaded
- **THEN** a `Binding` SHALL exist routing `payment.completed.queue` to `payment.exchange` with routing key `payment.completed`

#### Scenario: payment.failed.queue binding

- **WHEN** RabbitMQConfig is loaded
- **THEN** a `Binding` SHALL exist routing `payment.failed.queue` to `payment.exchange` with routing key `payment.failed`

#### Scenario: payment.refunded.queue binding

- **WHEN** RabbitMQConfig is loaded
- **THEN** a `Binding` SHALL exist routing `payment.refunded.queue` to `payment.exchange` with routing key `payment.refunded`

#### Scenario: fleet.confirmed.queue binding

- **WHEN** RabbitMQConfig is loaded
- **THEN** a `Binding` SHALL exist routing `fleet.confirmed.queue` to `fleet.exchange` with routing key `fleet.confirmed`

#### Scenario: fleet.rejected.queue binding

- **WHEN** RabbitMQConfig is loaded
- **THEN** a `Binding` SHALL exist routing `fleet.rejected.queue` to `fleet.exchange` with routing key `fleet.rejected`

### Requirement: Participant exchange declarations in Reservation RabbitMQConfig

Reservation Service's RabbitMQConfig SHALL declare the 3 participant exchanges as Spring beans so that bindings can reference them. These exchanges already exist in RabbitMQ — the declarations are idempotent.

#### Scenario: customer.exchange declared

- **WHEN** RabbitMQConfig is loaded
- **THEN** a `TopicExchange` bean `customer.exchange` SHALL be registered with `durable = true`

#### Scenario: payment.exchange declared

- **WHEN** RabbitMQConfig is loaded
- **THEN** a `TopicExchange` bean `payment.exchange` SHALL be registered with `durable = true`

#### Scenario: fleet.exchange declared

- **WHEN** RabbitMQConfig is loaded
- **THEN** a `TopicExchange` bean `fleet.exchange` SHALL be registered with `durable = true`
