payment-outbox-publishing
=========================

Purpose
-------

Outbox-based domain event publisher for Payment Service. Replaces the logger no-op adapter with an `OutboxPaymentDomainEventPublisher` that serializes domain events to JSON and persists them to the `outbox_events` table in the same transaction as the payment aggregate. Handles 3 event types: PaymentCompletedEvent, PaymentFailedEvent, PaymentRefundedEvent.

## ADDED Requirements

### Requirement: OutboxPaymentDomainEventPublisher implements PaymentDomainEventPublisher

OutboxPaymentDomainEventPublisher SHALL implement `PaymentDomainEventPublisher` from the application layer. It SHALL serialize each domain event to JSON and persist it as an `OutboxEvent` in the same database transaction as the payment entity.

#### Scenario: Publishes events by persisting to outbox table

- **WHEN** `publish(List<DomainEvent>)` is called with a list of domain events
- **THEN** it SHALL create one `OutboxEvent` per domain event using `OutboxEvent.create()`
- **AND** it SHALL save each `OutboxEvent` via `OutboxEventRepository.save()`

#### Scenario: Adapter is a Spring component

- **WHEN** OutboxPaymentDomainEventPublisher is inspected
- **THEN** it SHALL be annotated with `@Component`

#### Scenario: Adapter has no direct RabbitMQ imports

- **WHEN** OutboxPaymentDomainEventPublisher imports are inspected
- **THEN** it SHALL NOT import any type from `org.springframework.amqp.*` or `com.rabbitmq.*`
- **AND** it SHALL only depend on `OutboxEventRepository`, `ObjectMapper`, and domain event types

### Requirement: OutboxPaymentDomainEventPublisher uses PAYMENT aggregate type

OutboxPaymentDomainEventPublisher SHALL use `"PAYMENT"` as the aggregate type and `"payment.exchange"` as the exchange for all outbox events.

#### Scenario: Aggregate type is PAYMENT

- **WHEN** any domain event is persisted to the outbox
- **THEN** the `aggregate_type` field SHALL be `"PAYMENT"`

#### Scenario: Exchange is payment.exchange

- **WHEN** any domain event is persisted to the outbox
- **THEN** the `exchange` field SHALL be `"payment.exchange"`

### Requirement: OutboxPaymentDomainEventPublisher extracts aggregateId from PaymentId

OutboxPaymentDomainEventPublisher SHALL extract the aggregate ID from the typed `PaymentId` in each domain event.

#### Scenario: PaymentCompletedEvent aggregateId extraction

- **WHEN** a `PaymentCompletedEvent` is published
- **THEN** the `aggregate_id` field SHALL be the UUID string from `event.paymentId().value().toString()`

#### Scenario: PaymentFailedEvent aggregateId extraction

- **WHEN** a `PaymentFailedEvent` is published
- **THEN** the `aggregate_id` field SHALL be the UUID string from `event.paymentId().value().toString()`

#### Scenario: PaymentRefundedEvent aggregateId extraction

- **WHEN** a `PaymentRefundedEvent` is published
- **THEN** the `aggregate_id` field SHALL be the UUID string from `event.paymentId().value().toString()`

#### Scenario: Unknown event type throws exception

- **WHEN** an unknown `DomainEvent` type is published
- **THEN** it SHALL throw `IllegalArgumentException` with the event class name

### Requirement: OutboxPaymentDomainEventPublisher derives routing keys by convention

Routing keys SHALL follow the pattern `payment.{eventType}` derived from the event class name.

#### Scenario: PaymentCompletedEvent routing key

- **WHEN** a `PaymentCompletedEvent` is published
- **THEN** the `routing_key` field SHALL be `"payment.completed"`

#### Scenario: PaymentFailedEvent routing key

- **WHEN** a `PaymentFailedEvent` is published
- **THEN** the `routing_key` field SHALL be `"payment.failed"`

#### Scenario: PaymentRefundedEvent routing key

- **WHEN** a `PaymentRefundedEvent` is published
- **THEN** the `routing_key` field SHALL be `"payment.refunded"`

### Requirement: OutboxPaymentDomainEventPublisher serializes events to JSON

OutboxPaymentDomainEventPublisher SHALL use Jackson `ObjectMapper` to serialize domain events to JSON strings for the outbox payload.

#### Scenario: Event serialized to JSON payload

- **WHEN** any domain event is published
- **THEN** the `payload` field SHALL contain the JSON representation of the event produced by `ObjectMapper.writeValueAsString()`

#### Scenario: Serialization failure throws IllegalStateException

- **WHEN** `ObjectMapper.writeValueAsString()` throws `JsonProcessingException`
- **THEN** OutboxPaymentDomainEventPublisher SHALL throw `IllegalStateException` with the event class name in the message

### Requirement: PaymentDomainEventPublisherAdapter is removed

The logger no-op `PaymentDomainEventPublisherAdapter` SHALL be deleted. `OutboxPaymentDomainEventPublisher` is its replacement.

#### Scenario: No logger no-op adapter exists

- **WHEN** the payment-infrastructure source tree is inspected
- **THEN** `PaymentDomainEventPublisherAdapter.java` SHALL NOT exist

### Requirement: Outbox events persist atomically with payment entity

OutboxEvent records SHALL be persisted in the same database transaction as the payment aggregate. If the payment save fails, outbox events SHALL also roll back.

#### Scenario: Payment and outbox event persist together

- **WHEN** `ProcessPaymentUseCase.execute()` is called with a valid command
- **THEN** both the `payments` table and the `outbox_events` table SHALL have new rows

#### Scenario: Both roll back on domain failure

- **WHEN** a domain validation error occurs during payment processing
- **THEN** neither the `payments` table nor the `outbox_events` table SHALL have new rows from that operation

Constraint: Infrastructure layer only
--------------------------------------

OutboxPaymentDomainEventPublisher SHALL live in `com.vehiclerental.payment.infrastructure.adapter.output.event`.
