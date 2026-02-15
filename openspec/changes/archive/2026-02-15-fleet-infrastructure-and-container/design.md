## Context

Fleet Service has domain (50 tests) and application (17 tests) layers complete. This change adds the two outer layers of the hexagonal architecture: infrastructure (adapters for REST and persistence) and container (Spring Boot assembly). After this, Fleet Service will be the second runnable microservice in the platform, exposing a REST API backed by PostgreSQL on port 8182.

The existing codebase enforces strict layer separation: domain has zero Spring dependencies, application only depends on `spring-tx`. Infrastructure is the first layer where full Spring Boot is allowed. Container is the assembly point where all layers come together.

Customer Service already established this pattern (change #5). Fleet replicates it with minimal variation — the main differences are the Vehicle domain model (more fields, different lifecycle transitions) and the Flyway schema (numeric types for daily rate, description nullable).

## Goals / Non-Goals

**Goals:**

* Make Fleet Service a runnable Spring Boot microservice on port 8182
* Implement persistence adapter with JPA (separate JPA entity from domain entity)
* Implement REST input adapter exposing all 5 use cases (register, get, maintenance, activate, retire)
* Implement event publisher adapter as a logger placeholder
* Create BeanConfiguration for manual registration of domain and application beans
* Add Flyway migration for the vehicles table (including NUMERIC for daily_rate, VARCHAR(3) for currency)
* Add integration tests with Testcontainers (PostgreSQL) and MockMvc
* Replicate the customer-infrastructure-and-container pattern without deviations

**Non-Goals:**

* RabbitMQ / Outbox — event publisher logs only, real messaging is a future change
* Docker Compose — separate change when all services are ready
* Spring Security / authentication — out of POC scope
* OpenAPI / Swagger documentation — can be added later
* `GET /api/v1/vehicles` listing or pagination — not needed yet
* Vehicle update endpoint — not in fleet-domain, out of scope
* Actuator / health endpoints — can be added later

## Decisions

### Decision 1: Same 10 architectural decisions as Customer Service

Fleet infrastructure and container follow exactly the same decisions documented in the customer-infrastructure-and-container design (change #5):

1. JPA Entity fully separate from Domain Entity
2. RepositoryAdapter as the persistence adapter
3. REST Controller delegates to Input Ports, not Application Service
4. RegisterVehicleRequest as a separate REST DTO
5. Event publisher as a logger (no-op)
6. BeanConfiguration registers domain and application beans manually
7. Flyway for schema management, ddl-auto: validate
8. GlobalExceptionHandler maps exception types to HTTP status codes
9. API path versioning with /api/v1 prefix
10. Integration tests with Testcontainers PostgreSQL

The rationale and alternatives considered are identical. No new architectural decisions are needed — the pattern is validated and repeatable.

### Decision 11: Vehicle-specific column types in Flyway migration

**Choice**: The `vehicles` table uses `NUMERIC(10,2)` for `daily_rate_amount` and `VARCHAR(3)` for `daily_rate_currency`. This reflects the `Money` VO from common which enforces 2-decimal precision and ISO 4217 currency codes (3 characters). `description` is `VARCHAR(500)` nullable, matching the domain validation limit. `license_plate` has a `UNIQUE` constraint (same role as `email` in customers).

**Rationale**: Customer's table had simple VARCHAR fields. Vehicle introduces a numeric type (`NUMERIC(10,2)`) for the first time in the platform. Using `NUMERIC` instead of `DECIMAL` is PostgreSQL-idiomatic (they're aliases, but `NUMERIC` is conventional). `VARCHAR(3)` for currency is tight but correct — ISO 4217 codes are always 3 characters.

**Alternative rejected**: Store `daily_rate` as a single `VARCHAR` JSON blob. Would lose database-level type safety and prevent indexing/querying by amount.

### Decision 12: Port 8182 to avoid conflicts with Customer Service

**Choice**: Fleet Service runs on port 8182. Customer Service uses 8181.

**Rationale**: During development, both services may run simultaneously on the same machine. Assigning sequential ports (8181, 8182) is simple and predictable. Future services: Reservation → 8183, Payment → 8184.

### Decision 13: GlobalExceptionHandler replicates Customer's pattern

**Choice**: Fleet's `GlobalExceptionHandler` maps:
- `VehicleNotFoundException` → 404 Not Found
- `FleetDomainException` → 422 Unprocessable Entity
- `MethodArgumentNotValidException` → 400 Bad Request
- Generic `Exception` → 500 Internal Server Error

**Rationale**: Same mapping logic as Customer. The 422 for domain exceptions is particularly important for Fleet because state transition errors (`"Cannot send vehicle to maintenance in state RETIRED"`) are business rule violations, not malformed requests. The `errorCode` from `FleetDomainException` (e.g., `VEHICLE_INVALID_STATE`) is included in the response body.

## Package Structure

### Infrastructure Module

    fleet-service/fleet-infrastructure/src/main/java/com/vehiclerental/fleet/infrastructure/
    ├── adapter/
    │   ├── input/
    │   │   └── rest/
    │   │       ├── VehicleController.java
    │   │       └── dto/
    │   │           └── RegisterVehicleRequest.java
    │   └── output/
    │       ├── persistence/
    │       │   ├── VehicleJpaRepository.java
    │       │   ├── VehicleRepositoryAdapter.java
    │       │   ├── entity/
    │       │   │   └── VehicleJpaEntity.java
    │       │   └── mapper/
    │       │       └── VehiclePersistenceMapper.java
    │       └── event/
    │           └── FleetDomainEventPublisherAdapter.java
    └── config/
        └── GlobalExceptionHandler.java

### Container Module

    fleet-service/fleet-container/src/main/java/com/vehiclerental/fleet/
    ├── FleetServiceApplication.java
    └── config/
        └── BeanConfiguration.java

## Risks / Trade-offs

* **Separate JPA entity adds mapper code for more fields** — Vehicle has ~11 fields (vs Customer's ~7), including composed types (DailyRate → amount + currency, LicensePlate → value). The persistence mapper is ~30 lines instead of ~20. Mitigation: Still manageable, and the mapper logic is straightforward field extraction.
* **Logger-only event publisher hides missing messaging** — Same risk as Customer. Events are "published" but go nowhere. Mitigation: Log messages clearly state "EVENT LOGGED (not published)".
* **Two services with separate databases** — Fleet uses `fleet_db`, Customer uses `customer_db`. In production these would be separate PostgreSQL instances. During development, both share one PostgreSQL server with different databases. Mitigation: Testcontainers creates isolated containers per test suite, so there's no cross-contamination.
* **No CORS or API gateway** — With two services on different ports, browser clients would need CORS configuration. Mitigation: Explicit non-goal. API gateway is a future infrastructure concern.
