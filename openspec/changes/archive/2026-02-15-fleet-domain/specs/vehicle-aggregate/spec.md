# vehicle-aggregate Specification

## Purpose
Vehicle Aggregate Root with lifecycle management, identity by VehicleId, and domain event emission. Encapsulates all state changes through business methods with precondition validation. RETIRED is a terminal state.

## Requirements

### Requirement: Vehicle is an Aggregate Root

Vehicle SHALL be a concrete class extending `AggregateRoot<VehicleId>`. It SHALL hold licensePlate (LicensePlate VO), make (String), model (String), year (int), category (VehicleCategory enum), dailyRate (DailyRate VO), description (String, nullable), status (VehicleStatus enum), and createdAt (Instant).

#### Scenario: Vehicle fields accessible after creation

* **WHEN** a Vehicle is created with licensePlate "1234-BCD", make "Toyota", model "Corolla", year 2023, category SEDAN, dailyRate 50.00 EUR, description "GPS integrado"
* **THEN** `getLicensePlate()` SHALL return a LicensePlate with value "1234-BCD"
* **AND** `getMake()` SHALL return "Toyota"
* **AND** `getModel()` SHALL return "Corolla"
* **AND** `getYear()` SHALL return 2023
* **AND** `getCategory()` SHALL return SEDAN
* **AND** `getDailyRate()` SHALL return a DailyRate wrapping 50.00 EUR
* **AND** `getDescription()` SHALL return "GPS integrado"
* **AND** `getCreatedAt()` SHALL return a non-null Instant

### Requirement: Vehicle creation via factory method

Vehicle SHALL expose a static `create(LicensePlate licensePlate, String make, String model, int year, VehicleCategory category, DailyRate dailyRate, String description)` factory method. It SHALL generate a new VehicleId, set status to ACTIVE, set createdAt, and register a VehicleRegisteredEvent. `description` is nullable.

#### Scenario: Successful creation

* **WHEN** `Vehicle.create(licensePlate, "Toyota", "Corolla", 2023, SEDAN, dailyRate, "GPS integrado")` is called with valid arguments
* **THEN** the Vehicle SHALL have a non-null VehicleId
* **AND** the status SHALL be ACTIVE
* **AND** `getDomainEvents()` SHALL contain exactly one VehicleRegisteredEvent

#### Scenario: Null licensePlate rejected

* **WHEN** `Vehicle.create(null, "Toyota", "Corolla", 2023, SEDAN, dailyRate, null)` is called
* **THEN** it SHALL throw FleetDomainException

#### Scenario: Blank make rejected

* **WHEN** `Vehicle.create(licensePlate, "", "Corolla", 2023, SEDAN, dailyRate, null)` is called
* **THEN** it SHALL throw FleetDomainException

#### Scenario: Null make rejected

* **WHEN** `Vehicle.create(licensePlate, null, "Corolla", 2023, SEDAN, dailyRate, null)` is called
* **THEN** it SHALL throw FleetDomainException

#### Scenario: Blank model rejected

* **WHEN** `Vehicle.create(licensePlate, "Toyota", "", 2023, SEDAN, dailyRate, null)` is called
* **THEN** it SHALL throw FleetDomainException

#### Scenario: Null category rejected

* **WHEN** `Vehicle.create(licensePlate, "Toyota", "Corolla", 2023, null, dailyRate, null)` is called
* **THEN** it SHALL throw FleetDomainException

#### Scenario: Null dailyRate rejected

* **WHEN** `Vehicle.create(licensePlate, "Toyota", "Corolla", 2023, SEDAN, null, null)` is called
* **THEN** it SHALL throw FleetDomainException

#### Scenario: Year below minimum rejected

* **WHEN** `Vehicle.create(licensePlate, "Toyota", "Corolla", 1949, SEDAN, dailyRate, null)` is called
* **THEN** it SHALL throw FleetDomainException

#### Scenario: Year above maximum rejected

* **WHEN** `Vehicle.create(licensePlate, "Toyota", "Corolla", currentYear+2, SEDAN, dailyRate, null)` is called
* **THEN** it SHALL throw FleetDomainException

#### Scenario: Null description accepted

* **WHEN** `Vehicle.create(licensePlate, "Toyota", "Corolla", 2023, SEDAN, dailyRate, null)` is called
* **THEN** the Vehicle SHALL be created with description as null

#### Scenario: Description exceeding 500 characters rejected

* **WHEN** `Vehicle.create(licensePlate, "Toyota", "Corolla", 2023, SEDAN, dailyRate, descriptionOf501Chars)` is called
* **THEN** it SHALL throw FleetDomainException

### Requirement: Vehicle reconstruction from persistence

Vehicle SHALL expose a static `reconstruct(...)` factory method that rebuilds the aggregate from persisted state without generating events or running creation validations.

#### Scenario: Reconstruct does not emit events

* **WHEN** `Vehicle.reconstruct(...)` is called with valid persisted data
* **THEN** `getDomainEvents()` SHALL return an empty list
* **AND** all fields SHALL match the provided arguments

### Requirement: No public constructors

Vehicle SHALL NOT have public constructors. Instantiation SHALL only be possible through `create()` or `reconstruct()`.

#### Scenario: Constructor is not public

* **WHEN** the Vehicle class is inspected
* **THEN** it SHALL have no public constructors

### Requirement: SendToMaintenance lifecycle transition

Vehicle SHALL expose a `sendToMaintenance()` method that transitions from ACTIVE to UNDER_MAINTENANCE and registers a VehicleSentToMaintenanceEvent.

#### Scenario: Send active vehicle to maintenance

* **WHEN** `sendToMaintenance()` is called on a Vehicle with status ACTIVE
* **THEN** the status SHALL change to UNDER_MAINTENANCE
* **AND** `getDomainEvents()` SHALL contain a VehicleSentToMaintenanceEvent

#### Scenario: Send non-active vehicle to maintenance rejected

* **WHEN** `sendToMaintenance()` is called on a Vehicle with status UNDER_MAINTENANCE or RETIRED
* **THEN** it SHALL throw FleetDomainException

### Requirement: Activate lifecycle transition

Vehicle SHALL expose an `activate()` method that transitions from UNDER_MAINTENANCE to ACTIVE and registers a VehicleActivatedEvent.

#### Scenario: Activate a vehicle under maintenance

* **WHEN** `activate()` is called on a Vehicle with status UNDER_MAINTENANCE
* **THEN** the status SHALL change to ACTIVE
* **AND** `getDomainEvents()` SHALL contain a VehicleActivatedEvent

#### Scenario: Activate a non-maintenance vehicle rejected

* **WHEN** `activate()` is called on a Vehicle with status ACTIVE or RETIRED
* **THEN** it SHALL throw FleetDomainException

### Requirement: Retire lifecycle transition

Vehicle SHALL expose a `retire()` method that transitions from ACTIVE or UNDER_MAINTENANCE to RETIRED and registers a VehicleRetiredEvent.

#### Scenario: Retire an active vehicle

* **WHEN** `retire()` is called on a Vehicle with status ACTIVE
* **THEN** the status SHALL change to RETIRED
* **AND** `getDomainEvents()` SHALL contain a VehicleRetiredEvent

#### Scenario: Retire a vehicle under maintenance

* **WHEN** `retire()` is called on a Vehicle with status UNDER_MAINTENANCE
* **THEN** the status SHALL change to RETIRED
* **AND** `getDomainEvents()` SHALL contain a VehicleRetiredEvent

#### Scenario: Retire an already retired vehicle rejected

* **WHEN** `retire()` is called on a Vehicle with status RETIRED
* **THEN** it SHALL throw FleetDomainException

### Requirement: isAvailable convenience query

Vehicle SHALL expose an `isAvailable()` method that returns true only when status is ACTIVE. The semantics differ from Customer's `isActive()`: here `isAvailable()` means "can be assigned to a reservation", which for now equals ACTIVE but carries the correct intent for future Reservation Service integration.

#### Scenario: Available vehicle returns true

* **WHEN** `isAvailable()` is called on a Vehicle with status ACTIVE
* **THEN** it SHALL return true

#### Scenario: Non-available vehicle returns false

* **WHEN** `isAvailable()` is called on a Vehicle with status UNDER_MAINTENANCE or RETIRED
* **THEN** it SHALL return false

### Requirement: FleetDomainException extends DomainException with errorCode

FleetDomainException SHALL be a concrete class extending `DomainException` from common. It SHALL provide constructors that require a business-language errorCode (e.g., "VEHICLE_INVALID_STATE", "VEHICLE_ALREADY_RETIRED").

#### Scenario: Exception carries errorCode

* **WHEN** a lifecycle transition throws FleetDomainException
* **THEN** `getErrorCode()` SHALL return a non-blank business error code
* **AND** `getMessage()` SHALL return a descriptive message

#### Scenario: Invalid transition includes current state in message

* **WHEN** `sendToMaintenance()` is called on a RETIRED vehicle
* **THEN** the exception message SHALL indicate the current state that prevented the transition

## Constraint: Zero Spring dependencies

No class in `com.vehiclerental.fleet.domain` SHALL import any type from `org.springframework.*`.
