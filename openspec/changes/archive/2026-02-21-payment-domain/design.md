## Context

Three domain modules are complete: Customer (58 tests, 3 states), Fleet (50 tests, 3 states), and Reservation (80 tests, 6 states with inner entity). The common shared kernel (`BaseEntity`, `AggregateRoot`, `DomainEvent`, `Money`, `DomainException`) is consumed as-is by all three — no changes needed. The DDD tactical patterns are fully established.

Payment is the fourth and final bounded context. Its domain is structurally simpler than Reservation's (no inner entities, no 6-state machine, flat aggregate) but introduces a concept absent from the other three: **controlled state transitions where only specific paths are valid** (PENDING→COMPLETED, PENDING→FAILED, COMPLETED→REFUNDED) with each transition emitting its own domain event. Unlike Customer and Fleet where lifecycle transitions are symmetric (suspend↔activate), Payment's transitions are asymmetric and final — a completed payment can only be refunded, never re-completed or re-failed.

Payment also has cross-context references (`ReservationId`, `CustomerId`) following the same bounded-context isolation pattern established in Reservation.

## Goals / Non-Goals

**Goals:**

- Establish Payment Aggregate Root with asymmetric state transitions (complete, fail, refund)
- Implement typed Value Objects (PaymentId, ReservationId, CustomerId) as local cross-context records
- Define 3 Domain Events, one per transition (PaymentCompletedEvent, PaymentFailedEvent, PaymentRefundedEvent)
- Reuse Money from common for the payment amount
- Define the PaymentRepository output port with findByReservationId for application-layer idempotency
- Follow test-first approach derived from spec scenarios
- Validate that the shared kernel continues to work without modification for a fourth service

**Non-Goals:**

- Application layer (use cases, commands, DTOs, `@Transactional`) — separate change
- Infrastructure layer (JPA entities, REST controllers, persistence adapters) — separate change
- Actual payment gateway integration — charges are simulated; real gateway is out of scope
- SAGA step interface implementation — belongs in the SAGA orchestration change
- Domain Service — all logic fits within the Payment aggregate
- Input Ports — belong in application layer
- Idempotency enforcement — application-layer concern using findByReservationId before create()

## Decisions

### Decision 1: Factory methods `create()` and `reconstruct()` — same pattern, less complexity

**Choice**: Payment exposes two static factory methods:
- `Payment.create(reservationId, customerId, amount)` — generates PaymentId, sets status PENDING, registers no event (payment hasn't happened yet, just initialized).
- `Payment.reconstruct(id, reservationId, customerId, amount, status, createdAt)` — rehydration from persistence, no validation, no events.

**Rationale**: Identical pattern to Customer, Fleet, and Reservation. Unlike Reservation's create (which registers ReservationCreatedEvent), Payment's create does NOT emit an event — a pending payment is not a business fact worth communicating. The meaningful events happen on transitions (complete, fail, refund).

**Alternative rejected**: Emitting a `PaymentCreatedEvent` on create. A pending payment is not a business event — it's the SAGA orchestrator telling Payment to prepare. The events that matter are the outcomes (completed, failed, refunded).

### Decision 2: PaymentStatus transitions — asymmetric and final

**Choice**: The Payment aggregate enforces valid state transitions:
- `PENDING → COMPLETED` (via `complete()`) — registers PaymentCompletedEvent
- `PENDING → FAILED` (via `fail()`) — registers PaymentFailedEvent
- `COMPLETED → REFUNDED` (via `refund()`) — registers PaymentRefundedEvent

Invalid transitions throw `PaymentDomainException` with error code `"PAYMENT_INVALID_STATE"`. All terminal states (COMPLETED, FAILED, REFUNDED) have no outgoing transitions except COMPLETED→REFUNDED.

**Rationale**: This maps to the SAGA flow from `project.md`:
- Payment Service charges → `PaymentCompletedEvent` (happy path step 5)
- Payment fails → `PaymentFailedEvent` (CUSTOMER_VALIDATED → CANCELLED)
- Fleet unavailable after payment → refund → `PaymentRefundedEvent` (compensation)

Unlike Customer/Fleet where transitions are bidirectional (suspend↔activate, maintenance↔active), payment transitions are one-way. A failed payment cannot be retried (the SAGA creates a new Payment instead). A refunded payment cannot be un-refunded.

**Alternative rejected**: Adding a RETRY state. Retry logic belongs in the SAGA orchestrator, not the Payment aggregate. If the orchestrator wants to retry, it creates a new Payment.

### Decision 3: Cross-context IDs (ReservationId, CustomerId) local to payment

**Choice**: `ReservationId` and `CustomerId` are defined as record value objects within `payment-domain`, not imported from `reservation-domain` or `customer-domain`.

**Rationale**: Same bounded-context isolation as Reservation's local `CustomerId` and `VehicleId`. Importing would create Maven-level coupling between services. Payment's `ReservationId` is "the UUID that identifies which reservation this payment belongs to" — it validates not-null, nothing else.

**Alternative rejected**: Importing from other domains. Same reasoning as Reservation's Decision 3. Each bounded context owns its representation.

### Decision 4: Three domain events — one per transition, none on create

**Choice**: Each state transition registers exactly one domain event:
- `PaymentCompletedEvent(eventId, occurredOn, paymentId, reservationId, customerId, amount)` — full snapshot, the SAGA needs all fields
- `PaymentFailedEvent(eventId, occurredOn, paymentId, reservationId, failureMessages)` — carries failure reasons
- `PaymentRefundedEvent(eventId, occurredOn, paymentId, reservationId, amount)` — carries refunded amount

No event on `create()` (PENDING is not a business fact).

**Rationale**: Each event maps 1:1 to a SAGA step outcome. `PaymentCompletedEvent` carries the full snapshot because consumers (Reservation orchestrator) need paymentId, reservationId, and amount for correlation. `PaymentFailedEvent` carries failure messages — same pattern as `ReservationCancelledEvent`. `PaymentRefundedEvent` carries the refunded amount for audit.

**Alternative rejected**: A single `PaymentStatusChangedEvent` with a status field. Consumers would need to switch on status, losing type safety. Separate event types allow type-safe handling.

### Decision 5: failureMessages on fail() — same pattern as Reservation's cancel()

**Choice**: `fail(List<String> failureMessages)` accepts a list of failure reasons that get stored on the aggregate and carried in the `PaymentFailedEvent`.

**Rationale**: Same pattern as `Reservation.initCancel(failureMessages)`. The failure messages come from the payment processing logic (which lives in the Application Service or a simulated gateway). They are free-form strings for diagnostic purposes.

**Alternative rejected**: A single `failureReason` string. Multiple failure reasons can occur (e.g., "Card declined" + "Insufficient funds"). A list is more flexible.

### Decision 6: PaymentRepository with findByReservationId

**Choice**: `PaymentRepository` interface in domain with three methods: `save(Payment)`, `findById(PaymentId)`, and `findByReservationId(ReservationId)`.

**Rationale**: `findByReservationId` exists for application-layer idempotency: the Application Service will call it before `Payment.create()` to check if a payment already exists for the reservation. This method returns `Optional<Payment>` — a reservation has at most one active payment. The domain module provides the port interface; the application module uses it; the infrastructure module implements it.

**Alternative rejected**: Only `findById`. Would prevent idempotency checks without maintaining a separate ReservationId→PaymentId mapping.

### Decision 7: Money reused from common — no wrapper (unlike Fleet's DailyRate)

**Choice**: `Money` from common is used directly as the `amount` field. No wrapper like Fleet's `DailyRate`.

**Rationale**: `DailyRate` adds a business constraint ("must be strictly positive") on top of Money's "non-negative" rule. Payment's amount is simply a positive Money — but "positive" is a validation in `create()`, not a separate type. A `PaymentAmount` wrapper would add a class for a single validation (`amount.signum() > 0`) that only applies at creation time. The aggregate validates amount directly.

**Alternative rejected**: `PaymentAmount` wrapping Money. Unlike DailyRate which is used in calculations (dailyRate × days = subtotal), Payment's amount is just stored and carried in events — no domain operations on it justify a wrapper.

## Package Structure

```
payment-service/payment-domain/src/main/java/com/vehiclerental/payment/domain/
├── model/
│   ├── aggregate/
│   │   └── Payment.java                     ← Aggregate Root (create/reconstruct, complete/fail/refund)
│   └── vo/
│       ├── PaymentId.java                   ← Typed ID (record, UUID)
│       ├── ReservationId.java               ← Cross-context ID (record, UUID, local)
│       ├── CustomerId.java                  ← Cross-context ID (record, UUID, local)
│       └── PaymentStatus.java               ← Enum (PENDING, COMPLETED, FAILED, REFUNDED)
├── event/
│   ├── PaymentCompletedEvent.java           ← Domain Event (record, full snapshot)
│   ├── PaymentFailedEvent.java              ← Domain Event (record, with failureMessages)
│   └── PaymentRefundedEvent.java            ← Domain Event (record, with amount)
├── exception/
│   └── PaymentDomainException.java          ← Extends DomainException (error codes)
└── port/
    └── output/
        └── PaymentRepository.java           ← Output Port (save, findById, findByReservationId)
```

## Risks / Trade-offs

- **No event on create()** — If a consumer needs to know "a payment was initialized", there is no domain event to listen to. Mitigation: the SAGA orchestrator knows when it initiates a payment (it sent the command). A `PaymentCreatedEvent` can be added later if needed — it's a backwards-compatible addition.
- **failureMessages as `List<String>`** — Same trade-off as Reservation. No structure beyond non-null strings. Mitigation: failure messages are diagnostic, not queryable. A structured type can be added if audit requirements grow.
- **No DailyRate-style wrapper for amount** — The amount is not validated as a separate type. Risk: a zero-amount Payment could be created if the create() validation is weak. Mitigation: `create()` explicitly validates `amount.signum() > 0` and throws `PaymentDomainException("PAYMENT_AMOUNT_INVALID")`.
- **findByReservationId returns Optional, not List** — Assumes one payment per reservation. Risk: if the business allows multiple payments (partial, retry), this would need to change to `List<Payment>`. Mitigation: the SAGA flow creates one payment per reservation attempt. If the payment fails, the SAGA creates a new reservation (and thus a new payment), not a second payment for the same reservation.
