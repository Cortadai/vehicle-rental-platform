## 1. Module Setup

- [x] 1.1 Create `fleet-service/fleet-infrastructure/pom.xml` — inherits from root parent, depends on `fleet-application`, `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `postgresql` (runtime), `flyway-core`, `flyway-database-postgresql` (runtime)
- [x] 1.2 Create `fleet-service/fleet-container/pom.xml` — inherits from root parent, depends on `fleet-infrastructure`, includes `spring-boot-maven-plugin` and `maven-failsafe-plugin`
- [x] 1.3 Add `fleet-infrastructure` and `fleet-container` to root POM `<dependencyManagement>` with `${vehicle-rental.version}`
- [x] 1.4 Create source and test directory structures for both modules
- [x] 1.5 Verify both modules compile: `mvn clean compile` from each module directory

## 2. JPA Entity and Flyway Migration

- [x] 2.1 Create `VehicleJpaEntity.java` — `@Entity`, `@Table(name = "vehicles")`, fields: id (UUID, @Id), licensePlate, make, model, year (int), category (String), dailyRateAmount (BigDecimal), dailyRateCurrency (String), description (String, nullable), status (String), createdAt (Instant). Public no-arg constructor, getters, setters. No domain imports.
- [x] 2.2 Create `V1__create_vehicles_table.sql` — Flyway migration at `fleet-service/fleet-container/src/main/resources/db/migration/`. Columns: id (UUID PK), license_plate (VARCHAR NOT NULL UNIQUE), make (VARCHAR NOT NULL), model (VARCHAR NOT NULL), year (INTEGER NOT NULL), category (VARCHAR(50) NOT NULL), daily_rate_amount (NUMERIC(10,2) NOT NULL), daily_rate_currency (VARCHAR(3) NOT NULL), description (VARCHAR(500)), status (VARCHAR(50) NOT NULL), created_at (TIMESTAMPTZ NOT NULL)

## 3. Persistence Mapper

- [x] 3.1 Create `VehiclePersistenceMapper.java` — plain Java class, `toJpaEntity(Vehicle)` and `toDomainEntity(VehicleJpaEntity)`, uses `Vehicle.reconstruct()` for domain rebuild, handles nullable description, converts DailyRate ↔ amount+currency, converts LicensePlate ↔ String, converts VehicleCategory/VehicleStatus ↔ String

## 4. Spring Data Repository

- [x] 4.1 Create `VehicleJpaRepository.java` — interface extending `JpaRepository<VehicleJpaEntity, UUID>`, no custom methods

## 5. Persistence Adapter

- [x] 5.1 Create `VehicleRepositoryAdapter.java` — `@Component`, implements `VehicleRepository` from domain, injects `VehicleJpaRepository` and `VehiclePersistenceMapper`, converts domain ↔ JPA entity for save and findById
- [x] 5.2 Write `VehicleRepositoryAdapterIT.java` — integration test with Testcontainers PostgreSQL: save and findById round-trip, findById returns empty for non-existing, save persists all fields correctly including dailyRateAmount/currency and nullable description

## 6. Event Publisher Adapter

- [x] 6.1 Create `FleetDomainEventPublisherAdapter.java` — `@Component`, implements `FleetDomainEventPublisher`, logs each event class name and event ID at INFO level, handles empty list gracefully

## 7. REST Input DTO

- [x] 7.1 Create `RegisterVehicleRequest.java` — record with `@NotBlank` on licensePlate, make, model, category, dailyRateCurrency; `@NotNull` on dailyRateAmount; year as int (no annotation needed); description nullable. No application layer imports.

## 8. GlobalExceptionHandler

- [x] 8.1 Create `GlobalExceptionHandler.java` — `@RestControllerAdvice`, handles: `VehicleNotFoundException` → 404, `FleetDomainException` → 422 (includes errorCode), `MethodArgumentNotValidException` → 400 (field errors), generic `Exception` → 500 (no internal details)

## 9. REST Controller

- [x] 9.1 Create `VehicleController.java` — `@RestController`, `@RequestMapping("/api/v1/vehicles")`, injects 5 input port interfaces, POST registers (201 + ApiResponse), GET retrieves (200 + ApiResponse), POST maintenance (200), POST activate (200), POST retire (200), uses `@Valid` on register request body
- [x] 9.2 Write `VehicleControllerIT.java` — MockMvc integration test: POST /api/v1/vehicles returns 201, GET /api/v1/vehicles/{id} returns 200, GET non-existing returns 404, POST maintenance returns 200, POST activate returns 200, POST retire returns 200, POST with invalid body returns 400, POST with domain rule violation returns 422

## 10. Container Assembly

- [x] 10.1 Create `BeanConfiguration.java` — `@Configuration`, registers `VehiclePersistenceMapper` as `@Bean`, registers `FleetApplicationMapper` as `@Bean`, registers `FleetApplicationService` as `@Bean` with constructor injection, exposes 5 input port beans (RegisterVehicleUseCase, GetVehicleUseCase, SendToMaintenanceUseCase, ActivateVehicleUseCase, RetireVehicleUseCase) all returning the same service instance
- [x] 10.2 Create `FleetServiceApplication.java` — `@SpringBootApplication`, standard `main()` method
- [x] 10.3 Create `application.yml` — datasource with env var defaults (localhost:5432/fleet_db, postgres/postgres), `spring.jpa.hibernate.ddl-auto: validate`, `spring.jpa.open-in-view: false`, Flyway enabled, server port 8182
- [x] 10.4 Create `application-test.yml` — Testcontainers PostgreSQL configuration overrides

## 11. Context Startup Test

- [x] 11.1 Write `FleetServiceApplicationIT.java` — `@SpringBootTest` smoke test verifying the application context loads successfully with Testcontainers PostgreSQL

## 12. Verification

- [x] 12.1 Run `mvn clean install` from `fleet-service/fleet-infrastructure/` — compile + all tests pass
- [x] 12.2 Run `mvn clean verify` from `fleet-service/fleet-container/` — compile + unit tests + integration tests pass
- [x] 12.3 Verify domain and application modules still compile cleanly
- [x] 12.4 Verify no `@Service` or `@Component` in domain or application `src/main/`: `grep -r "@Service\|@Component"` returns nothing
