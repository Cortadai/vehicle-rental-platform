## ADDED Requirements

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

### Requirement: RabbitMQConfig declares command queue bindings

Each command queue SHALL be bound to `payment.exchange` with the appropriate routing key, following the convention `{service}.{action}.command`.

#### Scenario: Process command queue binding

- **WHEN** the application context is loaded
- **THEN** `payment.process.command.queue` SHALL be bound to `payment.exchange` with routing key `"payment.process.command"`

#### Scenario: Refund command queue binding

- **WHEN** the application context is loaded
- **THEN** `payment.refund.command.queue` SHALL be bound to `payment.exchange` with routing key `"payment.refund.command"`

### Requirement: RabbitMQConfig declares command queue DLQ bindings

Dead letters from each command queue SHALL be routed to `payment.dlq` via `dlx.exchange` with a distinct routing key for traceability.

#### Scenario: Process command DLQ binding

- **WHEN** the application context is loaded
- **THEN** `payment.dlq` SHALL be bound to `dlx.exchange` with routing key `"payment.process.command.dlq"`

#### Scenario: Refund command DLQ binding

- **WHEN** the application context is loaded
- **THEN** `payment.dlq` SHALL be bound to `dlx.exchange` with routing key `"payment.refund.command.dlq"`

## MODIFIED Requirements

### Requirement: RabbitMQConfig declares bindings

RabbitMQConfig SHALL bind each event queue and each command queue to the payment exchange with the appropriate routing key, and bind the DLQ to the DLX exchange. The total binding count increases from 6 (3 event + 3 DLQ) to 10 (3 event + 2 command + 3 event DLQ + 2 command DLQ).

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
