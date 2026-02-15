## Why

Fleet Service has a complete domain layer (Vehicle aggregate, VOs, events, 50 tests) and application layer (5 use cases, FleetApplicationService, DTOs, 17 tests), but it cannot start, receive requests, or persist data. This change adds the two remaining hexagonal architecture layers: infrastructure (input and output adapters) and container (Spring Boot assembly). With this, Fleet Service becomes the second functional microservice in the platform, replicating the proven pattern established by Customer Service.

## What Changes

### Included

- **Root POM update** — `fleet-service/fleet-infrastructure` and `fleet-service/fleet-container` modules already declared; add `fleet-infrastructure` and `fleet-container` to `<dependencyManagement>`
- **fleet-infrastructure Maven module** — depends on `fleet-application` (transitively on `fleet-domain` and `common`)
- **JPA Entity** — `VehicleJpaEntity`, separate class from domain entity, with `@Entity`, setters, public empty constructor
- **Spring Data JPA Repository** — `VehicleJpaRepository` interface
- **Persistence Adapter** — `VehicleRepositoryAdapter` implements `VehicleRepository` output port from domain, converts between domain and JPA entity
- **Persistence Mapper** — `VehiclePersistenceMapper`, manual, Domain ↔ JPA Entity (bidirectional)
- **REST Controller** — `VehicleController`, delegates to input ports, zero logic
- **REST DTOs** — `RegisterVehicleRequest` as input DTO (separate from `RegisterVehicleCommand`), API responses use `ApiResponse<VehicleResponse>` wrapper from common
- **GlobalExceptionHandler** — `@ControllerAdvice`, maps `FleetDomainException` → 422, `VehicleNotFoundException` → 404
- **Event Publisher Adapter** — `FleetDomainEventPublisherAdapter` implements output port, logs events (no-op placeholder for future RabbitMQ)
- **fleet-container Maven module** — depends on fleet-infrastructure; has `spring-boot-maven-plugin`
- **Spring Boot main** — `FleetServiceApplication` with `@SpringBootApplication`
- **BeanConfiguration** — `@Configuration`, manually registers domain and application beans (no `@Service` in internal layers)
- **application.yml** — base configuration + test profile, port 8182 (Customer uses 8181)
- **Flyway migration** — `V1__create_vehicles_table.sql` with `daily_rate_amount NUMERIC(10,2) NOT NULL`, `daily_rate_currency VARCHAR(3) NOT NULL`, `description VARCHAR(500)` nullable, `category VARCHAR(50) NOT NULL`, `status VARCHAR(50) NOT NULL`
- **Integration tests** — Repository adapter with Testcontainers (PostgreSQL), controller with MockMvc, context startup smoke test
- **REST endpoints** — `POST /api/v1/vehicles`, `GET /api/v1/vehicles/{id}`, `POST /api/v1/vehicles/{id}/maintenance`, `POST /api/v1/vehicles/{id}/activate`, `POST /api/v1/vehicles/{id}/retire`

### Excluded

- Docker Compose — dedicated change when all services are ready
- RabbitMQ / Outbox — event publisher is a logger for now
- Flyway migrations for other services
- Spring Security — out of POC scope
- OpenAPI/Swagger documentation — can be added later
- Pagination or listing endpoints (`GET /api/v1/vehicles`) — not needed yet
- Update vehicle use case — not in fleet-domain, out of scope

## Capabilities

### New Capabilities
- `fleet-jpa-persistence`: JPA Entity, Spring Data repository, persistence adapter implementing the domain output port, persistence mapper (Domain ↔ JPA), Flyway migration
- `fleet-rest-api`: REST controller exposing all 5 use cases, input DTO (`RegisterVehicleRequest`), response wrapping with `ApiResponse`, GlobalExceptionHandler mapping exceptions to HTTP status codes
- `fleet-event-publisher`: Domain event publisher adapter (logger no-op) implementing the application output port
- `fleet-container-assembly`: Spring Boot main class, BeanConfiguration for manual bean registration, application.yml with profiles

### Modified Capabilities
- `multi-module-build`: Root POM adds `fleet-infrastructure` and `fleet-container` to `<dependencyManagement>` (module declarations already exist)

## Impact

- **2 new modules**: `fleet-service/fleet-infrastructure/` and `fleet-service/fleet-container/`
- **Root POM**: 2 dependencyManagement entries added (modules already declared)
- **Dependencies**: `fleet-container` → `fleet-infrastructure` → `fleet-application` → `fleet-domain` → `common`
- **Spring**: Full Spring Boot in infrastructure and container. Infrastructure uses `@Component`, `@RestController`, `@ControllerAdvice`. Container has `@SpringBootApplication` and `@Configuration`.
- **Database**: PostgreSQL via Flyway, Testcontainers for tests
- **Port**: 8182 (Customer uses 8181, avoids conflict)
- **Estimated size**: ~20 production files + ~5 test files + 2 POMs + 2 YMLs + 1 SQL migration
