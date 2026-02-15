## Context

Customer Service established the DDD tactical patterns across its domain module (58 unit tests). Fleet Service is the second bounded context and follows the same structure: Aggregate Root extending `AggregateRoot<VehicleId>`, records as Value Objects, records as Domain Events implementing `DomainEvent`, single domain exception with error codes, and output port interface in the domain module. The common shared kernel (`BaseEntity`, `AggregateRoot`, `DomainEvent`, `Money`, `DomainException`) is consumed as-is — no changes needed.

Fleet manages vehicle lifecycle (register, maintain, activate, retire). It does NOT manage reservations or rental state — that responsibility belongs to Reservation Service, which will coordinate with Fleet through SAGA events in a future change.

## Goals / Non-Goals

**Goals:**

* Establish Vehicle Aggregate Root with lifecycle management (register, sendToMaintenance, activate, retire)
* Implement typed Value Objects (VehicleId, LicensePlate, VehicleCategory, DailyRate) with validation
* Define Domain Events for all lifecycle transitions
* Define the VehicleRepository output port interface
* Validate that the common shared kernel is reusable across services without modification
* Follow test-first approach derived from spec scenarios

**Non-Goals:**

* Application layer (use cases, commands, DTOs, `@Transactional`) — separate change
* Infrastructure layer (JPA entities, REST controllers, persistence adapters) — separate change
* RENTED status or availability tracking — managed by Reservation Service through date-range queries
* Domain Service — all logic fits within the Vehicle aggregate
* Input Ports — belong in application layer

## Decisions

### Decision 1: Factory methods `create()` and `reconstruct()` — same pattern as Customer

**Choice**: Vehicle exposes two static factory methods:
- `Vehicle.create(licensePlate, make, model, year, category, dailyRate, description)` — generates VehicleId, sets status ACTIVE, registers VehicleRegisteredEvent. `description` is nullable.
- `Vehicle.reconstruct(id, licensePlate, make, model, year, category, dailyRate, description, status, createdAt)` — rehydration from persistence, no validation, no events.

**Rationale**: Identical pattern to Customer. Proven to work with the JPA persistence adapter (separate entity + mapper + `reconstruct()` for rehydration). No reason to deviate.

### Decision 2: VehicleStatus transitions — RETIRED as terminal state

**Choice**: The Vehicle aggregate enforces valid state transitions:
- `ACTIVE → UNDER_MAINTENANCE` (via `sendToMaintenance()`)
- `UNDER_MAINTENANCE → ACTIVE` (via `activate()`)
- `ACTIVE → RETIRED`, `UNDER_MAINTENANCE → RETIRED` (via `retire()`)
- `RETIRED → *` (no transitions — terminal state)

Invalid transitions throw `FleetDomainException`.

**Rationale**: Same state machine pattern as Customer (where DELETED is terminal). Three states, four valid transitions. The aggregate validates preconditions, changes state, and registers the appropriate domain event. Note: there is no RENTED status — rental tracking belongs to Reservation Service.

**Alternative rejected**: Including RENTED as a vehicle status. This would couple Fleet's domain to Reservation's lifecycle. Instead, Reservation Service will query Fleet for availability through a separate mechanism (date-range overlap queries in infrastructure/application).

### Decision 3: DailyRate wraps Money — validates strictly positive

**Choice**: `DailyRate` is a record wrapping `Money` from common. Its compact constructor validates that the amount is strictly positive (`amount.signum() > 0`).

**Rationale**: `Money` already validates non-negative and non-null, but a daily rate of zero makes no business sense. `DailyRate` adds the business constraint on top of the technical constraint. This is the first real reuse of a shared kernel type — validates that the common module works as a dependency.

**Alternative rejected**: Using `Money` directly without a wrapper. Would lose the business semantics ("daily rate must be positive") and allow zero-amount rates.

### Decision 4: LicensePlate validation — alphanumeric pattern, not country-specific

**Choice**: `LicensePlate` validates that the value is non-blank, between 2 and 15 characters, and matches an alphanumeric pattern allowing hyphens and spaces (e.g., `1234-BCD`, `ABC 1234`, `M 1234 XY`).

**Rationale**: License plate formats vary enormously across countries. A strict regex for one country (e.g., Spain's `1234-BCD`) would reject valid plates from other countries. The validation catches obvious errors (empty strings, special characters) without being overly restrictive. This mirrors the pragmatic approach used for Email validation in Customer.

**Alternative rejected**: Country-specific format validation. Over-engineering for a POC — would require a country field on Vehicle and a registry of format patterns.

### Decision 5: Vehicle fields — make, model, year, and description as plain fields

**Choice**: `make` (brand) and `model` are plain `String` fields validated as non-blank. `year` is an `int` validated within a reasonable range (1950 to currentYear+1). `description` is a nullable `String` with a maximum of 500 characters — the only optional field on the aggregate (same pattern as `PhoneNumber` in Customer). None of these are Value Objects.

**Rationale**: Make, model, year, and description are simple descriptive attributes with no domain behavior beyond basic validation. Wrapping them in records would add ceremony without benefit. `description` is free-form text for details like "Color rojo, techo panorámico, GPS integrado" — it has no format to validate, just a length limit. Compare with `LicensePlate` which has format validation and uniqueness semantics, or `DailyRate` which wraps Money with a business constraint — those justify dedicated types.

**Alternative rejected**: `VehicleMake`, `VehicleModel`, `VehicleYear` records. Over-engineering — these are data, not behavior.

### Decision 6: Domain events — same snapshot pattern as Customer

**Choice**: Events implement `DomainEvent` from common:
- `VehicleRegisteredEvent` — full snapshot: vehicleId, licensePlate, make, model, year, category, dailyRate, description (nullable).
- `VehicleSentToMaintenanceEvent` — only vehicleId.
- `VehicleActivatedEvent` — only vehicleId.
- `VehicleRetiredEvent` — only vehicleId.

**Rationale**: Same reasoning as Customer. The registration event carries the full snapshot because consumers (like Reservation Service) may need vehicle details. Lifecycle events carry only the ID since consumers just need to know what happened to which vehicle.

### Decision 7: VehicleRepository output port in domain module

**Choice**: `VehicleRepository` interface in `fleet-domain/port/output/`, same as Customer.

**Rationale**: Same pragmatic decision as Customer — the interface is pure domain (no Spring types), and the application module doesn't exist yet. When fleet-application is created, the port can stay here.

## Package Structure

```
fleet-service/fleet-domain/src/main/java/com/vehiclerental/fleet/domain/
├── model/
│   ├── aggregate/
│   │   └── Vehicle.java                        ← Aggregate Root
│   └── vo/
│       ├── VehicleId.java                      ← Typed ID (record, UUID)
│       ├── LicensePlate.java                   ← Value Object (record, format validation)
│       ├── VehicleCategory.java                ← Enum (SEDAN, SUV, VAN, MOTORCYCLE)
│       ├── VehicleStatus.java                  ← Enum (ACTIVE, UNDER_MAINTENANCE, RETIRED)
│       └── DailyRate.java                      ← Value Object (record, wraps Money)
├── event/
│   ├── VehicleRegisteredEvent.java             ← Domain Event (record, snapshot)
│   ├── VehicleSentToMaintenanceEvent.java      ← Domain Event (record)
│   ├── VehicleActivatedEvent.java              ← Domain Event (record)
│   └── VehicleRetiredEvent.java                ← Domain Event (record)
├── exception/
│   └── FleetDomainException.java               ← Extends DomainException
└── port/
    └── output/
        └── VehicleRepository.java              ← Output Port (interface)
```

## Risks / Trade-offs

* **DailyRate wrapping Money adds a layer of indirection** — For a single field, this is minimal overhead. The benefit is explicit business semantics. If Fleet grows to have complex pricing (discounts, seasonal rates), DailyRate is the natural place to extend.
* **No RENTED status may confuse developers** — Someone might expect Fleet to track which vehicles are currently rented. Mitigation: the proposal explicitly documents that RENTED is excluded and why. The Reservation Service will handle availability through date-range queries against a reservations table.
* **LicensePlate validation is permissive** — Accepts patterns that aren't valid in any country. Mitigation: same pragmatic approach as Email in Customer — catches obvious errors, real validation happens at business process level.
* **Output port in domain vs application** — Same trade-off as Customer. Moving it later is a trivial refactor.
