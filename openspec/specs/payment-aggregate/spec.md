# payment-aggregate Specification

## Purpose
Payment Aggregate Root with create/reconstruct factory methods, state transitions (complete, fail, refund), domain invariants (positive amount, controlled transitions), and domain event registration.

## ADDED Requirements

### Requirement: Payment is an Aggregate Root

Payment SHALL be a concrete class extending `AggregateRoot<PaymentId>`. It SHALL hold reservationId (ReservationId), customerId (CustomerId), amount (Money), status (PaymentStatus enum), failureMessages (List\<String\>), createdAt (Instant), and updatedAt (Instant).

#### Scenario: Payment fields accessible after creation

* **WHEN** a Payment is created with valid arguments
* **THEN** `getId()` SHALL return a non-null PaymentId
* **AND** `getReservationId()` SHALL return the provided ReservationId
* **AND** `getCustomerId()` SHALL return the provided CustomerId
* **AND** `getAmount()` SHALL return the provided Money
* **AND** `getStatus()` SHALL return PENDING
* **AND** `getFailureMessages()` SHALL return an empty list
* **AND** `getCreatedAt()` SHALL return a non-null Instant
* **AND** `getUpdatedAt()` SHALL return a non-null Instant

### Requirement: Payment creation via factory method

Payment SHALL expose a static `create(ReservationId, CustomerId, Money)` factory method. It SHALL generate a new PaymentId, set status to PENDING, set createdAt and updatedAt, and SHALL NOT register any domain event (a pending payment is not a business fact).

#### Scenario: Successful creation

* **WHEN** `Payment.create(reservationId, customerId, amount)` is called with valid arguments
* **THEN** the Payment SHALL have a non-null PaymentId
* **AND** the status SHALL be PENDING
* **AND** `getDomainEvents()` SHALL be empty

#### Scenario: Null reservationId rejected

* **WHEN** `Payment.create(null, customerId, amount)` is called
* **THEN** it SHALL throw PaymentDomainException with error code "PAYMENT_RESERVATION_ID_REQUIRED"

#### Scenario: Null customerId rejected

* **WHEN** `Payment.create(reservationId, null, amount)` is called
* **THEN** it SHALL throw PaymentDomainException with error code "PAYMENT_CUSTOMER_ID_REQUIRED"

#### Scenario: Null amount rejected

* **WHEN** `Payment.create(reservationId, customerId, null)` is called
* **THEN** it SHALL throw PaymentDomainException with error code "PAYMENT_AMOUNT_REQUIRED"

#### Scenario: Zero amount rejected

* **WHEN** `Payment.create(reservationId, customerId, Money(0.00, EUR))` is called
* **THEN** it SHALL throw PaymentDomainException with error code "PAYMENT_AMOUNT_INVALID"

### Requirement: Payment reconstruction from persistence

Payment SHALL expose a static `reconstruct(PaymentId, ReservationId, CustomerId, Money, PaymentStatus, List<String>, Instant, Instant)` factory method that rebuilds the aggregate from persisted state without generating events or running creation validations.

#### Scenario: Reconstruct does not emit events

* **WHEN** `Payment.reconstruct(...)` is called with valid persisted data
* **THEN** `getDomainEvents()` SHALL return an empty list
* **AND** all fields SHALL match the provided arguments

#### Scenario: Reconstruct preserves failureMessages

* **WHEN** `Payment.reconstruct(...)` is called with 2 failure messages
* **THEN** `getFailureMessages()` SHALL return a list of size 2

### Requirement: No public constructors

Payment SHALL NOT have public constructors. Instantiation SHALL only be possible through `create()` or `reconstruct()`.

#### Scenario: Constructor is not public

* **WHEN** the Payment class is inspected via reflection
* **THEN** it SHALL have no public constructors

### Requirement: Complete state transition

Payment SHALL expose a `complete()` method that transitions from PENDING to COMPLETED. It SHALL register a PaymentCompletedEvent and update updatedAt.

#### Scenario: Complete from PENDING

* **WHEN** `complete()` is called on a Payment with status PENDING
* **THEN** the status SHALL change to COMPLETED
* **AND** `getDomainEvents()` SHALL contain exactly one PaymentCompletedEvent
* **AND** `getUpdatedAt()` SHALL be updated

#### Scenario: Complete from non-PENDING state

* **WHEN** `complete()` is called on a Payment with status COMPLETED, FAILED, or REFUNDED
* **THEN** it SHALL throw PaymentDomainException with error code "PAYMENT_INVALID_STATE"

### Requirement: Fail state transition

Payment SHALL expose a `fail(List<String> failureMessages)` method that transitions from PENDING to FAILED. It SHALL store the failure messages, register a PaymentFailedEvent, and update updatedAt.

#### Scenario: Fail from PENDING

* **WHEN** `fail(["Card declined"])` is called on a Payment with status PENDING
* **THEN** the status SHALL change to FAILED
* **AND** `getFailureMessages()` SHALL contain "Card declined"
* **AND** `getDomainEvents()` SHALL contain exactly one PaymentFailedEvent
* **AND** `getUpdatedAt()` SHALL be updated

#### Scenario: Fail from non-PENDING state

* **WHEN** `fail(messages)` is called on a Payment with status COMPLETED, FAILED, or REFUNDED
* **THEN** it SHALL throw PaymentDomainException with error code "PAYMENT_INVALID_STATE"

#### Scenario: Fail with null messages rejected

* **WHEN** `fail(null)` is called on a Payment with status PENDING
* **THEN** it SHALL throw PaymentDomainException with error code "PAYMENT_FAILURE_MESSAGES_REQUIRED"

#### Scenario: Fail with empty messages rejected

* **WHEN** `fail(emptyList)` is called on a Payment with status PENDING
* **THEN** it SHALL throw PaymentDomainException with error code "PAYMENT_FAILURE_MESSAGES_REQUIRED"

### Requirement: Refund state transition

Payment SHALL expose a `refund()` method that transitions from COMPLETED to REFUNDED. It SHALL register a PaymentRefundedEvent and update updatedAt.

#### Scenario: Refund from COMPLETED

* **WHEN** `refund()` is called on a Payment with status COMPLETED
* **THEN** the status SHALL change to REFUNDED
* **AND** `getDomainEvents()` SHALL contain exactly one PaymentRefundedEvent
* **AND** `getUpdatedAt()` SHALL be updated

#### Scenario: Refund from non-COMPLETED state

* **WHEN** `refund()` is called on a Payment with status PENDING, FAILED, or REFUNDED
* **THEN** it SHALL throw PaymentDomainException with error code "PAYMENT_INVALID_STATE"

### Requirement: PaymentDomainException extends DomainException with errorCode

PaymentDomainException SHALL be a concrete class extending `DomainException` from common. It SHALL provide constructors that require a business-language errorCode (e.g., "PAYMENT_INVALID_STATE", "PAYMENT_AMOUNT_INVALID").

#### Scenario: Exception carries errorCode

* **WHEN** a state transition throws PaymentDomainException
* **THEN** `getErrorCode()` SHALL return a non-blank business error code
* **AND** `getMessage()` SHALL return a descriptive message

#### Scenario: Invalid transition includes current state in message

* **WHEN** `complete()` is called on a COMPLETED payment
* **THEN** the exception message SHALL indicate the current state that prevented the transition

## Constraint: Zero Spring dependencies

No class in `com.vehiclerental.payment.domain` SHALL import any type from `org.springframework.*`.
