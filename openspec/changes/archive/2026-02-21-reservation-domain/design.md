## Context

Customer Service (58 domain tests) and Fleet Service (50 domain tests) established the DDD tactical patterns: Aggregate Root extending `AggregateRoot<ID>` from common, records as Value Objects, records as Domain Events implementing `DomainEvent` interface, single domain exception with error codes, and output port interface in the domain module. The common shared kernel (`BaseEntity`, `AggregateRoot`, `DomainEvent`, `Money`, `DomainException`) is consumed as-is — no changes needed.

Reservation is the third bounded context and the most complex. Unlike Customer (3 states, 4 transitions) and Fleet (3 states, 4 transitions), Reservation has a 6-state machine with 6 transition methods, an inner entity (`ReservationItem`), cross-context references (`CustomerId`, `VehicleId` as local typed IDs), and calculated fields (`totalPrice` derived from items). This is the first aggregate that contains another entity, introducing parent-child lifecycle management within the aggregate boundary.

The domain module follows the established pattern: pure Java, zero Spring dependencies, factory methods (`create`/`reconstruct`), and test-first with specs-derived scenarios.

## Goals / Non-Goals

**Goals:**

- Establish Reservation Aggregate Root with full SAGA-ready state machine (6 states, 6 transitions)
- Implement ReservationItem as an inner entity within the aggregate boundary
- Define cross-context typed IDs (CustomerId, VehicleId) local to the reservation bounded context
- Implement DateRange and PickupLocation as new domain Value Objects
- Reuse Money from common for pricing (totalPrice, dailyRate, subtotal)
- Define Domain Events for creation and cancellation with immutable snapshots
- Define the ReservationRepository output port with findByTrackingId
- Follow test-first approach derived from spec scenarios
- Validate that the aggregate-with-inner-entity pattern works with the shared kernel

**Non-Goals:**

- Application layer (use cases, commands, DTOs, `@Transactional`) — separate change
- Infrastructure layer (JPA entities, REST controllers, persistence adapters) — separate change
- Domain Service — no cross-aggregate logic exists in this phase; Application Service calls the factory method directly
- SAGA orchestration, Outbox pattern, or RabbitMQ messaging — Phase 2+
- Input Ports — belong in application layer
- Validation of `pickupDate >= today` — application-layer concern, not a domain invariant
- Intermediate domain events for `validateCustomer`, `pay`, `confirm`, `initCancel` — these transitions are triggered by the SAGA orchestrator, which manages its own events via Outbox

## Decisions

### Decision 1: Factory methods `create()` and `reconstruct()` — same pattern, higher complexity

**Choice**: Reservation exposes two static factory methods:
- `Reservation.create(customerId, pickupLocation, returnLocation, dateRange, items)` — generates ReservationId + TrackingId, calculates totalPrice from items, sets status PENDING, registers ReservationCreatedEvent.
- `Reservation.reconstruct(...)` — rehydration from persistence with all fields including items and failureMessages. No validation, no events.

**Rationale**: Identical pattern to Customer and Fleet, but `create()` does more work: it iterates over items to sum subtotals into totalPrice. The `reconstruct()` method accepts pre-built `ReservationItem` objects (the persistence adapter will reconstruct items first, then pass them to the aggregate's reconstruct). This keeps the pattern consistent — create is for business creation, reconstruct is for technical rehydration.

**Alternative rejected**: Builder pattern for the complex parameter list. Rejected because factory methods are the established convention and the parameter count (5 for create, ~10 for reconstruct) is manageable.

### Decision 2: ReservationItem as `BaseEntity<UUID>` — not a typed ID

**Choice**: `ReservationItem` extends `BaseEntity<UUID>` with a plain UUID as its identity. It does not have a typed `ReservationItemId`.

**Rationale**: ReservationItem is an entity within the Reservation aggregate boundary. It is never referenced from outside the aggregate — it has no external identity. A typed `ReservationItemId` would add a class that nobody outside the aggregate ever uses. Customer and Fleet have no inner entities, so there's no precedent to follow. Using `BaseEntity<UUID>` directly is the simplest approach that works with the shared kernel.

**Alternative rejected**: `ReservationItemId` typed ID record. Would add ceremony for an identity that is purely internal. If items later need external referencing (unlikely), it's a trivial refactor.

### Decision 3: Cross-context IDs (CustomerId, VehicleId) are local to reservation

**Choice**: `CustomerId` and `VehicleId` are defined as record value objects within `reservation-domain`, not imported from `customer-domain` or `fleet-domain`.

**Rationale**: Bounded context isolation. If Reservation imported `CustomerId` from customer-domain, it would create a Maven dependency from reservation-domain to customer-domain — coupling two bounded contexts at the module level. In DDD, each bounded context owns its own representation of cross-context concepts. Reservation's `CustomerId` is "the UUID that identifies the customer this reservation belongs to" — it has no knowledge of customer-domain's validation rules (e.g., CustomerDomainException). It just validates not-null.

**Alternative rejected**: Shared IDs in common module. Would pollute the shared kernel with every service's IDs. The kernel contains only truly universal concepts (Money, BaseEntity, DomainEvent).

### Decision 4: State machine with 6 states — `cancel()` validates multiple source states

**Choice**: The Reservation aggregate enforces valid state transitions:
- `initializeReservation()` — called by `create()`, sets PENDING
- `validateCustomer()` — PENDING → CUSTOMER_VALIDATED
- `pay()` — CUSTOMER_VALIDATED → PAID
- `confirm()` — PAID → CONFIRMED
- `initCancel(failureMessages)` — PAID → CANCELLING (stores failure messages)
- `cancel()` — PENDING/CUSTOMER_VALIDATED/CANCELLING → CANCELLED

Invalid transitions throw `ReservationDomainException` with error code `"RESERVATION_INVALID_STATE"`.

**Rationale**: This maps directly to the SAGA flow from `project.md`. `cancel()` accepts multiple source states because cancellation can happen at different points in the flow (customer validation fails from PENDING, payment fails from CUSTOMER_VALIDATED, fleet unavailable triggers initCancel→cancel from PAID). `cancel()` from CONFIRMED or PAID is invalid — CONFIRMED is a terminal success state, and PAID must go through initCancel first (the compensation flow requires explicit failure messages).

**Alternative rejected**: Separate cancel methods per source state (`cancelFromPending`, `cancelFromValidated`, `cancelFromCancelling`). Over-engineering — the business rule is "you can cancel unless confirmed or paid-without-initCancel", which is simpler as one method with state validation.

### Decision 5: Only two domain events — ReservationCreatedEvent and ReservationCancelledEvent

**Choice**: Only `create()` and `cancel()` register domain events. The intermediate transitions (`validateCustomer`, `pay`, `confirm`, `initCancel`) do NOT emit domain events.

**Rationale**: In the full SAGA flow, the intermediate transitions are triggered by incoming events from other services (CustomerValidatedEvent, PaymentCompletedEvent, FleetConfirmedEvent). The SAGA orchestrator in the Application/Infrastructure layer handles the event choreography via Outbox. The aggregate doesn't need to emit events for these transitions — it just needs to change state. The two events that matter at the domain level are: "a reservation was created" (triggers the SAGA) and "a reservation was cancelled" (carries failure messages for audit/notification).

**Alternative rejected**: One event per transition (6 events). Would create domain events that duplicate what the SAGA orchestrator already publishes via Outbox. The aggregate would register events that are never consumed — they'd just be noise.

### Decision 6: ReservationCreatedEvent carries full snapshot with ReservationItemSnapshot

**Choice**: `ReservationCreatedEvent` is a record implementing `DomainEvent` with: `eventId`, `occurredOn`, `reservationId`, `trackingId`, `customerId`, `totalPrice`, `dateRange`, `pickupLocation`, `returnLocation`, and `List<ReservationItemSnapshot>`.

`ReservationItemSnapshot` is a separate record: `vehicleId`, `dailyRate`, `days`, `subtotal`. It captures the item data at event time, decoupled from the live `ReservationItem` entity.

**Rationale**: The snapshot pattern prevents the event from holding a reference to the mutable entity. If someone later adds a method to ReservationItem that modifies state, the event remains immutable. This follows the general principle that events are facts about the past — they should be self-contained and immutable. `ReservationCancelledEvent` carries only `reservationId` and `failureMessages` since the reservation details were already published in the created event.

**Alternative rejected**: Events referencing live entities. Breaks immutability and creates confusing ownership — who controls the entity's lifecycle, the aggregate or the event?

### Decision 7: Money reused from common — no local redefinition

**Choice**: `Money(BigDecimal amount, Currency currency)` from common is used directly for `totalPrice` (Reservation), `dailyRate` and `subtotal` (ReservationItem).

**Rationale**: Money already exists in common with the right contract: non-null, non-negative, scale 2, same-currency arithmetic (`add`, `multiply`). Fleet's `DailyRate` already wraps Money successfully. Reservation's totalPrice calculation needs `Money.add()` to sum item subtotals and `Money.multiply(int)` for dailyRate × days. All operations are already available. Creating a local Money would duplicate ~40 lines of code and diverge from the shared kernel.

**Alternative rejected**: Simplified `Money(BigDecimal)` without currency. Would be inconsistent with common's Money and Fleet's DailyRate. Currency is part of the Money contract even if this POC uses a single currency.

### Decision 8: DateRange — domain invariant is structural, not temporal

**Choice**: `DateRange(LocalDate pickupDate, LocalDate returnDate)` validates: both dates not null, `returnDate` must be strictly after `pickupDate`. It provides `getDays()` returning the number of days between pickup and return. It does NOT validate that `pickupDate >= today`.

**Rationale**: "Return date must be after pickup date" is a structural invariant — a DateRange where return equals or precedes pickup is nonsensical regardless of when it's created. "Pickup must be in the future" is a business rule that depends on *when* the reservation is created — it's temporal context that belongs in the Application Service (use case). Additionally, not validating against `today` allows creating reservations with past dates in tests without needing to mock the clock.

**Alternative rejected**: Validate `pickupDate >= LocalDate.now()` in the VO. Would couple the VO to system time, complicate testing, and mix domain invariants with use-case rules.

### Decision 9: ReservationRepository with findByTrackingId

**Choice**: `ReservationRepository` interface in domain with three methods: `save(Reservation)`, `findById(ReservationId)`, and `findByTrackingId(TrackingId)`.

**Rationale**: `findByTrackingId` exists because TrackingId is the public-facing identifier for REST API and SAGA correlation. ReservationId is the internal aggregate identity. Clients (REST, SAGA messages) will reference reservations by TrackingId, so the repository needs to support lookup by it. Same pattern as Customer/Fleet having `findById`, but extended with a second lookup method for the public ID.

**Alternative rejected**: Only `findById` — would force the application layer to maintain a separate TrackingId→ReservationId mapping or expose internal IDs publicly.

### Decision 10: `failureMessages` as `List<String>` — not a Value Object

**Choice**: `failureMessages` on the Reservation aggregate is a `List<String>`. It is empty by default, populated by `initCancel(List<String> failureMessages)`, and carried in `ReservationCancelledEvent`.

**Rationale**: Failure messages are free-form text from external services ("Customer validation failed: license expired", "Fleet: vehicle unavailable for dates"). They have no structure to validate beyond non-null. Wrapping them in a Value Object would add ceremony for a list of strings that are purely informational. The aggregate stores them for audit and event purposes.

**Alternative rejected**: `FailureReason` value object with `source` and `message`. Over-engineering for Phase 1 — the messages are opaque strings passed through by the SAGA.

## Package Structure

```
reservation-service/reservation-domain/src/main/java/com/vehiclerental/reservation/domain/
├── model/
│   ├── aggregate/
│   │   └── Reservation.java                  ← Aggregate Root (6 state transitions)
│   ├── entity/
│   │   └── ReservationItem.java              ← Inner Entity (subtotal calculation)
│   └── vo/
│       ├── ReservationId.java                ← Typed ID (record, UUID)
│       ├── TrackingId.java                   ← Public-facing ID (record, UUID)
│       ├── CustomerId.java                   ← Cross-context ID (record, UUID, local)
│       ├── VehicleId.java                    ← Cross-context ID (record, UUID, local)
│       ├── DateRange.java                    ← Value Object (record, pickupDate + returnDate)
│       ├── PickupLocation.java               ← Value Object (record, address + city)
│       └── ReservationStatus.java            ← Enum (6 states)
├── event/
│   ├── ReservationCreatedEvent.java          ← Domain Event (record, full snapshot)
│   ├── ReservationCancelledEvent.java        ← Domain Event (record, with failure messages)
│   └── ReservationItemSnapshot.java          ← Immutable snapshot (record)
├── exception/
│   └── ReservationDomainException.java       ← Extends DomainException (error codes)
└── port/
    └── output/
        └── ReservationRepository.java        ← Output Port (save, findById, findByTrackingId)
```

## Risks / Trade-offs

- **`reconstruct()` with items is complex** — Unlike Customer/Fleet, reconstruct needs a `List<ReservationItem>` already built. The persistence adapter must reconstruct items first, then pass them to the aggregate. Risk: the adapter could pass items in an inconsistent state. Mitigation: reconstruct skips validation by design (data from DB is trusted), and the adapter uses `ReservationItem.reconstruct()` for each item.
- **Only 2 domain events for 6 transitions** — Intermediate transitions don't emit events. If a consumer needs to react to CUSTOMER_VALIDATED specifically, the SAGA orchestrator handles that via Outbox. Risk: someone might add a domain event for an intermediate transition later, creating dual publishing. Mitigation: document clearly that intermediate events belong to the SAGA layer, not the domain.
- **`cancel()` accepting multiple source states increases branching** — The method must validate `status ∈ {PENDING, CUSTOMER_VALIDATED, CANCELLING}` instead of a single source state. Risk: subtle bugs if a new state is added. Mitigation: use a set-based check (`Set.of(PENDING, CUSTOMER_VALIDATED, CANCELLING).contains(status)`) and explicit error codes.
- **Currency in a single-currency POC** — Money requires a Currency object even though the POC likely uses one currency. Risk: callers must pass currency everywhere. Mitigation: this is the established contract from common. The Application Service can default the currency when creating commands.
- **No findByTrackingId tests in domain** — The repository is an interface; findByTrackingId will be tested in the infrastructure change with Testcontainers. Risk: none for the domain change; the method signature is self-explanatory.
