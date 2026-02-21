# outbox-pattern Specification

## Purpose

Shared Outbox Pattern implementation in the `common-messaging` Maven module. Provides transactional event persistence, scheduled publishing to RabbitMQ, and automatic cleanup — reusable by all four services.

## ADDED Requirements

### Requirement: OutboxEvent JPA entity persists domain events

The `OutboxEvent` entity SHALL be a JPA `@Entity` mapped to the `outbox_events` table. It SHALL store all metadata required to publish a domain event to RabbitMQ. It SHALL use a static factory method `create(...)` for programmatic creation and a `protected` no-arg constructor for JPA.

#### Scenario: OutboxEvent fields

- **WHEN** an `OutboxEvent` is created via `OutboxEvent.create(aggregateType, aggregateId, eventType, payload, routingKey, exchange)`
- **THEN** it SHALL have: `id` (auto-generated BIGSERIAL), `aggregateType` (String, not null), `aggregateId` (String, not null), `eventType` (String, not null), `payload` (String/TEXT, not null), `routingKey` (String, not null), `exchange` (String, not null), `status` (OutboxStatus, default PENDING), `retryCount` (int, default 0), `createdAt` (Instant, default now), `publishedAt` (Instant, nullable)

#### Scenario: OutboxEvent created with PENDING status

- **WHEN** `OutboxEvent.create(...)` is called
- **THEN** the `status` SHALL be `PENDING`, `retryCount` SHALL be `0`, `createdAt` SHALL be set to `Instant.now()`, and `publishedAt` SHALL be `null`

#### Scenario: OutboxEvent has no-arg constructor for JPA

- **WHEN** JPA instantiates `OutboxEvent` via reflection
- **THEN** the `protected` no-arg constructor SHALL be available and SHALL NOT throw

### Requirement: OutboxStatus enum defines event lifecycle

`OutboxStatus` SHALL define the three states of an outbox event.

#### Scenario: OutboxStatus values

- **WHEN** `OutboxStatus` is inspected
- **THEN** it SHALL contain exactly three values: `PENDING`, `PUBLISHED`, `FAILED`

### Requirement: OutboxEventRepository provides persistence operations

`OutboxEventRepository` SHALL be a Spring Data JPA interface extending `JpaRepository<OutboxEvent, Long>`.

#### Scenario: Find pending events ordered by creation time

- **WHEN** `findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)` is called
- **THEN** it SHALL return up to 100 `OutboxEvent` records with status `PENDING`, ordered by `createdAt` ascending (oldest first)

#### Scenario: Delete old published events

- **WHEN** `deletePublishedBefore(Instant cutoff)` is called
- **THEN** it SHALL delete all `OutboxEvent` records with status `PUBLISHED` and `publishedAt` before the cutoff timestamp, and SHALL return the count of deleted records

### Requirement: OutboxPublisher scheduler polls and publishes pending events

`OutboxPublisher` SHALL be a Spring `@Component` that polls the `outbox_events` table and publishes pending events to RabbitMQ.

#### Scenario: Polls every 500ms

- **WHEN** the application is running with `@EnableScheduling` active
- **THEN** `OutboxPublisher` SHALL execute its publish method with `@Scheduled(fixedDelay = 500)`

#### Scenario: Publishes pending event to RabbitMQ

- **WHEN** a PENDING `OutboxEvent` is found
- **THEN** it SHALL send the `payload` to RabbitMQ using `RabbitTemplate.convertAndSend(exchange, routingKey, payload)` with message headers `X-Aggregate-Type`, `X-Aggregate-Id`, and `messageId` set to the outbox event ID

#### Scenario: Marks event as PUBLISHED on success

- **WHEN** `RabbitTemplate.convertAndSend()` succeeds for an event
- **THEN** the event's `status` SHALL be set to `PUBLISHED`, `publishedAt` SHALL be set to `Instant.now()`, and the event SHALL be saved to the database

#### Scenario: Increments retryCount on failure

- **WHEN** `RabbitTemplate.convertAndSend()` throws an exception for an event
- **THEN** the event's `retryCount` SHALL be incremented by 1 and the event SHALL be saved

#### Scenario: Marks event as FAILED after 5 retries

- **WHEN** an event's `retryCount` reaches 5
- **THEN** the event's `status` SHALL be set to `FAILED` and SHALL be logged at ERROR level

#### Scenario: Each event is processed in its own transaction

- **WHEN** a batch of pending events is processed
- **THEN** each event's publish-and-status-update SHALL be committed independently, so that a failure on event N does NOT roll back events 1 through N-1

### Requirement: OutboxCleanupScheduler removes old published events

`OutboxCleanupScheduler` SHALL be a Spring `@Component` that periodically deletes old PUBLISHED events to prevent table growth.

#### Scenario: Runs daily cleanup

- **WHEN** the application is running with `@EnableScheduling` active
- **THEN** `OutboxCleanupScheduler` SHALL run daily (via `@Scheduled(cron = ...)`) and delete `OutboxEvent` records with status `PUBLISHED` and `publishedAt` older than 7 days

#### Scenario: Logs cleanup result

- **WHEN** cleanup completes
- **THEN** it SHALL log the number of deleted events at INFO level

### Requirement: MessagingSchedulingConfig enables scheduling

`MessagingSchedulingConfig` SHALL be a `@Configuration` class in `common-messaging` annotated with `@EnableScheduling`.

#### Scenario: Auto-enables scheduling for importing services

- **WHEN** a service adds `common-messaging` as a Maven dependency and the package is component-scanned
- **THEN** `@EnableScheduling` SHALL be active without any additional annotation on the service's main class

### Requirement: MessageConverterConfig provides Jackson-based RabbitMQ serialization

`MessageConverterConfig` SHALL be a `@Configuration` class that provides a `Jackson2JsonMessageConverter` bean and customizes the Spring Boot auto-configured `RabbitTemplate` to use it. It SHALL NOT declare its own `RabbitTemplate` bean to avoid conflicting with Spring Boot's auto-configuration.

#### Scenario: Jackson2JsonMessageConverter bean

- **WHEN** the application context loads `MessageConverterConfig`
- **THEN** a `MessageConverter` bean SHALL be registered using `Jackson2JsonMessageConverter` backed by the application's `ObjectMapper` (which includes `JavaTimeModule`)

#### Scenario: RabbitTemplate uses JSON converter via auto-configuration

- **WHEN** the application context loads and Spring Boot auto-configures `RabbitTemplate`
- **THEN** the auto-configured `RabbitTemplate` SHALL pick up the `Jackson2JsonMessageConverter` bean automatically (Spring Boot's `RabbitAutoConfiguration` detects a single `MessageConverter` bean and applies it to the `RabbitTemplate`)

### Requirement: Outbox table Flyway migration

Each service that uses the outbox pattern SHALL create the `outbox_events` table via a Flyway migration in its own schema.

#### Scenario: Migration creates outbox_events table

- **WHEN** Flyway runs the migration `V2__create_outbox_events_table.sql`
- **THEN** the `outbox_events` table SHALL be created with columns: `id` (BIGSERIAL PK), `aggregate_type` (VARCHAR(50) NOT NULL), `aggregate_id` (VARCHAR(50) NOT NULL), `event_type` (VARCHAR(100) NOT NULL), `payload` (TEXT NOT NULL), `routing_key` (VARCHAR(100) NOT NULL), `exchange` (VARCHAR(100) NOT NULL), `status` (VARCHAR(20) NOT NULL DEFAULT 'PENDING'), `retry_count` (INTEGER NOT NULL DEFAULT 0), `created_at` (TIMESTAMPTZ NOT NULL DEFAULT NOW()), `published_at` (TIMESTAMPTZ nullable)

#### Scenario: Index for polling query

- **WHEN** the migration completes
- **THEN** an index `idx_outbox_events_status_created` SHALL exist on columns `(status, created_at)` to support the `findTop100ByStatusOrderByCreatedAtAsc` query

## Constraint: Module location

All outbox-pattern classes SHALL live in the `common-messaging` module under package `com.vehiclerental.common.messaging`. The Flyway migration lives in each service's container module (`src/main/resources/db/migration/`).
