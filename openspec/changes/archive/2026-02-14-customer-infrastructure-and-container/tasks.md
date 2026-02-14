## 1. Module Setup

- [x] 1.1 Create `customer-service/customer-infrastructure/pom.xml` — inherits from root parent, depends on `customer-application`, `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `postgresql` (runtime), `flyway-core`, `flyway-database-postgresql` (runtime)
- [x] 1.2 Create `customer-service/customer-container/pom.xml` — inherits from root parent, depends on `customer-infrastructure`, includes `spring-boot-maven-plugin`
- [x] 1.3 Add `customer-infrastructure` and `customer-container` to root POM `<dependencyManagement>` with `${vehicle-rental.version}`
- [x] 1.4 Add `customer-service/customer-infrastructure` and `customer-service/customer-container` to root POM `<modules>` list (after `customer-application`, before other services)
- [x] 1.5 Create source and test directory structures for both modules
- [x] 1.6 Verify both modules compile: `mvn clean compile` from each module directory

## 2. JPA Entity and Flyway Migration

- [x] 2.1 Create `CustomerJpaEntity.java` — `@Entity`, `@Table(name = "customers")`, fields: id (UUID, @Id), firstName, lastName, email, phone (nullable), status (String), createdAt (Instant). Protected no-arg constructor, getters, setters. No domain imports.
- [x] 2.2 Create `V1__create_customer_table.sql` — Flyway migration at `customer-service/customer-container/src/main/resources/db/migration/`. Columns: id (UUID PK), first_name (VARCHAR NOT NULL), last_name (VARCHAR NOT NULL), email (VARCHAR NOT NULL UNIQUE), phone (VARCHAR), status (VARCHAR NOT NULL), created_at (TIMESTAMPTZ NOT NULL)

## 3. Persistence Mapper

- [x] 3.1 Create `CustomerPersistenceMapper.java` — plain Java class, `toJpaEntity(Customer)` and `toDomainEntity(CustomerJpaEntity)`, uses `Customer.reconstruct()` for domain rebuild, handles nullable phone

## 4. Spring Data Repository

- [x] 4.1 Create `CustomerJpaRepository.java` — interface extending `JpaRepository<CustomerJpaEntity, UUID>`, no custom methods

## 5. Persistence Adapter

- [x] 5.1 Write `CustomerRepositoryAdapterIT.java` — integration test with Testcontainers PostgreSQL: save and findById round-trip, findById returns empty for non-existing, save persists all fields correctly
- [x] 5.2 Create `CustomerRepositoryAdapter.java` — `@Component`, implements `CustomerRepository` from domain, injects `CustomerJpaRepository` and `CustomerPersistenceMapper`, converts domain ↔ JPA entity for save and findById

## 6. Event Publisher Adapter

- [x] 6.1 Create `CustomerDomainEventPublisherAdapter.java` — `@Component`, implements `CustomerDomainEventPublisher`, logs each event class name and event ID at INFO level, handles empty list gracefully

## 7. REST Input DTO

- [x] 7.1 Create `CreateCustomerRequest.java` — record with `@NotBlank` on firstName, lastName; `@NotBlank @Email` on email; phone nullable. No application layer imports.

## 8. GlobalExceptionHandler

- [x] 8.1 Create `GlobalExceptionHandler.java` — `@RestControllerAdvice`, handles: `CustomerNotFoundException` → 404, `CustomerDomainException` → 422 (includes errorCode), `MethodArgumentNotValidException` → 400 (field errors), generic `Exception` → 500 (no internal details)

## 9. REST Controller

- [x] 9.1 Write `CustomerControllerIT.java` — MockMvc integration test: POST /api/v1/customers returns 201, GET /api/v1/customers/{id} returns 200, GET non-existing returns 404, POST suspend returns 200, POST activate returns 200, DELETE returns 204, POST with invalid body returns 400
- [x] 9.2 Create `CustomerController.java` — `@RestController`, `@RequestMapping("/api/v1/customers")`, injects 5 input port interfaces, POST creates (201 + ApiResponse), GET retrieves (200 + ApiResponse), POST suspend (200), POST activate (200), DELETE (204 No Content), uses `@Valid` on create request body

## 10. Container Assembly

- [x] 10.1 Create `BeanConfiguration.java` — `@Configuration`, registers `CustomerApplicationMapper` as `@Bean`, registers `CustomerApplicationService` as `@Bean` with constructor injection, exposes 5 input port beans (CreateCustomerUseCase, GetCustomerUseCase, SuspendCustomerUseCase, ActivateCustomerUseCase, DeleteCustomerUseCase) all returning the same service instance
- [x] 10.2 Create `CustomerServiceApplication.java` — `@SpringBootApplication`, standard `main()` method
- [x] 10.3 Create `application.yml` — datasource with env var defaults (localhost:5432/customer_db, postgres/postgres), `spring.jpa.hibernate.ddl-auto: validate`, `spring.jpa.open-in-view: false`, Flyway enabled, server port 8181
- [x] 10.4 Create `application-test.yml` — Testcontainers PostgreSQL configuration overrides

## 11. Context Startup Test

- [x] 11.1 Write `CustomerServiceApplicationIT.java` — `@SpringBootTest` smoke test verifying the application context loads successfully with Testcontainers PostgreSQL

## 12. Verification

- [x] 12.1 Run `mvn clean install` from `customer-service/customer-infrastructure/` — compile + all tests pass
- [x] 12.2 Run `mvn clean verify` from `customer-service/customer-container/` — compile + unit tests + integration tests pass
- [x] 12.3 Verify domain and application modules still compile cleanly
- [x] 12.4 Verify no `@Service` or `@Component` in domain or application `src/main/`: grep returns nothing
