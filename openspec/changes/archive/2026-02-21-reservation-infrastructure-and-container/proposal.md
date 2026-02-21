## Why

The reservation-domain and reservation-application modules are complete (aggregate root with 6-state machine, ReservationItem entity, typed IDs, application service with create + track use cases). The Reservation Service has no way to run — there's no REST API, no database persistence, no Spring Boot assembly. Adding infrastructure + container closes the Walking Skeleton: all three services (Customer, Fleet, Reservation) become fully runnable microservices. This follows the exact same pattern established by customer-infrastructure-and-container (change #5) and fleet-infrastructure-and-container (change #8).

## What Changes

- Add `reservation-infrastructure` Maven module with REST controller, JPA persistence adapter, event publisher adapter, and global exception handler
- Add `reservation-container` Maven module with Spring Boot main class, BeanConfiguration, application.yml, Flyway migration, and integration tests
- REST API: `POST /api/v1/reservations` (create, 201), `GET /api/v1/reservations/{trackingId}` (track, 200) — both wrapped in `ApiResponse<T>` consistent with Customer/Fleet
- JPA entities: `ReservationJpaEntity` (reservations table) and `ReservationItemJpaEntity` (reservation_items table) with `@OneToMany` / `@ManyToOne` relationship — first parent-child JPA mapping in the platform
- Flyway migration: `V1__create_reservation_tables.sql` with reservations + reservation_items tables; `tracking_id` column with UNIQUE constraint and index
- `ReservationRepositoryAdapter` implementing domain `ReservationRepository` port with save, findById, findByTrackingId
- `ReservationJpaRepository` Spring Data interface with `Optional<ReservationJpaEntity> findByTrackingId(UUID trackingId)` custom query method
- `ReservationPersistenceMapper` for bidirectional domain ↔ JPA mapping — key novelty: must reconstruct aggregate with children (`Reservation.reconstruct()` receiving items built via `ReservationItem.reconstruct()`), handle Money (amount + currency), DateRange (pickupDate + returnDate), PickupLocation (address + city), and all typed IDs
- `ReservationDomainEventPublisherAdapter` — logger stub (same as Customer/Fleet, real Outbox in future change)
- `CreateReservationRequest` REST DTO with Jakarta validation and inner `CreateReservationItemRequest` for items
- `GlobalExceptionHandler` mapping ReservationNotFoundException → 404, ReservationDomainException → 422, validation → 400
- `BeanConfiguration` manually registering persistence mapper, application mapper, application service, and use case interface beans (2 use cases: CreateReservationUseCase, TrackReservationUseCase)
- Integration tests: context smoke test, repository adapter round-trip (including items persistence), controller endpoints (Testcontainers PostgreSQL)
- Update root POM: add module declarations and dependencyManagement entries for reservation-infrastructure and reservation-container
- Datasource: separate database `reservation_db` on same PostgreSQL server (consistent with `customer_db`, `fleet_db` — no schema separation, public schema)
- Application port 8183 (following Customer 8181, Fleet 8182)

## Capabilities

### New Capabilities

- `reservation-jpa-persistence`: JPA entities (ReservationJpaEntity + ReservationItemJpaEntity with @OneToMany/@ManyToOne), Spring Data repository with findByTrackingId, persistence adapter implementing 3 domain port methods (save, findById, findByTrackingId), persistence mapper with parent-child reconstruct, and Flyway migration for reservations (tracking_id UNIQUE) + reservation_items tables
- `reservation-rest-api`: REST controller with create (POST, 201) and track (GET, 200) endpoints wrapped in ApiResponse, CreateReservationRequest DTO with inner item record and Jakarta validation, GlobalExceptionHandler (404/422/400/500)
- `reservation-event-publisher`: Logger-based stub adapter implementing ReservationDomainEventPublisher port
- `reservation-container-assembly`: Spring Boot main class, BeanConfiguration (mappers + service + 2 use case beans), application.yml with reservation_db datasource and port 8183, test profile with Testcontainers, and integration tests

### Modified Capabilities

- `multi-module-build`: Add reservation-infrastructure and reservation-container module declarations and dependencyManagement entries

## Impact

- **New modules**: `reservation-service/reservation-infrastructure/` and `reservation-service/reservation-container/`
- **Root POM**: 2 new modules in `<modules>`, 2 new entries in `<dependencyManagement>`
- **Dependencies**: reservation-infrastructure depends on reservation-application + Spring Boot starters (web, data-jpa, validation) + PostgreSQL + Flyway. reservation-container depends on reservation-infrastructure + Testcontainers (test)
- **Database**: New `reservation_db` database (separate DB, public schema — same pattern as customer_db and fleet_db) with 2 tables (reservations, reservation_items)
- **Network**: New microservice on port 8183
- **Pattern consistency**: Replicates customer/fleet infrastructure pattern. Key novelty vs. Customer/Fleet: parent-child JPA relationship (reservations → reservation_items with cascade), findByTrackingId custom query (tracking_id UNIQUE), persistence mapper reconstructing aggregate with child entities, REST request with nested items list
