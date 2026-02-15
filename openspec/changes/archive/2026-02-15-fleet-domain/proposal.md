Why
---

Customer Service established the DDD tactical patterns (Aggregate Root, Value Objects, Domain Events, Typed IDs) and the hexagonal module structure. Building Fleet's domain layer as the second bounded context consolidates those patterns through repetition and validates that the structure is repeatable across services. Fleet is self-contained like Customer — no SAGA coordination, no cross-service dependencies at the domain level — making it ideal for reinforcing the workflow before tackling the more complex Reservation Service.
What Changes
------------

### Included

* **Parent POM update** — add `fleet-service/fleet-domain` to root `<modules>` section and `<dependencyManagement>`
* **fleet-domain Maven module** — `fleet-service/fleet-domain/pom.xml`, pure Java, depends only on `common`
* **Vehicle Aggregate Root** — extends `AggregateRoot<VehicleId>`, factory methods `create()` and `reconstruct()`, lifecycle transitions (sendToMaintenance, activate, retire), domain event registration
* **Value Objects (records)** — `VehicleId` (UUID typed ID), `LicensePlate` (format validation), `VehicleCategory` (enum: SEDAN, SUV, VAN, MOTORCYCLE), `DailyRate` (wraps Money from common, validates positive amount)
* **Enum** — `VehicleStatus` (ACTIVE, UNDER_MAINTENANCE, RETIRED)
* **Domain Events (records)** — `VehicleRegisteredEvent` (snapshot with all vehicle data), `VehicleSentToMaintenanceEvent`, `VehicleActivatedEvent`, `VehicleRetiredEvent`
* **Domain Exception** — `FleetDomainException extends DomainException` with error codes
* **Output Port** — `VehicleRepository` interface (save, findById) using only domain types
* **Full test-first coverage** — tests written before implementation, derived from spec scenarios

### Excluded

* Application layer (use cases, DTOs, commands) — separate change `fleet-application`
* Infrastructure layer (JPA, REST, Flyway) — separate change `fleet-infrastructure-and-container`
* Vehicle availability queries (findAvailableByCategory, date-range overlap checks) — belongs in application/infrastructure when Reservation integration arrives
* RENTED status — vehicle rental state is managed by Reservation Service through date-range availability, not as a Vehicle lifecycle state
* Reservation/booking logic — belongs in Reservation Service, Fleet only manages vehicle lifecycle
* Domain Service — not needed, all business logic fits within the Vehicle aggregate

Capabilities
------------

### New Capabilities

* `vehicle-aggregate`: Vehicle aggregate root with create/reconstruct factory methods, state transitions (sendToMaintenance, activate, retire), domain invariants, RETIRED as terminal state
* `vehicle-value-objects`: VehicleId (typed UUID), LicensePlate (format validation), VehicleCategory (enum), VehicleStatus (enum with allowed transitions), DailyRate (Money wrapper, positive amount validation)
* `fleet-domain-events`: VehicleRegisteredEvent (snapshot), VehicleSentToMaintenanceEvent, VehicleActivatedEvent, VehicleRetiredEvent — all records implementing DomainEvent interface
* `vehicle-repository-port`: VehicleRepository output port interface with save and findById, pure domain types

### Modified Capabilities

* `multi-module-build`: Root POM adds `fleet-service/fleet-domain` module declaration and dependencyManagement entry

Impact
------

1 parent POM update + 1 new pom.xml + ~12 production classes + ~8 test classes. Second service domain module — validates that the DDD patterns and hexagonal structure from Customer are repeatable and establishes Fleet's domain model for future SAGA integration.
