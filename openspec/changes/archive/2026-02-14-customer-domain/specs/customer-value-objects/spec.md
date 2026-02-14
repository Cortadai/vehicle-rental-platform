customer-value-objects
======================

Purpose
-------

Typed ID (CustomerId), Email with format validation, PhoneNumber with optional validation, and CustomerStatus enum for the Customer domain.
ADDED Requirements
------------------

### Requirement: CustomerId is a typed ID

CustomerId SHALL be a Java record wrapping a UUID. It SHALL reject null UUID values.

#### Scenario: Valid CustomerId construction

* **WHEN** a CustomerId is created with a non-null UUID
* **THEN** the `value()` accessor SHALL return that UUID

#### Scenario: Null UUID rejected

* **WHEN** a CustomerId is created with a null UUID
* **THEN** it SHALL throw CustomerDomainException

#### Scenario: Equality by value

* **WHEN** two CustomerId instances are created with the same UUID
* **THEN** they SHALL be equal

### Requirement: Email validates format

Email SHALL be a Java record wrapping a String value. It SHALL validate that the value matches a practical email format (contains @ with non-empty local and domain parts).

#### Scenario: Valid email accepted

* **WHEN** an Email is created with "john@example.com"
* **THEN** the `value()` accessor SHALL return "john@example.com"

#### Scenario: Email with subdomains accepted

* **WHEN** an Email is created with "user@mail.example.com"
* **THEN** it SHALL be created successfully

#### Scenario: Null value rejected

* **WHEN** an Email is created with a null value
* **THEN** it SHALL throw CustomerDomainException

#### Scenario: Blank value rejected

* **WHEN** an Email is created with "" or " "
* **THEN** it SHALL throw CustomerDomainException

#### Scenario: Missing @ rejected

* **WHEN** an Email is created with "johnexample.com"
* **THEN** it SHALL throw CustomerDomainException

#### Scenario: Missing domain rejected

* **WHEN** an Email is created with "john@"
* **THEN** it SHALL throw CustomerDomainException

#### Scenario: Missing local part rejected

* **WHEN** an Email is created with "@example.com"
* **THEN** it SHALL throw CustomerDomainException

### Requirement: PhoneNumber validates format

PhoneNumber SHALL be a Java record wrapping a String value. It SHALL validate that the value is non-null, non-blank, between 3 and 20 characters, and contains only digits, spaces, hyphens, parentheses, and an optional leading +.

#### Scenario: Valid phone number accepted

* **WHEN** a PhoneNumber is created with "+1-555-123-4567"
* **THEN** the `value()` accessor SHALL return "+1-555-123-4567"

#### Scenario: Phone with parentheses accepted

* **WHEN** a PhoneNumber is created with "(555) 123-4567"
* **THEN** it SHALL be created successfully

#### Scenario: Null value rejected

* **WHEN** a PhoneNumber is created with a null value
* **THEN** it SHALL throw CustomerDomainException

#### Scenario: Blank value rejected

* **WHEN** a PhoneNumber is created with "" or " "
* **THEN** it SHALL throw CustomerDomainException

#### Scenario: Too short rejected

* **WHEN** a PhoneNumber is created with "12"
* **THEN** it SHALL throw CustomerDomainException

#### Scenario: Too long rejected

* **WHEN** a PhoneNumber is created with a value longer than 20 characters
* **THEN** it SHALL throw CustomerDomainException

#### Scenario: Letters rejected

* **WHEN** a PhoneNumber is created with "555-ABC-1234"
* **THEN** it SHALL throw CustomerDomainException

### Requirement: CustomerStatus enum

CustomerStatus SHALL be an enum with values ACTIVE, SUSPENDED, and DELETED.

#### Scenario: All statuses exist

* **WHEN** CustomerStatus values are listed
* **THEN** they SHALL contain exactly ACTIVE, SUSPENDED, and DELETED

Constraint: Zero Spring dependencies
------------------------------------

No class in `com.vehiclerental.customer.domain.model.vo` SHALL import any type from `org.springframework.*`.
