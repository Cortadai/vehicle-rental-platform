## Context

The fleet-domain module is complete with: Vehicle aggregate root (create, reconstruct, sendToMaintenance, activate, retire), typed Value Objects (VehicleId, LicensePlate, VehicleCategory, VehicleStatus, DailyRate), domain events (VehicleRegistered, VehicleSentToMaintenance, VehicleActivated, VehicleRetired), FleetDomainException, and a VehicleRepository output port interface. The domain has 50 passing tests and zero Spring dependencies.

The application layer bridges the domain with the outside world. Per docs/17, it's a separate Maven module (`fleet-application`) that depends on `fleet-domain` and adds only `spring-tx` for `@Transactional`. Infrastructure adapters (future change) will depend on this module to call use cases through input ports.

This is the second application module in the platform. The customer-application module established the pattern (17 tests, 13 classes). Fleet replicates it with adaptations for the Vehicle domain: different use case names (Register instead of Create, SendToMaintenance/Activate/Retire instead of Suspend/Activate/Delete), more fields in the response (licensePlate, make, model, year, category, dailyRate, description), and DailyRate/LicensePlate as command input types expressed as strings.

## Goals / Non-Goals

**Goals:**

* Replicate the application layer pattern established by customer-application
* Define input ports (one interface per use case) with command/response DTOs
* Add FleetDomainEventPublisher output port for event dispatch
* Implement FleetApplicationService as pure orchestrator (zero business logic)
* Add VehicleNotFoundException for "not found" scenarios (application-level exception)
* Add FleetApplicationMapper (manual, plain Java)
* Unit test the application service with mocked output ports
* Update root POM to include fleet-application module

**Non-Goals:**

* Domain Service — Vehicle doesn't need cross-aggregate coordination
* SAGA step implementation — belongs in a future change when Reservation Service coordinates
* Outbox scheduler — belongs in infrastructure layer
* MapStruct mapper — manual mapper is sufficient for ~10 fields
* Input validation on commands — validation happens in domain Value Objects (LicensePlate, DailyRate constructors)
* License plate uniqueness validation — belongs in infrastructure/persistence layer
* Relocating VehicleRepository — the output port stays in domain where it was placed in fleet-domain change
* Update use case (modifying vehicle details after registration) — not in fleet-domain scope

## Decisions

### Decision 1: VehicleRepository stays in domain

**Choice**: Leave `VehicleRepository` in `fleet-domain/port/output/`. The application module depends on fleet-domain and uses the port directly.

**Rationale**: Same decision as customer-application. The repository interface uses only domain types (`Vehicle`, `VehicleId`). Moving it adds noise for no benefit. Consistent with the established pattern.

**Alternative rejected**: Move to `fleet-application/port/output/`. Unnecessary churn — the interface is framework-free regardless of location.

### Decision 2: One Application Service implementing all input ports

**Choice**: A single `FleetApplicationService` implements all 5 use case interfaces (RegisterVehicle, GetVehicle, SendToMaintenance, ActivateVehicle, RetireVehicle).

**Rationale**: Same reasoning as customer-application. Five simple orchestration methods don't justify five separate classes. Each use case already has its own interface, so extraction is trivial if a use case grows complex.

**Alternative rejected**: One service class per use case.

### Decision 3: RegisterVehicleCommand carries primitive/string types

**Choice**: `RegisterVehicleCommand` carries `String licensePlate`, `String make`, `String model`, `int year`, `String category`, `BigDecimal dailyRateAmount`, `String dailyRateCurrency`, `String description`. The Application Service converts these to domain types (`new LicensePlate(...)`, `VehicleCategory.valueOf(...)`, `new DailyRate(new Money(...))`).

**Rationale**: Commands are the boundary between the outside world and the domain. REST controllers work with JSON primitives. The translation to typed domain objects is the application layer's responsibility. DailyRate requires amount + currency because `Money` is a compound type — splitting into two fields is more natural for API consumers than a nested object.

**Alternative rejected**: Commands with domain types (LicensePlate, DailyRate). Would leak domain types into infrastructure.

### Decision 4: Lifecycle commands carry String vehicleId

**Choice**: `SendToMaintenanceCommand`, `ActivateVehicleCommand`, `RetireVehicleCommand` carry `String vehicleId`. The Application Service converts to `VehicleId` via `new VehicleId(UUID.fromString(...))`.

**Rationale**: Same pattern as customer-application — the outside world speaks strings, the domain speaks typed IDs. The application layer translates.

### Decision 5: VehicleNotFoundException extends RuntimeException

**Choice**: `VehicleNotFoundException` extends `RuntimeException` directly, NOT `FleetDomainException`. Lives in the application module under `exception/`. Carries the vehicle ID as a String.

**Rationale**: Same reasoning as customer-application. "Not found" is an application concern, not a domain invariant violation. Allows GlobalExceptionHandler to map: `FleetDomainException → 422`, `VehicleNotFoundException → 404`.

**Alternative rejected**: Extend `FleetDomainException`. Conflates domain invariant violations with application-level errors.

### Decision 6: Lifecycle use cases return void, RegisterVehicle returns VehicleResponse

**Choice**: SendToMaintenance, Activate, Retire return `void` (CQS). RegisterVehicle and GetVehicle return `VehicleResponse`.

**Rationale**: Same CQS pattern as customer-application. Commands don't return state.

### Decision 7: Manual mapper, no MapStruct

**Choice**: `FleetApplicationMapper` is a plain Java class with `toResponse(Vehicle)`. Maps ~10 fields.

**Rationale**: Same reasoning as customer-application. Manual mapping is ~20 lines. MapStruct overhead not justified.

## Package Structure

```
fleet-service/fleet-application/src/main/java/com/vehiclerental/fleet/application/
├── port/
│   ├── input/
│   │   ├── RegisterVehicleUseCase.java
│   │   ├── GetVehicleUseCase.java
│   │   ├── SendToMaintenanceUseCase.java
│   │   ├── ActivateVehicleUseCase.java
│   │   └── RetireVehicleUseCase.java
│   └── output/
│       └── FleetDomainEventPublisher.java
├── service/
│   └── FleetApplicationService.java
├── dto/
│   ├── command/
│   │   ├── RegisterVehicleCommand.java
│   │   ├── GetVehicleCommand.java
│   │   ├── SendToMaintenanceCommand.java
│   │   ├── ActivateVehicleCommand.java
│   │   └── RetireVehicleCommand.java
│   └── response/
│       └── VehicleResponse.java
├── mapper/
│   └── FleetApplicationMapper.java
└── exception/
    └── VehicleNotFoundException.java
```

## Risks / Trade-offs

* **spring-tx dependency in application** — Same trade-off as customer-application. `@Transactional` is a single annotation import with no runtime dependency on Spring Context. The pragmatic choice over a TransactionPort abstraction.
* **Single Application Service may not scale** — If SAGA coordination is added between Fleet and Reservation, the single service could grow. Mitigation: each use case has its own interface, extraction is mechanical.
* **DailyRate split into amount + currency in command** — Two fields instead of one compound type. This is slightly more verbose but maps naturally to JSON primitives. If Money becomes more complex (e.g., multi-currency support), the command structure would need updating.
* **VehicleNotFoundException as standalone RuntimeException** — If more application exceptions appear, a common `FleetApplicationException` base could be introduced. One exception doesn't justify a hierarchy.
