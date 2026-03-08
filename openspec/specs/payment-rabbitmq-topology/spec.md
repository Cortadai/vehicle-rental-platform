payment-rabbitmq-topology
=========================

Purpose
-------

RabbitMQ topology declaration for Payment Service. Declares the payment exchange, 3 event queues (completed, failed, refunded), 2 command queues (process, refund), a shared DLQ, and all bindings as Spring beans. Also updates `definitions.json` with the missing `payment.refunded.queue` topology.

## ADDED Requirements

### Requirement: RabbitMQConfig declares payment exchange

RabbitMQConfig SHALL be a `@Configuration` class that declares the Payment Service RabbitMQ topology as Spring beans.

#### Scenario: Payment exchange is a durable TopicExchange

- **WHEN** the application context is loaded
- **THEN** a `TopicExchange` bean named `"payment.exchange"` SHALL be available
- **AND** it SHALL be durable and not auto-delete

#### Scenario: DLX exchange is a durable DirectExchange

- **WHEN** the application context is loaded
- **THEN** a `DirectExchange` bean named `"dlx.exchange"` SHALL be available
- **AND** it SHALL be durable and not auto-delete

### Requirement: RabbitMQConfig declares 3 event queues with DLQ routing

RabbitMQConfig SHALL declare one queue per payment event type, each with dead-letter routing to the DLX exchange.

#### Scenario: payment.completed.queue is durable with DLQ

- **WHEN** the application context is loaded
- **THEN** a `Queue` bean named `"payment.completed.queue"` SHALL be available
- **AND** it SHALL be durable
- **AND** it SHALL have `x-dead-letter-exchange` set to `"dlx.exchange"`
- **AND** it SHALL have `x-dead-letter-routing-key` set to `"payment.completed.dlq"`

#### Scenario: payment.failed.queue is durable with DLQ

- **WHEN** the application context is loaded
- **THEN** a `Queue` bean named `"payment.failed.queue"` SHALL be available
- **AND** it SHALL be durable
- **AND** it SHALL have `x-dead-letter-exchange` set to `"dlx.exchange"`
- **AND** it SHALL have `x-dead-letter-routing-key` set to `"payment.failed.dlq"`

#### Scenario: payment.refunded.queue is durable with DLQ

- **WHEN** the application context is loaded
- **THEN** a `Queue` bean named `"payment.refunded.queue"` SHALL be available
- **AND** it SHALL be durable
- **AND** it SHALL have `x-dead-letter-exchange` set to `"dlx.exchange"`
- **AND** it SHALL have `x-dead-letter-routing-key` set to `"payment.refunded.dlq"`

#### Scenario: payment.dlq is a durable shared DLQ

- **WHEN** the application context is loaded
- **THEN** a `Queue` bean named `"payment.dlq"` SHALL be available
- **AND** it SHALL be durable

### Requirement: RabbitMQConfig declares 2 command queues with DLQ routing

RabbitMQConfig SHALL declare one queue per SAGA command type, each with dead-letter routing to the DLX exchange, following the same pattern as the existing event queues.

#### Scenario: payment.process.command.queue is durable with DLQ

- **WHEN** the application context is loaded
- **THEN** a `Queue` bean named `"payment.process.command.queue"` SHALL be available
- **AND** it SHALL be durable
- **AND** it SHALL have `x-dead-letter-exchange` set to `"dlx.exchange"`
- **AND** it SHALL have `x-dead-letter-routing-key` set to `"payment.process.command.dlq"`

#### Scenario: payment.refund.command.queue is durable with DLQ

- **WHEN** the application context is loaded
- **THEN** a `Queue` bean named `"payment.refund.command.queue"` SHALL be available
- **AND** it SHALL be durable
- **AND** it SHALL have `x-dead-letter-exchange` set to `"dlx.exchange"`
- **AND** it SHALL have `x-dead-letter-routing-key` set to `"payment.refund.command.dlq"`

### Requirement: RabbitMQConfig declares bindings

RabbitMQConfig SHALL bind each event queue and each command queue to the payment exchange with the appropriate routing key, and bind the DLQ to the DLX exchange. The total binding count is 10 (3 event + 2 command + 3 event DLQ + 2 command DLQ).

#### Scenario: Completed queue binding

- **WHEN** the application context is loaded
- **THEN** `payment.completed.queue` SHALL be bound to `payment.exchange` with routing key `"payment.completed"`

#### Scenario: Failed queue binding

- **WHEN** the application context is loaded
- **THEN** `payment.failed.queue` SHALL be bound to `payment.exchange` with routing key `"payment.failed"`

#### Scenario: Refunded queue binding

- **WHEN** the application context is loaded
- **THEN** `payment.refunded.queue` SHALL be bound to `payment.exchange` with routing key `"payment.refunded"`

#### Scenario: Process command queue binding

- **WHEN** the application context is loaded
- **THEN** `payment.process.command.queue` SHALL be bound to `payment.exchange` with routing key `"payment.process.command"`

#### Scenario: Refund command queue binding

- **WHEN** the application context is loaded
- **THEN** `payment.refund.command.queue` SHALL be bound to `payment.exchange` with routing key `"payment.refund.command"`

#### Scenario: DLQ bindings for completed

- **WHEN** the application context is loaded
- **THEN** `payment.dlq` SHALL be bound to `dlx.exchange` with routing key `"payment.completed.dlq"`

#### Scenario: DLQ bindings for failed

- **WHEN** the application context is loaded
- **THEN** `payment.dlq` SHALL be bound to `dlx.exchange` with routing key `"payment.failed.dlq"`

#### Scenario: DLQ bindings for refunded

- **WHEN** the application context is loaded
- **THEN** `payment.dlq` SHALL be bound to `dlx.exchange` with routing key `"payment.refunded.dlq"`

#### Scenario: DLQ bindings for process command

- **WHEN** the application context is loaded
- **THEN** `payment.dlq` SHALL be bound to `dlx.exchange` with routing key `"payment.process.command.dlq"`

#### Scenario: DLQ bindings for refund command

- **WHEN** the application context is loaded
- **THEN** `payment.dlq` SHALL be bound to `dlx.exchange` with routing key `"payment.refund.command.dlq"`

### Requirement: definitions.json includes payment.refunded topology

The `docker/rabbitmq/definitions.json` SHALL include the `payment.refunded.queue` with its binding and DLQ binding, in addition to the existing completed and failed queues.

#### Scenario: payment.refunded.queue exists in definitions.json

- **WHEN** `definitions.json` is inspected
- **THEN** it SHALL declare a durable queue `"payment.refunded.queue"` with `x-dead-letter-exchange: "dlx.exchange"` and `x-dead-letter-routing-key: "payment.refunded.dlq"`

#### Scenario: payment.refunded binding exists in definitions.json

- **WHEN** `definitions.json` is inspected
- **THEN** it SHALL declare a binding from `"payment.exchange"` to `"payment.refunded.queue"` with routing key `"payment.refunded"`

#### Scenario: payment.refunded DLQ binding exists in definitions.json

- **WHEN** `definitions.json` is inspected
- **THEN** it SHALL declare a binding from `"dlx.exchange"` to `"payment.dlq"` with routing key `"payment.refunded.dlq"`

Constraint: Infrastructure layer only
--------------------------------------

RabbitMQConfig SHALL live in `com.vehiclerental.payment.infrastructure.config`.
