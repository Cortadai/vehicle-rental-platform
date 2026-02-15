# fleet-application-dtos Specification

## Purpose
Command and Response records for Vehicle use cases. Commands carry input data for write operations. Responses carry output data for queries and registration results. All are Java records (immutable).

## Requirements

### Requirement: RegisterVehicleCommand carries registration data

RegisterVehicleCommand SHALL be a Java record with fields: licensePlate (String), make (String), model (String), year (int), category (String), dailyRateAmount (BigDecimal), dailyRateCurrency (String), description (String, nullable).

#### Scenario: All fields accessible

* **WHEN** a RegisterVehicleCommand is constructed
* **THEN** `licensePlate()`, `make()`, `model()`, `category()` SHALL return the provided String values
* **AND** `year()` SHALL return the provided int
* **AND** `dailyRateAmount()` SHALL return the provided BigDecimal
* **AND** `dailyRateCurrency()` SHALL return the provided String
* **AND** `description()` SHALL return the provided value or null

### Requirement: GetVehicleCommand carries vehicle identity

GetVehicleCommand SHALL be a Java record with a single field: vehicleId (String).

#### Scenario: VehicleId accessible

* **WHEN** a GetVehicleCommand is constructed with "some-uuid"
* **THEN** `vehicleId()` SHALL return "some-uuid"

### Requirement: SendToMaintenanceCommand carries vehicle identity

SendToMaintenanceCommand SHALL be a Java record with a single field: vehicleId (String).

#### Scenario: VehicleId accessible

* **WHEN** a SendToMaintenanceCommand is constructed
* **THEN** `vehicleId()` SHALL return the provided value

### Requirement: ActivateVehicleCommand carries vehicle identity

ActivateVehicleCommand SHALL be a Java record with a single field: vehicleId (String).

#### Scenario: VehicleId accessible

* **WHEN** an ActivateVehicleCommand is constructed
* **THEN** `vehicleId()` SHALL return the provided value

### Requirement: RetireVehicleCommand carries vehicle identity

RetireVehicleCommand SHALL be a Java record with a single field: vehicleId (String).

#### Scenario: VehicleId accessible

* **WHEN** a RetireVehicleCommand is constructed
* **THEN** `vehicleId()` SHALL return the provided value

### Requirement: VehicleResponse carries vehicle snapshot

VehicleResponse SHALL be a Java record with fields: vehicleId (String), licensePlate (String), make (String), model (String), year (int), category (String), dailyRateAmount (BigDecimal), dailyRateCurrency (String), description (String, nullable), status (String), createdAt (Instant).

#### Scenario: All fields accessible

* **WHEN** a VehicleResponse is constructed
* **THEN** all fields SHALL be accessible via record accessors

### Requirement: Commands and Responses are plain Java records

All commands and responses SHALL be Java records with no Spring annotations and no validation annotations. They SHALL have zero framework dependencies.

#### Scenario: No framework imports

* **WHEN** any command or response class is inspected
* **THEN** it SHALL NOT import any type from `org.springframework.*` or `jakarta.*`

## Constraints

### Constraint: Zero Spring dependencies

No class in `com.vehiclerental.fleet.application.dto` SHALL import any type from `org.springframework.*`.
