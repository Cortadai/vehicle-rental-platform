## Context

Payment Service has domain and application layers complete. This change adds the two outer layers of the hexagonal architecture: infrastructure (adapters for REST and persistence) and container (Spring Boot assembly). After this, Payment Service will be the fourth runnable microservice in the platform, exposing a REST API backed by PostgreSQL on port 8184.

Customer (change #5) and Fleet (change #8) established and validated the infrastructure+container pattern. Payment replicates it with three notable differences: (1) a `SimulatedPaymentGateway` adapter for an external system port, (2) `failureMessages` stored as JSON in a TEXT column, and (3) `findByReservationId` as an additional repository query for idempotency and refunds.

## Goals / Non-Goals

**Goals:**

* Make Payment Service a runnable Spring Boot microservice on port 8184
* Implement persistence adapter with JPA (separate JPA entity from domain entity)
* Implement REST input adapter exposing 3 use cases (process, refund, get) + reservation lookup
* Implement `SimulatedPaymentGateway` as an infrastructure adapter for the `PaymentGateway` output port
* Implement event publisher adapter as a logger placeholder
* Create BeanConfiguration for manual registration of domain and application beans
* Add Flyway migration for the payments table
* Add integration tests with Testcontainers (PostgreSQL) and MockMvc
* Replicate the proven Customer/Fleet infrastructure pattern, adding only payment-specific adaptations

**Non-Goals:**

* RabbitMQ / Outbox — event publisher logs only, Outbox integration is a separate change (`payment-outbox-and-messaging`)
* Real payment gateway integration (Stripe, etc.) — SimulatedPaymentGateway always succeeds
* Docker Compose changes — `payment` schema and `payment_user` already exist in `docker/postgres/init-schemas.sql`
* Spring Security / authentication — out of POC scope
* OpenAPI / Swagger documentation — can be added later
* Listing or pagination endpoints — not needed yet
* Actuator / health endpoints — can be added later

## Decisions

### Decisions 1–10: Same architectural decisions as Customer/Fleet

Payment infrastructure and container follow exactly the same 10 decisions documented in the customer-infrastructure-and-container design:

1. JPA Entity fully separate from Domain Entity
2. RepositoryAdapter as the persistence adapter
3. REST Controller delegates to Input Ports, not Application Service
4. ProcessPaymentRequest / RefundPaymentRequest as separate REST DTOs
5. Event publisher as a logger (no-op)
6. BeanConfiguration registers domain and application beans manually
7. Flyway for schema management, ddl-auto: validate
8. GlobalExceptionHandler maps exception types to HTTP status codes
9. API path versioning with /api/v1 prefix
10. Integration tests with Testcontainers PostgreSQL

The rationale and alternatives considered are identical. No new analysis needed.

### Decision 11: SimulatedPaymentGateway as a @Component infrastructure adapter

**Choice**: `SimulatedPaymentGateway` is a `@Component` in `adapter/output/gateway/` that implements the `PaymentGateway` output port. It always returns `new PaymentGatewayResult(true, List.of())`. It logs the charge attempt for observability.

**Rationale**: This is the first external-system adapter in the platform. Customer and Fleet only have persistence and event-publishing adapters. Payment introduces a third adapter type: an integration with an external system (payment processor). The `@Component` annotation is correct here because it's an infrastructure adapter discovered by Spring's component scan — unlike `PaymentApplicationService` which is registered manually in `BeanConfiguration` because it lives in the application layer (no Spring annotations). `BeanConfiguration` receives the gateway as a constructor-injected dependency when building the application service bean.

**Alternative rejected**: Register `SimulatedPaymentGateway` manually in `BeanConfiguration`. Unnecessary — it's an infrastructure bean in a package scanned by Spring Boot. Manual registration is only needed for domain/application beans that have no Spring annotations.

**Alternative rejected**: Make the gateway return random success/failure. Would make tests non-deterministic. A predictable "always success" is better for establishing the pattern. Failure scenarios can be tested by injecting a mock gateway in tests.

### Decision 12: failureMessages stored as JSON-serialized TEXT

**Choice**: The `failure_messages` column is `TEXT` (nullable). The `PaymentPersistenceMapper` serializes `List<String>` to JSON (`["msg1","msg2"]`) on save and deserializes on load. Uses Jackson `ObjectMapper` for serialization.

**Rationale**: `failureMessages` is a `List<String>` of diagnostic messages — it's read-only diagnostic data, never queried or indexed. The options were:

| Option | Pros | Cons |
|--------|------|------|
| **TEXT with JSON** | Simple, portable, no schema changes for list growth | Can't query individual messages in SQL |
| **VARCHAR[] (PG array)** | Native PostgreSQL type, can use `ANY()` | Not portable, JPA doesn't support natively, needs custom converter |
| **Separate table** | Normalized, queryable | Overkill for diagnostic strings, adds JPA relationship complexity |

JSON-in-TEXT is the pragmatic choice for a list of diagnostic strings that only needs to be read as a whole. The mapper handles serialization transparently.

**Implementation detail**: When `failureMessages` is empty, store `null` (not `"[]"`). On load, `null` maps to `List.of()`. This avoids storing unnecessary data for the majority of payments (successful ones).

**Mapper dependency**: Unlike Customer/Fleet where the persistence mapper was a plain POJO, `PaymentPersistenceMapper` needs an `ObjectMapper` for JSON serialization. The mapper becomes a `@Component` with the Spring-managed `ObjectMapper` injected via constructor. This is appropriate — the mapper lives in the infrastructure layer where Spring is fully allowed. The `PaymentRepositoryAdapter` receives the mapper as a constructor dependency (both are `@Component`s discovered by component scan).

### Decision 13: findByReservationId as a Spring Data derived query

**Choice**: `PaymentJpaRepository` declares `Optional<PaymentJpaEntity> findByReservationId(UUID reservationId)` as a Spring Data derived query method. `PaymentRepositoryAdapter` wraps this and converts types.

**Rationale**: The domain's `PaymentRepository` output port requires `findByReservationId(ReservationId)` for two critical flows: (1) idempotency check in `ProcessPaymentUseCase` — if a payment for this reservation already exists, return it instead of creating a duplicate, and (2) `RefundPaymentUseCase` — finds the payment to refund by reservation, not by payment ID. Spring Data's derived query is the simplest implementation — no `@Query` annotation needed.

### Decision 14: Database strategy — payment_db default, payment schema for docker-compose

**Choice**: `application.yml` uses `payment_db` as the default database name (via `${DB_NAME:payment_db}`), consistent with `customer_db`, `fleet_db`, `reservation_db`. For docker-compose, `init-schemas.sql` already creates a `payment` schema with `payment_user`. Flyway is configured with `default-schema: payment` so tables go to the correct schema when running against the shared `vehicle_rental_db` instance.

**Rationale**: The platform has a dual database strategy:
- **Local dev (no Docker)**: Each service gets its own database (`payment_db`), tables in `public` schema. Simple, no schema config needed.
- **Docker Compose**: Single `vehicle_rental_db` with 4 schemas (`reservation`, `customer`, `payment`, `fleet`) + dedicated users. `init-schemas.sql` already handles this.

Adding `spring.flyway.default-schema: payment` to application.yml ensures Flyway creates the `payments` table in the `payment` schema (not `public`) when running against docker-compose. This is a configuration that Customer and Fleet should also have but currently don't — it will be addressed in the technical retrospective.

### Decision 15: REST endpoints — 3 endpoints mapping to 3 existing input ports

**Choice**: Three endpoints, each mapping 1:1 to an existing input port:
- `POST /api/v1/payments` → `ProcessPaymentUseCase` (body: `ProcessPaymentRequest`)
- `POST /api/v1/payments/refund` → `RefundPaymentUseCase` (body: `RefundPaymentRequest`)
- `GET /api/v1/payments/{id}` → `GetPaymentUseCase`

**Rationale**: These three cover all existing use cases in `payment-application`. A `GET /api/v1/payments/reservation/{reservationId}` endpoint was considered but deferred — nobody needs it now, and adding it would require a new input port + command in `payment-application` (a module already closed by a previous change). When the SAGA orchestrator needs to look up payments by reservation, the endpoint can be added as part of that change. For debug during development, direct DB queries suffice.

**Alternative rejected**: Adding `GET by reservationId` now. Would expand scope into application-layer modifications within an infrastructure change, and the consumer (SAGA orchestrator) doesn't exist yet.

### Decision 16: GlobalExceptionHandler mapping

**Choice**: Payment's `GlobalExceptionHandler` maps:
- `PaymentNotFoundException` → 404 Not Found
- `PaymentDomainException` → 422 Unprocessable Entity
- `MethodArgumentNotValidException` → 400 Bad Request
- Generic `Exception` → 500 Internal Server Error

**Rationale**: Same pattern as Customer and Fleet. The `errorCode` from `PaymentDomainException` (e.g., `PAYMENT_INVALID_STATE`, `PAYMENT_ALREADY_COMPLETED`) is included in the response body for client diagnostics.

## Package Structure

### Infrastructure Module

    payment-service/payment-infrastructure/src/main/java/com/vehiclerental/payment/infrastructure/
    ├── adapter/
    │   ├── input/
    │   │   └── rest/
    │   │       ├── PaymentController.java
    │   │       └── dto/
    │   │           ├── ProcessPaymentRequest.java
    │   │           └── RefundPaymentRequest.java
    │   └── output/
    │       ├── persistence/
    │       │   ├── PaymentJpaRepository.java
    │       │   ├── PaymentRepositoryAdapter.java
    │       │   ├── entity/
    │       │   │   └── PaymentJpaEntity.java
    │       │   └── mapper/
    │       │       └── PaymentPersistenceMapper.java
    │       ├── event/
    │       │   └── PaymentDomainEventPublisherAdapter.java
    │       └── gateway/
    │           └── SimulatedPaymentGateway.java
    └── config/
        └── GlobalExceptionHandler.java

### Container Module

    payment-service/payment-container/src/main/java/com/vehiclerental/payment/
    ├── PaymentServiceApplication.java
    └── config/
        └── BeanConfiguration.java

## Risks / Trade-offs

* **SimulatedPaymentGateway hides real integration complexity** — A real payment gateway (Stripe, PayPal) has retries, webhooks, idempotency keys, and error taxonomy. The simulated version abstracts all of this behind `PaymentGatewayResult(true, List.of())`. Mitigation: The `PaymentGateway` port interface is designed to be simple enough to swap implementations. When a real gateway is needed, only the adapter changes.
* **JSON in TEXT column loses queryability** — `failure_messages` cannot be efficiently queried in SQL. Mitigation: This data is purely diagnostic. If querying becomes needed, PostgreSQL's `jsonb` type could be used instead (would require a migration).
* **Logger-only event publisher hides missing messaging** — Same risk as Customer/Fleet. Events are "published" but go nowhere. Mitigation: Log messages clearly state "EVENT LOGGED (not published)". Outbox integration is the next planned change.
* **PaymentPersistenceMapper is a @Component unlike Customer/Fleet mappers** — Breaks the "plain POJO mapper" pattern established by the other services because it needs `ObjectMapper` for JSON serialization of `failureMessages`. Mitigation: This is a legitimate infrastructure concern — the mapper lives in infrastructure where Spring is allowed. The difference is well-documented and driven by a real need (JSON serialization), not arbitrary.
* **Flyway default-schema inconsistency with other services** — Payment will have `spring.flyway.default-schema: payment` but Customer/Fleet don't have equivalent config. Mitigation: Note for technical retrospective — align all services.
