## 1. Module Setup

- [x] 1.1 Add `fleet-domain` to root POM `<dependencyManagement>` with `${vehicle-rental.version}` and add `fleet-service/fleet-domain` to `<modules>` list
- [x] 1.2 Create `fleet-service/fleet-domain/pom.xml` — inherits from root parent POM, depends on `common`, zero Spring dependencies in compile scope, packaging `jar`
- [x] 1.3 Create directory structure under `fleet-service/fleet-domain/src/main/java/com/vehiclerental/fleet/domain/` with subdirectories: `model/aggregate/`, `model/vo/`, `event/`, `exception/`, `port/output/`
- [x] 1.4 Create test directory structure under `fleet-service/fleet-domain/src/test/java/com/vehiclerental/fleet/domain/` with subdirectories: `model/aggregate/`, `model/vo/`, `event/`, `exception/`
- [x] 1.5 Verify module compiles: `mvn clean compile` from `fleet-service/fleet-domain/`

## 2. Domain Exception (first — needed by VOs and aggregate)

- [x] 2.1 Create `FleetDomainExceptionTest.java` — tests for errorCode accessible, message accessible, constructor with cause
- [x] 2.2 Create `FleetDomainException.java` — extends DomainException from common, constructors requiring errorCode

## 3. Value Objects and Enums (Test-First)

- [x] 3.1 Create `VehicleIdTest.java` — tests for valid construction, null UUID rejected (throws FleetDomainException), equality by value
- [x] 3.2 Create `VehicleId.java` — record wrapping UUID, null validation in compact constructor
- [x] 3.3 Create `LicensePlateTest.java` — tests for valid plate ("1234-BCD"), spaces accepted ("ABC 1234"), null rejected, blank rejected, too short (<2), too long (>15), special characters rejected
- [x] 3.4 Create `LicensePlate.java` — record wrapping String, alphanumeric pattern with hyphens/spaces, length 2-15
- [x] 3.5 Create `VehicleCategory.java` — enum with SEDAN, SUV, VAN, MOTORCYCLE
- [x] 3.6 Create `VehicleStatus.java` — enum with ACTIVE, UNDER_MAINTENANCE, RETIRED
- [x] 3.7 Create `DailyRateTest.java` — tests for valid construction (50.00 EUR), null Money rejected, zero amount rejected, equality by value
- [x] 3.8 Create `DailyRate.java` — record wrapping Money, validates strictly positive (`amount.signum() > 0`)

## 4. Domain Events (Test-First)

- [x] 4.1 Create `FleetDomainEventsTest.java` — tests for: VehicleRegisteredEvent fields accessible (vehicleId, licensePlate, make, model, year, category, dailyRate, description), null description accepted, all events implement DomainEvent, lifecycle events (SentToMaintenance, Activated, Retired) carry vehicleId with non-null eventId/occurredOn, events are records
- [x] 4.2 Create `VehicleRegisteredEvent.java` — record implementing DomainEvent with vehicleId, licensePlate, make, model, year, category, dailyRate (Money), description (nullable)
- [x] 4.3 Create `VehicleSentToMaintenanceEvent.java` — record implementing DomainEvent with vehicleId
- [x] 4.4 Create `VehicleActivatedEvent.java` — record implementing DomainEvent with vehicleId
- [x] 4.5 Create `VehicleRetiredEvent.java` — record implementing DomainEvent with vehicleId

## 5. Vehicle Aggregate Root (Test-First)

- [x] 5.1 Create `VehicleTest.java` — tests for: successful creation (status ACTIVE, VehicleId generated, VehicleRegisteredEvent emitted), null/blank make rejected, null/blank model rejected, null licensePlate rejected, null category rejected, null dailyRate rejected, year below 1950 rejected, year above currentYear+1 rejected, null description accepted, description >500 chars rejected, fields accessible after creation, reconstruct does not emit events, no public constructors
- [x] 5.2 Create `VehicleLifecycleTest.java` — tests for: sendToMaintenance on active (UNDER_MAINTENANCE + event), sendToMaintenance on non-active throws, activate on under-maintenance (ACTIVE + event), activate on non-maintenance throws, retire active (RETIRED + event), retire under-maintenance (RETIRED + event), retire already retired throws, isAvailable true for ACTIVE, isAvailable false for UNDER_MAINTENANCE/RETIRED, exception carries errorCode, exception message includes current state
- [x] 5.3 Create `Vehicle.java` — Aggregate Root extending AggregateRoot<VehicleId>, private constructor, `create()` factory method, `reconstruct()` factory method, `sendToMaintenance()`, `activate()`, `retire()`, `isAvailable()`, getters only

## 6. Output Port

- [x] 6.1 Create `VehicleRepository.java` — interface with `save(Vehicle)` returning Vehicle and `findById(VehicleId)` returning `Optional<Vehicle>`, domain types only

## 7. Verification

- [x] 7.1 Run `mvn clean install` from `fleet-service/fleet-domain/` — compile + all tests pass
- [x] 7.2 Verify zero Spring imports: grep `org.springframework` in `fleet-service/fleet-domain/src/main/` returns nothing
- [x] 7.3 Run `mvn clean install` from root — full platform build passes (common + customer modules + fleet-domain)
