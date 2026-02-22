## ADDED Requirements

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
