payment-jpa-persistence
=======================

Purpose
-------

JPA persistence layer for Payment Service. Includes a JPA entity separate from the domain entity, a Spring Data repository with `findByReservationId`, a persistence adapter implementing the domain output port, a bidirectional mapper with JSON serialization for `failureMessages`, and the Flyway migration for the payments table.

## ADDED Requirements

### Requirement: PaymentJpaEntity is separate from domain Payment

PaymentJpaEntity SHALL be a JPA entity class (`@Entity`, `@Table`) in the infrastructure layer. It SHALL NOT extend or reference the domain `Payment` class. It SHALL have a protected no-arg constructor, getters, setters, and JPA annotations.

#### Scenario: JPA entity has required JPA annotations

- **WHEN** PaymentJpaEntity is inspected
- **THEN** it SHALL be annotated with `@Entity` and `@Table(name = "payments")`
- **AND** it SHALL have a field `id` of type `UUID` annotated with `@Id`

#### Scenario: JPA entity has all payment fields

- **WHEN** PaymentJpaEntity is inspected
- **THEN** it SHALL have fields: `id` (UUID), `reservationId` (UUID), `customerId` (UUID), `amount` (BigDecimal), `currency` (String), `status` (String), `failureMessages` (String, nullable — JSON-serialized), `createdAt` (Instant), `updatedAt` (Instant)

#### Scenario: JPA entity has no domain imports

- **WHEN** PaymentJpaEntity imports are inspected
- **THEN** it SHALL NOT import any type from `com.vehiclerental.payment.domain.*`

### Requirement: PaymentJpaRepository is a Spring Data interface with findByReservationId

PaymentJpaRepository SHALL extend `JpaRepository<PaymentJpaEntity, UUID>`. It SHALL declare a `findByReservationId(UUID)` derived query method.

#### Scenario: Repository extends JpaRepository

- **WHEN** PaymentJpaRepository is inspected
- **THEN** it SHALL extend `JpaRepository<PaymentJpaEntity, UUID>`

#### Scenario: Repository has findByReservationId

- **WHEN** PaymentJpaRepository is inspected
- **THEN** it SHALL declare `Optional<PaymentJpaEntity> findByReservationId(UUID reservationId)`

### Requirement: PaymentRepositoryAdapter implements domain output port

PaymentRepositoryAdapter SHALL implement `PaymentRepository` from the domain layer. It SHALL use `PaymentJpaRepository` for persistence and `PaymentPersistenceMapper` for conversion. It SHALL be annotated with `@Component`.

#### Scenario: Save converts domain to JPA and persists

- **WHEN** `save(Payment)` is called on the adapter
- **THEN** it SHALL convert the domain Payment to PaymentJpaEntity via the mapper
- **AND** it SHALL call `PaymentJpaRepository.save()` with the JPA entity
- **AND** it SHALL convert the saved JPA entity back to a domain Payment via the mapper
- **AND** it SHALL return the reconstructed domain Payment

#### Scenario: FindById converts JPA to domain

- **WHEN** `findById(PaymentId)` is called with an existing payment ID
- **THEN** it SHALL call `PaymentJpaRepository.findById()` with the UUID value
- **AND** it SHALL convert the JPA entity to a domain Payment via the mapper
- **AND** it SHALL return `Optional.of(payment)`

#### Scenario: FindById returns empty for non-existing payment

- **WHEN** `findById(PaymentId)` is called with a non-existing payment ID
- **THEN** it SHALL return `Optional.empty()`

#### Scenario: FindByReservationId converts JPA to domain

- **WHEN** `findByReservationId(ReservationId)` is called with an existing reservation ID
- **THEN** it SHALL call `PaymentJpaRepository.findByReservationId()` with the UUID value
- **AND** it SHALL convert the JPA entity to a domain Payment via the mapper
- **AND** it SHALL return `Optional.of(payment)`

#### Scenario: FindByReservationId returns empty for non-existing reservation

- **WHEN** `findByReservationId(ReservationId)` is called with a reservation ID that has no payment
- **THEN** it SHALL return `Optional.empty()`

#### Scenario: Adapter is a Spring component

- **WHEN** PaymentRepositoryAdapter is inspected
- **THEN** it SHALL be annotated with `@Component`

### Requirement: PaymentPersistenceMapper converts bidirectionally with JSON serialization

PaymentPersistenceMapper SHALL convert between domain `Payment` and `PaymentJpaEntity` in both directions. It SHALL be a `@Component` with Spring's `ObjectMapper` injected via constructor for JSON serialization of `failureMessages`.

#### Scenario: Domain to JPA entity mapping

- **WHEN** `toJpaEntity(Payment)` is called
- **THEN** it SHALL return a PaymentJpaEntity with all fields mapped from the domain Payment
- **AND** `id` SHALL be mapped from `payment.getId().value()`
- **AND** `reservationId` SHALL be mapped from `payment.getReservationId().value()`
- **AND** `customerId` SHALL be mapped from `payment.getCustomerId().value()`
- **AND** `amount` SHALL be mapped from `payment.getAmount().amount()`
- **AND** `currency` SHALL be mapped from `payment.getAmount().currency().getCurrencyCode()`
- **AND** `status` SHALL be mapped from `payment.getStatus().name()`
- **AND** `failureMessages` SHALL be serialized to JSON string if non-empty, or `null` if empty

#### Scenario: JPA entity to domain mapping

- **WHEN** `toDomainEntity(PaymentJpaEntity)` is called
- **THEN** it SHALL return a domain Payment using `Payment.reconstruct()`
- **AND** it SHALL convert `id` to `PaymentId`, `reservationId` to `ReservationId`, `customerId` to `CustomerId`
- **AND** it SHALL convert `amount` and `currency` to `Money`
- **AND** it SHALL convert `status` string to `PaymentStatus` enum
- **AND** it SHALL deserialize `failureMessages` JSON string to `List<String>`, or `List.of()` if null

#### Scenario: Mapper is a Spring component with ObjectMapper

- **WHEN** PaymentPersistenceMapper is inspected
- **THEN** it SHALL be annotated with `@Component`
- **AND** it SHALL receive `ObjectMapper` via constructor injection

#### Scenario: Empty failureMessages serializes to null

- **WHEN** `toJpaEntity(Payment)` is called with a payment that has an empty failureMessages list
- **THEN** the `failureMessages` field in the JPA entity SHALL be `null`

#### Scenario: Null failureMessages deserializes to empty list

- **WHEN** `toDomainEntity(PaymentJpaEntity)` is called with a JPA entity that has null failureMessages
- **THEN** the `failureMessages` in the domain Payment SHALL be `List.of()` (empty list, not null)

### Requirement: Flyway migration creates payments table

A Flyway migration `V1__create_payments_table.sql` SHALL create the `payments` table with the correct schema.

#### Scenario: Table schema matches JPA entity

- **WHEN** the migration is executed
- **THEN** the `payments` table SHALL be created with columns: `id` (UUID, primary key), `reservation_id` (UUID, not null, unique), `customer_id` (UUID, not null), `amount` (NUMERIC(10,2), not null), `currency` (VARCHAR(3), not null), `status` (VARCHAR(20), not null), `failure_messages` (TEXT, nullable), `created_at` (TIMESTAMP WITH TIME ZONE, not null), `updated_at` (TIMESTAMP WITH TIME ZONE, not null)

#### Scenario: Reservation ID has unique constraint

- **WHEN** the migration is executed
- **THEN** the `payments` table SHALL have a `UNIQUE` constraint on `reservation_id`
- **AND** this SHALL enforce at the database level that only one payment exists per reservation (belt-and-suspenders with the application-level idempotency check in `ProcessPaymentUseCase`)

Constraint: Infrastructure layer only
--------------------------------------

All persistence classes SHALL live in `com.vehiclerental.payment.infrastructure.adapter.output.persistence` or its sub-packages.
