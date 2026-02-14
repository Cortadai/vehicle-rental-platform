## Context

The common shared kernel provides BaseEntity, AggregateRoot, DomainEvent, Money, and DomainException. This change creates the first service domain module (`customer-domain`), establishing the DDD tactical pattern that the remaining 3 services will follow. Customer is the simplest service — no SAGA coordination, no cross-service dependencies — making it ideal for validating the patterns.

The `project.md` architecture defines 3 Maven modules per service (domain, infrastructure, container). The domain module is pure Java with zero Spring dependencies. All state changes go through business methods on the Aggregate Root (no public setters). Domain events are accumulated internally and dispatched after persistence by the Application Service (which doesn't exist yet — next change).

## Goals / Non-Goals

**Goals:**
- Establish Customer Aggregate Root with full lifecycle management (create, suspend, activate, delete)
- Implement typed Value Objects (CustomerId, Email, PhoneNumber) with validation
- Define Domain Events for all lifecycle transitions
- Define the CustomerRepository output port interface
- Follow test-first approach: tests before implementation, derived from spec scenarios
- Validate that the common shared kernel works when consumed by a real service

**Non-Goals:**
- Application layer (use cases, commands, DTOs, @Transactional) — next change
- Infrastructure layer (JPA entities, REST controllers, persistence adapters) — next change
- Domain Service — all logic fits in the aggregate, no cross-aggregate coordination needed
- Input Ports (use case interfaces) — belong in application layer

## Decisions

### Decision 1: Factory methods `create()` and `reconstruct()` instead of public constructor

**Choice**: Customer exposes two static factory methods:
- `Customer.create(...)` — for new customers. Generates CustomerId, sets status ACTIVE, registers CustomerCreatedEvent.
- `Customer.reconstruct(...)` — for rehydration from persistence. Sets all fields without validation or event registration.

**Rationale**: `create()` enforces business invariants and emits domain events. `reconstruct()` is needed because JPA adapters in infrastructure will need to rebuild the domain object from database state without triggering creation logic. This follows the pattern from docs/17.

**Alternative rejected**: Public constructor with optional event flag. Rejected because it exposes internal mechanics and makes it easy to misuse.

### Decision 2: Output Port (CustomerRepository) lives in domain module

**Choice**: Place `CustomerRepository` interface in `customer-domain` under `com.vehiclerental.customer.domain.port.output`.

**Rationale**: The `project.md` architecture shows ports under `application/port/`, but we don't have an application module yet. The repository interface is a pure domain contract (no Spring types) — it defines what the domain *needs* without knowing *how* it's fulfilled. Placing it in domain keeps this change self-contained. When we create the application layer, we can either leave it here (domain depends on nothing, so it's valid) or move it to application if the port needs application-level types.

**Alternative rejected**: Wait for the application module to define the port. Rejected because the Customer aggregate's specs and tests need to reference the repository contract, and deferring it would leave an incomplete domain model.

### Decision 3: Email validation uses simple regex, not RFC 5322

**Choice**: Email validates with a practical regex (`^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$`) rather than full RFC 5322 compliance.

**Rationale**: Full RFC 5322 validation is notoriously complex and allows technically valid but practically useless addresses (e.g., `"quoted spaces"@example.com`). A practical regex catches the real errors (missing @, empty parts) without over-engineering. If stricter validation is needed later, the Email record's compact constructor is the single place to change.

**Alternative rejected**: Full RFC 5322 regex or `javax.mail.InternetAddress`. Both add complexity or dependencies for marginal benefit.

### Decision 4: PhoneNumber is nullable on Customer, validated when present

**Choice**: The `phone` field on Customer is nullable (`PhoneNumber` or `null`). When a PhoneNumber is provided, it validates format in its compact constructor.

**Rationale**: Not all customers provide a phone number. Making it nullable (vs. wrapping in Optional as a field) is the conventional Java approach for optional aggregate fields. The PhoneNumber VO validates only that the string is non-blank and contains a reasonable phone pattern — not full E.164 validation (that belongs in infrastructure/external services).

**Alternative rejected**: Optional<PhoneNumber> as field type. Records with Optional fields serialize poorly and Optional was designed for return types, not fields.

### Decision 5: CustomerStatus transitions validated by the aggregate

**Choice**: The Customer aggregate enforces valid state transitions:
- `ACTIVE → SUSPENDED` (via `suspend()`)
- `SUSPENDED → ACTIVE` (via `activate()`)
- `ACTIVE → DELETED`, `SUSPENDED → DELETED` (via `delete()`)
- `DELETED → *` (no transitions — terminal state)

Invalid transitions throw CustomerDomainException.

**Rationale**: State machine logic belongs in the aggregate, not in application services. Each transition method validates the precondition, changes state, and registers the appropriate domain event. This prevents invalid states regardless of who calls the method.

**Alternative rejected**: Separate state machine class. Over-engineering for 3 states and 4 transitions.

### Decision 6: Domain events carry business data beyond the DomainEvent contract

**Choice**: All events implement `DomainEvent` from common (which provides `eventId()` and `occurredOn()` via the interface contract). Beyond that base contract, each event carries business-specific data:
- `CustomerCreatedEvent` — adds `CustomerId`, `firstName`, `lastName`, `Email` (full snapshot for consumers)
- `CustomerSuspendedEvent` — adds only `CustomerId`
- `CustomerActivatedEvent` — adds only `CustomerId`
- `CustomerDeletedEvent` — adds only `CustomerId`

**Rationale**: Created event carries the full snapshot because consumers (like Reservation Service) may need customer details without a separate query. Lifecycle events (suspended, activated, deleted) carry only the customer ID since consumers just need to know *what happened to whom*.

**Alternative rejected**: All events carry full snapshot. Unnecessary data duplication for simple state transitions.

## Package Structure

```
customer-service/customer-domain/src/main/java/com/vehiclerental/customer/domain/
├── model/
│   ├── aggregate/
│   │   └── Customer.java                  ← Aggregate Root
│   └── vo/
│       ├── CustomerId.java                ← Typed ID (record)
│       ├── Email.java                     ← Value Object (record)
│       ├── PhoneNumber.java               ← Value Object (record)
│       └── CustomerStatus.java            ← Enum
├── event/
│   ├── CustomerCreatedEvent.java          ← Domain Event (record)
│   ├── CustomerSuspendedEvent.java        ← Domain Event (record)
│   ├── CustomerActivatedEvent.java        ← Domain Event (record)
│   └── CustomerDeletedEvent.java          ← Domain Event (record)
├── exception/
│   └── CustomerDomainException.java       ← Extends DomainException
└── port/
    └── output/
        └── CustomerRepository.java        ← Output Port (interface)
```

## Risks / Trade-offs

- **Output port in domain vs application** — Placing CustomerRepository in domain is pragmatic but non-standard per project.md. Risk: when we create the application module, we may want to reorganize. Mitigation: the interface has no framework dependencies, so moving it is a simple refactor.
- **No reconstruct() tests in this change** — `reconstruct()` is primarily tested via persistence integration tests (Testcontainers), which belong in the infrastructure change. We'll add a basic unit test for it, but full coverage comes later.
- **Email regex may be too permissive** — The practical regex accepts some invalid addresses. Mitigation: email verification (sending a confirmation) is the only reliable validation anyway. The regex catches obvious errors.
