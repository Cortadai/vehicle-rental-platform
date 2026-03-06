## ADDED Requirements

### Requirement: Domain event catalog includes SAGA response events

The fleet domain event catalog SHALL include three new SAGA response events: `FleetConfirmedEvent`, `FleetRejectedEvent`, and `FleetReleasedEvent`, alongside the 4 existing lifecycle events. These events are fully specified in the `fleet-saga-events` capability. Unlike the lifecycle events (which are emitted by the aggregate via `registerDomainEvent()`), the SAGA events are created directly by the application service and carry a `reservationId` (UUID) for SAGA correlation.

#### Scenario: Seven event types exist in the domain event package

- **WHEN** the contents of `com.vehiclerental.fleet.domain.event` are inspected
- **THEN** it SHALL contain VehicleRegisteredEvent, VehicleSentToMaintenanceEvent, VehicleActivatedEvent, VehicleRetiredEvent, FleetConfirmedEvent, FleetRejectedEvent, and FleetReleasedEvent
- **AND** all seven SHALL implement `DomainEvent`
