## 1. Module Setup

- [x] 1.1 Create `reservation-service/reservation-infrastructure/pom.xml` — inherits from root parent, depends on `reservation-application`, `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `postgresql` (runtime), `flyway-core`, `flyway-database-postgresql` (runtime)
- [x] 1.2 Create `reservation-service/reservation-container/pom.xml` — inherits from root parent, depends on `reservation-infrastructure`, includes `spring-boot-maven-plugin` and `maven-failsafe-plugin`
- [x] 1.3 Add `reservation-infrastructure` and `reservation-container` to root POM `<dependencyManagement>` with `${vehicle-rental.version}`
- [x] 1.4 Add `reservation-service/reservation-infrastructure` and `reservation-service/reservation-container` to root POM `<modules>` list (after `reservation-application`, before `customer-service`)
- [x] 1.5 Create source and test directory structures for both modules
- [x] 1.6 Verify both modules compile: `mvn clean compile` from each module directory

## 2. JPA Entities and Flyway Migration

- [x] 2.1 Create `ReservationJpaEntity.java` — `@Entity`, `@Table(name = "reservations")`, fields: id (UUID, @Id), trackingId (UUID, @Column unique=true), customerId (UUID), pickupAddress, pickupCity, returnAddress, returnCity, pickupDate (LocalDate), returnDate (LocalDate), totalPriceAmount (BigDecimal), totalPriceCurrency (String), status (String), failureMessages (String, nullable), createdAt (Instant), updatedAt (Instant). `@OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)` for items. Protected no-arg constructor, getters, setters. No domain imports.
- [x] 2.2 Create `ReservationItemJpaEntity.java` — `@Entity`, `@Table(name = "reservation_items")`, fields: id (UUID, @Id), vehicleId (UUID), dailyRateAmount (BigDecimal), dailyRateCurrency (String), days (int), subtotalAmount (BigDecimal), subtotalCurrency (String). `@ManyToOne(fetch = FetchType.LAZY)` + `@JoinColumn(name = "reservation_id")` back-reference to ReservationJpaEntity. Protected no-arg constructor, getters, setters. No domain imports.
- [x] 2.3 Create `V1__create_reservation_tables.sql` — Flyway migration at `reservation-service/reservation-container/src/main/resources/db/migration/`. Two tables: `reservations` (id UUID PK, tracking_id UUID NOT NULL UNIQUE, customer_id UUID NOT NULL, pickup_address VARCHAR NOT NULL, pickup_city VARCHAR NOT NULL, return_address VARCHAR NOT NULL, return_city VARCHAR NOT NULL, pickup_date DATE NOT NULL, return_date DATE NOT NULL, total_price_amount NUMERIC NOT NULL, total_price_currency VARCHAR NOT NULL, status VARCHAR NOT NULL, failure_messages TEXT, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL) and `reservation_items` (id UUID PK, reservation_id UUID NOT NULL FK→reservations(id), vehicle_id UUID NOT NULL, daily_rate_amount NUMERIC NOT NULL, daily_rate_currency VARCHAR NOT NULL, days INTEGER NOT NULL, subtotal_amount NUMERIC NOT NULL, subtotal_currency VARCHAR NOT NULL). Index on tracking_id.

## 3. Persistence Mapper

- [x] 3.1 Create `ReservationPersistenceMapper.java` — plain Java class (no Spring annotations), `toJpaEntity(Reservation)` decomposes VOs to flat columns (PickupLocation→address+city, DateRange→pickupDate+returnDate, Money→amount+currency, typed IDs→UUID), maps items to ReservationItemJpaEntity with parent back-reference set. `toDomainEntity(ReservationJpaEntity)` uses `Reservation.reconstruct()` with items built via `ReservationItem.reconstruct()`, converts failureMessages comma-separated↔List<String>

## 4. Spring Data Repository

- [x] 4.1 Create `ReservationJpaRepository.java` — interface extending `JpaRepository<ReservationJpaEntity, UUID>`, declares `Optional<ReservationJpaEntity> findByTrackingId(UUID trackingId)`

## 5. Persistence Adapter

- [x] 5.1 Create `ReservationRepositoryAdapter.java` — `@Component`, implements `ReservationRepository` from domain, injects `ReservationJpaRepository` and `ReservationPersistenceMapper`, implements save (domain→JPA→save→JPA→domain), findById (UUID extraction→findById→map), findByTrackingId (UUID extraction→findByTrackingId→map)
- [x] 5.2 Write `ReservationRepositoryAdapterIT.java` — integration test with Testcontainers PostgreSQL: save and findById round-trip with items, findByTrackingId round-trip, findById returns empty for non-existing, all value objects (Money, DateRange, PickupLocation, typed IDs) correctly reconstructed, items count and fields preserved

## 6. Event Publisher Adapter

- [x] 6.1 Create `ReservationDomainEventPublisherAdapter.java` — `@Component`, implements `ReservationDomainEventPublisher`, logs each event class name and event ID at INFO level, handles empty list gracefully

## 7. REST Input DTO

- [x] 7.1 Create `CreateReservationRequest.java` — record with inner `CreateReservationItemRequest` record. Outer: `@NotBlank` on customerId, pickupAddress, pickupCity, returnAddress, returnCity, pickupDate, returnDate, currency; `@NotEmpty @Valid` on items list. Inner: `@NotBlank` on vehicleId, `@NotNull @Positive` on dailyRate (BigDecimal), `@Positive` on days (int). No application layer imports.

## 8. GlobalExceptionHandler

- [x] 8.1 Create `GlobalExceptionHandler.java` — `@RestControllerAdvice` in `infrastructure.config`, handles: `ReservationNotFoundException` → 404, `ReservationDomainException` → 422 (includes errorCode), `MethodArgumentNotValidException` → 400 (field errors), generic `Exception` → 500 (no internal details)

## 9. REST Controller

- [x] 9.1 Create `ReservationController.java` — `@RestController`, `@RequestMapping("/api/v1/reservations")`, injects CreateReservationUseCase and TrackReservationUseCase (input ports, NOT application service directly). POST /api/v1/reservations: converts CreateReservationRequest→CreateReservationCommand (including nested items), returns 201 + ApiResponse<CreateReservationResponse>. GET /api/v1/reservations/{trackingId}: converts path variable→TrackReservationCommand, returns 200 + ApiResponse<TrackReservationResponse>. Uses `@Valid` on create request body.
- [x] 9.2 Write `ReservationControllerIT.java` — MockMvc integration test: POST /api/v1/reservations returns 201 with trackingId+status, GET /api/v1/reservations/{trackingId} returns 200 with full snapshot including items, GET non-existing returns 404, POST with invalid body returns 400, POST with empty items returns 400

## 10. Container Assembly

- [x] 10.1 Create `BeanConfiguration.java` — `@Configuration` in `com.vehiclerental.reservation.config`, registers `ReservationPersistenceMapper` as `@Bean`, registers `ReservationApplicationMapper` as `@Bean`, registers `ReservationApplicationService` as `@Bean` with constructor injection (ReservationRepository, ReservationDomainEventPublisher, ReservationApplicationMapper), exposes 2 input port beans (CreateReservationUseCase, TrackReservationUseCase) returning the same service instance
- [x] 10.2 Create `ReservationServiceApplication.java` — `@SpringBootApplication` in `com.vehiclerental.reservation`, standard `main()` method
- [x] 10.3 Create `application.yml` — datasource with env var defaults (localhost:5432/reservation_db, postgres/postgres), `spring.jpa.hibernate.ddl-auto: validate`, `spring.jpa.open-in-view: false`, Flyway enabled, server port 8183
- [x] 10.4 Create `application-test.yml` — Testcontainers PostgreSQL configuration overrides

## 11. Context Startup Test

- [x] 11.1 Write `ReservationServiceApplicationIT.java` — `@SpringBootTest` smoke test verifying the application context loads successfully with Testcontainers PostgreSQL

## 12. Verification

- [x] 12.1 Run `mvn clean install` from `reservation-service/reservation-infrastructure/` — compile + all tests pass
- [x] 12.2 Run `mvn clean verify` from `reservation-service/reservation-container/` — compile + unit tests + integration tests pass
- [x] 12.3 Verify reservation-domain and reservation-application still compile independently: `mvn compile` from each module directory
- [x] 12.4 Verify no `@Service` or `@Component` in domain or application `src/main/`: grep returns nothing
- [x] 12.5 Run `mvn clean install` from root — full platform build (all modules) passes green
