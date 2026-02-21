# reservation-event-publisher Delta Specification

## MODIFIED Requirements

### Requirement: ReservationDomainEventPublisherAdapter implements output port

`OutboxReservationDomainEventPublisher` SHALL implement `ReservationDomainEventPublisher` from the application layer. It SHALL serialize each domain event to JSON and persist it as an `OutboxEvent` in the `outbox_events` table within the current database transaction.

#### Scenario: Publishes events by writing to outbox table

- **WHEN** `publish(List<DomainEvent>)` is called with a list of domain events
- **THEN** it SHALL create one `OutboxEvent` per domain event using `OutboxEvent.create(...)` with `aggregateType` set to `"RESERVATION"`, `aggregateId` extracted from the event, `eventType` set to the event's class simple name (e.g., `"ReservationCreatedEvent"`), `payload` set to the Jackson JSON serialization of the event, `routingKey` derived from the event type (e.g., `"reservation.created"`), and `exchange` set to `"reservation.exchange"`, and SHALL save each `OutboxEvent` via `OutboxEventRepository`

#### Scenario: Handles empty event list

- **WHEN** `publish(List<DomainEvent>)` is called with an empty list
- **THEN** it SHALL NOT create any `OutboxEvent` records and SHALL NOT throw an exception

#### Scenario: Adapter is a Spring component

- **WHEN** `OutboxReservationDomainEventPublisher` is inspected
- **THEN** it SHALL be annotated with `@Component`

#### Scenario: Events persisted in same transaction as business entity

- **WHEN** `publish(List<DomainEvent>)` is called within a `@Transactional` method (e.g., `ReservationApplicationService.execute()`)
- **THEN** the `OutboxEvent` records SHALL be persisted in the same ACID transaction as the business entity (reservation), so that both commit or both roll back together

### Requirement: No messaging infrastructure dependency

`OutboxReservationDomainEventPublisher` SHALL NOT directly depend on RabbitMQ client libraries. It SHALL only depend on `OutboxEventRepository` (JPA) and `ObjectMapper` (Jackson) for serialization.

#### Scenario: No direct RabbitMQ imports

- **WHEN** `OutboxReservationDomainEventPublisher` imports are inspected
- **THEN** it SHALL NOT import any type from `org.springframework.amqp.*` or `com.rabbitmq.*` — RabbitMQ publishing is handled by `OutboxPublisher` in `common-messaging`

## REMOVED Requirements

### Requirement: No messaging infrastructure dependency

**Reason:** The original requirement prohibited all messaging dependencies because the adapter was a no-op logger. The new adapter depends on `OutboxEventRepository` (JPA) and `ObjectMapper` (Jackson) but still does NOT depend on RabbitMQ client libraries directly. The replacement MODIFIED requirement above redefines this boundary.

**Migration:** The `OutboxReservationDomainEventPublisher` replaces `ReservationDomainEventPublisherAdapter`. Delete the old class. The new class depends on `common-messaging` for `OutboxEvent` and `OutboxEventRepository`.

## Constraint: Infrastructure layer only

The event publisher adapter SHALL live in `com.vehiclerental.reservation.infrastructure.adapter.output.event`.
