# fleet-domain-events Specification

## Purpose
Domain events for the Vehicle aggregate lifecycle: registration (full snapshot) and state transitions (ID-only). All events are records implementing the DomainEvent interface from common.

## Requirements

### Requirement: VehicleRegisteredEvent carries full vehicle snapshot

VehicleRegisteredEvent SHALL be a Java record implementing `DomainEvent`. It SHALL carry eventId (UUID), occurredOn (Instant), vehicleId (VehicleId), licensePlate (String), make (String), model (String), year (int), category (VehicleCategory), dailyRate (Money), and description (String, nullable).

#### Scenario: VehicleRegisteredEvent satisfies DomainEvent contract

* **WHEN** a VehicleRegisteredEvent is created
* **THEN** `eventId()` SHALL return a non-null UUID
* **AND** `occurredOn()` SHALL return a non-null Instant

#### Scenario: VehicleRegisteredEvent carries vehicle data

* **WHEN** a Vehicle is created with licensePlate "1234-BCD", make "Toyota", model "Corolla", year 2023, category SEDAN, dailyRate 50.00 EUR, description "GPS integrado"
* **THEN** the VehicleRegisteredEvent SHALL carry all those values
* **AND** `description()` SHALL return "GPS integrado"

#### Scenario: VehicleRegisteredEvent with null description

* **WHEN** a Vehicle is created with description as null
* **THEN** the VehicleRegisteredEvent `description()` SHALL return null

### Requirement: VehicleSentToMaintenanceEvent carries vehicle ID

VehicleSentToMaintenanceEvent SHALL be a Java record implementing `DomainEvent`. It SHALL carry eventId (UUID), occurredOn (Instant), and vehicleId (VehicleId).

#### Scenario: VehicleSentToMaintenanceEvent satisfies DomainEvent contract

* **WHEN** a VehicleSentToMaintenanceEvent is created
* **THEN** `eventId()` SHALL return a non-null UUID
* **AND** `occurredOn()` SHALL return a non-null Instant
* **AND** `vehicleId()` SHALL return the vehicle's VehicleId

### Requirement: VehicleActivatedEvent carries vehicle ID

VehicleActivatedEvent SHALL be a Java record implementing `DomainEvent`. It SHALL carry eventId (UUID), occurredOn (Instant), and vehicleId (VehicleId).

#### Scenario: VehicleActivatedEvent satisfies DomainEvent contract

* **WHEN** a VehicleActivatedEvent is created
* **THEN** `eventId()` SHALL return a non-null UUID
* **AND** `occurredOn()` SHALL return a non-null Instant
* **AND** `vehicleId()` SHALL return the vehicle's VehicleId

### Requirement: VehicleRetiredEvent carries vehicle ID

VehicleRetiredEvent SHALL be a Java record implementing `DomainEvent`. It SHALL carry eventId (UUID), occurredOn (Instant), and vehicleId (VehicleId).

#### Scenario: VehicleRetiredEvent satisfies DomainEvent contract

* **WHEN** a VehicleRetiredEvent is created
* **THEN** `eventId()` SHALL return a non-null UUID
* **AND** `occurredOn()` SHALL return a non-null Instant
* **AND** `vehicleId()` SHALL return the vehicle's VehicleId

### Requirement: Events are immutable records

All fleet domain events SHALL be Java records. They SHALL have no setters and no mutable state.

#### Scenario: Event fields are read-only

* **WHEN** a VehicleRegisteredEvent is inspected
* **THEN** it SHALL be a record type
* **AND** it SHALL have no setter methods

## Constraint: Zero Spring dependencies

No class in `com.vehiclerental.fleet.domain.event` SHALL import any type from `org.springframework.*`.
