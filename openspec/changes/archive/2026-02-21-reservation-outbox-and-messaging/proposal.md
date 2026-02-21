## Why

The Walking Skeleton is complete — Reservation, Customer, and Fleet services implement the full hexagonal architecture with domain events, application services, JPA persistence, and REST APIs. However, event publishing is currently a no-op (`LoggingReservationMessagePublisher` logs events to console). For the SAGA orchestration to work, Reservation Service needs to reliably publish domain events to a message broker.

Direct publishing to RabbitMQ from within a `@Transactional` method creates the **dual-write problem**: if the database commit succeeds but the RabbitMQ publish fails (or vice versa), the system becomes inconsistent. The Outbox Pattern solves this by writing the event to an `outbox_events` table in the **same database transaction** as the business entity, then a separate scheduler polls and publishes pending events to RabbitMQ.

This change introduces the complete Outbox + RabbitMQ infrastructure in Reservation Service, and provides the shared Outbox components in a new `common-messaging` module that all four services will reuse. It also introduces `docker-compose.yml` for local development infrastructure (PostgreSQL + RabbitMQ).

## What Changes

### New module: `common-messaging`
- `OutboxEvent` JPA entity (aggregateType, aggregateId, eventType, payload, routingKey, exchange, status, retryCount, timestamps)
- `OutboxStatus` enum: PENDING, PUBLISHED, FAILED
- `OutboxEventRepository` Spring Data JPA interface (findTop100ByStatus, deletePublishedBefore)
- `OutboxPublisher` scheduled component (polls every 500ms, publishes to RabbitMQ, marks as PUBLISHED)
- `OutboxCleanupScheduler` (daily cleanup of old PUBLISHED events)
- `Jackson2JsonMessageConverter` bean for RabbitMQ message serialization
- Module POM with dependencies on `spring-boot-starter-amqp`, `spring-boot-starter-data-jpa`

### Modified module: `reservation-infrastructure`
- `RabbitMQConfig`: declares `reservation.exchange` (topic), queues, bindings, DLQ
- `OutboxReservationMessagePublisher`: implements the existing `ReservationMessagePublisher` port by writing to the outbox table (replaces `LoggingReservationMessagePublisher`)
- POM adds dependency on `common-messaging`

### Modified module: `reservation-container`
- Flyway migration `V2__create_outbox_events_table.sql` in reservation_db schema
- `application.yml` updated with RabbitMQ connection properties
- `application-test.yml` updated for Testcontainers RabbitMQ
- `BeanConfiguration` updated to wire the new `OutboxReservationMessagePublisher` instead of the logger
- `@EnableScheduling` on the main application class

### New files at project root
- `docker-compose.yml` with PostgreSQL 16 + RabbitMQ 3.13 (profile `infra`)
- `docker/postgres/init-schemas.sql` — creates the 4 schemas (reservation, customer, payment, fleet) with dedicated users
- `docker/rabbitmq/definitions.json` — pre-loaded topology (exchanges, queues, bindings, DLQs)
- `docker/rabbitmq/rabbitmq.conf` — loads definitions on startup
- `Makefile` with helper targets (infra-up, infra-down, reset-db, status, logs)

### Modified: root `pom.xml`
- Add `common-messaging` to `<modules>`
- Add `common-messaging` version entry in `<dependencyManagement>`

### Integration tests (reservation-container)
- `OutboxAtomicityIT`: verifies reservation + outbox event persist in same transaction, and both rollback on domain failure (the most critical test of the POC)
- `OutboxPublisherIT`: verifies the scheduler picks up PENDING events and publishes them to RabbitMQ (Testcontainers PostgreSQL + RabbitMQ)

## Capabilities

### New Capabilities
- `outbox-pattern`: Complete Outbox implementation in a shared `common-messaging` module — OutboxEvent JPA entity persisted in the same ACID transaction as the business entity, a scheduler that polls pending events and publishes them to RabbitMQ with status tracking (PENDING → PUBLISHED/FAILED), retry count, and daily cleanup of old published events. Reusable by all four services.
- `rabbitmq-topology`: RabbitMQ exchange (`reservation.exchange`), queues, bindings, and DLQs are pre-configured via `definitions.json` and available for consumer services
- `docker-compose-infra`: Local development infrastructure (PostgreSQL + RabbitMQ) available via `docker compose --profile infra up -d` with health checks, named volumes, and init scripts

### Modified Capabilities
- `reservation-event-publisher`: Replaces the current no-op `LoggingReservationMessagePublisher` with `OutboxReservationMessagePublisher` that writes domain events to the outbox table instead of logging them. The application output port interface remains unchanged.

## Impact

| Artifact | Action | Notes |
|----------|--------|-------|
| `common-messaging/pom.xml` | NEW | New Maven module for shared Outbox + RabbitMQ base |
| `common-messaging/src/...` | NEW | ~5-6 classes: OutboxEvent, OutboxStatus, OutboxEventRepository, OutboxPublisher, OutboxCleanupScheduler, messaging config |
| `reservation-infrastructure` (POM) | MODIFIED | Adds `common-messaging` dependency |
| `reservation-infrastructure/src/...` | NEW + MODIFIED | RabbitMQConfig (new), OutboxReservationMessagePublisher (new, replaces LoggingReservationMessagePublisher) |
| `reservation-container/src/.../V2__create_outbox_events_table.sql` | NEW | Flyway migration for outbox_events table |
| `reservation-container/src/.../application.yml` | MODIFIED | RabbitMQ connection properties |
| `reservation-container/src/.../application-test.yml` | MODIFIED | Testcontainers RabbitMQ config |
| `reservation-container/src/.../BeanConfiguration.java` | MODIFIED | Wire OutboxReservationMessagePublisher |
| `reservation-container/src/test/...` | NEW | OutboxAtomicityIT, OutboxPublisherIT |
| `pom.xml` (root) | MODIFIED | Add common-messaging module + dependency management |
| `docker-compose.yml` | NEW | PostgreSQL 16 + RabbitMQ 3.13, profile `infra` |
| `docker/postgres/init-schemas.sql` | NEW | Multi-schema init (4 schemas, 4 users) |
| `docker/rabbitmq/definitions.json` | NEW | Pre-loaded RabbitMQ topology |
| `docker/rabbitmq/rabbitmq.conf` | NEW | Loads definitions.json |
| `Makefile` | NEW | Helper targets for docker compose |

## Out of Scope

- **No consumers yet** — this change only publishes `ReservationCreatedEvent` to RabbitMQ. No service listens for it. Consumers arrive in Phase 3 (saga-participants).
- **No SAGA orchestrator** — the event flows one-way (Reservation → RabbitMQ). Orchestration arrives in Phase 4.
- **No Dockerfiles for services** — `docker-compose.yml` only includes infrastructure containers (PostgreSQL + RabbitMQ). Services run from IDE connecting to `localhost`. Service containers are a future concern.
- **No Payment Service** — arrives in Phase 2 (payment-walking-skeleton).

## Verification Criteria

1. `mvn clean install` from root — all modules build green, all existing tests pass, new ITs pass
2. `docker compose --profile infra up -d` — PostgreSQL and RabbitMQ start healthy
3. Run Reservation Service from IDE → create a reservation via REST → verify outbox_events table has a PENDING row → within 500ms the OutboxPublisher marks it PUBLISHED → RabbitMQ Management UI shows the message in `reservation.created.queue`
4. `OutboxAtomicityIT` — reservation + outbox event in same transaction, rollback on domain failure
5. `OutboxPublisherIT` — scheduler publishes PENDING events to RabbitMQ (verified with Testcontainers)
