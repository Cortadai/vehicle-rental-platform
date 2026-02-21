## Why

Three of the four services are complete through their walking skeletons (Customer, Fleet, Reservation). Payment Service is the last bounded context needed before SAGA orchestration can begin. Its domain layer is simpler than Reservation's (no inner entities, no 6-state machine) but introduces a concept absent from Customer and Fleet: **idempotency** — a payment must not be processed twice for the same reservation. Building the domain first follows the established pattern: domain → application → infrastructure+container.

## What Changes

- New Maven module `payment-service/payment-domain` with dependency on `common` only
- Aggregate Root `Payment` extending `AggregateRoot<PaymentId>` from common, with factory methods `create()` and `reconstruct()`, state transitions (complete, fail, refund) and idempotency enforcement
- 3 local Value Objects as records: `PaymentId` (typed UUID), `ReservationId` (cross-context, local to this bounded context — not imported from reservation-domain), `CustomerId` (cross-context, local)
- Enum `PaymentStatus` with 4 states: PENDING, COMPLETED, FAILED, REFUNDED
- `Money` reused from common (BigDecimal + Currency), not redefined locally
- 3 Domain Events as records implementing `DomainEvent` interface from common: `PaymentCompletedEvent`, `PaymentFailedEvent`, `PaymentRefundedEvent`
- `PaymentDomainException extends DomainException` — single exception class with error codes, following Customer/Fleet/Reservation pattern
- Output port `PaymentRepository` interface in domain (save, findById, findByReservationId)
- Domain invariants: amount must be positive, state transitions controlled (only PENDING can complete or fail, only COMPLETED can be refunded). Idempotency (reject duplicate payments for the same reservation) is an application-layer orchestration concern using findByReservationId, not an aggregate-internal rule
- Test-First unit tests for aggregate, value objects, and domain events
- Root POM updated: module declaration + dependencyManagement entry for `payment-domain`

### Excluded

- Application layer (use cases, commands, DTOs) — separate change `payment-application`
- Infrastructure layer (JPA, REST, Flyway) — separate change `payment-infrastructure-and-container`
- Actual payment gateway integration — Payment processes simulated charges; real gateway is out of scope for this POC
- SAGA step interface implementation — belongs in the SAGA orchestration change
- Domain Service — not needed, all business logic fits within the Payment aggregate

## Capabilities

### New Capabilities
- `payment-aggregate`: Payment Aggregate Root with create/reconstruct factory methods, state transitions (complete, fail, refund), domain invariants (positive amount, controlled transitions), and domain event registration. Note: idempotency (no duplicate payments per reservation) is an application-layer concern — the Application Service will query findByReservationId before calling create()
- `payment-value-objects`: PaymentId (typed UUID), ReservationId and CustomerId (cross-context typed IDs local to the payment bounded context), PaymentStatus enum with allowed transitions
- `payment-domain-events`: PaymentCompletedEvent, PaymentFailedEvent, PaymentRefundedEvent — all records implementing DomainEvent interface from common
- `payment-repository-port`: PaymentRepository output port interface with save, findById, and findByReservationId operations

### Modified Capabilities
- `multi-module-build`: Root POM adds `payment-service/payment-domain` module and dependencyManagement entry

## Impact

- **New module**: `payment-service/payment-domain/` with `pom.xml` depending only on `common`
- **Root POM**: New module declaration and dependencyManagement entry
- **No changes to existing code**: common, customer-service, fleet-service, and reservation-service remain untouched
- **Test count increase**: ~40-55 new domain unit tests (estimated — simpler than Reservation's 80 but richer than Customer's 58 due to idempotency rules)
- **Build chain**: `mvn install -N` → `common` → `payment-domain` (same incremental pattern)
- **Stub POMs**: The existing `payment-domain`, `payment-infrastructure`, and `payment-container` stub POMs will be replaced with real content incrementally across this and future changes
