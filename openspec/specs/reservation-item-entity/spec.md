# reservation-item-entity Specification

## Purpose
ReservationItem inner entity with factory method, subtotal calculation (dailyRate x days), and validation rules.

## Requirements

### Requirement: ReservationItem is an inner entity

ReservationItem SHALL be a concrete class extending `BaseEntity<UUID>` from common. It SHALL hold vehicleId (VehicleId), dailyRate (Money), days (int), and subtotal (Money). It exists only within the Reservation aggregate boundary.

#### Scenario: ReservationItem fields accessible after creation

* **WHEN** a ReservationItem is created with vehicleId, dailyRate of 100.00 EUR, and 3 days
* **THEN** `getVehicleId()` SHALL return the provided VehicleId
* **AND** `getDailyRate()` SHALL return Money with amount 100.00
* **AND** `getDays()` SHALL return 3
* **AND** `getSubtotal()` SHALL return Money with amount 300.00
* **AND** `getId()` SHALL return a non-null UUID

### Requirement: ReservationItem creation via factory method

ReservationItem SHALL expose a static `create(VehicleId, Money dailyRate, int days)` factory method. It SHALL generate a UUID as its identity, calculate subtotal as dailyRate multiplied by days, and validate inputs.

#### Scenario: Subtotal calculated correctly

* **WHEN** `ReservationItem.create(vehicleId, Money(50.00, EUR), 5)` is called
* **THEN** `getSubtotal()` SHALL return Money with amount 250.00 and currency EUR

#### Scenario: Single day reservation

* **WHEN** `ReservationItem.create(vehicleId, Money(100.00, EUR), 1)` is called
* **THEN** `getSubtotal()` SHALL equal the dailyRate (100.00 EUR)

#### Scenario: Null vehicleId rejected

* **WHEN** `ReservationItem.create(null, dailyRate, 3)` is called
* **THEN** it SHALL throw ReservationDomainException

#### Scenario: Null dailyRate rejected

* **WHEN** `ReservationItem.create(vehicleId, null, 3)` is called
* **THEN** it SHALL throw ReservationDomainException

#### Scenario: Zero dailyRate amount rejected

* **WHEN** `ReservationItem.create(vehicleId, Money(0.00, EUR), 3)` is called
* **THEN** it SHALL throw ReservationDomainException

#### Scenario: Zero days rejected

* **WHEN** `ReservationItem.create(vehicleId, dailyRate, 0)` is called
* **THEN** it SHALL throw ReservationDomainException

#### Scenario: Negative days rejected

* **WHEN** `ReservationItem.create(vehicleId, dailyRate, -1)` is called
* **THEN** it SHALL throw ReservationDomainException

### Requirement: ReservationItem reconstruction from persistence

ReservationItem SHALL expose a static `reconstruct(UUID id, VehicleId, Money dailyRate, int days, Money subtotal)` factory method that rebuilds the entity from persisted state without validation.

#### Scenario: Reconstruct preserves all fields

* **WHEN** `ReservationItem.reconstruct(id, vehicleId, dailyRate, 3, subtotal)` is called
* **THEN** all fields SHALL match the provided arguments exactly

### Requirement: No public constructors

ReservationItem SHALL NOT have public constructors. Instantiation SHALL only be possible through `create()` or `reconstruct()`.

#### Scenario: Constructor is not public

* **WHEN** the ReservationItem class is inspected via reflection
* **THEN** it SHALL have no public constructors

## Constraint: Zero Spring dependencies

No class in `com.vehiclerental.reservation.domain.model.entity` SHALL import any type from `org.springframework.*`.
