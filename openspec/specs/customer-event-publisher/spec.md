customer-event-publisher
========================

Purpose
-------

Domain event publisher adapter for Customer Service. Implements the application output port via OutboxCustomerDomainEventPublisher, which persists events to the outbox table for reliable delivery. The full implementation is specified in the `customer-outbox-publishing` capability.

## Requirements

### Requirement: Infrastructure layer only

The event publisher adapter SHALL live in `com.vehiclerental.customer.infrastructure.adapter.output.event`. The implementation class is `OutboxCustomerDomainEventPublisher`.

#### Scenario: Outbox publisher is in correct package

- **WHEN** OutboxCustomerDomainEventPublisher is inspected
- **THEN** it SHALL be in package `com.vehiclerental.customer.infrastructure.adapter.output.event`
