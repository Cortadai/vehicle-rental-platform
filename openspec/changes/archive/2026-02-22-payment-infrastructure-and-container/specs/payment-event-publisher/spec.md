payment-event-publisher
=======================

Purpose
-------

Domain event publisher adapter for Payment Service. Implements the application output port as a logger-based no-op. Provides observability for events while deferring real messaging (RabbitMQ/Outbox) to a future change (`payment-outbox-and-messaging`).

## ADDED Requirements

### Requirement: PaymentDomainEventPublisherAdapter implements output port

PaymentDomainEventPublisherAdapter SHALL implement `PaymentDomainEventPublisher` from the application layer. It SHALL log each event using SLF4J.

#### Scenario: Publishes events by logging

- **WHEN** `publish(List<DomainEvent>)` is called with a list of domain events
- **THEN** it SHALL log each event's class name and event ID using SLF4J at INFO level

#### Scenario: Handles empty event list

- **WHEN** `publish(List<DomainEvent>)` is called with an empty list
- **THEN** it SHALL NOT log any events and SHALL NOT throw an exception

#### Scenario: Adapter is a Spring component

- **WHEN** PaymentDomainEventPublisherAdapter is inspected
- **THEN** it SHALL be annotated with `@Component`

### Requirement: No messaging infrastructure dependency

PaymentDomainEventPublisherAdapter SHALL NOT depend on RabbitMQ, Kafka, or any messaging library. It SHALL only use SLF4J for logging.

#### Scenario: No messaging imports

- **WHEN** PaymentDomainEventPublisherAdapter imports are inspected
- **THEN** it SHALL NOT import any type from `org.springframework.amqp.*`, `org.apache.kafka.*`, or `org.springframework.jms.*`

Constraint: Infrastructure layer only
--------------------------------------

The event publisher adapter SHALL live in `com.vehiclerental.payment.infrastructure.adapter.output.event`.
