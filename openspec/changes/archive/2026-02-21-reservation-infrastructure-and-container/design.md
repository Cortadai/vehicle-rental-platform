## Context

Reservation Service has domain (34 tests) and application (18 tests) layers complete. This change adds the two outer layers of the hexagonal architecture: infrastructure (adapters for REST and persistence) and container (Spring Boot assembly). After this, Reservation Service will be the third and final runnable microservice in the platform, closing the Walking Skeleton — all three services (Customer, Fleet, Reservation) become fully runnable.

The existing codebase enforces strict layer separation: domain has zero Spring dependencies, application only depends on `spring-tx`. Infrastructure is the first layer where full Spring Boot is allowed. Container is the assembly point where all layers come together.

Customer Service (change #5) and Fleet Service (change #8) already established this pattern. Reservation replicates it but introduces key novelties: parent-child JPA relationship (Reservation → ReservationItems with @OneToMany/@ManyToOne), a custom `findByTrackingId` query method, and a persistence mapper that must reconstruct the aggregate with its child entities.

## Goals / Non-Goals

**Goals:**

* Make Reservation Service a runnable Spring Boot microservice on port 8183
* Implement persistence adapter with parent-child JPA entities (ReservationJpaEntity + ReservationItemJpaEntity)
* Implement REST input adapter exposing 2 use cases (create, track)
* Implement event publisher adapter as a logger placeholder
* Create BeanConfiguration for manual registration of domain and application beans (2 mappers + service + 2 use cases)
* Add Flyway migration for reservations + reservation_items tables (with tracking_id UNIQUE constraint)
* Add integration tests with Testcontainers (PostgreSQL) — including parent-child persistence round-trip
* Close the Walking Skeleton: all three services runnable

**Non-Goals:**

* RabbitMQ / Outbox — event publisher logs only, real messaging is a future change
* Docker Compose update — separate change when SAGA orchestration is ready
* Spring Security / authentication — out of POC scope
* OpenAPI / Swagger documentation — can be added later
* `GET /api/v1/reservations` listing or pagination — not needed yet
* Reservation state transition endpoints (confirm, cancel, etc.) — only create and track for now
* Actuator / health endpoints — can be added later

## Decisions

### Decision 1: Same 10 architectural decisions as Customer and Fleet Services

Reservation infrastructure and container follow exactly the same decisions documented in the customer-infrastructure-and-container design (change #5):

1. JPA Entity fully separate from Domain Entity
2. RepositoryAdapter as the persistence adapter
3. REST Controller delegates to Input Ports, not Application Service
4. CreateReservationRequest as a separate REST DTO
5. Event publisher as a logger (no-op)
6. BeanConfiguration registers domain and application beans manually
7. Flyway for schema management, ddl-auto: validate
8. GlobalExceptionHandler maps exception types to HTTP status codes
9. API path versioning with /api/v1 prefix
10. Integration tests with Testcontainers PostgreSQL

The rationale and alternatives considered are identical. No new architectural decisions are needed for these — the pattern is validated and repeatable across two services already.

### Decision 11: Parent-child JPA relationship with CascadeType.ALL

**Choice**: `ReservationJpaEntity` has a `@OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)` relationship to `ReservationItemJpaEntity`. `ReservationItemJpaEntity` has a `@ManyToOne(fetch = FetchType.LAZY)` back-reference with `@JoinColumn(name = "reservation_id")`. This is the first parent-child JPA mapping in the platform.

**Rationale**: The domain model has `Reservation` (aggregate root) containing `List<ReservationItem>` (entities). The JPA mapping must mirror this: saving a Reservation must cascade to its items. `CascadeType.ALL` ensures insert/update/delete operations propagate. `orphanRemoval = true` handles item deletion. `FetchType.LAZY` on the ManyToOne avoids N+1 queries when loading items.

**Alternative rejected**: Separate save calls for parent and children in the adapter. Would break transactional atomicity and add complexity. JPA cascade handles this correctly.

### Decision 12: Persistence mapper reconstructs aggregate with children

**Choice**: `ReservationPersistenceMapper.toDomainEntity()` calls `Reservation.reconstruct()` passing a list of `ReservationItem` objects built via `ReservationItem.reconstruct()`. Each child item's `VehicleId`, `Money` (dailyRate, subtotal) are reconstructed from their flat JPA columns. The parent's `PickupLocation`, `DateRange`, `Money` (totalPrice), and typed IDs are also reconstructed from flat columns.

**Rationale**: The domain aggregate uses factory methods (`create()` for new, `reconstruct()` for existing). The mapper must use `reconstruct()` because the data is coming from the database, not from user input — validation is skipped, IDs are preserved. Customer and Fleet mappers were simpler (single entity, fewer VOs). Reservation's mapper is more complex because it decomposes/recomposes: `Money` → `(amount, currency)`, `DateRange` → `(pickupDate, returnDate)`, `PickupLocation` → `(address, city)`, plus the parent-child hierarchy.

**Alternative rejected**: Use MapStruct. The reconstruct pattern with factory methods and typed IDs doesn't map well to MapStruct's getter/setter convention. Manual mappers are explicit and match the pattern established by Customer and Fleet.

### Decision 13: findByTrackingId as Spring Data derived query

**Choice**: `ReservationJpaRepository` declares `Optional<ReservationJpaEntity> findByTrackingId(UUID trackingId)` as a derived query method. Spring Data generates the SQL from the method name. The `tracking_id` column has a UNIQUE constraint and index in Flyway.

**Rationale**: The track use case needs to look up reservations by their public-facing tracking ID (not the internal primary key). Spring Data derived queries are sufficient — the method name `findByTrackingId` maps directly to `SELECT ... WHERE tracking_id = ?`. The UNIQUE constraint ensures at most one result. The index makes the lookup O(log n).

**Alternative rejected**: Custom `@Query` annotation. Unnecessary for a simple single-column lookup that Spring Data can derive automatically.

### Decision 14: Failure messages stored as comma-separated TEXT

**Choice**: `ReservationJpaEntity.failureMessages` is stored as a single `TEXT` column. The mapper joins `List<String>` with comma separation for persistence and splits on load. Null/empty list maps to null in the column.

**Rationale**: Failure messages are diagnostic strings used during SAGA compensation. They don't need to be queried individually or indexed. A separate table for failure messages would be overkill. A JSON column would add a Jackson dependency to the persistence layer. Comma-separated text is the simplest approach and matches the read-only nature of these messages.

**Alternative rejected**: JSON column (`@Convert` with Jackson). Adds dependency and complexity for data that's only ever read as a whole list. Also rejected: separate `reservation_failure_messages` table. Too much overhead for simple diagnostic strings.

### Decision 15: Port 8183 for Reservation Service

**Choice**: Reservation Service runs on port 8183. Following Customer 8181, Fleet 8182.

**Rationale**: Sequential port assignment is simple, predictable, and avoids conflicts when running services simultaneously during development.

### Decision 16: Two tables in a single Flyway migration

**Choice**: `V1__create_reservation_tables.sql` creates both `reservations` and `reservation_items` tables in a single migration file. The foreign key from `reservation_items.reservation_id` to `reservations.id` is declared inline.

**Rationale**: Both tables are part of the same aggregate boundary and are always created together. Splitting into two migrations (V1 for parent, V2 for child) would add unnecessary file management. The foreign key is a simple reference, not a complex constraint.

**Alternative rejected**: Separate V1/V2 migrations. Adds overhead with no benefit — the tables are tightly coupled and always deployed together.

### Decision 17: CreateReservationRequest with nested CreateReservationItemRequest

**Choice**: `CreateReservationRequest` is a Java record with an inner `CreateReservationItemRequest` record. The items field uses `@NotEmpty @Valid` to ensure at least one item is provided and each item is validated. The controller converts the nested structure to `CreateReservationCommand` with `CreateReservationItemCommand` list.

**Rationale**: The REST API accepts a JSON body with a nested `items` array — this is the natural structure for creating a reservation with its items in a single request. Jakarta validation cascades via `@Valid` on the list. This is the first nested request DTO in the platform.

**Alternative rejected**: Flat request without items, followed by separate item creation endpoints. Would require multiple HTTP calls for a single logical operation.

## Package Structure

### Infrastructure Module

    reservation-service/reservation-infrastructure/src/main/java/com/vehiclerental/reservation/infrastructure/
    ├── adapter/
    │   ├── input/
    │   │   └── rest/
    │   │       ├── ReservationController.java
    │   │       └── dto/
    │   │           └── CreateReservationRequest.java
    │   └── output/
    │       ├── persistence/
    │       │   ├── ReservationJpaRepository.java
    │       │   ├── ReservationRepositoryAdapter.java
    │       │   ├── entity/
    │       │   │   ├── ReservationJpaEntity.java
    │       │   │   └── ReservationItemJpaEntity.java
    │       │   └── mapper/
    │       │       └── ReservationPersistenceMapper.java
    │       └── event/
    │           └── ReservationDomainEventPublisherAdapter.java
    └── config/
        └── GlobalExceptionHandler.java

### Container Module

    reservation-service/reservation-container/src/main/java/com/vehiclerental/reservation/
    ├── ReservationServiceApplication.java
    └── config/
        └── BeanConfiguration.java

## Risks / Trade-offs

* **Parent-child JPA mapping adds complexity** — This is the first aggregate with child entities in the platform. The persistence mapper is significantly more complex than Customer/Fleet (~50 lines vs ~20). Mitigation: The Reservation aggregate is the most complex in the platform; this complexity is inherent to the domain model, not accidental.
* **Cascade operations can be tricky** — `CascadeType.ALL` with `orphanRemoval` can cause unexpected behavior if items are modified incorrectly. Mitigation: Items are immutable in the current domain model (no update use case). The cascade is only used for initial insert. Future state changes (confirm, cancel) don't modify items.
* **Failure messages as comma-separated text** — If a failure message contains a comma, the split on load will be incorrect. Mitigation: Failure messages are system-generated strings (from SAGA compensation), not user input. We control the format. If this becomes a problem, migrating to a JSON column or separator character is straightforward.
* **Logger-only event publisher** — Same risk as Customer/Fleet. Events are "published" but go nowhere. Mitigation: Log messages clearly state the event type and ID. Real messaging is the next logical change after the Walking Skeleton is complete.
* **Testcontainers requires Docker** — Same risk as Customer/Fleet. Mitigation: Unit tests run without Docker. Integration tests are in the `verify` phase.
* **Three services with separate databases** — Reservation uses `reservation_db`, Customer uses `customer_db`, Fleet uses `fleet_db`. During development, all share one PostgreSQL server. Mitigation: Testcontainers creates isolated containers per test suite. In production, these would be separate instances.

## Open Questions

None. All decisions follow established patterns or have clear rationale for the new elements (parent-child JPA, findByTrackingId, nested request DTO).
