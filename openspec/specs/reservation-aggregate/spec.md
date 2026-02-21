# reservation-aggregate Specification

## Purpose
Reservation Aggregate Root with full SAGA-ready state machine (6 states, 6 transitions), factory methods (create/reconstruct), totalPrice calculation from items, failure message tracking, and domain event registration.

## Requirements

### Requirement: Reservation is an Aggregate Root

Reservation SHALL be a concrete class extending `AggregateRoot<ReservationId>`. It SHALL hold trackingId (TrackingId), customerId (CustomerId), pickupLocation (PickupLocation), returnLocation (PickupLocation), dateRange (DateRange), totalPrice (Money), status (ReservationStatus enum), items (List\<ReservationItem\>), failureMessages (List\<String\>), createdAt (Instant), and updatedAt (Instant).

#### Scenario: Reservation fields accessible after creation

* **WHEN** a Reservation is created with valid arguments
* **THEN** `getId()` SHALL return a non-null ReservationId
* **AND** `getTrackingId()` SHALL return a non-null TrackingId
* **AND** `getCustomerId()` SHALL return the provided CustomerId
* **AND** `getPickupLocation()` SHALL return the provided PickupLocation
* **AND** `getReturnLocation()` SHALL return the provided PickupLocation
* **AND** `getDateRange()` SHALL return the provided DateRange
* **AND** `getItems()` SHALL return a non-empty unmodifiable list
* **AND** `getCreatedAt()` SHALL return a non-null Instant
* **AND** `getUpdatedAt()` SHALL return a non-null Instant

### Requirement: Reservation creation via factory method

Reservation SHALL expose a static `create(CustomerId, PickupLocation, PickupLocation, DateRange, List<ReservationItem>)` factory method. It SHALL generate a new ReservationId and TrackingId, calculate totalPrice by summing all item subtotals, set status to PENDING, set createdAt and updatedAt, and register a ReservationCreatedEvent.

#### Scenario: Successful creation

* **WHEN** `Reservation.create(customerId, pickupLocation, returnLocation, dateRange, items)` is called with valid arguments
* **THEN** the Reservation SHALL have a non-null ReservationId
* **AND** the Reservation SHALL have a non-null TrackingId (different from ReservationId)
* **AND** the status SHALL be PENDING
* **AND** `getDomainEvents()` SHALL contain exactly one ReservationCreatedEvent
* **AND** failureMessages SHALL be empty

#### Scenario: TotalPrice calculated from items

* **WHEN** a Reservation is created with two items having subtotals of 300.00 EUR and 200.00 EUR
* **THEN** `getTotalPrice()` SHALL return Money with amount 500.00 and same currency

#### Scenario: Null customerId rejected

* **WHEN** `Reservation.create(null, pickupLocation, returnLocation, dateRange, items)` is called
* **THEN** it SHALL throw ReservationDomainException with error code "RESERVATION_CUSTOMER_ID_REQUIRED"

#### Scenario: Null pickupLocation rejected

* **WHEN** `Reservation.create(customerId, null, returnLocation, dateRange, items)` is called
* **THEN** it SHALL throw ReservationDomainException

#### Scenario: Null returnLocation rejected

* **WHEN** `Reservation.create(customerId, pickupLocation, null, dateRange, items)` is called
* **THEN** it SHALL throw ReservationDomainException

#### Scenario: Null dateRange rejected

* **WHEN** `Reservation.create(customerId, pickupLocation, returnLocation, null, items)` is called
* **THEN** it SHALL throw ReservationDomainException

#### Scenario: Null items rejected

* **WHEN** `Reservation.create(customerId, pickupLocation, returnLocation, dateRange, null)` is called
* **THEN** it SHALL throw ReservationDomainException

#### Scenario: Empty items rejected

* **WHEN** `Reservation.create(customerId, pickupLocation, returnLocation, dateRange, emptyList)` is called
* **THEN** it SHALL throw ReservationDomainException with error code "RESERVATION_ITEMS_REQUIRED"

### Requirement: Reservation reconstruction from persistence

Reservation SHALL expose a static `reconstruct(...)` factory method that rebuilds the aggregate from persisted state without generating events or running creation validations. It SHALL accept all fields including pre-built ReservationItem list and failureMessages.

#### Scenario: Reconstruct does not emit events

* **WHEN** `Reservation.reconstruct(...)` is called with valid persisted data
* **THEN** `getDomainEvents()` SHALL return an empty list
* **AND** all fields SHALL match the provided arguments

#### Scenario: Reconstruct preserves items and failureMessages

* **WHEN** `Reservation.reconstruct(...)` is called with 2 items and 1 failure message
* **THEN** `getItems()` SHALL return a list of size 2
* **AND** `getFailureMessages()` SHALL return a list of size 1

### Requirement: No public constructors

Reservation SHALL NOT have public constructors. Instantiation SHALL only be possible through `create()` or `reconstruct()`.

#### Scenario: Constructor is not public

* **WHEN** the Reservation class is inspected via reflection
* **THEN** it SHALL have no public constructors

### Requirement: ValidateCustomer state transition

Reservation SHALL expose a `validateCustomer()` method that transitions from PENDING to CUSTOMER_VALIDATED. It SHALL update updatedAt.

#### Scenario: Validate customer from PENDING

* **WHEN** `validateCustomer()` is called on a Reservation with status PENDING
* **THEN** the status SHALL change to CUSTOMER_VALIDATED
* **AND** `getUpdatedAt()` SHALL be updated

#### Scenario: Validate customer from non-PENDING state

* **WHEN** `validateCustomer()` is called on a Reservation with status CUSTOMER_VALIDATED, PAID, CONFIRMED, CANCELLING, or CANCELLED
* **THEN** it SHALL throw ReservationDomainException with error code "RESERVATION_INVALID_STATE"

### Requirement: Pay state transition

Reservation SHALL expose a `pay()` method that transitions from CUSTOMER_VALIDATED to PAID. It SHALL update updatedAt.

#### Scenario: Pay from CUSTOMER_VALIDATED

* **WHEN** `pay()` is called on a Reservation with status CUSTOMER_VALIDATED
* **THEN** the status SHALL change to PAID
* **AND** `getUpdatedAt()` SHALL be updated

#### Scenario: Pay from non-CUSTOMER_VALIDATED state

* **WHEN** `pay()` is called on a Reservation with status PENDING, PAID, CONFIRMED, CANCELLING, or CANCELLED
* **THEN** it SHALL throw ReservationDomainException with error code "RESERVATION_INVALID_STATE"

### Requirement: Confirm state transition

Reservation SHALL expose a `confirm()` method that transitions from PAID to CONFIRMED. It SHALL update updatedAt.

#### Scenario: Confirm from PAID

* **WHEN** `confirm()` is called on a Reservation with status PAID
* **THEN** the status SHALL change to CONFIRMED
* **AND** `getUpdatedAt()` SHALL be updated

#### Scenario: Confirm from non-PAID state

* **WHEN** `confirm()` is called on a Reservation with status PENDING, CUSTOMER_VALIDATED, CONFIRMED, CANCELLING, or CANCELLED
* **THEN** it SHALL throw ReservationDomainException with error code "RESERVATION_INVALID_STATE"

### Requirement: InitCancel state transition

Reservation SHALL expose an `initCancel(List<String> failureMessages)` method that transitions from PAID to CANCELLING. It SHALL store the failure messages and update updatedAt.

#### Scenario: InitCancel from PAID

* **WHEN** `initCancel(["Fleet unavailable"])` is called on a Reservation with status PAID
* **THEN** the status SHALL change to CANCELLING
* **AND** `getFailureMessages()` SHALL contain "Fleet unavailable"
* **AND** `getUpdatedAt()` SHALL be updated

#### Scenario: InitCancel from non-PAID state

* **WHEN** `initCancel(messages)` is called on a Reservation with status PENDING, CUSTOMER_VALIDATED, CONFIRMED, CANCELLING, or CANCELLED
* **THEN** it SHALL throw ReservationDomainException with error code "RESERVATION_INVALID_STATE"

#### Scenario: InitCancel with null messages rejected

* **WHEN** `initCancel(null)` is called on a Reservation with status PAID
* **THEN** it SHALL throw ReservationDomainException

#### Scenario: InitCancel with empty messages rejected

* **WHEN** `initCancel(emptyList)` is called on a Reservation with status PAID
* **THEN** it SHALL throw ReservationDomainException

### Requirement: Cancel state transition

Reservation SHALL expose a `cancel()` method that transitions from PENDING, CUSTOMER_VALIDATED, or CANCELLING to CANCELLED. It SHALL register a ReservationCancelledEvent and update updatedAt.

#### Scenario: Cancel from PENDING

* **WHEN** `cancel()` is called on a Reservation with status PENDING
* **THEN** the status SHALL change to CANCELLED
* **AND** `getDomainEvents()` SHALL contain a ReservationCancelledEvent

#### Scenario: Cancel from CUSTOMER_VALIDATED

* **WHEN** `cancel()` is called on a Reservation with status CUSTOMER_VALIDATED
* **THEN** the status SHALL change to CANCELLED
* **AND** `getDomainEvents()` SHALL contain a ReservationCancelledEvent

#### Scenario: Cancel from CANCELLING

* **WHEN** `cancel()` is called on a Reservation with status CANCELLING that has failure messages
* **THEN** the status SHALL change to CANCELLED
* **AND** the failure messages SHALL be preserved
* **AND** `getDomainEvents()` SHALL contain a ReservationCancelledEvent

#### Scenario: Cancel from CONFIRMED rejected

* **WHEN** `cancel()` is called on a Reservation with status CONFIRMED
* **THEN** it SHALL throw ReservationDomainException with error code "RESERVATION_INVALID_STATE"

#### Scenario: Cancel from PAID rejected

* **WHEN** `cancel()` is called on a Reservation with status PAID
* **THEN** it SHALL throw ReservationDomainException with error code "RESERVATION_INVALID_STATE"

#### Scenario: Cancel from CANCELLED rejected

* **WHEN** `cancel()` is called on a Reservation with status CANCELLED
* **THEN** it SHALL throw ReservationDomainException with error code "RESERVATION_INVALID_STATE"

### Requirement: ReservationDomainException extends DomainException with errorCode

ReservationDomainException SHALL be a concrete class extending `DomainException` from common. It SHALL provide constructors that require a business-language errorCode (e.g., "RESERVATION_INVALID_STATE", "RESERVATION_CUSTOMER_ID_REQUIRED").

#### Scenario: Exception carries errorCode

* **WHEN** a state transition throws ReservationDomainException
* **THEN** `getErrorCode()` SHALL return a non-blank business error code
* **AND** `getMessage()` SHALL return a descriptive message

#### Scenario: Invalid transition includes current state in message

* **WHEN** `pay()` is called on a PENDING reservation
* **THEN** the exception message SHALL indicate the current state that prevented the transition

## Constraint: Zero Spring dependencies

No class in `com.vehiclerental.reservation.domain` SHALL import any type from `org.springframework.*`.
