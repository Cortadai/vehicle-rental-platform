## Why

Payment Service has a complete domain layer (Payment aggregate, 4-state machine, typed IDs, idempotency, domain events) and application layer (3 use cases, PaymentGateway output port, PaymentApplicationService), but it cannot start, receive requests, or persist data. This change adds the two remaining hexagonal architecture layers: infrastructure (input and output adapters) and container (Spring Boot assembly). With this, Payment Service becomes the fourth functional microservice in the platform, replicating the proven pattern from Customer and Fleet while introducing a new adapter type: `SimulatedPaymentGateway` — an infrastructure adapter for an external system port that didn't exist in the other services.

## What Changes

### Included

- **Root POM update** — add `payment-service/payment-infrastructure` and `payment-service/payment-container` to `<dependencyManagement>` (module declarations should already exist from payment-domain/application changes)
- **payment-infrastructure Maven module** — depends on `payment-application` (transitively on `payment-domain` and `common`)
- **JPA Entity** — `PaymentJpaEntity`, separate class from domain entity, with `@Entity`, setters, protected empty constructor. `failure_messages` stored as JSON-serialized TEXT column (pragmatic for a `List<String>` of diagnostic messages)
- **Spring Data JPA Repository** — `PaymentJpaRepository` interface with `findByReservationId(UUID)` query method
- **Persistence Adapter** — `PaymentRepositoryAdapter` implements `PaymentRepository` output port, converts between domain and JPA entity via mapper
- **Persistence Mapper** — `PaymentPersistenceMapper`, manual, Domain ↔ JPA Entity (bidirectional). Handles `failure_messages` JSON serialization/deserialization
- **REST Controller** — `PaymentController`, delegates to input ports, zero logic
- **REST DTOs** — `ProcessPaymentRequest` (reservationId, customerId, amount, currency) and `RefundPaymentRequest` (reservationId) as input DTOs separate from commands. API responses use `ApiResponse<PaymentResponse>` wrapper from common
- **GlobalExceptionHandler** — `@ControllerAdvice`, maps `PaymentDomainException` → 422, `PaymentNotFoundException` → 404
- **Event Publisher Adapter** — `PaymentDomainEventPublisherAdapter` implements output port, logs events (no-op placeholder, same pattern as Customer/Fleet; Outbox integration deferred to `payment-outbox-and-messaging`)
- **SimulatedPaymentGateway** — `@Component` implementing `PaymentGateway` output port, always returns `PaymentGatewayResult(true, List.of())`. This is the novel adapter in Payment: an infrastructure adapter for an external system abstraction that Customer and Fleet didn't have
- **payment-container Maven module** — depends on payment-infrastructure; has `spring-boot-maven-plugin`
- **Spring Boot main** — `PaymentServiceApplication` with `@SpringBootApplication`
- **BeanConfiguration** — `@Configuration`, manually registers domain and application beans (no `@Service` in internal layers)
- **application.yml** — base configuration + test profile, port 8184 (Customer 8181, Fleet 8182, Reservation 8183)
- **Flyway migration** — `V1__create_payments_table.sql` with columns: `id UUID PK`, `reservation_id UUID NOT NULL`, `customer_id UUID NOT NULL`, `amount NUMERIC(10,2) NOT NULL`, `currency VARCHAR(3) NOT NULL`, `status VARCHAR(20) NOT NULL`, `failure_messages TEXT` (JSON), `created_at TIMESTAMPTZ NOT NULL`, `updated_at TIMESTAMPTZ NOT NULL`. Flyway default schema configured to `payment` in application.yml to align with docker-compose init-schemas.sql
- **Integration tests** — Repository adapter with Testcontainers (PostgreSQL), controller with MockMvc, context startup smoke test
- **REST endpoints**:
  - `POST /api/v1/payments` — process payment
  - `POST /api/v1/payments/refund` — refund by reservationId
  - `GET /api/v1/payments/{id}` — get payment by id

### Excluded

- **Outbox + RabbitMQ integration** — deferred to `payment-outbox-and-messaging` change; event publisher is logger no-op for now
- **Real payment gateway** — SimulatedPaymentGateway always succeeds; real integration out of POC scope
- **Spring Security** — out of POC scope
- **OpenAPI/Swagger** — can be added later
- **Pagination or listing endpoints** — not needed yet

## Capabilities

### New Capabilities
- `payment-jpa-persistence`: JPA Entity, Spring Data repository (with `findByReservationId`), persistence adapter implementing the domain output port, persistence mapper (Domain ↔ JPA) with JSON serialization for `failureMessages`, Flyway migration
- `payment-rest-api`: REST controller exposing 3 use cases (process, refund, get), input DTOs (`ProcessPaymentRequest`, `RefundPaymentRequest`), response wrapping with `ApiResponse`, GlobalExceptionHandler mapping exceptions to HTTP status codes
- `payment-event-publisher`: Domain event publisher adapter (logger no-op) implementing the application output port
- `payment-gateway-adapter`: `SimulatedPaymentGateway` implementing `PaymentGateway` output port — always returns success. Novel adapter type for this service
- `payment-container-assembly`: Spring Boot main class, BeanConfiguration for manual bean registration, application.yml with profiles

### Modified Capabilities
- `multi-module-build`: Root POM adds `payment-infrastructure` and `payment-container` to `<dependencyManagement>`

## Impact

- **2 new modules**: `payment-service/payment-infrastructure/` and `payment-service/payment-container/`
- **Root POM**: 2 dependencyManagement entries added (module declarations may already exist)
- **Dependencies**: `payment-container` → `payment-infrastructure` → `payment-application` → `payment-domain` → `common`
- **Spring**: Full Spring Boot in infrastructure and container. Infrastructure uses `@Component`, `@RestController`, `@ControllerAdvice`. Container has `@SpringBootApplication` and `@Configuration`
- **Database**: PostgreSQL via Flyway, Testcontainers for tests. application.yml uses `payment_db` as default DB name (same pattern as `customer_db`, `fleet_db`, `reservation_db`). Docker-compose uses a single `vehicle_rental_db` instance with dedicated `payment` schema and `payment_user` — already configured in `docker/postgres/init-schemas.sql`. Flyway `default-schema: payment` ensures tables are created in the correct schema when running against docker-compose
- **Port**: 8184 (Customer 8181, Fleet 8182, Reservation 8183)
- **Novel pattern**: `SimulatedPaymentGateway` — first external-system adapter in the platform, establishes the pattern for future integrations
- **Estimated size**: ~22 production files + ~5 test files + 2 POMs + 2 YMLs + 1 SQL migration
