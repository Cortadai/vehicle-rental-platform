## Context

Customer Service has domain (58 tests) and application (17 tests) layers complete. This change adds the two outer layers of the hexagonal architecture: infrastructure (adapters for REST and persistence) and container (Spring Boot assembly). After this, Customer Service will be a runnable microservice exposing a REST API backed by PostgreSQL.

The existing codebase enforces strict layer separation: domain has zero Spring dependencies, application only depends on `spring-tx`. Infrastructure is the first layer where full Spring Boot is allowed. Container is the assembly point where all layers come together.

## Goals / Non-Goals

**Goals:**

* Make Customer Service a runnable Spring Boot microservice
* Implement persistence adapter with JPA (separate JPA entity from domain entity)
* Implement REST input adapter exposing all 5 use cases
* Implement event publisher adapter as a logger placeholder
* Create BeanConfiguration for manual registration of domain and application beans
* Add Flyway migration for the customers table
* Add integration tests with Testcontainers (PostgreSQL) and MockMvc
* Establish the infrastructure + container pattern for all services to follow

**Non-Goals:**

* RabbitMQ / Outbox — event publisher logs only, real messaging is a future change
* Docker Compose — separate change
* Spring Security / authentication — out of POC scope
* OpenAPI / Swagger documentation — can be added later
* `GET /customers` listing or pagination — not needed yet
* Custom `@ConfigurationProperties` — no custom config needed yet
* Actuator / health endpoints — can be added later

## Decisions

### Decision 1: JPA Entity fully separate from Domain Entity

**Choice**: `CustomerJpaEntity` is a plain JPA class (`@Entity`, `@Table`, `@Column`, protected no-arg constructor, getters/setters). It is NOT the domain `Customer` class. A `CustomerPersistenceMapper` converts between them bidirectionally.

**Rationale**: This is the core hexagonal principle. The domain `Customer` has factory methods (`create`, `reconstruct`), immutable fields, and business logic. JPA requires mutable setters, a no-arg constructor, and framework annotations. Mixing these concerns in one class creates a "God object" that serves two masters. The mapper is ~20 lines and eliminates the coupling entirely.

**Alternative rejected**: Single class with `@Entity` on the domain `Customer`. Would force Spring dependencies into the domain module, break the zero-Spring rule, and require exposing setters on an aggregate root.

### Decision 2: CustomerRepositoryAdapter as the persistence adapter

**Choice**: `CustomerRepositoryAdapter` implements the domain's `CustomerRepository` output port. It uses `CustomerJpaRepository` (Spring Data JPA) internally and delegates all conversion to `CustomerPersistenceMapper`. Annotated with `@Component`.

**Rationale**: The adapter translates between the domain contract (`save(Customer)`, `findById(CustomerId)`) and the JPA contract (`save(CustomerJpaEntity)`, `findById(UUID)`). The domain never knows about JPA — it only depends on its own `CustomerRepository` interface. The adapter is the "plug" that connects them.

**Alternative rejected**: Making `CustomerJpaRepository` extend the domain port directly. Spring Data generates the implementation, but the method signatures don't match (domain uses `CustomerId`, JPA uses `UUID`), so a manual adapter is required.

### Decision 3: REST Controller delegates to Input Ports, not Application Service

**Choice**: `CustomerController` injects the 5 input port interfaces (`CreateCustomerUseCase`, `GetCustomerUseCase`, etc.), NOT `CustomerApplicationService` directly.

**Rationale**: The controller depends on abstractions (interfaces), not implementations. If a use case is later extracted into its own class (e.g., when SAGA coordination is added), the controller doesn't change. This is Dependency Inversion in practice.

**Alternative rejected**: Inject `CustomerApplicationService` directly. Would work but creates tight coupling — the controller would need to change if the service class is split.

### Decision 4: CreateCustomerRequest as a separate REST DTO

**Choice**: `CreateCustomerRequest` is a record in the infrastructure layer (`adapter/input/rest/dto/`). It carries the same fields as `CreateCustomerCommand` but is a separate class. The controller converts request → command explicitly.

**Rationale**: REST DTOs and application commands serve different purposes. REST DTOs may have validation annotations (`@NotBlank`, `@Email`), JSON annotations, or API-specific fields. Application commands are pure data carriers. Keeping them separate means the API can evolve independently from the application layer. For responses, we reuse `CustomerResponse` from the application layer wrapped in `ApiResponse<>` from common — there's no need for a separate REST response DTO since it maps 1:1.

**Alternative rejected**: Reuse `CreateCustomerCommand` directly as `@RequestBody`. Would leak application types into the REST layer and prevent adding REST-specific validation annotations.

### Decision 5: Event publisher as a logger (no-op)

**Choice**: `CustomerDomainEventPublisherAdapter` implements `CustomerDomainEventPublisher` output port. It uses SLF4J to log each event's type and ID. No RabbitMQ, no outbox table.

**Rationale**: The full event pipeline (outbox table → scheduler → RabbitMQ) is a separate concern with its own complexity. For now, the adapter satisfies the port contract and provides observability (you can see events being fired in logs). When RabbitMQ is added, only this adapter class changes — the domain and application layers are untouched.

**Alternative rejected**: Skip the adapter entirely and pass a no-op lambda. Would work but loses observability and doesn't establish the adapter pattern for when real messaging is added.

### Decision 6: BeanConfiguration registers domain and application beans manually

**Choice**: A `@Configuration` class in the container module creates `@Bean` methods for `CustomerApplicationMapper` and `CustomerApplicationService`. The application service bean is also exposed as its 5 input port interfaces via separate `@Bean` methods that return the same instance.

**Rationale**: Domain and application classes have NO `@Service` or `@Component` annotations (by design — they don't depend on Spring). Someone needs to tell Spring about them. `BeanConfiguration` is that "someone". It lives in the container module because that's the assembly layer. Exposing input ports as separate beans lets controllers depend on interfaces, not the concrete service.

**Alternative rejected**: Add `@Service` to `CustomerApplicationService`. Would require adding `spring-context` dependency to customer-application module, breaking the "application depends only on spring-tx" rule.

### Decision 7: Flyway for schema management, ddl-auto: validate

**Choice**: Schema is managed by Flyway migration `V1__create_customer_table.sql`. Hibernate's `ddl-auto` is set to `validate` (production) — Hibernate checks the schema matches the JPA entity but never modifies it.

**Rationale**: Flyway provides versioned, repeatable, reviewable migrations. `ddl-auto: create-drop` or `update` are dangerous in any environment beyond throwaway prototypes. `validate` catches mismatches early.

**Alternative rejected**: `ddl-auto: update` everywhere. Convenient but hides schema drift and can produce unexpected ALTER TABLE statements.

### Decision 8: GlobalExceptionHandler maps exception types to HTTP status codes

**Choice**: `@RestControllerAdvice` in the infrastructure module handles:
- `CustomerNotFoundException` → 404 Not Found
- `CustomerDomainException` → 422 Unprocessable Entity
- `MethodArgumentNotValidException` → 400 Bad Request
- Generic `Exception` → 500 Internal Server Error

All responses use a consistent error body with timestamp, status, error code, and message.

**Rationale**: Exception-to-HTTP mapping is an infrastructure concern. The domain throws `CustomerDomainException` with business error codes (`CUSTOMER_INVALID_STATE`). The application throws `CustomerNotFoundException`. The exception handler translates these to HTTP semantics. Using 422 (not 400) for domain rule violations distinguishes "your request was malformed" (400) from "your request was understood but violates business rules" (422).

**Alternative rejected**: Return error details from the controller with try-catch. Would duplicate error handling across every endpoint and mix HTTP concerns with business logic.

### Decision 9: API path versioning with /api/v1 prefix

**Choice**: All endpoints use `/api/v1/customers` prefix. Versioning in the path, not headers.

**Rationale**: Path versioning is explicit, discoverable, and easy to route at the load balancer level. For a POC, it's the simplest approach. Header-based versioning (Accept header) is more RESTful but adds complexity.

**Alternative rejected**: No versioning. Would make breaking changes impossible to manage later.

### Decision 10: Integration tests with Testcontainers PostgreSQL

**Choice**: Integration tests (`*IT.java`) use Testcontainers to spin up a real PostgreSQL instance. No H2 or embedded databases. The repository adapter IT tests the full save/load cycle through JPA. The controller IT uses `@SpringBootTest` with `MockMvc`.

**Rationale**: H2 has different SQL dialect and behavior from PostgreSQL. Testing with the real database catches issues that H2 would hide (e.g., column types, constraints, case sensitivity). Testcontainers starts in ~3 seconds and is disposable.

**Alternative rejected**: H2 in-memory database. Faster startup but hides PostgreSQL-specific issues. We've seen too many "works in tests, breaks in prod" bugs from dialect differences.

## Package Structure

### Infrastructure Module

    customer-service/customer-infrastructure/src/main/java/com/vehiclerental/customer/infrastructure/
    ├── adapter/
    │   ├── input/
    │   │   └── rest/
    │   │       ├── CustomerController.java
    │   │       └── dto/
    │   │           └── CreateCustomerRequest.java
    │   └── output/
    │       ├── persistence/
    │       │   ├── CustomerJpaRepository.java
    │       │   ├── CustomerRepositoryAdapter.java
    │       │   ├── entity/
    │       │   │   └── CustomerJpaEntity.java
    │       │   └── mapper/
    │       │       └── CustomerPersistenceMapper.java
    │       └── event/
    │           └── CustomerDomainEventPublisherAdapter.java
    └── config/
        └── GlobalExceptionHandler.java

### Container Module

    customer-service/customer-container/src/main/java/com/vehiclerental/customer/
    ├── CustomerServiceApplication.java
    └── config/
        └── BeanConfiguration.java

## Risks / Trade-offs

* **Separate JPA entity doubles the number of entity classes** — For Customer (1 aggregate, 7 fields) it's manageable. For services with many entities and relationships, the mapper/entity duplication is significant. Mitigation: Customer is simple enough that this is ~20 extra lines. The pattern is worth establishing for when complex aggregates arrive.
* **Logger-only event publisher hides missing messaging** — Events are "published" but go nowhere. A developer might assume the event pipeline works. Mitigation: Log messages clearly state "EVENT LOGGED (not published)" and the proposal explicitly marks this as a placeholder.
* **Testcontainers requires Docker** — Developers without Docker cannot run integration tests. Mitigation: Unit tests (`*Test.java`) run without Docker. Integration tests are in the `verify` phase, not `test`.
* **No Actuator or health checks** — The service starts but has no observability endpoints. Mitigation: Explicit non-goal. Can be added as a small follow-up change.
