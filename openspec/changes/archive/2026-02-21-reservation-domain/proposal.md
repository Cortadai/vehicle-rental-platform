## Why

The Reservation Service is the SAGA coordinator and the most complex service in the platform. Its domain layer is the foundation: a rich aggregate with a 6-state machine, an inner entity (ReservationItem), cross-context references (CustomerId, VehicleId), and domain events — all in pure Java with zero Spring dependencies.

Customer Service and Fleet Service domains are complete (108 domain tests, shared kernel validated). Reservation is the natural next step per Phase 1 of the roadmap (Walking Skeleton). Building the domain first follows the established pattern: domain → application → infrastructure+container.

## What Changes

- New Maven module `reservation-service/reservation-domain` with dependency on `common` only
- Aggregate Root `Reservation` extending `AggregateRoot<ReservationId>` from common, with 6 state transitions (initializeReservation, validateCustomer, pay, confirm, initCancel, cancel) and failure message tracking
- Inner entity `ReservationItem` extending `BaseEntity<UUID>` from common, with automatic subtotal calculation (dailyRate × days)
- 6 local Value Objects as records: `ReservationId`, `TrackingId`, `CustomerId`, `VehicleId`, `DateRange`, `PickupLocation` — CustomerId and VehicleId are local to this bounded context, not imported from other domains
- Enum `ReservationStatus` with 6 states: PENDING, CUSTOMER_VALIDATED, PAID, CONFIRMED, CANCELLING, CANCELLED
- `Money` reused from common (BigDecimal + Currency), not redefined locally
- 2 Domain Events as records implementing `DomainEvent` interface from common: `ReservationCreatedEvent` (full snapshot), `ReservationCancelledEvent` (with failure messages)
- `ReservationItemSnapshot` record for immutable event payloads
- `ReservationDomainException extends DomainException` — single exception class with error codes, following Customer/Fleet pattern
- Output port `ReservationRepository` interface in domain (save, findById, findByTrackingId)
- Test-First unit tests for aggregate, inner entity, value objects, and domain events
- Root POM updated: module declaration + dependencyManagement entry for `reservation-domain`

## Capabilities

### New Capabilities
- `reservation-aggregate`: Reservation Aggregate Root with full state machine (6 transitions), factory methods (create/reconstruct), totalPrice calculation from items, failure message tracking, and domain event registration
- `reservation-item-entity`: ReservationItem inner entity with factory method, subtotal calculation (dailyRate × days), and validation rules
- `reservation-value-objects`: Typed IDs (ReservationId, TrackingId, CustomerId, VehicleId), DateRange (with getDays()), PickupLocation, and ReservationStatus enum — all local to the reservation bounded context
- `reservation-domain-events`: ReservationCreatedEvent (full snapshot with items), ReservationCancelledEvent (with failure messages), ReservationItemSnapshot record — all implementing DomainEvent interface from common
- `reservation-repository-port`: ReservationRepository output port interface with save, findById, and findByTrackingId operations

### Modified Capabilities
- `multi-module-build`: Root POM adds `reservation-service/reservation-domain` module and dependencyManagement entry

## Impact

- **New module**: `reservation-service/reservation-domain/` with `pom.xml` depending only on `common`
- **Root POM**: New module declaration and dependencyManagement entry
- **No changes to existing code**: common, customer-service, and fleet-service remain untouched
- **Test count increase**: ~60-80 new domain unit tests (estimated based on Customer's 58 and Fleet's 50, adjusted for higher complexity)
- **Build chain**: `mvn install -N` → `common` → `reservation-domain` (same incremental pattern)
