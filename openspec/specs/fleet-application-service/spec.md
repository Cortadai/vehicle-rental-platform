# fleet-application-service Specification

## Purpose
Application Service orchestrating all Vehicle use cases. Implements input ports, delegates business logic to the domain, persists through output ports, and dispatches domain events. Contains zero business logic — pure orchestration.

## Requirements

### Requirement: FleetApplicationService implements all input ports

FleetApplicationService SHALL implement RegisterVehicleUseCase, GetVehicleUseCase, SendToMaintenanceUseCase, ActivateVehicleUseCase, and RetireVehicleUseCase.

#### Scenario: Service implements all use case interfaces

* **WHEN** FleetApplicationService is inspected
* **THEN** it SHALL implement all five use case interfaces

### Requirement: Register vehicle orchestration

The register use case SHALL convert command fields to domain Value Objects (LicensePlate, VehicleCategory, DailyRate), call `Vehicle.create()`, persist via VehicleRepository, publish domain events via FleetDomainEventPublisher, and return a VehicleResponse.

#### Scenario: Successful registration flow

* **WHEN** `execute(RegisterVehicleCommand)` is called with valid data
* **THEN** VehicleRepository.save() SHALL be called with the created Vehicle
* **AND** FleetDomainEventPublisher.publish() SHALL be called with the aggregate's domain events
* **AND** Vehicle.clearDomainEvents() SHALL be called after publishing
* **AND** a VehicleResponse SHALL be returned with the new vehicle's data

### Requirement: Get vehicle orchestration

The get use case SHALL load the vehicle by ID and return a VehicleResponse.

#### Scenario: Vehicle found

* **WHEN** `execute(GetVehicleCommand)` is called with an existing vehicle ID
* **THEN** a VehicleResponse SHALL be returned with the vehicle's data

#### Scenario: Vehicle not found

* **WHEN** `execute(GetVehicleCommand)` is called with a non-existing vehicle ID
* **THEN** it SHALL throw VehicleNotFoundException

### Requirement: Send to maintenance orchestration

The send-to-maintenance use case SHALL load the vehicle, call `vehicle.sendToMaintenance()`, persist, and publish events.

#### Scenario: Successful send to maintenance

* **WHEN** `execute(SendToMaintenanceCommand)` is called with an active vehicle's ID
* **THEN** VehicleRepository.save() SHALL be called
* **AND** FleetDomainEventPublisher.publish() SHALL be called with the aggregate's domain events
* **AND** Vehicle.clearDomainEvents() SHALL be called after publishing

#### Scenario: Vehicle not found for send to maintenance

* **WHEN** `execute(SendToMaintenanceCommand)` is called with a non-existing ID
* **THEN** it SHALL throw VehicleNotFoundException

### Requirement: Activate vehicle orchestration

The activate use case SHALL load the vehicle, call `vehicle.activate()`, persist, and publish events.

#### Scenario: Successful activation

* **WHEN** `execute(ActivateVehicleCommand)` is called with a vehicle under maintenance
* **THEN** VehicleRepository.save() SHALL be called
* **AND** FleetDomainEventPublisher.publish() SHALL be called with the aggregate's domain events
* **AND** Vehicle.clearDomainEvents() SHALL be called after publishing

#### Scenario: Vehicle not found for activation

* **WHEN** `execute(ActivateVehicleCommand)` is called with a non-existing ID
* **THEN** it SHALL throw VehicleNotFoundException

### Requirement: Retire vehicle orchestration

The retire use case SHALL load the vehicle, call `vehicle.retire()`, persist, and publish events.

#### Scenario: Successful retirement

* **WHEN** `execute(RetireVehicleCommand)` is called with an active or under-maintenance vehicle's ID
* **THEN** VehicleRepository.save() SHALL be called
* **AND** FleetDomainEventPublisher.publish() SHALL be called with the aggregate's domain events
* **AND** Vehicle.clearDomainEvents() SHALL be called after publishing

#### Scenario: Vehicle not found for retirement

* **WHEN** `execute(RetireVehicleCommand)` is called with a non-existing ID
* **THEN** it SHALL throw VehicleNotFoundException

### Requirement: Application Service has no business logic

FleetApplicationService SHALL NOT contain business logic (validation rules, state transitions, calculations). It SHALL only orchestrate: receive command, convert types, delegate to domain, persist, publish events, return response.

#### Scenario: Domain handles state transitions

* **WHEN** the send-to-maintenance use case is executed
* **THEN** the state transition logic SHALL be in `Vehicle.sendToMaintenance()`, not in the Application Service

### Requirement: VehicleNotFoundException for missing aggregates

VehicleNotFoundException SHALL extend RuntimeException directly. It is an application-level exception, NOT a domain exception. It SHALL carry the vehicle ID that was not found and use a descriptive message. It lives in the application module under `exception/`.

#### Scenario: Exception carries vehicle ID

* **WHEN** a VehicleNotFoundException is thrown for vehicle ID "abc-123"
* **THEN** `getMessage()` SHALL contain "abc-123"

#### Scenario: Exception is not a domain exception

* **WHEN** VehicleNotFoundException is inspected
* **THEN** it SHALL NOT extend FleetDomainException or DomainException

### Requirement: FleetApplicationMapper converts domain to DTOs

FleetApplicationMapper SHALL be a plain Java class that converts Vehicle domain objects to VehicleResponse records. It SHALL have no Spring annotations.

#### Scenario: Maps Vehicle to VehicleResponse

* **WHEN** `toResponse(Vehicle)` is called
* **THEN** it SHALL return a VehicleResponse with all fields mapped from the Vehicle aggregate

### Requirement: Transaction boundaries on write operations

Write use cases (register, sendToMaintenance, activate, retire) SHALL be annotated with `@Transactional`. Read use cases (get) SHALL be annotated with `@Transactional(readOnly = true)`.

#### Scenario: Register is transactional

* **WHEN** the register method is inspected
* **THEN** it SHALL be annotated with `@Transactional`

#### Scenario: Get is read-only transactional

* **WHEN** the get method is inspected
* **THEN** it SHALL be annotated with `@Transactional(readOnly = true)`

### Requirement: Constructor injection with no Spring annotations

FleetApplicationService SHALL use constructor injection with no `@Autowired` or `@Service` annotations. Bean registration happens in the container module's BeanConfiguration.

#### Scenario: No Spring annotations on class

* **WHEN** FleetApplicationService is inspected
* **THEN** it SHALL NOT have `@Service`, `@Component`, or `@Autowired` annotations

## Constraints

### Constraint: Minimal Spring dependency

FleetApplicationService SHALL only depend on `spring-tx` (for `@Transactional`). It SHALL NOT import types from `spring-context`, `spring-web`, `spring-data`, or any other Spring module.
