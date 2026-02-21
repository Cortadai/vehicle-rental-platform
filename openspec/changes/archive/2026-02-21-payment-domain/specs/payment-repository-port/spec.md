# payment-repository-port Specification

## Purpose
PaymentRepository output port interface with save, findById, and findByReservationId operations.

## ADDED Requirements

### Requirement: PaymentRepository defines save, findById, and findByReservationId

PaymentRepository SHALL be a Java interface with three methods:
- `Payment save(Payment payment)` — persists a Payment and returns the persisted instance
- `Optional<Payment> findById(PaymentId paymentId)` — retrieves a Payment by its internal typed ID
- `Optional<Payment> findByReservationId(ReservationId reservationId)` — retrieves a Payment by its associated reservation ID (for application-layer idempotency checks)

#### Scenario: Save method signature

* **WHEN** the PaymentRepository interface is inspected
* **THEN** it SHALL declare a `save` method accepting `Payment` and returning `Payment`

#### Scenario: FindById method signature

* **WHEN** the PaymentRepository interface is inspected
* **THEN** it SHALL declare a `findById` method accepting `PaymentId` and returning `Optional<Payment>`

#### Scenario: FindByReservationId method signature

* **WHEN** the PaymentRepository interface is inspected
* **THEN** it SHALL declare a `findByReservationId` method accepting `ReservationId` and returning `Optional<Payment>`

### Requirement: PaymentRepository uses only domain types

PaymentRepository SHALL NOT reference any Spring, JPA, or infrastructure types. Method signatures SHALL use only `Payment`, `PaymentId`, `ReservationId`, and `java.util.Optional`.

#### Scenario: No framework imports

* **WHEN** the PaymentRepository source file is inspected
* **THEN** it SHALL contain no imports from `org.springframework.*`
* **AND** it SHALL contain no imports from `jakarta.persistence.*`

## Constraint: Zero Spring dependencies

No class in `com.vehiclerental.payment.domain.port.output` SHALL import any type from `org.springframework.*`.
