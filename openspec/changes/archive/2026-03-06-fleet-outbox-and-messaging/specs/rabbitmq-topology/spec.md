## ADDED Requirements

### Requirement: fleet.release.command.queue exists in definitions.json

The `docker/rabbitmq/definitions.json` SHALL declare a durable queue `fleet.release.command.queue` for SAGA compensation commands, following the same pattern as the existing `fleet.confirm.command.queue`.

#### Scenario: fleet.release.command.queue is declared

- **WHEN** `definitions.json` is inspected
- **THEN** it SHALL declare a durable queue `"fleet.release.command.queue"`
- **AND** it SHALL have `x-dead-letter-exchange` set to `"dlx.exchange"`
- **AND** it SHALL have `x-dead-letter-routing-key` set to `"fleet.release.command.dlq"`

### Requirement: fleet.release.command.queue binding to fleet.exchange

The `fleet.release.command.queue` SHALL be bound to `fleet.exchange` with routing key `fleet.release.command`, following the convention that command queues bind to the receiver service's exchange.

#### Scenario: fleet.release.command.queue binding

- **WHEN** `definitions.json` is inspected
- **THEN** it SHALL declare a binding from `"fleet.exchange"` to `"fleet.release.command.queue"` with routing key `"fleet.release.command"`

### Requirement: fleet.release.command DLQ binding routes to fleet.dlq

Dead letters from `fleet.release.command.queue` SHALL be routed to `fleet.dlq` via `dlx.exchange` with a distinct routing key for traceability.

#### Scenario: fleet.release.command DLQ binding

- **WHEN** `definitions.json` is inspected
- **THEN** it SHALL declare a binding from `"dlx.exchange"` to `"fleet.dlq"` with routing key `"fleet.release.command.dlq"`

## MODIFIED Requirements

### Requirement: Command queues for SAGA orchestration

The `docker/rabbitmq/definitions.json` SHALL declare 5 command queues for SAGA orchestration (previously 4). The new `fleet.release.command.queue` is added for Fleet compensation alongside the existing `customer.validate.command.queue`, `payment.process.command.queue`, `payment.refund.command.queue`, and `fleet.confirm.command.queue`.

#### Scenario: fleet.release.command.queue exists in definitions.json

- **WHEN** `definitions.json` is inspected
- **THEN** it SHALL declare a durable queue `"fleet.release.command.queue"`
- **AND** it SHALL have `x-dead-letter-exchange` set to `"dlx.exchange"`
- **AND** it SHALL have `x-dead-letter-routing-key` set to `"fleet.release.command.dlq"`

### Requirement: Command queue bindings use receiver's exchange

Each command queue SHALL be bound to the exchange of the service that will consume the command. The new `fleet.release.command.queue` binds to `fleet.exchange`.

#### Scenario: fleet.release.command.queue binding

- **WHEN** `definitions.json` is inspected
- **THEN** it SHALL declare a binding from `"fleet.exchange"` to `"fleet.release.command.queue"` with routing key `"fleet.release.command"`

### Requirement: Command queue DLQ bindings route to receiver's DLQ

Each command queue's dead letters SHALL be routed to the DLQ of the receiver service via `dlx.exchange`. The new `fleet.release.command` DLQ routes to `fleet.dlq`.

#### Scenario: fleet.release.command DLQ binding

- **WHEN** `definitions.json` is inspected
- **THEN** it SHALL declare a binding from `"dlx.exchange"` to `"fleet.dlq"` with routing key `"fleet.release.command.dlq"`

### Requirement: All service queues declared

The `definitions.json` SHALL now declare `fleet.release.command.queue` in addition to all previously declared queues, bringing the total command queue count from 4 to 5.

#### Scenario: fleet.release.command.queue is included in the platform topology

- **WHEN** RabbitMQ starts with `definitions.json` loaded
- **THEN** `fleet.release.command.queue` SHALL exist as a durable queue with DLQ routing to `dlx.exchange`
- **AND** it SHALL coexist alongside the 4 previously declared command queues: `customer.validate.command.queue`, `payment.process.command.queue`, `payment.refund.command.queue`, `fleet.confirm.command.queue`
