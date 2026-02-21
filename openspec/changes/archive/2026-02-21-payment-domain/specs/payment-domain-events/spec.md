# payment-domain-events Specification

## Purpose
PaymentCompletedEvent, PaymentFailedEvent, PaymentRefundedEvent — all records implementing DomainEvent interface from common.

## ADDED Requirements

### Requirement: PaymentCompletedEvent carries full snapshot

PaymentCompletedEvent SHALL be a Java record implementing DomainEvent from common. Beyond the base contract (eventId, occurredOn), it SHALL carry paymentId (PaymentId), reservationId (ReservationId), customerId (CustomerId), and amount (Money).

#### Scenario: Completed event has all fields

* **WHEN** a PaymentCompletedEvent is constructed
* **THEN** `eventId()` SHALL return a non-null UUID
* **AND** `occurredOn()` SHALL return a non-null Instant
* **AND** `paymentId()` SHALL return the payment's PaymentId
* **AND** `reservationId()` SHALL return the payment's ReservationId
* **AND** `customerId()` SHALL return the payment's CustomerId
* **AND** `amount()` SHALL return the payment's Money amount

#### Scenario: Null eventId rejected

* **WHEN** a PaymentCompletedEvent is constructed with a null eventId
* **THEN** it SHALL throw PaymentDomainException

#### Scenario: Null occurredOn rejected

* **WHEN** a PaymentCompletedEvent is constructed with a null occurredOn
* **THEN** it SHALL throw PaymentDomainException

### Requirement: PaymentFailedEvent carries failure messages

PaymentFailedEvent SHALL be a Java record implementing DomainEvent from common. Beyond the base contract (eventId, occurredOn), it SHALL carry paymentId (PaymentId), reservationId (ReservationId), and failureMessages (List\<String\>).

#### Scenario: Failed event has payment identity and failure messages

* **WHEN** a PaymentFailedEvent is constructed
* **THEN** `eventId()` SHALL return a non-null UUID
* **AND** `occurredOn()` SHALL return a non-null Instant
* **AND** `paymentId()` SHALL return the payment's PaymentId
* **AND** `reservationId()` SHALL return the payment's ReservationId
* **AND** `failureMessages()` SHALL return the list of failure messages

#### Scenario: Failed event carries failure reasons from fail()

* **WHEN** `fail(["Card declined", "Insufficient funds"])` is called on a PENDING payment
* **THEN** the PaymentFailedEvent's `failureMessages()` SHALL contain both messages

### Requirement: PaymentRefundedEvent carries refunded amount

PaymentRefundedEvent SHALL be a Java record implementing DomainEvent from common. Beyond the base contract (eventId, occurredOn), it SHALL carry paymentId (PaymentId), reservationId (ReservationId), and amount (Money).

#### Scenario: Refunded event has all fields

* **WHEN** a PaymentRefundedEvent is constructed
* **THEN** `eventId()` SHALL return a non-null UUID
* **AND** `occurredOn()` SHALL return a non-null Instant
* **AND** `paymentId()` SHALL return the payment's PaymentId
* **AND** `reservationId()` SHALL return the payment's ReservationId
* **AND** `amount()` SHALL return the refunded Money amount

### Requirement: All payment events implement DomainEvent interface

All payment domain events SHALL implement `com.vehiclerental.common.domain.event.DomainEvent`.

#### Scenario: Events satisfy DomainEvent contract

* **WHEN** any payment event record is checked
* **THEN** it SHALL be an instance of DomainEvent

### Requirement: Events are emitted on transitions, not on create

The Payment aggregate SHALL register domain events only on state transitions: `complete()` (PaymentCompletedEvent), `fail()` (PaymentFailedEvent), `refund()` (PaymentRefundedEvent). The `create()` factory method SHALL NOT register any domain event.

#### Scenario: Create does not register events

* **WHEN** `Payment.create(reservationId, customerId, amount)` is called
* **THEN** `getDomainEvents()` SHALL be empty

#### Scenario: Complete registers PaymentCompletedEvent

* **WHEN** `complete()` is called on a PENDING payment
* **THEN** `getDomainEvents()` SHALL contain exactly one PaymentCompletedEvent

#### Scenario: Fail registers PaymentFailedEvent

* **WHEN** `fail(messages)` is called on a PENDING payment
* **THEN** `getDomainEvents()` SHALL contain exactly one PaymentFailedEvent

#### Scenario: Refund registers PaymentRefundedEvent

* **WHEN** `refund()` is called on a COMPLETED payment
* **THEN** `getDomainEvents()` SHALL contain exactly one PaymentRefundedEvent

## Constraint: Zero Spring dependencies

No class in `com.vehiclerental.payment.domain.event` SHALL import any type from `org.springframework.*`.
