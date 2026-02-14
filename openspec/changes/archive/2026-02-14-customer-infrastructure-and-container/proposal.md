## Why

Customer Service has a complete domain layer (aggregate, VOs, events, 58 tests) and application layer (5 use cases, Application Service, DTOs, 17 tests), but it cannot start, receive requests, or persist data. This change adds the two remaining hexagonal architecture layers: infrastructure (input and output adapters) and container (Spring Boot assembly). With this, Customer Service becomes a functional microservice with a REST API and PostgreSQL persistence.

## What Changes

### Included

- **Root POM update** — add `customer-service/customer-infrastructure` and `customer-service/customer-container` to modules and `<dependencyManagement>`
- **customer-infrastructure Maven module** — depends on `customer-application` (transitively on `customer-domain` and `common`)
- **JPA Entity** — `CustomerJpaEntity`, separate class from domain entity, with `@Entity`, setters, protected empty constructor
- **Spring Data JPA Repository** — `CustomerJpaRepository` interface
- **Persistence Adapter** — `CustomerRepositoryAdapter` implements `CustomerRepository` output port from domain, converts between domain and JPA entity
- **Persistence Mapper** — `CustomerPersistenceMapper`, manual, Domain ↔ JPA Entity (bidirectional)
- **REST Controller** — `CustomerController`, delegates to input ports, zero logic
- **REST DTOs** — `CreateCustomerRequest` as input DTO (separate from `CreateCustomerCommand`), API responses use `ApiResponse<CustomerResponse>` wrapper from common
- **GlobalExceptionHandler** — `@ControllerAdvice`, maps `CustomerDomainException` → 422, `CustomerNotFoundException` → 404
- **Event Publisher Adapter** — `CustomerDomainEventPublisherAdapter` implements output port, logs events (no-op placeholder for future RabbitMQ)
- **customer-container Maven module** — depends on infrastructure, application, domain, common; has `spring-boot-maven-plugin`
- **Spring Boot main** — `CustomerServiceApplication` with `@SpringBootApplication`
- **BeanConfiguration** — `@Configuration`, manually registers domain and application beans (no `@Service` in internal layers)
- **application.yml** — base configuration + test profile
- **Flyway migration** — `V1__create_customer_table.sql`
- **Integration tests** — Repository adapter with Testcontainers (PostgreSQL), controller with MockMvc, context startup smoke test
- **REST endpoints** — `POST /customers`, `GET /customers/{id}`, `POST /customers/{id}/suspend`, `POST /customers/{id}/activate`, `DELETE /customers/{id}`

### Excluded

- Docker Compose — dedicated change when the module is fully complete
- RabbitMQ / Outbox — event publisher is a logger for now
- Flyway migrations for other services
- Spring Security — out of POC scope
- OpenAPI/Swagger documentation — can be added later
- Pagination or listing endpoints (`GET /customers`) — not needed yet

## Capabilities

### New Capabilities
- `customer-jpa-persistence`: JPA Entity, Spring Data repository, persistence adapter implementing the domain output port, persistence mapper (Domain ↔ JPA), Flyway migration
- `customer-rest-api`: REST controller exposing all 5 use cases, input DTO (`CreateCustomerRequest`), response wrapping with `ApiResponse`, GlobalExceptionHandler mapping exceptions to HTTP status codes
- `customer-event-publisher`: Domain event publisher adapter (logger no-op) implementing the application output port
- `customer-container-assembly`: Spring Boot main class, BeanConfiguration for manual bean registration, application.yml with profiles

### Modified Capabilities
- `multi-module-build`: Root POM adds `customer-service/customer-infrastructure` and `customer-service/customer-container` module declarations and dependencyManagement entries

## Impact

- **2 new modules**: `customer-service/customer-infrastructure/` and `customer-service/customer-container/`
- **Root POM**: 2 modules + 2 dependencyManagement entries added
- **Dependencies**: `customer-container` → `customer-infrastructure` → `customer-application` → `customer-domain` → `common`
- **Spring**: Full Spring Boot in infrastructure and container. Infrastructure uses `@Component`, `@Repository`, `@RestController`, `@ControllerAdvice`. Container has `@SpringBootApplication` and `@Configuration`.
- **Database**: PostgreSQL via Flyway, Testcontainers for tests
- **Estimated size**: ~20 production files + ~5 test files + 2 POMs + 2 YMLs + 1 SQL migration
