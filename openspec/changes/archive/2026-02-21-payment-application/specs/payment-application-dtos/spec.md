# payment-application-dtos Specification

## Purpose
Command and Response records for Payment use cases. Commands carry input data as primitives from the outside world. The single PaymentResponse carries output data for all use cases. All are Java records (immutable, no framework dependencies).

## ADDED Requirements

### Requirement: ProcessPaymentCommand carries payment creation data

ProcessPaymentCommand SHALL be a Java record with fields: reservationId (String), customerId (String), amount (BigDecimal), currency (String).

#### Scenario: All fields accessible

* **WHEN** a ProcessPaymentCommand is constructed with reservationId, customerId, amount, and currency
* **THEN** `reservationId()`, `customerId()`, `amount()`, and `currency()` SHALL return the provided values

#### Scenario: Amount is BigDecimal for precision

* **WHEN** a ProcessPaymentCommand is constructed
* **THEN** `amount()` SHALL return a BigDecimal value, not a double or float

### Requirement: RefundPaymentCommand carries reservation identity

RefundPaymentCommand SHALL be a Java record with a single field: reservationId (String). The Application Service uses `findByReservationId()` to locate the payment to refund, because the SAGA orchestrator knows reservations, not internal payment IDs.

#### Scenario: ReservationId accessible

* **WHEN** a RefundPaymentCommand is constructed with "reservation-uuid"
* **THEN** `reservationId()` SHALL return "reservation-uuid"

### Requirement: GetPaymentCommand carries payment identity

GetPaymentCommand SHALL be a Java record with a single field: paymentId (String).

#### Scenario: PaymentId accessible

* **WHEN** a GetPaymentCommand is constructed with "payment-uuid"
* **THEN** `paymentId()` SHALL return "payment-uuid"

### Requirement: PaymentResponse carries payment snapshot

PaymentResponse SHALL be a Java record with fields: paymentId (String), reservationId (String), customerId (String), amount (BigDecimal), currency (String), status (String), failureMessages (List<String>), createdAt (Instant), updatedAt (Instant).

#### Scenario: All fields accessible

* **WHEN** a PaymentResponse is constructed
* **THEN** all fields SHALL be accessible via record accessors

#### Scenario: Single response for all use cases

* **WHEN** ProcessPaymentUseCase, RefundPaymentUseCase, or GetPaymentUseCase returns a result
* **THEN** the return type SHALL be PaymentResponse

#### Scenario: FailureMessages is empty list for successful payments

* **WHEN** a PaymentResponse represents a COMPLETED payment
* **THEN** `failureMessages()` SHALL return an empty list, not null

### Requirement: Commands and Responses are plain Java records

All commands and responses SHALL be Java records with no Spring annotations and no validation annotations. They SHALL have zero framework dependencies.

#### Scenario: No framework imports

* **WHEN** any command or response class is inspected
* **THEN** it SHALL NOT import any type from `org.springframework.*` or `jakarta.*`

## Constraints

### Constraint: Zero Spring dependencies

No class in `com.vehiclerental.payment.application.dto` SHALL import any type from `org.springframework.*`.
