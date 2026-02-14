customer-domain-events
======================

Purpose
-------

Event records for all Customer lifecycle transitions (created, suspended, activated, deleted). All events implement the DomainEvent interface from common, satisfying the `eventId()` and `occurredOn()` contract via record accessors.
ADDED Requirements
------------------

### Requirement: CustomerCreatedEvent carries creation snapshot

CustomerCreatedEvent SHALL be a Java record implementing DomainEvent. Beyond the base contract (eventId, occurredOn), it SHALL carry customerId (CustomerId), firstName (String), lastName (String), and email (Email).

#### Scenario: Created event has all fields

* **WHEN** a CustomerCreatedEvent is constructed
* **THEN** `eventId()` SHALL return a non-null UUID
* **AND** `occurredOn()` SHALL return a non-null Instant
* **AND** `customerId()` SHALL return the customer's CustomerId
* **AND** `firstName()` SHALL return the customer's first name
* **AND** `lastName()` SHALL return the customer's last name
* **AND** `email()` SHALL return the customer's Email

#### Scenario: Null eventId rejected

* **WHEN** a CustomerCreatedEvent is constructed with a null eventId
* **THEN** it SHALL throw CustomerDomainException

#### Scenario: Null occurredOn rejected

* **WHEN** a CustomerCreatedEvent is constructed with a null occurredOn
* **THEN** it SHALL throw CustomerDomainException

### Requirement: CustomerSuspendedEvent carries customer identity

CustomerSuspendedEvent SHALL be a Java record implementing DomainEvent. Beyond the base contract, it SHALL carry only customerId (CustomerId).

#### Scenario: Suspended event has customer identity

* **WHEN** a CustomerSuspendedEvent is constructed
* **THEN** `customerId()` SHALL return the customer's CustomerId
* **AND** `eventId()` and `occurredOn()` SHALL be non-null

### Requirement: CustomerActivatedEvent carries customer identity

CustomerActivatedEvent SHALL be a Java record implementing DomainEvent. Beyond the base contract, it SHALL carry only customerId (CustomerId).

#### Scenario: Activated event has customer identity

* **WHEN** a CustomerActivatedEvent is constructed
* **THEN** `customerId()` SHALL return the customer's CustomerId
* **AND** `eventId()` and `occurredOn()` SHALL be non-null

### Requirement: CustomerDeletedEvent carries customer identity

CustomerDeletedEvent SHALL be a Java record implementing DomainEvent. Beyond the base contract, it SHALL carry only customerId (CustomerId).

#### Scenario: Deleted event has customer identity

* **WHEN** a CustomerDeletedEvent is constructed
* **THEN** `customerId()` SHALL return the customer's CustomerId
* **AND** `eventId()` and `occurredOn()` SHALL be non-null

### Requirement: All events implement DomainEvent interface

All customer domain events SHALL implement `com.vehiclerental.common.domain.event.DomainEvent`.

#### Scenario: Events satisfy DomainEvent contract

* **WHEN** any customer event record is checked
* **THEN** it SHALL be an instance of DomainEvent

Constraint: Zero Spring dependencies
------------------------------------

No class in `com.vehiclerental.customer.domain.event` SHALL import any type from `org.springframework.*`.
