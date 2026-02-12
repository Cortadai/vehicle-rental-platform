## Context

The vehicle-rental-platform already has its root parent POM (from the first OpenSpec cycle). The next logical step is the `common` module — the shared kernel that all 4 services depend on. Without it, no service can define entities, aggregate roots, or value objects.

## Why

Without the common module, the 4 services cannot start. They need to inherit BaseEntity, AggregateRoot, DomainEvent, and Money from a shared location.

## What Changes

### Included

- **BaseEntity** — abstract class with identity equality
- **AggregateRoot** — extends BaseEntity, accumulates domain events
- **DomainEvent** — interface with contract (`eventId()` + `occurredOn()`), compatible with records
- **Money** — record value object with arithmetic
- **DomainException** — abstract base exception with `String errorCode` (business language, NO HTTP concepts)
- **ApiResponse** — record wrapper for REST responses
- **ApiMetadata** — record with timestamp + requestId
- **common/pom.xml** — pure Java library, zero Spring dependencies

### Excluded

- Specific exception subclasses (NotFoundException, ValidationException, etc.)
- PagedResponse and PageInfo (require Spring Data, belong in infrastructure)
- Typed ID base class (each service defines its own typed IDs as records)

## Capabilities

- `domain-base-classes` — BaseEntity, AggregateRoot, DomainEvent
- `shared-value-objects` — Money with arithmetic operations
- `domain-exceptions` — Base domain exception hierarchy
- `api-response-wrapper` — ApiResponse + ApiMetadata, framework-agnostic

## Impact

1 pom.xml + ~7 Java classes. Small but foundational module.
