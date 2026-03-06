fleet-event-publisher
=====================

Purpose
-------

Domain event publisher adapter for Fleet Service. Implements the application output port via `OutboxFleetDomainEventPublisher`, which persists events to the outbox table for reliable publishing to RabbitMQ. The adapter is fully specified in the `fleet-outbox-publishing` capability.

Constraint: Infrastructure layer only
--------------------------------------

The event publisher adapter SHALL live in `com.vehiclerental.fleet.infrastructure.adapter.output.event`. The implementation class is `OutboxFleetDomainEventPublisher`.

#### Scenario: Outbox publisher is in correct package

- **WHEN** OutboxFleetDomainEventPublisher is inspected
- **THEN** it SHALL be in package `com.vehiclerental.fleet.infrastructure.adapter.output.event`
