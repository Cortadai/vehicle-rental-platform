# reservation-value-objects Specification

## Purpose
Typed IDs (ReservationId, TrackingId, CustomerId, VehicleId), DateRange (with getDays()), PickupLocation, and ReservationStatus enum — all local to the reservation bounded context.

## Requirements

### Requirement: ReservationId is a typed ID

ReservationId SHALL be a Java record wrapping a UUID. It SHALL reject null UUID values with ReservationDomainException.

#### Scenario: Valid ReservationId construction

* **WHEN** a ReservationId is created with a non-null UUID
* **THEN** the `value()` accessor SHALL return that UUID

#### Scenario: Null UUID rejected

* **WHEN** a ReservationId is created with a null UUID
* **THEN** it SHALL throw ReservationDomainException

#### Scenario: Equality by value

* **WHEN** two ReservationId instances are created with the same UUID
* **THEN** they SHALL be equal

### Requirement: TrackingId is a typed ID

TrackingId SHALL be a Java record wrapping a UUID. It is the public-facing identifier for REST API queries and SAGA correlation. It SHALL reject null UUID values with ReservationDomainException.

#### Scenario: Valid TrackingId construction

* **WHEN** a TrackingId is created with a non-null UUID
* **THEN** the `value()` accessor SHALL return that UUID

#### Scenario: Null UUID rejected

* **WHEN** a TrackingId is created with a null UUID
* **THEN** it SHALL throw ReservationDomainException

#### Scenario: Equality by value

* **WHEN** two TrackingId instances are created with the same UUID
* **THEN** they SHALL be equal

### Requirement: CustomerId is a local typed ID

CustomerId SHALL be a Java record wrapping a UUID, local to the reservation bounded context. It SHALL NOT be imported from customer-domain. It SHALL reject null UUID values with ReservationDomainException.

#### Scenario: Valid CustomerId construction

* **WHEN** a CustomerId is created with a non-null UUID
* **THEN** the `value()` accessor SHALL return that UUID

#### Scenario: Null UUID rejected

* **WHEN** a CustomerId is created with a null UUID
* **THEN** it SHALL throw ReservationDomainException

#### Scenario: Equality by value

* **WHEN** two CustomerId instances are created with the same UUID
* **THEN** they SHALL be equal

### Requirement: VehicleId is a local typed ID

VehicleId SHALL be a Java record wrapping a UUID, local to the reservation bounded context. It SHALL NOT be imported from fleet-domain. It SHALL reject null UUID values with ReservationDomainException.

#### Scenario: Valid VehicleId construction

* **WHEN** a VehicleId is created with a non-null UUID
* **THEN** the `value()` accessor SHALL return that UUID

#### Scenario: Null UUID rejected

* **WHEN** a VehicleId is created with a null UUID
* **THEN** it SHALL throw ReservationDomainException

#### Scenario: Equality by value

* **WHEN** two VehicleId instances are created with the same UUID
* **THEN** they SHALL be equal

### Requirement: DateRange validates structural invariant

DateRange SHALL be a Java record with pickupDate (LocalDate) and returnDate (LocalDate). It SHALL validate that both dates are non-null and that returnDate is strictly after pickupDate. It SHALL NOT validate that pickupDate is in the future.

#### Scenario: Valid DateRange construction

* **WHEN** a DateRange is created with pickupDate 2026-03-01 and returnDate 2026-03-05
* **THEN** `pickupDate()` SHALL return 2026-03-01
* **AND** `returnDate()` SHALL return 2026-03-05

#### Scenario: getDays returns correct count

* **WHEN** a DateRange is created with pickupDate 2026-03-01 and returnDate 2026-03-05
* **THEN** `getDays()` SHALL return 4

#### Scenario: Single day range

* **WHEN** a DateRange is created with pickupDate 2026-03-01 and returnDate 2026-03-02
* **THEN** `getDays()` SHALL return 1

#### Scenario: Null pickupDate rejected

* **WHEN** a DateRange is created with a null pickupDate
* **THEN** it SHALL throw ReservationDomainException

#### Scenario: Null returnDate rejected

* **WHEN** a DateRange is created with a null returnDate
* **THEN** it SHALL throw ReservationDomainException

#### Scenario: ReturnDate equals pickupDate rejected

* **WHEN** a DateRange is created with pickupDate and returnDate both set to 2026-03-01
* **THEN** it SHALL throw ReservationDomainException

#### Scenario: ReturnDate before pickupDate rejected

* **WHEN** a DateRange is created with pickupDate 2026-03-05 and returnDate 2026-03-01
* **THEN** it SHALL throw ReservationDomainException

#### Scenario: Past dates accepted

* **WHEN** a DateRange is created with pickupDate 2020-01-01 and returnDate 2020-01-05
* **THEN** it SHALL be created successfully (no future-date validation in domain)

### Requirement: PickupLocation validates address and city

PickupLocation SHALL be a Java record with address (String) and city (String). Both fields SHALL be non-null and non-blank.

#### Scenario: Valid PickupLocation construction

* **WHEN** a PickupLocation is created with address "123 Main St" and city "Madrid"
* **THEN** `address()` SHALL return "123 Main St"
* **AND** `city()` SHALL return "Madrid"

#### Scenario: Null address rejected

* **WHEN** a PickupLocation is created with a null address
* **THEN** it SHALL throw ReservationDomainException

#### Scenario: Blank address rejected

* **WHEN** a PickupLocation is created with address "" or "   "
* **THEN** it SHALL throw ReservationDomainException

#### Scenario: Null city rejected

* **WHEN** a PickupLocation is created with a null city
* **THEN** it SHALL throw ReservationDomainException

#### Scenario: Blank city rejected

* **WHEN** a PickupLocation is created with city "" or "   "
* **THEN** it SHALL throw ReservationDomainException

### Requirement: ReservationStatus enum

ReservationStatus SHALL be an enum with values PENDING, CUSTOMER_VALIDATED, PAID, CONFIRMED, CANCELLING, and CANCELLED.

#### Scenario: All statuses exist

* **WHEN** ReservationStatus values are listed
* **THEN** they SHALL contain exactly PENDING, CUSTOMER_VALIDATED, PAID, CONFIRMED, CANCELLING, and CANCELLED (6 values)

## Constraint: Zero Spring dependencies

No class in `com.vehiclerental.reservation.domain.model.vo` SHALL import any type from `org.springframework.*`.
