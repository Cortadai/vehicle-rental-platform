# customer-aggregate Specification

## Purpose
Customer Aggregate Root with lifecycle management, identity by CustomerId, and domain event emission. Encapsulates all state changes through business methods with precondition validation.

## Requirements
### Requirement: Customer is an Aggregate Root

Customer SHALL be a concrete class extending `AggregateRoot<CustomerId>`. It SHALL hold firstName, lastName, email (Email VO), phone (PhoneNumber VO, nullable), status (CustomerStatus enum), and createdAt (Instant).

#### Scenario: Customer fields accessible after creation

* **WHEN** a Customer is created with firstName "John", lastName "Doe", email "john@example.com"
* **THEN** `getFirstName()` SHALL return "John"
* **AND** `getLastName()` SHALL return "Doe"
* **AND** `getEmail()` SHALL return an Email with value "john@example.com"
* **AND** `getCreatedAt()` SHALL return a non-null Instant

### Requirement: Customer creation via factory method

Customer SHALL expose a static `create(String firstName, String lastName, Email email, PhoneNumber phone)` factory method. It SHALL generate a new CustomerId, set status to ACTIVE, set createdAt, and register a CustomerCreatedEvent.

#### Scenario: Successful creation

* **WHEN** `Customer.create("John", "Doe", email, phone)` is called with valid arguments
* **THEN** the Customer SHALL have a non-null CustomerId
* **AND** the status SHALL be ACTIVE
* **AND** `getDomainEvents()` SHALL contain exactly one CustomerCreatedEvent

#### Scenario: Null firstName rejected

* **WHEN** `Customer.create(null, "Doe", email, phone)` is called
* **THEN** it SHALL throw CustomerDomainException

#### Scenario: Blank firstName rejected

* **WHEN** `Customer.create("", "Doe", email, phone)` is called
* **THEN** it SHALL throw CustomerDomainException

#### Scenario: Null lastName rejected

* **WHEN** `Customer.create("John", null, email, phone)` is called
* **THEN** it SHALL throw CustomerDomainException

#### Scenario: Null email rejected

* **WHEN** `Customer.create("John", "Doe", null, phone)` is called
* **THEN** it SHALL throw CustomerDomainException

#### Scenario: Null phone accepted

* **WHEN** `Customer.create("John", "Doe", email, null)` is called
* **THEN** the Customer SHALL be created with phone as null

### Requirement: Customer reconstruction from persistence

Customer SHALL expose a static `reconstruct(...)` factory method that rebuilds the aggregate from persisted state without generating events or running creation validations.

#### Scenario: Reconstruct does not emit events

* **WHEN** `Customer.reconstruct(...)` is called with valid persisted data
* **THEN** `getDomainEvents()` SHALL return an empty list
* **AND** all fields SHALL match the provided arguments

### Requirement: No public constructors

Customer SHALL NOT have public constructors. Instantiation SHALL only be possible through `create()` or `reconstruct()`.

#### Scenario: Constructor is not public

* **WHEN** the Customer class is inspected
* **THEN** it SHALL have no public constructors

### Requirement: Suspend lifecycle transition

Customer SHALL expose a `suspend()` method that transitions from ACTIVE to SUSPENDED and registers a CustomerSuspendedEvent.

#### Scenario: Suspend an active customer

* **WHEN** `suspend()` is called on a Customer with status ACTIVE
* **THEN** the status SHALL change to SUSPENDED
* **AND** `getDomainEvents()` SHALL contain a CustomerSuspendedEvent

#### Scenario: Suspend a non-active customer

* **WHEN** `suspend()` is called on a Customer with status SUSPENDED or DELETED
* **THEN** it SHALL throw CustomerDomainException

### Requirement: Activate lifecycle transition

Customer SHALL expose an `activate()` method that transitions from SUSPENDED to ACTIVE and registers a CustomerActivatedEvent.

#### Scenario: Activate a suspended customer

* **WHEN** `activate()` is called on a Customer with status SUSPENDED
* **THEN** the status SHALL change to ACTIVE
* **AND** `getDomainEvents()` SHALL contain a CustomerActivatedEvent

#### Scenario: Activate a non-suspended customer

* **WHEN** `activate()` is called on a Customer with status ACTIVE or DELETED
* **THEN** it SHALL throw CustomerDomainException

### Requirement: Delete lifecycle transition

Customer SHALL expose a `delete()` method that transitions from ACTIVE or SUSPENDED to DELETED and registers a CustomerDeletedEvent.

#### Scenario: Delete an active customer

* **WHEN** `delete()` is called on a Customer with status ACTIVE
* **THEN** the status SHALL change to DELETED
* **AND** `getDomainEvents()` SHALL contain a CustomerDeletedEvent

#### Scenario: Delete a suspended customer

* **WHEN** `delete()` is called on a Customer with status SUSPENDED
* **THEN** the status SHALL change to DELETED
* **AND** `getDomainEvents()` SHALL contain a CustomerDeletedEvent

#### Scenario: Delete an already deleted customer

* **WHEN** `delete()` is called on a Customer with status DELETED
* **THEN** it SHALL throw CustomerDomainException

### Requirement: Active status check

Customer SHALL expose an `isActive()` method that returns true only when status is ACTIVE.

#### Scenario: Active customer returns true

* **WHEN** `isActive()` is called on a Customer with status ACTIVE
* **THEN** it SHALL return true

#### Scenario: Non-active customer returns false

* **WHEN** `isActive()` is called on a Customer with status SUSPENDED or DELETED
* **THEN** it SHALL return false

### Requirement: CustomerDomainException extends DomainException with errorCode

CustomerDomainException SHALL be a concrete class extending `DomainException` from common. It SHALL provide constructors that require a business-language errorCode (e.g., "CUSTOMER_INVALID_STATE", "CUSTOMER_ALREADY_DELETED").

#### Scenario: Exception carries errorCode

* **WHEN** a lifecycle transition throws CustomerDomainException
* **THEN** `getErrorCode()` SHALL return a non-blank business error code
* **AND** `getMessage()` SHALL return a descriptive message

#### Scenario: Invalid transition includes current state in message

* **WHEN** `suspend()` is called on a DELETED customer
* **THEN** the exception message SHALL indicate the current state that prevented the transition

## Constraint: Zero Spring dependencies

No class in `com.vehiclerental.customer.domain` SHALL import any type from `org.springframework.*`.
