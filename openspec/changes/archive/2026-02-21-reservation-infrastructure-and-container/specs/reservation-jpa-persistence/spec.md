# reservation-jpa-persistence Specification

## Purpose

JPA persistence layer for Reservation Service. Includes parent-child JPA entities (ReservationJpaEntity + ReservationItemJpaEntity with @OneToMany/@ManyToOne), a Spring Data repository with custom findByTrackingId query, a persistence adapter implementing the domain output port (save, findById, findByTrackingId), a bidirectional mapper handling aggregate reconstruction with child entities, and Flyway migration for reservations + reservation_items tables.

## Requirements

### Requirement: ReservationJpaEntity is separate from domain Reservation

ReservationJpaEntity SHALL be a JPA entity class (`@Entity`, `@Table`) in the infrastructure layer. It SHALL NOT extend or reference the domain `Reservation` class. It SHALL have a protected no-arg constructor, getters, setters, and JPA annotations.

#### Scenario: JPA entity has required JPA annotations

- **WHEN** ReservationJpaEntity is inspected
- **THEN** it SHALL be annotated with `@Entity` and `@Table(name = "reservations")`
- **AND** it SHALL have a field `id` of type `UUID` annotated with `@Id`

#### Scenario: JPA entity has all reservation fields

- **WHEN** ReservationJpaEntity is inspected
- **THEN** it SHALL have fields: `id` (UUID), `trackingId` (UUID), `customerId` (UUID), `pickupAddress` (String), `pickupCity` (String), `returnAddress` (String), `returnCity` (String), `pickupDate` (LocalDate), `returnDate` (LocalDate), `totalPriceAmount` (BigDecimal), `totalPriceCurrency` (String), `status` (String), `failureMessages` (String, nullable), `createdAt` (Instant), `updatedAt` (Instant)

#### Scenario: JPA entity has items collection

- **WHEN** ReservationJpaEntity is inspected
- **THEN** it SHALL have a field `items` of type `List<ReservationItemJpaEntity>`
- **AND** it SHALL be annotated with `@OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)`

#### Scenario: JPA entity has no domain imports

- **WHEN** ReservationJpaEntity imports are inspected
- **THEN** it SHALL NOT import any type from `com.vehiclerental.reservation.domain.*`

#### Scenario: trackingId column has unique constraint

- **WHEN** the `trackingId` field is inspected
- **THEN** it SHALL be annotated with `@Column(name = "tracking_id", nullable = false, unique = true)`

### Requirement: ReservationItemJpaEntity is a child JPA entity

ReservationItemJpaEntity SHALL be a JPA entity class with a `@ManyToOne` back-reference to ReservationJpaEntity. It SHALL NOT extend or reference the domain `ReservationItem` class.

#### Scenario: JPA entity has required JPA annotations

- **WHEN** ReservationItemJpaEntity is inspected
- **THEN** it SHALL be annotated with `@Entity` and `@Table(name = "reservation_items")`
- **AND** it SHALL have a field `id` of type `UUID` annotated with `@Id`

#### Scenario: JPA entity has all item fields

- **WHEN** ReservationItemJpaEntity is inspected
- **THEN** it SHALL have fields: `id` (UUID), `vehicleId` (UUID), `dailyRateAmount` (BigDecimal), `dailyRateCurrency` (String), `days` (int), `subtotalAmount` (BigDecimal), `subtotalCurrency` (String)

#### Scenario: JPA entity has back-reference to parent

- **WHEN** ReservationItemJpaEntity is inspected
- **THEN** it SHALL have a field `reservation` of type `ReservationJpaEntity`
- **AND** it SHALL be annotated with `@ManyToOne(fetch = FetchType.LAZY)` and `@JoinColumn(name = "reservation_id")`

#### Scenario: JPA entity has no domain imports

- **WHEN** ReservationItemJpaEntity imports are inspected
- **THEN** it SHALL NOT import any type from `com.vehiclerental.reservation.domain.*`

### Requirement: ReservationJpaRepository is a Spring Data interface

ReservationJpaRepository SHALL extend `JpaRepository<ReservationJpaEntity, UUID>`. It SHALL have a custom query method `findByTrackingId(UUID trackingId)` returning `Optional<ReservationJpaEntity>`.

#### Scenario: Repository extends JpaRepository

- **WHEN** ReservationJpaRepository is inspected
- **THEN** it SHALL extend `JpaRepository<ReservationJpaEntity, UUID>`

#### Scenario: Repository has findByTrackingId custom query

- **WHEN** ReservationJpaRepository is inspected
- **THEN** it SHALL declare `Optional<ReservationJpaEntity> findByTrackingId(UUID trackingId)`

### Requirement: ReservationRepositoryAdapter implements domain output port

ReservationRepositoryAdapter SHALL implement `ReservationRepository` from the domain layer. It SHALL use `ReservationJpaRepository` for persistence and `ReservationPersistenceMapper` for conversion. It SHALL implement 3 methods: save, findById, findByTrackingId.

> **Pattern note — @Transactional on adapter reads with lazy collections:**
> When an aggregate has `@OneToMany` children with `FetchType.LAZY` (the JPA default), the repository adapter's find methods MUST be annotated with `@Transactional(readOnly = true)`. Without this, the Hibernate session closes after `JpaRepository.findById()` returns, and the mapper's call to `getItems()` triggers a `LazyInitializationException` because the lazy proxy can't initialize outside a session.
>
> This was not needed in Customer or Fleet adapters because they have no child entities — flat field mapping works fine within the scope of a single Spring Data call. Any future service with `@OneToMany` relationships (e.g., Payment with line items) MUST follow this pattern.
>
> The alternative — `FetchType.EAGER` on the `@OneToMany` — avoids the problem but loads children on every query, even when not needed. `@Transactional(readOnly = true)` on the adapter is preferred: it keeps lazy loading as the default while ensuring the session stays open for the full find-then-map operation.

#### Scenario: Save converts domain to JPA and persists

- **WHEN** `save(Reservation)` is called on the adapter
- **THEN** it SHALL convert the domain Reservation to ReservationJpaEntity via the mapper
- **AND** it SHALL call `ReservationJpaRepository.save()` with the JPA entity
- **AND** it SHALL convert the saved JPA entity back to a domain Reservation via the mapper
- **AND** it SHALL return the domain Reservation

#### Scenario: FindById converts JPA to domain

- **WHEN** `findById(ReservationId)` is called with an existing reservation ID
- **THEN** it SHALL call `ReservationJpaRepository.findById()` with the UUID value
- **AND** it SHALL convert the JPA entity to a domain Reservation via the mapper
- **AND** it SHALL return `Optional.of(reservation)`

#### Scenario: FindById returns empty for non-existing reservation

- **WHEN** `findById(ReservationId)` is called with a non-existing reservation ID
- **THEN** it SHALL return `Optional.empty()`

#### Scenario: FindByTrackingId converts JPA to domain

- **WHEN** `findByTrackingId(TrackingId)` is called with an existing tracking ID
- **THEN** it SHALL call `ReservationJpaRepository.findByTrackingId()` with the UUID value
- **AND** it SHALL convert the JPA entity to a domain Reservation via the mapper
- **AND** it SHALL return `Optional.of(reservation)`

#### Scenario: FindByTrackingId returns empty for non-existing tracking ID

- **WHEN** `findByTrackingId(TrackingId)` is called with a non-existing tracking ID
- **THEN** it SHALL return `Optional.empty()`

#### Scenario: Adapter is a Spring component

- **WHEN** ReservationRepositoryAdapter is inspected
- **THEN** it SHALL be annotated with `@Component`

#### Scenario: Find methods are transactional to support lazy collection loading

- **WHEN** `findById` or `findByTrackingId` is called on the adapter
- **THEN** the method SHALL be annotated with `@Transactional(readOnly = true)`
- **AND** the Hibernate session SHALL remain open during the full find-then-map operation
- **AND** lazy `@OneToMany` collections SHALL be accessible within the mapper without `LazyInitializationException`

#### Scenario: Save method is transactional

- **WHEN** `save` is called on the adapter
- **THEN** the method SHALL be annotated with `@Transactional`

### Requirement: ReservationPersistenceMapper converts bidirectionally with parent-child reconstruct

ReservationPersistenceMapper SHALL convert between domain `Reservation` (with `ReservationItem` children) and `ReservationJpaEntity` (with `ReservationItemJpaEntity` children) in both directions. It SHALL be a plain Java class with no Spring annotations. The JPA-to-domain direction SHALL use `Reservation.reconstruct()` and `ReservationItem.reconstruct()` to rebuild the aggregate with its children.

#### Scenario: Domain to JPA entity mapping

- **WHEN** `toJpaEntity(Reservation)` is called
- **THEN** it SHALL return a ReservationJpaEntity with all fields mapped from the domain Reservation
- **AND** `id` SHALL be mapped from `reservation.getId().value()`
- **AND** `trackingId` SHALL be mapped from `reservation.getTrackingId().value()`
- **AND** `customerId` SHALL be mapped from `reservation.getCustomerId().value()`
- **AND** `pickupAddress` and `pickupCity` SHALL be mapped from `reservation.getPickupLocation()`
- **AND** `returnAddress` and `returnCity` SHALL be mapped from `reservation.getReturnLocation()`
- **AND** `pickupDate` and `returnDate` SHALL be mapped from `reservation.getDateRange()`
- **AND** `totalPriceAmount` and `totalPriceCurrency` SHALL be mapped from `reservation.getTotalPrice()`
- **AND** `status` SHALL be mapped from `reservation.getStatus().name()`
- **AND** `failureMessages` SHALL be mapped from the list joined as comma-separated string (or null if empty)
- **AND** each `ReservationItem` SHALL be mapped to a `ReservationItemJpaEntity` with the parent back-reference set

#### Scenario: JPA entity to domain mapping with aggregate reconstruct

- **WHEN** `toDomainEntity(ReservationJpaEntity)` is called
- **THEN** it SHALL return a domain Reservation using `Reservation.reconstruct()`
- **AND** it SHALL convert `id` to `ReservationId`, `trackingId` to `TrackingId`, `customerId` to `CustomerId`
- **AND** it SHALL convert `pickupAddress`+`pickupCity` to `PickupLocation`, `returnAddress`+`returnCity` to `PickupLocation`
- **AND** it SHALL convert `pickupDate`+`returnDate` to `DateRange`
- **AND** it SHALL convert `totalPriceAmount`+`totalPriceCurrency` to `Money`
- **AND** it SHALL convert `status` string to `ReservationStatus` enum
- **AND** it SHALL convert `failureMessages` from comma-separated string to `List<String>` (or empty list if null)

#### Scenario: Child items are reconstructed via ReservationItem.reconstruct()

- **WHEN** `toDomainEntity(ReservationJpaEntity)` is called with a JPA entity that has items
- **THEN** each `ReservationItemJpaEntity` SHALL be converted to a domain `ReservationItem` using `ReservationItem.reconstruct(id, vehicleId, dailyRate, days, subtotal)`
- **AND** `vehicleId` SHALL be converted to `VehicleId`
- **AND** `dailyRateAmount`+`dailyRateCurrency` SHALL be converted to `Money`
- **AND** `subtotalAmount`+`subtotalCurrency` SHALL be converted to `Money`

#### Scenario: Domain item to JPA item mapping sets parent reference

- **WHEN** items are mapped from domain to JPA
- **THEN** each `ReservationItemJpaEntity` SHALL have its `reservation` field set to the parent `ReservationJpaEntity`

### Requirement: Flyway migration creates reservations and reservation_items tables

A Flyway migration `V1__create_reservation_tables.sql` SHALL create the `reservations` and `reservation_items` tables with the correct schema.

#### Scenario: Reservations table schema

- **WHEN** the migration is executed
- **THEN** the `reservations` table SHALL be created with columns: `id` (UUID, primary key), `tracking_id` (UUID, not null, unique), `customer_id` (UUID, not null), `pickup_address` (VARCHAR, not null), `pickup_city` (VARCHAR, not null), `return_address` (VARCHAR, not null), `return_city` (VARCHAR, not null), `pickup_date` (DATE, not null), `return_date` (DATE, not null), `total_price_amount` (NUMERIC, not null), `total_price_currency` (VARCHAR, not null), `status` (VARCHAR, not null), `failure_messages` (TEXT, nullable), `created_at` (TIMESTAMP WITH TIME ZONE, not null), `updated_at` (TIMESTAMP WITH TIME ZONE, not null)

#### Scenario: Tracking ID has unique constraint and index

- **WHEN** the migration is executed
- **THEN** the `reservations` table SHALL have a UNIQUE constraint on `tracking_id`
- **AND** an index SHALL exist on the `tracking_id` column

#### Scenario: Reservation items table schema

- **WHEN** the migration is executed
- **THEN** the `reservation_items` table SHALL be created with columns: `id` (UUID, primary key), `reservation_id` (UUID, not null, foreign key to reservations), `vehicle_id` (UUID, not null), `daily_rate_amount` (NUMERIC, not null), `daily_rate_currency` (VARCHAR, not null), `days` (INTEGER, not null), `subtotal_amount` (NUMERIC, not null), `subtotal_currency` (VARCHAR, not null)

#### Scenario: Foreign key constraint on reservation_items

- **WHEN** the migration is executed
- **THEN** `reservation_items.reservation_id` SHALL have a FOREIGN KEY constraint referencing `reservations(id)`

## Constraint: Infrastructure layer only

All persistence classes SHALL live in `com.vehiclerental.reservation.infrastructure.adapter.output.persistence` or its sub-packages.
