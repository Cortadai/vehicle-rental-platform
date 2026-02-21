# payment-value-objects Specification

## Purpose
PaymentId (typed UUID), ReservationId and CustomerId (cross-context typed IDs local to the payment bounded context), PaymentStatus enum with allowed transitions.

## ADDED Requirements

### Requirement: PaymentId is a typed ID

PaymentId SHALL be a Java record wrapping a UUID. It SHALL reject null UUID values with PaymentDomainException.

#### Scenario: Valid PaymentId construction

* **WHEN** a PaymentId is created with a non-null UUID
* **THEN** the `value()` accessor SHALL return that UUID

#### Scenario: Null UUID rejected

* **WHEN** a PaymentId is created with a null UUID
* **THEN** it SHALL throw PaymentDomainException

#### Scenario: Equality by value

* **WHEN** two PaymentId instances are created with the same UUID
* **THEN** they SHALL be equal

### Requirement: ReservationId is a local typed ID

ReservationId SHALL be a Java record wrapping a UUID, local to the payment bounded context. It SHALL NOT be imported from reservation-domain. It SHALL reject null UUID values with PaymentDomainException.

#### Scenario: Valid ReservationId construction

* **WHEN** a ReservationId is created with a non-null UUID
* **THEN** the `value()` accessor SHALL return that UUID

#### Scenario: Null UUID rejected

* **WHEN** a ReservationId is created with a null UUID
* **THEN** it SHALL throw PaymentDomainException

#### Scenario: Equality by value

* **WHEN** two ReservationId instances are created with the same UUID
* **THEN** they SHALL be equal

### Requirement: CustomerId is a local typed ID

CustomerId SHALL be a Java record wrapping a UUID, local to the payment bounded context. It SHALL NOT be imported from customer-domain. It SHALL reject null UUID values with PaymentDomainException.

#### Scenario: Valid CustomerId construction

* **WHEN** a CustomerId is created with a non-null UUID
* **THEN** the `value()` accessor SHALL return that UUID

#### Scenario: Null UUID rejected

* **WHEN** a CustomerId is created with a null UUID
* **THEN** it SHALL throw PaymentDomainException

#### Scenario: Equality by value

* **WHEN** two CustomerId instances are created with the same UUID
* **THEN** they SHALL be equal

### Requirement: PaymentStatus enum

PaymentStatus SHALL be an enum with values PENDING, COMPLETED, FAILED, and REFUNDED.

#### Scenario: All statuses exist

* **WHEN** PaymentStatus values are listed
* **THEN** they SHALL contain exactly PENDING, COMPLETED, FAILED, and REFUNDED (4 values)

## Constraint: Zero Spring dependencies

No class in `com.vehiclerental.payment.domain.model.vo` SHALL import any type from `org.springframework.*`.
