# fleet-application-ports Specification

## Purpose
Input and output port interfaces for the Fleet Service application layer. Input ports define use case contracts (one interface per use case). Output ports define what the application needs from external systems (event publishing).

## Requirements

### Requirement: RegisterVehicleUseCase input port

RegisterVehicleUseCase SHALL be an interface with a single method `execute(RegisterVehicleCommand)` returning `VehicleResponse`.

#### Scenario: Interface declares execute method

* **WHEN** the RegisterVehicleUseCase interface is inspected
* **THEN** it SHALL declare `VehicleResponse execute(RegisterVehicleCommand command)`

### Requirement: GetVehicleUseCase input port

GetVehicleUseCase SHALL be an interface with a single method `execute(GetVehicleCommand)` returning `VehicleResponse`.

#### Scenario: Interface declares execute method

* **WHEN** the GetVehicleUseCase interface is inspected
* **THEN** it SHALL declare `VehicleResponse execute(GetVehicleCommand command)`

### Requirement: SendToMaintenanceUseCase input port

SendToMaintenanceUseCase SHALL be an interface with a single method `execute(SendToMaintenanceCommand)` returning void.

#### Scenario: Interface declares execute method

* **WHEN** the SendToMaintenanceUseCase interface is inspected
* **THEN** it SHALL declare `void execute(SendToMaintenanceCommand command)`

### Requirement: ActivateVehicleUseCase input port

ActivateVehicleUseCase SHALL be an interface with a single method `execute(ActivateVehicleCommand)` returning void.

#### Scenario: Interface declares execute method

* **WHEN** the ActivateVehicleUseCase interface is inspected
* **THEN** it SHALL declare `void execute(ActivateVehicleCommand command)`

### Requirement: RetireVehicleUseCase input port

RetireVehicleUseCase SHALL be an interface with a single method `execute(RetireVehicleCommand)` returning void.

#### Scenario: Interface declares execute method

* **WHEN** the RetireVehicleUseCase interface is inspected
* **THEN** it SHALL declare `void execute(RetireVehicleCommand command)`

### Requirement: FleetDomainEventPublisher output port

FleetDomainEventPublisher SHALL be an interface with a single method `publish(List<DomainEvent>)` returning void. It SHALL use only domain types from common.

#### Scenario: Interface declares publish method

* **WHEN** the FleetDomainEventPublisher interface is inspected
* **THEN** it SHALL declare `void publish(List<DomainEvent> events)`

#### Scenario: No framework types in signature

* **WHEN** the FleetDomainEventPublisher interface is inspected
* **THEN** it SHALL NOT import any type from `org.springframework.*`

### Requirement: All ports use application-layer types only

Input port interfaces SHALL use Command and Response DTOs from the application layer, not raw primitives or domain types directly.

#### Scenario: Input ports reference application DTOs

* **WHEN** any input port interface is inspected
* **THEN** its method parameters SHALL be Command records from `dto.command`
* **AND** its return types SHALL be Response records from `dto.response` or void

## Constraints

### Constraint: Zero Spring dependencies in port interfaces

No interface in `com.vehiclerental.fleet.application.port` SHALL import any type from `org.springframework.*`.
