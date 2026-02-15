## 1. Maven Module Setup

- [x] 1.1 Create `fleet-service/fleet-application/pom.xml` with parent inheritance, `fleet-domain` dependency, and `spring-tx` dependency (same structure as customer-application POM)
- [x] 1.2 Add `fleet-service/fleet-application` to root POM `<modules>` (after `fleet-service/fleet-domain`, before `fleet-service/fleet-infrastructure`)
- [x] 1.3 Add `fleet-application` to root POM `<dependencyManagement>` with `${vehicle-rental.version}`
- [x] 1.4 Create source directory structure: `fleet-service/fleet-application/src/main/java/com/vehiclerental/fleet/application/` and `src/test/java/com/vehiclerental/fleet/application/`
- [x] 1.5 Verify `mvn clean install -pl fleet-service/fleet-application` compiles successfully

## 2. Command and Response DTOs

- [x] 2.1 Create `RegisterVehicleCommand` record with fields: licensePlate (String), make (String), model (String), year (int), category (String), dailyRateAmount (BigDecimal), dailyRateCurrency (String), description (String)
- [x] 2.2 Create `GetVehicleCommand` record with field: vehicleId (String)
- [x] 2.3 Create `SendToMaintenanceCommand` record with field: vehicleId (String)
- [x] 2.4 Create `ActivateVehicleCommand` record with field: vehicleId (String)
- [x] 2.5 Create `RetireVehicleCommand` record with field: vehicleId (String)
- [x] 2.6 Create `VehicleResponse` record with fields: vehicleId (String), licensePlate (String), make (String), model (String), year (int), category (String), dailyRateAmount (BigDecimal), dailyRateCurrency (String), description (String), status (String), createdAt (Instant)

## 3. Port Interfaces

- [x] 3.1 Create `RegisterVehicleUseCase` interface with `VehicleResponse execute(RegisterVehicleCommand command)`
- [x] 3.2 Create `GetVehicleUseCase` interface with `VehicleResponse execute(GetVehicleCommand command)`
- [x] 3.3 Create `SendToMaintenanceUseCase` interface with `void execute(SendToMaintenanceCommand command)`
- [x] 3.4 Create `ActivateVehicleUseCase` interface with `void execute(ActivateVehicleCommand command)`
- [x] 3.5 Create `RetireVehicleUseCase` interface with `void execute(RetireVehicleCommand command)`
- [x] 3.6 Create `FleetDomainEventPublisher` output port interface with `void publish(List<DomainEvent> events)`

## 4. Exception and Mapper

- [x] 4.1 Create `VehicleNotFoundException` extending `RuntimeException` in `exception/` package, carrying vehicleId String in message
- [x] 4.2 Create `FleetApplicationMapper` in `mapper/` package with `toResponse(Vehicle)` method mapping all fields (plain Java, no annotations)

## 5. Application Service

- [x] 5.1 Create `FleetApplicationService` implementing all 5 input port interfaces, with constructor injection of VehicleRepository, FleetDomainEventPublisher, and FleetApplicationMapper
- [x] 5.2 Implement `RegisterVehicleUseCase.execute`: convert command to domain VOs, call `Vehicle.create()`, save, publish events, clear events, return mapped response
- [x] 5.3 Implement `GetVehicleUseCase.execute`: parse VehicleId, find by ID or throw VehicleNotFoundException, return mapped response (annotated `@Transactional(readOnly = true)`)
- [x] 5.4 Implement `SendToMaintenanceUseCase.execute`: find vehicle or throw, call `vehicle.sendToMaintenance()`, save, publish events, clear events
- [x] 5.5 Implement `ActivateVehicleUseCase.execute`: find vehicle or throw, call `vehicle.activate()`, save, publish events, clear events
- [x] 5.6 Implement `RetireVehicleUseCase.execute`: find vehicle or throw, call `vehicle.retire()`, save, publish events, clear events
- [x] 5.7 Add `@Transactional` to write methods and `@Transactional(readOnly = true)` to read method

## 6. Unit Tests

- [x] 6.1 Create `FleetApplicationServiceTest` with mocked VehicleRepository, FleetDomainEventPublisher, and FleetApplicationMapper
- [x] 6.2 Test register: verifies save, publish, clearDomainEvents, and response mapping
- [x] 6.3 Test get (found): verifies findById and response mapping
- [x] 6.4 Test get (not found): verifies VehicleNotFoundException thrown
- [x] 6.5 Test sendToMaintenance (found): verifies save and publish called
- [x] 6.6 Test sendToMaintenance (not found): verifies VehicleNotFoundException thrown
- [x] 6.7 Test activate (found): verifies save and publish called
- [x] 6.8 Test activate (not found): verifies VehicleNotFoundException thrown
- [x] 6.9 Test retire (found): verifies save and publish called
- [x] 6.10 Test retire (not found): verifies VehicleNotFoundException thrown
- [x] 6.11 Create `FleetApplicationMapperTest` — verifies mapping of all fields including description null case
- [x] 6.12 Create `VehicleNotFoundExceptionTest` — verifies it does NOT extend FleetDomainException, and that getMessage() contains the vehicleId

## 7. Build Verification

- [x] 7.1 Run `mvn clean install -pl fleet-service/fleet-application` — all tests pass
- [x] 7.2 Run `grep -r "@Service\|@Component" fleet-service/fleet-application/src/main/` and verify zero matches (only `@Transactional` allowed)
- [x] 7.3 Run full `mvn clean install` from root — all modules compile and tests pass
