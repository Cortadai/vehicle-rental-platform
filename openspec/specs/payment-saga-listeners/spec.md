payment-saga-listeners
======================

Purpose
-------

RabbitMQ listeners for Payment Service SAGA participation. Receives process payment and refund payment commands from the SAGA orchestrator via command queues and delegates to existing use cases.

## Requirements

### Requirement: PaymentProcessListener receives SAGA process commands

PaymentProcessListener SHALL be a Spring `@Component` annotated with `@RabbitListener` on queue `payment.process.command.queue`. It SHALL parse the incoming raw Message as JSON and delegate to `ProcessPaymentUseCase`.

#### Scenario: Receives and processes a payment command

- **WHEN** a JSON message arrives on `payment.process.command.queue` with fields `reservationId`, `customerId`, `amount`, `currency`
- **THEN** PaymentProcessListener SHALL parse the message body using ObjectMapper
- **AND** SHALL construct a `ProcessPaymentCommand` with the extracted fields
- **AND** SHALL invoke `processPaymentUseCase.execute(command)`

#### Scenario: Message with missing fields causes exception

- **WHEN** a JSON message arrives on `payment.process.command.queue` with missing required fields
- **THEN** the listener SHALL throw an exception
- **AND** Spring AMQP retry policy SHALL apply (3 attempts, exponential backoff)
- **AND** after exhausting retries, the message SHALL be routed to `payment.dlq`

### Requirement: PaymentRefundListener receives SAGA refund commands

PaymentRefundListener SHALL be a Spring `@Component` annotated with `@RabbitListener` on queue `payment.refund.command.queue`. It SHALL parse the incoming raw Message as JSON and delegate to `RefundPaymentUseCase`.

#### Scenario: Receives and processes a refund command

- **WHEN** a JSON message arrives on `payment.refund.command.queue` with field `reservationId`
- **THEN** PaymentRefundListener SHALL parse the message body using ObjectMapper
- **AND** SHALL construct a `RefundPaymentCommand` with the extracted `reservationId`
- **AND** SHALL invoke `refundPaymentUseCase.execute(command)`

#### Scenario: Refund for non-existent payment causes exception

- **WHEN** a JSON message arrives on `payment.refund.command.queue` with a `reservationId` that has no associated payment
- **THEN** the use case SHALL throw an exception
- **AND** Spring AMQP retry policy SHALL apply
- **AND** after exhausting retries, the message SHALL be routed to `payment.dlq`

### Requirement: Listeners live in infrastructure input messaging package

Both listeners SHALL be located in `com.vehiclerental.payment.infrastructure.adapter.input.messaging`, following the hexagonal convention for input adapters.

#### Scenario: Package location

- **WHEN** PaymentProcessListener and PaymentRefundListener are inspected
- **THEN** both SHALL be in package `com.vehiclerental.payment.infrastructure.adapter.input.messaging`

### Requirement: Listeners use constructor injection for use cases

Each listener SHALL receive its use case dependency via constructor injection, using the bean name that matches the use case interface (e.g., `processPaymentUseCase`, `refundPaymentUseCase`).

#### Scenario: PaymentProcessListener constructor

- **WHEN** PaymentProcessListener is inspected
- **THEN** it SHALL have a constructor parameter of type `ProcessPaymentUseCase`

#### Scenario: PaymentRefundListener constructor

- **WHEN** PaymentRefundListener is inspected
- **THEN** it SHALL have a constructor parameter of type `RefundPaymentUseCase`

Constraint: Infrastructure layer only
--------------------------------------

Listeners SHALL live in the payment-infrastructure module as input adapters. No domain or application layer changes required.
