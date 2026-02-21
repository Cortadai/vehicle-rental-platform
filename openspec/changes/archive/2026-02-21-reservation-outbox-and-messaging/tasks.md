## 1. Docker Compose & Infrastructure Setup

- [x] 1.1 Create `docker-compose.yml` at project root with PostgreSQL 16 and RabbitMQ 3.13 containers under profile `infra`, with health checks, named volumes, and exposed ports (5432, 5672, 15672)
- [x] 1.2 Create `docker/postgres/init-schemas.sql` — idempotent script creating 4 schemas (reservation, customer, payment, fleet) with dedicated users, search_path per user, and schema-restricted permissions
- [x] 1.3 Create `docker/rabbitmq/definitions.json` — full platform topology: 4 topic exchanges (reservation, customer, payment, fleet), dlx.exchange (direct), all queues with DLQ routing, all bindings with routing keys
- [x] 1.4 Create `docker/rabbitmq/rabbitmq.conf` with `load_definitions = /etc/rabbitmq/definitions.json`
- [x] 1.5 Create `Makefile` with targets: `infra-up`, `infra-down`, `infra-reset`, `infra-status`, `infra-logs`
- [x] 1.6 Verify: `docker compose --profile infra up -d` starts both containers healthy, Management UI accessible at localhost:15672, schemas visible in PostgreSQL

## 2. common-messaging Maven Module

- [x] 2.1 Create `common-messaging/pom.xml` — parent is vehicle-rental-platform, dependencies on `spring-boot-starter-data-jpa` and `spring-boot-starter-amqp`
- [x] 2.2 Update root `pom.xml` — add `common-messaging` to `<modules>` and `<dependencyManagement>`
- [x] 2.3 Create `OutboxStatus` enum (PENDING, PUBLISHED, FAILED) at `com.vehiclerental.common.messaging.outbox`
- [x] 2.4 Create `OutboxEvent` JPA entity — `@Entity` with static factory `create(...)`, protected no-arg constructor, all fields per spec (id, aggregateType, aggregateId, eventType, payload, routingKey, exchange, status, retryCount, createdAt, publishedAt)
- [x] 2.5 Create `OutboxEventRepository` — Spring Data JPA interface with `findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus)` and `deletePublishedBefore(Instant)` (custom `@Modifying @Query`)
- [x] 2.6 Create `OutboxPublisher` — `@Component` with `@Scheduled(fixedDelay = 500)`, polls PENDING events, publishes to RabbitMQ via `RabbitTemplate`, sets message headers (X-Aggregate-Type, X-Aggregate-Id, messageId), marks PUBLISHED on success, increments retryCount on failure, marks FAILED after 5 retries. Each event's publish-and-update cycle MUST run in its own transaction — use `TransactionTemplate` programmatically inside the loop (the `@Scheduled` method itself is NOT `@Transactional`)
- [x] 2.7 Create `OutboxCleanupScheduler` — `@Component` with `@Scheduled(cron = ...)`, deletes PUBLISHED events older than 7 days, logs count at INFO
- [x] 2.8 Create `MessagingSchedulingConfig` — `@Configuration` + `@EnableScheduling`
- [x] 2.9 Create `MessageConverterConfig` — `@Configuration` providing `Jackson2JsonMessageConverter` bean backed by application `ObjectMapper` (do NOT declare custom `RabbitTemplate` — let Spring Boot auto-config pick up the converter)
- [x] 2.10 Verify: `mvn clean install` from root builds `common-messaging` module successfully

## 3. RabbitMQ Topology (reservation-infrastructure)

- [x] 3.1 Add `common-messaging` dependency to `reservation-infrastructure/pom.xml`
- [x] 3.2 Create `RabbitMQConfig` at `com.vehiclerental.reservation.infrastructure.config` — declares `reservation.exchange` (TopicExchange), `reservation.created.queue` (with DLQ args), `reservation.dlq`, `dlx.exchange` (DirectExchange), and all bindings
- [x] 3.3 Verify: `mvn clean compile` on reservation-infrastructure succeeds

## 4. Outbox Event Publisher (reservation-infrastructure)

- [x] 4.1 Create `OutboxReservationDomainEventPublisher` at `com.vehiclerental.reservation.infrastructure.adapter.output.event` — `@Component` implementing `ReservationDomainEventPublisher`, serializes each `DomainEvent` to JSON via `ObjectMapper`, creates `OutboxEvent.create(...)` with aggregateType "RESERVATION", derives routingKey from event class name, exchange "reservation.exchange", saves via `OutboxEventRepository`
- [x] 4.2 Delete `ReservationDomainEventPublisherAdapter` (the old no-op logger adapter)
- [x] 4.3 Verify: no `BeanConfiguration` change needed — Spring auto-detects the new `@Component` as the sole implementation of `ReservationDomainEventPublisher`

## 5. Reservation Container Configuration

- [x] 5.1 Create Flyway migration `V2__create_outbox_events_table.sql` in `reservation-container/src/main/resources/db/migration/` — per spec schema (BIGSERIAL PK, all columns, index on status+created_at)
- [x] 5.2 Update `application.yml` — add `spring.rabbitmq.*` connection properties with env-var defaults (host: localhost, port: 5672, username: guest, password: guest)
- [x] 5.3 Verify: reservation-container compiles, Flyway migration runs on app startup against Docker Compose PostgreSQL

## 6. Integration Tests

- [x] 6.1 Add `org.testcontainers:rabbitmq` test-scoped dependency to `reservation-container/pom.xml`
- [x] 6.2 Create `OutboxAtomicityIT` — `@SpringBootTest` + Testcontainers PostgreSQL: (1) create reservation via application service, assert both `reservations` and `outbox_events` tables have rows after the call completes (atomicity proven by observable outcome); (2) trigger domain validation failure, assert both tables have zero rows (rollback proven by absence of both)
- [x] 6.3 Create `OutboxPublisherIT` — `@SpringBootTest` + Testcontainers PostgreSQL + RabbitMQ (`@Container @ServiceConnection RabbitMQContainer`): insert PENDING OutboxEvent, wait with Awaitility (max 2s) for status to become PUBLISHED, assert message arrived in RabbitMQ queue
- [x] 6.4 Add Awaitility test dependency to `reservation-container/pom.xml` if not already present
- [x] 6.5 Verify: `mvn verify` from root — all existing tests pass, both new ITs pass

## 7. Final Verification

- [x] 7.1 Full build: `mvn clean install` from root — all modules green
- [ ] 7.2 Manual smoke test: start Docker Compose infra → run Reservation Service from IDE → POST create reservation → verify outbox_events table has PENDING row → within 500ms row becomes PUBLISHED → RabbitMQ Management UI shows message in `reservation.created.queue`
