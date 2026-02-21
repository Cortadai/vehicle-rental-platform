## ADDED Requirements

### Requirement: ReservationCreatedEvent carries full snapshot

ReservationCreatedEvent SHALL be a Java record implementing DomainEvent from common. Beyond the base contract (eventId, occurredOn), it SHALL carry reservationId (ReservationId), trackingId (TrackingId), customerId (CustomerId), totalPrice (Money), dateRange (DateRange), pickupLocation (PickupLocation), returnLocation (PickupLocation), and items (List\<ReservationItemSnapshot\>).

#### Scenario: Created event has all fields

* **WHEN** a ReservationCreatedEvent is constructed
* **THEN** `eventId()` SHALL return a non-null UUID
* **AND** `occurredOn()` SHALL return a non-null Instant
* **AND** `reservationId()` SHALL return the reservation's ReservationId
* **AND** `trackingId()` SHALL return the reservation's TrackingId
* **AND** `customerId()` SHALL return the reservation's CustomerId
* **AND** `totalPrice()` SHALL return the reservation's total price
* **AND** `dateRange()` SHALL return the reservation's DateRange
* **AND** `pickupLocation()` SHALL return the reservation's PickupLocation
* **AND** `returnLocation()` SHALL return the reservation's PickupLocation
* **AND** `items()` SHALL return a non-empty list of ReservationItemSnapshot

#### Scenario: Null eventId rejected

* **WHEN** a ReservationCreatedEvent is constructed with a null eventId
* **THEN** it SHALL throw ReservationDomainException

#### Scenario: Null occurredOn rejected

* **WHEN** a ReservationCreatedEvent is constructed with a null occurredOn
* **THEN** it SHALL throw ReservationDomainException

### Requirement: ReservationCancelledEvent carries failure messages

ReservationCancelledEvent SHALL be a Java record implementing DomainEvent from common. Beyond the base contract (eventId, occurredOn), it SHALL carry reservationId (ReservationId) and failureMessages (List\<String\>).

#### Scenario: Cancelled event has reservation identity and failure messages

* **WHEN** a ReservationCancelledEvent is constructed
* **THEN** `eventId()` SHALL return a non-null UUID
* **AND** `occurredOn()` SHALL return a non-null Instant
* **AND** `reservationId()` SHALL return the reservation's ReservationId
* **AND** `failureMessages()` SHALL return the list of failure messages

#### Scenario: Cancelled event from PENDING has empty failure messages

* **WHEN** a Reservation in PENDING state is cancelled (no prior initCancel)
* **THEN** the ReservationCancelledEvent's `failureMessages()` SHALL be an empty list

#### Scenario: Cancelled event from CANCELLING carries failure messages

* **WHEN** a Reservation in CANCELLING state (after initCancel with messages) is cancelled
* **THEN** the ReservationCancelledEvent's `failureMessages()` SHALL contain the messages from initCancel

### Requirement: ReservationItemSnapshot is an immutable record

ReservationItemSnapshot SHALL be a Java record with vehicleId (VehicleId), dailyRate (Money), days (int), and subtotal (Money). It captures item data at event time, decoupled from the live ReservationItem entity.

#### Scenario: Snapshot has all item fields

* **WHEN** a ReservationItemSnapshot is constructed from a ReservationItem with vehicleId, dailyRate 100.00 EUR, 3 days, subtotal 300.00 EUR
* **THEN** `vehicleId()` SHALL return the item's VehicleId
* **AND** `dailyRate()` SHALL return Money 100.00 EUR
* **AND** `days()` SHALL return 3
* **AND** `subtotal()` SHALL return Money 300.00 EUR

### Requirement: All reservation events implement DomainEvent interface

All reservation domain events SHALL implement `com.vehiclerental.common.domain.event.DomainEvent`.

#### Scenario: Events satisfy DomainEvent contract

* **WHEN** any reservation event record is checked
* **THEN** it SHALL be an instance of DomainEvent

### Requirement: Only create and cancel register domain events

The Reservation aggregate SHALL register domain events only in `create()` (ReservationCreatedEvent) and `cancel()` (ReservationCancelledEvent). The intermediate transitions (`validateCustomer`, `pay`, `confirm`, `initCancel`) SHALL NOT register domain events.

#### Scenario: ValidateCustomer does not register events

* **WHEN** `validateCustomer()` is called on a PENDING Reservation
* **THEN** `getDomainEvents()` SHALL NOT contain any new events beyond the initial ReservationCreatedEvent

#### Scenario: Pay does not register events

* **WHEN** `pay()` is called on a CUSTOMER_VALIDATED Reservation (after clearing creation events)
* **THEN** `getDomainEvents()` SHALL be empty

#### Scenario: Confirm does not register events

* **WHEN** `confirm()` is called on a PAID Reservation (after clearing previous events)
* **THEN** `getDomainEvents()` SHALL be empty

#### Scenario: InitCancel does not register events

* **WHEN** `initCancel(messages)` is called on a PAID Reservation (after clearing previous events)
* **THEN** `getDomainEvents()` SHALL be empty

## Constraint: Zero Spring dependencies

No class in `com.vehiclerental.reservation.domain.event` SHALL import any type from `org.springframework.*`.
