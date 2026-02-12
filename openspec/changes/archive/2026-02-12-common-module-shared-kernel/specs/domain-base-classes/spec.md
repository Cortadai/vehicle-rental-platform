# domain-base-classes

## Purpose

Provides DDD tactical base classes for all microservice domain layers: identity equality for entities, domain event accumulation for aggregate roots, and a record-compatible event contract.

## Requirement: BaseEntity identity equality

BaseEntity SHALL implement equals/hashCode based exclusively on the entity's ID field, not on other attributes.

### Scenario: Same non-null ID means equal

- WHEN two BaseEntity instances have the same non-null ID
- THEN equals() SHALL return true
- AND hashCode() SHALL return the same value

### Scenario: Different IDs means not equal

- WHEN two BaseEntity instances have different IDs
- THEN equals() SHALL return false

### Scenario: Null ID means reference equality only

- WHEN a BaseEntity has a null ID
- THEN equals() SHALL return true only when compared to itself (same reference)
- AND equals() SHALL return false when compared to another BaseEntity with null ID

### Scenario: Null ID produces constant hashCode

- WHEN a BaseEntity has a null ID
- THEN hashCode() SHALL return a constant value (to remain stable in hash-based collections)

### Scenario: Comparison with null or different type

- WHEN a BaseEntity is compared via equals() to null or a different class
- THEN it SHALL return false

## Requirement: BaseEntity is abstract

BaseEntity SHALL be an abstract generic class `BaseEntity<ID>` that cannot be instantiated directly.

## Requirement: AggregateRoot event accumulation

AggregateRoot SHALL extend BaseEntity and provide methods to register, retrieve, and clear domain events.

### Scenario: Register a single event

- WHEN `registerDomainEvent()` is called with a DomainEvent
- THEN `getDomainEvents()` SHALL return a list containing that event

### Scenario: Multiple events preserve order

- WHEN `registerDomainEvent()` is called with events A, B, C in sequence
- THEN `getDomainEvents()` SHALL return [A, B, C] in registration order

### Scenario: Defensive copy on getDomainEvents

- WHEN a caller modifies the list returned by `getDomainEvents()`
- THEN the aggregate's internal event list SHALL NOT be affected

### Scenario: Clear events

- WHEN `clearDomainEvents()` is called
- THEN `getDomainEvents()` SHALL return an empty list

## Requirement: AggregateRoot is abstract

AggregateRoot SHALL be an abstract generic class `AggregateRoot<ID>` that extends `BaseEntity<ID>`.

## Requirement: DomainEvent interface contract

DomainEvent SHALL be an interface declaring `UUID eventId()` and `Instant occurredOn()`.

### Scenario: Record satisfies contract

- WHEN a Java record declares fields `eventId` (UUID) and `occurredOn` (Instant) and implements DomainEvent
- THEN the record's accessor methods SHALL satisfy the interface automatically

### Scenario: Null eventId rejected

- WHEN a record implementing DomainEvent is constructed with a null eventId
- THEN the compact constructor SHALL throw NullPointerException or IllegalArgumentException

### Scenario: Null occurredOn rejected

- WHEN a record implementing DomainEvent is constructed with a null occurredOn
- THEN the compact constructor SHALL throw NullPointerException or IllegalArgumentException

### Scenario: Event can be registered in AggregateRoot

- WHEN a record implementing DomainEvent is passed to `registerDomainEvent()`
- THEN it SHALL appear in `getDomainEvents()`

## Constraint: Zero Spring dependencies

No class in the `com.vehiclerental.common.domain` package SHALL import any type from `org.springframework.*`.
