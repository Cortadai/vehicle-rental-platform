## Context

The walking skeleton is complete with 3 services (Reservation, Customer, Fleet) following strict hexagonal architecture. Domain events exist in all aggregates (`ReservationCreatedEvent`, `ReservationCancelledEvent`) but are currently published via a no-op logger adapter (`ReservationDomainEventPublisherAdapter` logs to SLF4J INFO).

The application service already follows the correct flow — `Reservation.create()` accumulates events, `reservationRepository.save()` persists, then `eventPublisher.publish(savedReservation.getDomainEvents())` dispatches them. The output port `ReservationDomainEventPublisher` accepts `List<DomainEvent>` and remains unchanged.

This design introduces the Outbox Pattern to replace the no-op publisher, a `common-messaging` module for shared infrastructure, RabbitMQ topology for Reservation Service, and Docker Compose for local infrastructure.

### Current Code Flow (unchanged by this design)
```
ReservationApplicationService.execute()          // @Transactional
  → Reservation.create(...)                      // aggregate accumulates events
  → reservationRepository.save(reservation)      // JPA persist
  → eventPublisher.publish(domainEvents)         // ← THIS adapter changes
  → savedReservation.clearDomainEvents()
```

### Key Constraint
The `eventPublisher.publish()` call happens **inside the same `@Transactional` method** as the repository save. This is critical — the new `OutboxReservationDomainEventPublisher` writes to `outbox_events` in the same transaction, guaranteeing atomicity without any changes to the application layer.

### Naming Convention (verified against codebase)
The walking skeleton establishes a consistent naming pattern across all 3 services:
- **Output port (interface):** `{Service}DomainEventPublisher` — e.g., `CustomerDomainEventPublisher`, `FleetDomainEventPublisher`, `ReservationDomainEventPublisher`
- **Adapter (implementation):** `{Service}DomainEventPublisherAdapter` — e.g., `CustomerDomainEventPublisherAdapter`, `ReservationDomainEventPublisherAdapter`
- **Location:** `{service}-infrastructure/.../adapter/output/event/`
- **Wiring:** All adapters use `@Component` (auto-detected by Spring), none are registered in `BeanConfiguration`

The new Outbox adapter follows this convention: `OutboxReservationDomainEventPublisher` (prefix `Outbox` + port name `ReservationDomainEventPublisher`).

## Goals / Non-Goals

**Goals:**
- Guarantee at-least-once delivery of domain events from Reservation Service to RabbitMQ via the Outbox Pattern
- Provide reusable Outbox infrastructure in `common-messaging` for all 4 services
- Establish the RabbitMQ topology (exchanges, queues, DLQ) for Reservation Service
- Provide Docker Compose for local PostgreSQL + RabbitMQ development
- Validate the pattern with integration tests (atomicity + end-to-end publishing)

**Non-Goals:**
- No consumers — no service listens for events yet (Phase 3)
- No SAGA orchestrator — event flows one-way (Phase 4)
- No idempotent consumer infrastructure — not needed until consumers exist
- No service Dockerfiles — services run from IDE against `localhost`
- No Payment Service — arrives in Phase 2

## Decisions

### Decision 1: `common-messaging` as a new Maven module (not in `common`, not duplicated per service)

**Choice:** New `common-messaging` module at project root, alongside `common`.

**Why not `common`:** The existing `common` module is pure Java — `DomainEvent`, `BaseEntity`, `AggregateRoot`, `Money`. Adding JPA entities and Spring scheduling would violate the hexagonal layer separation this POC is teaching.

**Why not duplicate per service:** `OutboxEvent`, `OutboxEventRepository`, and `OutboxPublisher` are identical across all 4 services. Duplicating 5-6 classes x4 services adds maintenance burden for zero learning value.

**Module scope:**
```
common-messaging/
└── src/main/java/com/vehiclerental/common/messaging/
    ├── outbox/
    │   ├── OutboxEvent.java           // @Entity with static factory + private no-arg constructor for JPA
    │   ├── OutboxStatus.java          // enum: PENDING, PUBLISHED, FAILED
    │   ├── OutboxEventRepository.java // Spring Data JPA
    │   └── OutboxPublisher.java       // @Scheduled, polls + publishes
    ├── cleanup/
    │   └── OutboxCleanupScheduler.java // @Scheduled cron, deletes old PUBLISHED
    └── config/
        ├── MessagingSchedulingConfig.java // @Configuration + @EnableScheduling
        └── MessageConverterConfig.java    // Jackson2JsonMessageConverter + RabbitTemplate
```

**`@EnableScheduling` lives in `common-messaging`:** The `MessagingSchedulingConfig` class is annotated with `@Configuration` and `@EnableScheduling`. Any service that imports `common-messaging` gets scheduling enabled automatically — no need to add `@EnableScheduling` to each service's main class. This is auto-detected via Spring Boot's component scanning when the module is on the classpath.

**Dependencies:** `spring-boot-starter-data-jpa`, `spring-boot-starter-amqp`. Each `*-infrastructure` module that needs outbox adds `common-messaging` as a dependency.

**Alternatives considered:**
- A separate `outbox` module + `messaging-config` module: too fine-grained for a POC
- Abstract base class approach: premature; the outbox is data, not behavior
- `@EnableScheduling` on each service's main class: error-prone, easy to forget

### Decision 2: Outbox event serialization — Jackson to JSON string, stored as TEXT

**Choice:** The `OutboxReservationDomainEventPublisher` serializes each `DomainEvent` to a JSON string using Jackson `ObjectMapper` and stores it in the `payload` TEXT column. The `OutboxPublisher` sends this raw JSON string to RabbitMQ.

**Why JSON string (not binary, not JSONB):**
- `TEXT` is portable across databases (H2 for future tests, PostgreSQL for real)
- JSON is human-readable in the outbox table for debugging
- Jackson with `JavaTimeModule` handles `Instant`, `UUID`, typed IDs natively
- The `payload` is opaque to the outbox — it just stores and forwards

**Serialization flow:**
```
DomainEvent (Java record)
  → ObjectMapper.writeValueAsString()
  → OutboxEvent.payload (TEXT column)
  → OutboxPublisher sends payload as RabbitMQ message body
  → Consumer deserializes with Jackson on the other side
```

**Important:** Domain events use typed IDs (`ReservationId`, `CustomerId`) which are Java records wrapping `UUID`. Jackson serializes these as nested objects by default (`{"value": "uuid-string"}`). This is acceptable — consumers will use the same domain types. No custom serializer needed.

**Outbox table schema (Flyway V2):**
```sql
CREATE TABLE outbox_events (
    id              BIGSERIAL       PRIMARY KEY,
    aggregate_type  VARCHAR(50)     NOT NULL,
    aggregate_id    VARCHAR(50)     NOT NULL,
    event_type      VARCHAR(100)    NOT NULL,
    payload         TEXT            NOT NULL,
    routing_key     VARCHAR(100)    NOT NULL,
    exchange        VARCHAR(100)    NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    retry_count     INTEGER         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    published_at    TIMESTAMPTZ
);

CREATE INDEX idx_outbox_events_status_created ON outbox_events (status, created_at);
```

The index on `(status, created_at)` supports the `findTop100ByStatusOrderByCreatedAtAsc` query used by the `OutboxPublisher` scheduler.

### Decision 3: Outbox polling strategy — fixed delay, not CDC

**Choice:** `@Scheduled(fixedDelay = 500)` polling with batch size 100.

**Why not Change Data Capture (Debezium):**
- CDC requires Kafka Connect + Debezium infrastructure — massive complexity for a learning POC
- Polling is simple, debuggable, and sufficient for the expected volume
- The 500ms latency is acceptable for a rental platform (not real-time trading)

**Why not `@TransactionalEventListener`:**
- Spring's `@TransactionalEventListener(phase = AFTER_COMMIT)` could trigger publishing after the transaction commits
- But if the publish fails at that point, the event is lost — there's no outbox to retry from
- The polling approach is retry-safe by design: if publishing fails, the event stays PENDING

**Retry strategy:**
- On publish failure, increment `retryCount` on the `OutboxEvent`
- After 5 failures, mark as `FAILED` (logged at ERROR for alerting)
- No exponential backoff on the outbox side — the 500ms fixed delay provides natural spacing
- FAILED events require manual investigation (operational concern, not automated recovery)

**Transaction granularity:** Each event is published and status-updated in its own `@Transactional` call within the loop. If event #47 fails, events #1-46 are already committed as PUBLISHED. This is the resilient choice — a per-batch transaction would roll back all 46 successful publishes on a single failure. The trade-off is more database commits, but at POC scale (tens of events/minute) this is negligible.

### Decision 4: RabbitMQ topology — Topic Exchange with routing key pattern

**Choice:** One `TopicExchange` per service with routing keys following `{service}.{event-type}` pattern.

**Topology scope — two layers:**

1. **`definitions.json` (Docker/RabbitMQ):** Pre-loads the **full topology for all 4 services** — all exchanges, queues, bindings, and DLQs. Empty queues cost nothing, and having the topology ready before any service starts prevents race conditions. This is consistent with the proposal's "pre-loaded topology" statement.

2. **Java `@Bean` declarations (per service):** This change only declares Java beans for `reservation.exchange` and its queue/binding in `RabbitMQConfig`. Other services will declare their own beans when they are implemented. The Java beans serve as documentation and idempotent validation of what `definitions.json` already created.

**Full topology (in definitions.json):**
```
reservation.exchange (TopicExchange, durable)
  └── reservation.created.queue  → routing key "reservation.created"
      └── DLQ → dlx.exchange → reservation.dlq

customer.exchange (TopicExchange, durable)
  └── customer.validated.queue   → routing key "customer.validated"
  └── customer.rejected.queue    → routing key "customer.rejected"
      └── DLQ → dlx.exchange → customer.dlq

payment.exchange (TopicExchange, durable)
  └── payment.completed.queue    → routing key "payment.completed"
  └── payment.failed.queue       → routing key "payment.failed"
      └── DLQ → dlx.exchange → payment.dlq

fleet.exchange (TopicExchange, durable)
  └── fleet.confirmed.queue      → routing key "fleet.confirmed"
  └── fleet.rejected.queue       → routing key "fleet.rejected"
      └── DLQ → dlx.exchange → fleet.dlq

dlx.exchange (DirectExchange, durable)
  └── {service}.dlq per service
```

**Java beans declared in this change (reservation-infrastructure only):**
```
reservation.exchange (TopicExchange)
reservation.created.queue (with DLQ args)
reservation.dlq
dlx.exchange (DirectExchange)
bindings for the above
```

**Why Topic Exchange (not Direct, not Fanout):**
- Topic allows wildcard bindings (`reservation.#` to catch all reservation events)
- Future services can bind selectively (e.g., audit service binds `*.created` across all exchanges)
- Direct is too rigid; Fanout broadcasts everything

### Decision 5: Docker Compose profile `infra` with schema-per-service PostgreSQL

**Choice:** Single `docker-compose.yml` at project root with profile `infra`. One PostgreSQL instance with 4 schemas, one RabbitMQ instance with management plugin.

**PostgreSQL setup:**
- Single container `vehicle-rental-postgres` on port 5432
- Init script `docker/postgres/init-schemas.sql` creates: `reservation`, `customer`, `payment`, `fleet` schemas
- Each schema has a dedicated user (e.g., `reservation_user`) with permissions restricted to its schema
- `search_path` set per user so Flyway and JPA work without schema prefixes

**RabbitMQ setup:**
- Single container `vehicle-rental-rabbitmq` on ports 5672 (AMQP) + 15672 (Management UI)
- `docker/rabbitmq/definitions.json` pre-loads all exchanges, queues, bindings, DLQs for all 4 services (see Decision 4)
- Management UI available at `http://localhost:15672` (guest/guest) for visual verification

**Why a single PostgreSQL instance (not one per service):**
- This is a learning POC, not a production deployment
- Schema isolation provides the same logical separation as separate instances
- Reduces Docker resource usage on developer machines
- Real microservices would use separate databases — but the pattern is identical

### Decision 6: OutboxReservationDomainEventPublisher replaces the no-op adapter

**Choice:** New class `OutboxReservationDomainEventPublisher` implements `ReservationDomainEventPublisher`. The existing `ReservationDomainEventPublisherAdapter` (logger no-op) is deleted.

**Naming rationale:** Follows the codebase convention — prefix `Outbox` + the port interface name `ReservationDomainEventPublisher`. This is consistent with how the other adapters are named: `{Qualifier}{PortName}` (e.g., the persistence adapter could be `JpaReservationRepository`).

**Why delete (not keep as fallback):**
- Two implementations of the same port creates ambiguity (`@Primary`, `@ConditionalOnProperty`, etc.)
- The no-op was explicitly documented as temporary ("deferring real messaging to a future change")
- If RabbitMQ is down, the outbox still works — events stay PENDING in the database

**Wiring — no change in BeanConfiguration required:**
- Currently: `ReservationDomainEventPublisherAdapter` is `@Component` (verified in codebase — NOT registered in `BeanConfiguration`)
- `BeanConfiguration.reservationApplicationService()` accepts `ReservationDomainEventPublisher` (the interface) as a parameter — Spring auto-injects the single `@Component` that implements it
- After: Delete `ReservationDomainEventPublisherAdapter`, add `OutboxReservationDomainEventPublisher` with `@Component` in the same package
- Spring still finds exactly one implementation → no `BeanConfiguration` change needed

**OutboxEvent as JPA entity with static factory:**
`OutboxEvent` uses a dual-constructor pattern common in JPA entities that follow DDD style:
- `protected OutboxEvent()` — no-arg constructor required by JPA (never called by application code)
- `public static OutboxEvent create(...)` — factory method for programmatic creation with validation and defaults (`status = PENDING`, `createdAt = Instant.now()`, `retryCount = 0`)

**Implementation sketch:**
```java
@Component
public class OutboxReservationDomainEventPublisher implements ReservationDomainEventPublisher {
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void publish(List<DomainEvent> domainEvents) {
        for (DomainEvent event : domainEvents) {
            OutboxEvent outboxEvent = OutboxEvent.create(
                "RESERVATION",                              // aggregateType
                extractAggregateId(event),                  // aggregateId
                event.getClass().getSimpleName(),           // eventType
                objectMapper.writeValueAsString(event),     // payload (JSON)
                deriveRoutingKey(event),                    // e.g., "reservation.created"
                "reservation.exchange"                      // exchange
            );
            outboxRepository.save(outboxEvent);
        }
    }
}
```

### Decision 7: Integration test strategy — two focused ITs

**`OutboxAtomicityIT`** — The most critical test of this entire change:
1. Create a reservation via the application service
2. Assert both `reservations` and `outbox_events` tables have rows
3. Trigger a domain validation failure (e.g., invalid date range)
4. Assert both tables rolled back — zero rows
5. Uses: Testcontainers PostgreSQL, `@SpringBootTest`, `@Transactional`

**`OutboxPublisherIT`** — End-to-end publishing:
1. Insert a PENDING `OutboxEvent` directly into the database
2. Wait for the scheduler to pick it up (Awaitility, max 2 seconds)
3. Assert the event status changed to PUBLISHED
4. Assert the message arrived in RabbitMQ (via `RabbitTemplate.receive()` or `@RabbitListener` in test)
5. Uses: Testcontainers PostgreSQL + RabbitMQ, `@SpringBootTest`

**Why not test the full flow (REST → DB → Outbox → RabbitMQ) in one IT:**
- Coupling too many layers makes failures hard to diagnose
- The two focused tests cover the two critical boundaries independently
- The manual verification (Verification Criteria #3 in proposal) covers the full flow

## Risks / Trade-offs

**[Risk] Polling latency (500ms) delays event delivery** → Acceptable for a rental platform. Not suitable for real-time systems, but the alternative (CDC) adds disproportionate infrastructure complexity for this POC.

**[Risk] OutboxPublisher processes events one-by-one in a loop** → For high volume, batch publishing or parallel processing would be needed. At POC scale (tens of events/minute), this is a non-issue. Can be optimized later without changing the schema.

**[Risk] FAILED events (retryCount >= 5) require manual intervention** → No automated alerting in this POC. In production, you'd alert on FAILED count via metrics. For now, log at ERROR level and rely on log monitoring.

**[Risk] Jackson serialization of typed IDs produces nested JSON** → `ReservationId` serializes as `{"value": "uuid"}` instead of flat `"uuid"`. This is consistent — consumers use the same domain types and Jackson config. If flat UUIDs are needed later, add `@JsonValue` to the record.

**[Risk] Docker Compose definitions.json pre-declares topology for all 4 services** → Some queues/exchanges won't have publishers/consumers yet. This is intentional — topology is stable infrastructure that should exist before services start. No runtime cost for empty queues.

**[Trade-off] common-messaging has Spring dependencies** → Unlike `common` (pure Java), `common-messaging` depends on Spring Data JPA and Spring AMQP. This is by design — it's an infrastructure module, not a domain module. The hexagonal boundary is preserved: domain modules never depend on `common-messaging`.

**[Trade-off] Per-event transaction in OutboxPublisher (not per-batch)** → More database commits but more resilient. A failure on event #47 doesn't roll back 46 already-published events. At POC scale the overhead is negligible.

## Open Questions

**Q: Should `common-messaging` component scanning be explicit or rely on Spring Boot auto-detection?**
The module uses `@Component` and `@Configuration` annotations. When a service adds `common-messaging` as a dependency, Spring Boot's `@SpringBootApplication` will auto-scan `com.vehiclerental.common.messaging.*` only if the base package `com.vehiclerental` is in the scan path. Since all container modules use `@SpringBootApplication` at `com.vehiclerental.{service}`, the `com.vehiclerental.common.messaging` package may or may not be auto-scanned depending on Spring Boot's default behavior. If not, a `@ComponentScan(basePackages = "com.vehiclerental")` or a `spring.factories` / `AutoConfiguration` entry in `common-messaging` will be needed. This will be resolved during implementation — the solution is a one-line fix either way.
