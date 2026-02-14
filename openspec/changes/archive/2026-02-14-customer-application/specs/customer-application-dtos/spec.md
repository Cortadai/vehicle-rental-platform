customer-application-dtos
=========================

Purpose
-------

Command and Response records for Customer use cases. Commands carry input data for write operations. Responses carry output data for queries and creation results. All are Java records (immutable).

ADDED Requirements
------------------

### Requirement: CreateCustomerCommand carries creation data

CreateCustomerCommand SHALL be a Java record with fields: firstName (String), lastName (String), email (String), phone (String, nullable).

#### Scenario: All fields accessible

* **WHEN** a CreateCustomerCommand is constructed
* **THEN** `firstName()`, `lastName()`, `email()` SHALL return the provided values
* **AND** `phone()` SHALL return the provided value or null

### Requirement: GetCustomerCommand carries customer identity

GetCustomerCommand SHALL be a Java record with a single field: customerId (String).

#### Scenario: CustomerId accessible

* **WHEN** a GetCustomerCommand is constructed with "some-uuid"
* **THEN** `customerId()` SHALL return "some-uuid"

### Requirement: SuspendCustomerCommand carries customer identity

SuspendCustomerCommand SHALL be a Java record with a single field: customerId (String).

#### Scenario: CustomerId accessible

* **WHEN** a SuspendCustomerCommand is constructed
* **THEN** `customerId()` SHALL return the provided value

### Requirement: ActivateCustomerCommand carries customer identity

ActivateCustomerCommand SHALL be a Java record with a single field: customerId (String).

#### Scenario: CustomerId accessible

* **WHEN** an ActivateCustomerCommand is constructed
* **THEN** `customerId()` SHALL return the provided value

### Requirement: DeleteCustomerCommand carries customer identity

DeleteCustomerCommand SHALL be a Java record with a single field: customerId (String).

#### Scenario: CustomerId accessible

* **WHEN** a DeleteCustomerCommand is constructed
* **THEN** `customerId()` SHALL return the provided value

### Requirement: CustomerResponse carries customer snapshot

CustomerResponse SHALL be a Java record with fields: customerId (String), firstName (String), lastName (String), email (String), phone (String, nullable), status (String), createdAt (Instant).

#### Scenario: All fields accessible

* **WHEN** a CustomerResponse is constructed
* **THEN** all fields SHALL be accessible via record accessors

### Requirement: Commands and Responses are plain Java records

All commands and responses SHALL be Java records with no Spring annotations and no validation annotations. They SHALL have zero framework dependencies.

#### Scenario: No framework imports

* **WHEN** any command or response class is inspected
* **THEN** it SHALL NOT import any type from `org.springframework.*` or `jakarta.*`

Constraint: Zero Spring dependencies
-------------------------------------

No class in `com.vehiclerental.customer.application.dto` SHALL import any type from `org.springframework.*`.
