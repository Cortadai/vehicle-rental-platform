## Context

The common module is the shared kernel for 4 microservices following hexagonal architecture with DDD tactical patterns. All classes must be pure Java — zero Spring dependencies.

## Goals / Non-Goals

**Goals:**
- Establish DDD tactical base classes (BaseEntity, AggregateRoot, DomainEvent)
- Provide shared value objects (Money) and domain exception base
- Provide framework-agnostic API response wrapper
- Ensure zero Spring dependencies in the module

**Non-Goals:**
- Specific exception subclasses (future change when services need them)
- PagedResponse (requires Spring Data concepts)
- Typed ID base class (each service defines its own)

## Decisions

### Decision 1: Zero Spring in common — String errorCode instead of HttpStatus

**Choice**: Domain exceptions use `String errorCode` (e.g., "CUSTOMER_NOT_FOUND", "INSUFFICIENT_FUNDS") instead of `HttpStatus` or `int statusCode`.

**Rationale**: The domain speaks business language, not HTTP protocol. A 404 means nothing to the domain — "customer not found" does. The GlobalExceptionHandler in infrastructure maps errorCode to HttpStatus. This keeps common 100% pure Java.

**Alternative rejected**: Using `HttpStatus` directly (docs/03) or `int statusCode`. Both leak HTTP concepts into the domain, violating hexagonal isolation.

### Decision 2: Reduced scope (~8 classes instead of ~13)

**Choice**: Only implement DomainException base without concrete subclasses (NotFoundException, etc.) and without PagedResponse.

**Rationale**: This is a learning POC focused on the OpenSpec workflow. Fewer classes = faster cycle = more focus on the process. Subclasses and PagedResponse are natural extensions for future changes when a real service needs them.

**Alternative rejected**: Full scope (~13 classes). Rejected for extending the cycle without adding value to the learning goal.

### Decision 3: Single module (not split common-domain + common-api)

**Choice**: Everything in a single `common/` module with separate packages (`domain/*` and `api/*`).

**Rationale**: For a POC, package separation is sufficient for conceptual clarity. Splitting into two Maven sub-modules adds build complexity without real benefit with ~8 classes. The package structure (`com.vehiclerental.common.domain.*` vs `com.vehiclerental.common.api.*`) already communicates intent.

**Alternative rejected**: Split into `common-domain` + `common-api`. Rejected because ApiResponse doesn't need Spring — it's a pure Java record.

### Decision 4: DomainEvent as interface with contract (not abstract class)

**Choice**: DomainEvent is an interface with methods `eventId()` and `occurredOn()`, not an abstract class.

**Rationale**: Concrete domain events will be immutable records. Records cannot extend classes but can implement interfaces. With `interface DomainEvent { UUID eventId(); Instant occurredOn(); }`, each service defines its events as `record ReservationCreatedEvent(UUID eventId, Instant occurredOn, ...) implements DomainEvent {}` — the record accessors satisfy the interface automatically.

**Alternative rejected**: Abstract class with common fields (docs/17). Rejected because it forces class inheritance, incompatible with Java records.

### Decision 5: Records for Value Objects and API responses

**Choice**: Money, ApiResponse, and ApiMetadata are implemented as Java records.

**Rationale**: Immutable by design, automatic equals/hashCode/toString, compact constructor for validation. Jackson 2.12+ serializes/deserializes records without annotations, maintaining zero framework dependencies.

**Alternative rejected**: Classes with Lombok `@Value`. Rejected because records are Java 16+'s native solution for immutable data.

### Decision 6: Test-First for domain classes

**Choice**: Write tests BEFORE implementing BaseEntity, AggregateRoot, and Money.

**Rationale**: CLAUDE.md defines that domain layer (40% of testing) follows Test-First approach. WHEN/THEN scenarios from specs translate directly to test cases before implementation.

## Package Structure

```
common/src/main/java/com/vehiclerental/common/
├── domain/
│   ├── entity/
│   │   ├── BaseEntity.java
│   │   └── AggregateRoot.java
│   ├── event/
│   │   └── DomainEvent.java
│   ├── vo/
│   │   └── Money.java
│   └── exception/
│       └── DomainException.java
└── api/
    ├── ApiResponse.java
    └── ApiMetadata.java
```

## Risks / Trade-offs

- **Zero Spring means no `@Valid` annotations** — Validation happens in compact constructors (records) or explicit checks (abstract classes). Infrastructure layer adds Bean Validation if needed.
- **String errorCode requires mapping** — The GlobalExceptionHandler must map business error codes to HTTP status codes. This is a small cost for proper domain isolation.
- **DomainEvent interface with contract** — Concrete events must include `eventId` and `occurredOn` fields. This is a minor constraint that ensures consistent event metadata.
