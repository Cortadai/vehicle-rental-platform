# vehicle-value-objects Specification

## Purpose
Typed ID (VehicleId), LicensePlate with format validation, VehicleCategory enum, VehicleStatus enum, and DailyRate wrapping Money for the Fleet domain.

## Requirements

### Requirement: VehicleId is a typed ID

VehicleId SHALL be a Java record wrapping a UUID. It SHALL reject null UUID values by throwing FleetDomainException.

#### Scenario: Valid VehicleId construction

* **WHEN** a VehicleId is created with a non-null UUID
* **THEN** the `value()` accessor SHALL return that UUID

#### Scenario: Null UUID rejected

* **WHEN** a VehicleId is created with a null UUID
* **THEN** it SHALL throw FleetDomainException

#### Scenario: Equality by value

* **WHEN** two VehicleId instances are created with the same UUID
* **THEN** they SHALL be equal

### Requirement: LicensePlate validates format

LicensePlate SHALL be a Java record wrapping a String value. It SHALL validate that the value is non-null, non-blank, between 2 and 15 characters, and matches an alphanumeric pattern allowing hyphens and spaces.

#### Scenario: Valid license plate accepted

* **WHEN** a LicensePlate is created with "1234-BCD"
* **THEN** the `value()` accessor SHALL return "1234-BCD"

#### Scenario: License plate with spaces accepted

* **WHEN** a LicensePlate is created with "ABC 1234"
* **THEN** it SHALL be created successfully

#### Scenario: Null value rejected

* **WHEN** a LicensePlate is created with a null value
* **THEN** it SHALL throw FleetDomainException

#### Scenario: Blank value rejected

* **WHEN** a LicensePlate is created with "" or " "
* **THEN** it SHALL throw FleetDomainException

#### Scenario: Too short rejected

* **WHEN** a LicensePlate is created with "A"
* **THEN** it SHALL throw FleetDomainException

#### Scenario: Too long rejected

* **WHEN** a LicensePlate is created with a value longer than 15 characters
* **THEN** it SHALL throw FleetDomainException

#### Scenario: Special characters rejected

* **WHEN** a LicensePlate is created with "AB@#123"
* **THEN** it SHALL throw FleetDomainException

### Requirement: VehicleCategory enum

VehicleCategory SHALL be an enum with values SEDAN, SUV, VAN, and MOTORCYCLE.

#### Scenario: All categories exist

* **WHEN** VehicleCategory values are listed
* **THEN** they SHALL contain exactly SEDAN, SUV, VAN, and MOTORCYCLE

### Requirement: VehicleStatus enum

VehicleStatus SHALL be an enum with values ACTIVE, UNDER_MAINTENANCE, and RETIRED.

#### Scenario: All statuses exist

* **WHEN** VehicleStatus values are listed
* **THEN** they SHALL contain exactly ACTIVE, UNDER_MAINTENANCE, and RETIRED

### Requirement: DailyRate wraps Money with positive amount validation

DailyRate SHALL be a Java record wrapping a `Money` instance from common. It SHALL validate that the Money amount is strictly positive (greater than zero).

#### Scenario: Valid DailyRate construction

* **WHEN** a DailyRate is created with a Money of 50.00 EUR
* **THEN** the `money()` accessor SHALL return that Money instance

#### Scenario: Null Money rejected

* **WHEN** a DailyRate is created with null
* **THEN** it SHALL throw FleetDomainException

#### Scenario: Zero amount rejected

* **WHEN** a DailyRate is created with a Money of 0.00 EUR
* **THEN** it SHALL throw FleetDomainException

#### Scenario: DailyRate equality by value

* **WHEN** two DailyRate instances are created with the same Money (50.00 EUR)
* **THEN** they SHALL be equal

## Constraint: Zero Spring dependencies

No class in `com.vehiclerental.fleet.domain.model.vo` SHALL import any type from `org.springframework.*`.
