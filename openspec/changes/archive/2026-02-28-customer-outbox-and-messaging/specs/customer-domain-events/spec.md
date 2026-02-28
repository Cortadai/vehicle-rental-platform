## ADDED Requirements

### Requirement: Domain event catalog includes SAGA response events

The customer domain event catalog SHALL include two new SAGA response events: `CustomerValidatedEvent` and `CustomerRejectedEvent`, alongside the 4 existing lifecycle events. These events are fully specified in the `customer-saga-events` capability. Unlike the lifecycle events (which are emitted by the aggregate via `registerDomainEvent()`), the SAGA events are created directly by the application service and carry a `reservationId` (UUID) for SAGA correlation.

#### Scenario: Six event types exist in the domain event package

- **WHEN** the contents of `com.vehiclerental.customer.domain.event` are inspected
- **THEN** it SHALL contain CustomerCreatedEvent, CustomerSuspendedEvent, CustomerActivatedEvent, CustomerDeletedEvent, CustomerValidatedEvent, and CustomerRejectedEvent
- **AND** all six SHALL implement `DomainEvent`
