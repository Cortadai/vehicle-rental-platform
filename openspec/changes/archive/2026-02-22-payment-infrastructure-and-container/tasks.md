## 1. Module Setup

- [x] 1.1 Create `payment-service/payment-infrastructure/pom.xml` — inherits from root parent, depends on `payment-application`, `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `postgresql` (runtime), `flyway-core`, `flyway-database-postgresql` (runtime), `jackson-databind` (for PaymentPersistenceMapper JSON serialization)
- [x] 1.2 Create `payment-service/payment-container/pom.xml` — inherits from root parent, depends on `payment-infrastructure`, includes `spring-boot-maven-plugin` and `maven-failsafe-plugin`
- [x] 1.3 Add `payment-infrastructure` and `payment-container` to root POM `<dependencyManagement>` with `${vehicle-rental.version}`. Verify module declarations already exist from payment-domain/application changes.
- [x] 1.4 Create source and test directory structures for both modules
- [x] 1.5 Verify both modules compile: `mvn clean compile` from each module directory

## 2. JPA Entity and Flyway Migration

- [x] 2.1 Create `PaymentJpaEntity.java` — `@Entity`, `@Table(name = "payments")`, fields: id (UUID, @Id), reservationId (UUID), customerId (UUID), amount (BigDecimal), currency (String), status (String), failureMessages (String, nullable — JSON-serialized TEXT), createdAt (Instant), updatedAt (Instant). Protected no-arg constructor, getters, setters. No domain imports.
- [x] 2.2 Create `V1__create_payments_table.sql` — Flyway migration at `payment-service/payment-container/src/main/resources/db/migration/`. Columns: id (UUID PK), reservation_id (UUID NOT NULL UNIQUE), customer_id (UUID NOT NULL), amount (NUMERIC(10,2) NOT NULL), currency (VARCHAR(3) NOT NULL), status (VARCHAR(20) NOT NULL), failure_messages (TEXT nullable), created_at (TIMESTAMPTZ NOT NULL), updated_at (TIMESTAMPTZ NOT NULL). UNIQUE constraint on reservation_id enforces one-payment-per-reservation at DB level.

## 3. Persistence Mapper

- [x] 3.1 Create `PaymentPersistenceMapper.java` — `@Component` with `ObjectMapper` injected via constructor. `toJpaEntity(Payment)` and `toDomainEntity(PaymentJpaEntity)`. Uses `Payment.reconstruct()` for domain rebuild. Converts PaymentId/ReservationId/CustomerId ↔ UUID, Money ↔ amount+currency, PaymentStatus ↔ String. Serializes `failureMessages` List<String> to JSON string (null if empty), deserializes JSON string to List<String> (List.of() if null).

## 4. Spring Data Repository

- [x] 4.1 Create `PaymentJpaRepository.java` — interface extending `JpaRepository<PaymentJpaEntity, UUID>`, with `Optional<PaymentJpaEntity> findByReservationId(UUID reservationId)` derived query method

## 5. Persistence Adapter

- [x] 5.1 Create `PaymentRepositoryAdapter.java` — `@Component`, implements `PaymentRepository` from domain, injects `PaymentJpaRepository` and `PaymentPersistenceMapper`, converts domain ↔ JPA entity for save, findById, and findByReservationId
- [x] 5.2 Write `PaymentRepositoryAdapterIT.java` — integration test with Testcontainers PostgreSQL: save and findById round-trip, findById returns empty for non-existing, findByReservationId returns payment, findByReservationId returns empty for non-existing reservation, save persists all fields correctly including failureMessages JSON serialization, UNIQUE constraint on reservation_id prevents duplicate payments

## 6. Event Publisher Adapter

- [x] 6.1 Create `PaymentDomainEventPublisherAdapter.java` — `@Component`, implements `PaymentDomainEventPublisher`, logs each event class name and event ID at INFO level, handles empty list gracefully

## 7. Simulated Payment Gateway

- [x] 7.1 Create `SimulatedPaymentGateway.java` — `@Component`, implements `PaymentGateway`, `charge(Money amount)` logs the attempt at INFO level and returns `new PaymentGatewayResult(true, List.of())`. No external payment SDK dependencies.

## 8. REST Input DTOs

- [x] 8.1 Create `ProcessPaymentRequest.java` — record with `@NotBlank` on reservationId, customerId, currency; `@NotNull` on amount (BigDecimal). No application layer imports.
- [x] 8.2 Create `RefundPaymentRequest.java` — record with `@NotBlank` on reservationId. No application layer imports.

## 9. GlobalExceptionHandler

- [x] 9.1 Create `GlobalExceptionHandler.java` — `@RestControllerAdvice`, handles: `PaymentNotFoundException` → 404, `PaymentDomainException` → 422 (includes errorCode), `MethodArgumentNotValidException` → 400 (field errors), generic `Exception` → 500 (no internal details)

## 10. REST Controller

- [x] 10.1 Create `PaymentController.java` — `@RestController`, `@RequestMapping("/api/v1/payments")`, injects 3 input port interfaces (ProcessPaymentUseCase, RefundPaymentUseCase, GetPaymentUseCase). POST / processes payment (201 + ApiResponse<PaymentResponse>), POST /refund refunds (200 + ApiResponse<PaymentResponse>), GET /{id} retrieves (200 + ApiResponse<PaymentResponse>). Uses `@Valid` on request bodies.
- [x] 10.2 Write `PaymentControllerIT.java` — MockMvc integration test: POST /api/v1/payments returns 201, GET /api/v1/payments/{id} returns 200, GET non-existing returns 404, POST /api/v1/payments/refund returns 200, POST with invalid body returns 400, POST with domain rule violation returns 422

## 11. Container Assembly

- [x] 11.1 Create `BeanConfiguration.java` — `@Configuration`, registers `PaymentApplicationMapper` as `@Bean`, registers `PaymentApplicationService` as `@Bean` with constructor injection of `PaymentRepository`, `PaymentDomainEventPublisher`, `PaymentGateway`, and `PaymentApplicationMapper`. Exposes 3 input port beans (ProcessPaymentUseCase, RefundPaymentUseCase, GetPaymentUseCase) all returning the same service instance.
- [x] 11.2 Create `PaymentServiceApplication.java` — `@SpringBootApplication`, standard `main()` method
- [x] 11.3 Create `application.yml` — datasource with env var defaults (localhost:5432/payment_db, postgres/postgres), `spring.jpa.hibernate.ddl-auto: validate`, `spring.jpa.open-in-view: false`, `spring.flyway.default-schema: payment`, Flyway enabled, server port 8184
- [x] 11.4 Create `application-test.yml` — Testcontainers PostgreSQL configuration overrides

## 12. Context Startup Test

- [x] 12.1 Write `PaymentServiceApplicationIT.java` — `@SpringBootTest` smoke test verifying the application context loads successfully with Testcontainers PostgreSQL

## 13. Verification

- [x] 13.1 Run `mvn clean install` from `payment-service/payment-infrastructure/` — compile + all tests pass
- [x] 13.2 Run `mvn clean verify` from `payment-service/payment-container/` — compile + unit tests + integration tests pass
- [x] 13.3 Verify domain and application modules still compile cleanly
- [x] 13.4 Verify no `@Service` or `@Component` in `payment-service/payment-domain/src/main/` and `payment-service/payment-application/src/main/`: `grep -r "@Service\|@Component"` on those two paths returns nothing (infrastructure has legitimate `@Component`s)
