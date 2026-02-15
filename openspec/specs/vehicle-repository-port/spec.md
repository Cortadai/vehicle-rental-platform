# vehicle-repository-port Specification

## Purpose
Output port interface for Vehicle persistence. Uses only domain types. Lives in the domain module until the application module is created.

## Requirements

### Requirement: VehicleRepository defines save and findById

VehicleRepository SHALL be a Java interface with two methods:
- `Vehicle save(Vehicle vehicle)` — persists a Vehicle and returns the persisted instance
- `Optional<Vehicle> findById(VehicleId vehicleId)` — retrieves a Vehicle by its typed ID

#### Scenario: Save method signature

* **WHEN** the VehicleRepository interface is inspected
* **THEN** it SHALL declare a `save` method accepting `Vehicle` and returning `Vehicle`

#### Scenario: FindById method signature

* **WHEN** the VehicleRepository interface is inspected
* **THEN** it SHALL declare a `findById` method accepting `VehicleId` and returning `Optional<Vehicle>`

### Requirement: VehicleRepository uses only domain types

VehicleRepository SHALL NOT reference any Spring, JPA, or infrastructure types. Method signatures SHALL use only `Vehicle`, `VehicleId`, and `java.util.Optional`.

#### Scenario: No framework imports

* **WHEN** the VehicleRepository source file is inspected
* **THEN** it SHALL contain no imports from `org.springframework.*`
* **AND** it SHALL contain no imports from `jakarta.persistence.*`

## Constraint: Zero Spring dependencies

No class in `com.vehiclerental.fleet.domain.port.output` SHALL import any type from `org.springframework.*`.
