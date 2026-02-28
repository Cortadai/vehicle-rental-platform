## ADDED Requirements

### Requirement: CustomerValidatedEvent carries validation success with SAGA correlation

CustomerValidatedEvent SHALL be a Java record in `com.vehiclerental.customer.domain.event` implementing `DomainEvent`. Beyond the base contract (eventId, occurredOn), it SHALL carry `customerId` (CustomerId) and `reservationId` (UUID). The reservationId uses raw `java.util.UUID` (not a typed ID) to avoid cross-domain dependency.

#### Scenario: Validated event has all fields

- **WHEN** a CustomerValidatedEvent is constructed
- **THEN** `eventId()` SHALL return a non-null UUID
- **AND** `occurredOn()` SHALL return a non-null Instant
- **AND** `customerId()` SHALL return the customer's CustomerId
- **AND** `reservationId()` SHALL return the reservation's UUID for SAGA correlation

#### Scenario: Null eventId rejected

- **WHEN** a CustomerValidatedEvent is constructed with a null eventId
- **THEN** it SHALL throw CustomerDomainException

#### Scenario: Null occurredOn rejected

- **WHEN** a CustomerValidatedEvent is constructed with a null occurredOn
- **THEN** it SHALL throw CustomerDomainException

### Requirement: CustomerRejectedEvent carries validation failure with SAGA correlation

CustomerRejectedEvent SHALL be a Java record in `com.vehiclerental.customer.domain.event` implementing `DomainEvent`. Beyond the base contract (eventId, occurredOn), it SHALL carry `customerId` (CustomerId), `reservationId` (UUID), and `failureMessages` (List<String>). The naming `Rejected` is consistent with the FleetRejectedEvent convention and enables routing key auto-derivation to `customer.rejected`.

#### Scenario: Rejected event has all fields

- **WHEN** a CustomerRejectedEvent is constructed
- **THEN** `eventId()` SHALL return a non-null UUID
- **AND** `occurredOn()` SHALL return a non-null Instant
- **AND** `customerId()` SHALL return the customer's CustomerId
- **AND** `reservationId()` SHALL return the reservation's UUID for SAGA correlation
- **AND** `failureMessages()` SHALL return the list of failure reason strings

#### Scenario: Null eventId rejected

- **WHEN** a CustomerRejectedEvent is constructed with a null eventId
- **THEN** it SHALL throw CustomerDomainException

#### Scenario: Null occurredOn rejected

- **WHEN** a CustomerRejectedEvent is constructed with a null occurredOn
- **THEN** it SHALL throw CustomerDomainException

### Requirement: SAGA events have zero Spring dependencies

CustomerValidatedEvent and CustomerRejectedEvent SHALL NOT import any type from `org.springframework.*`. They SHALL reside in the domain layer alongside the 4 existing lifecycle events.

#### Scenario: No Spring imports in SAGA events

- **WHEN** the imports of CustomerValidatedEvent and CustomerRejectedEvent are inspected
- **THEN** neither SHALL import any type from `org.springframework.*`
