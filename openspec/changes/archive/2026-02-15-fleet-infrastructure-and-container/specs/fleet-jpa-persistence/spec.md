fleet-jpa-persistence
=====================

Purpose
-------

JPA persistence layer for Fleet Service. Includes a JPA entity separate from the domain entity, a Spring Data repository, a persistence adapter implementing the domain output port, a bidirectional mapper, and the Flyway migration for the vehicles table.

## ADDED Requirements

### Requirement: VehicleJpaEntity is separate from domain Vehicle

VehicleJpaEntity SHALL be a JPA entity class (`@Entity`, `@Table`) in the infrastructure layer. It SHALL NOT extend or reference the domain `Vehicle` class. It SHALL have a public no-arg constructor, getters, setters, and JPA annotations.

#### Scenario: JPA entity has required JPA annotations

- **WHEN** VehicleJpaEntity is inspected
- **THEN** it SHALL be annotated with `@Entity` and `@Table(name = "vehicles")`
- **AND** it SHALL have a field `id` of type `UUID` annotated with `@Id`

#### Scenario: JPA entity has all vehicle fields

- **WHEN** VehicleJpaEntity is inspected
- **THEN** it SHALL have fields: `id` (UUID), `licensePlate` (String), `make` (String), `model` (String), `year` (int), `category` (String), `dailyRateAmount` (BigDecimal), `dailyRateCurrency` (String), `description` (String, nullable), `status` (String), `createdAt` (Instant)

#### Scenario: JPA entity has no domain imports

- **WHEN** VehicleJpaEntity imports are inspected
- **THEN** it SHALL NOT import any type from `com.vehiclerental.fleet.domain.*`

### Requirement: VehicleJpaRepository is a Spring Data interface

VehicleJpaRepository SHALL extend `JpaRepository<VehicleJpaEntity, UUID>`. It SHALL have no custom query methods beyond what JPA provides.

#### Scenario: Repository extends JpaRepository

- **WHEN** VehicleJpaRepository is inspected
- **THEN** it SHALL extend `JpaRepository<VehicleJpaEntity, UUID>`

### Requirement: VehicleRepositoryAdapter implements domain output port

VehicleRepositoryAdapter SHALL implement `VehicleRepository` from the domain layer. It SHALL use `VehicleJpaRepository` for persistence and `VehiclePersistenceMapper` for conversion.

#### Scenario: Save converts domain to JPA and persists

- **WHEN** `save(Vehicle)` is called on the adapter
- **THEN** it SHALL convert the domain Vehicle to VehicleJpaEntity via the mapper
- **AND** it SHALL call `VehicleJpaRepository.save()` with the JPA entity
- **AND** it SHALL convert the saved JPA entity back to a domain Vehicle via the mapper
- **AND** it SHALL return the domain Vehicle

#### Scenario: FindById converts JPA to domain

- **WHEN** `findById(VehicleId)` is called with an existing vehicle ID
- **THEN** it SHALL call `VehicleJpaRepository.findById()` with the UUID value
- **AND** it SHALL convert the JPA entity to a domain Vehicle via the mapper
- **AND** it SHALL return `Optional.of(vehicle)`

#### Scenario: FindById returns empty for non-existing vehicle

- **WHEN** `findById(VehicleId)` is called with a non-existing vehicle ID
- **THEN** it SHALL return `Optional.empty()`

#### Scenario: Adapter is a Spring component

- **WHEN** VehicleRepositoryAdapter is inspected
- **THEN** it SHALL be annotated with `@Component`

### Requirement: VehiclePersistenceMapper converts bidirectionally

VehiclePersistenceMapper SHALL convert between domain `Vehicle` and `VehicleJpaEntity` in both directions. It SHALL be a plain Java class with no Spring annotations.

#### Scenario: Domain to JPA entity mapping

- **WHEN** `toJpaEntity(Vehicle)` is called
- **THEN** it SHALL return a VehicleJpaEntity with all fields mapped from the domain Vehicle
- **AND** `id` SHALL be mapped from `vehicle.getId().value()`
- **AND** `licensePlate` SHALL be mapped from `vehicle.getLicensePlate().value()`
- **AND** `category` SHALL be mapped from `vehicle.getCategory().name()`
- **AND** `dailyRateAmount` SHALL be mapped from `vehicle.getDailyRate().money().amount()`
- **AND** `dailyRateCurrency` SHALL be mapped from `vehicle.getDailyRate().money().currency().getCurrencyCode()`
- **AND** `status` SHALL be mapped from `vehicle.getStatus().name()`

#### Scenario: JPA entity to domain mapping

- **WHEN** `toDomainEntity(VehicleJpaEntity)` is called
- **THEN** it SHALL return a domain Vehicle using `Vehicle.reconstruct()`
- **AND** it SHALL convert `id` to `VehicleId`, `licensePlate` to `LicensePlate`
- **AND** it SHALL convert `category` string to `VehicleCategory` enum
- **AND** it SHALL convert `dailyRateAmount` and `dailyRateCurrency` to `DailyRate(new Money(...))`
- **AND** it SHALL convert `status` string to `VehicleStatus` enum
- **AND** it SHALL pass `description` as-is (may be null)

### Requirement: Flyway migration creates vehicles table

A Flyway migration `V1__create_vehicles_table.sql` SHALL create the `vehicles` table with the correct schema.

#### Scenario: Table schema matches JPA entity

- **WHEN** the migration is executed
- **THEN** the `vehicles` table SHALL be created with columns: `id` (UUID, primary key), `license_plate` (VARCHAR, not null, unique), `make` (VARCHAR, not null), `model` (VARCHAR, not null), `year` (INTEGER, not null), `category` (VARCHAR(50), not null), `daily_rate_amount` (NUMERIC(10,2), not null), `daily_rate_currency` (VARCHAR(3), not null), `description` (VARCHAR(500), nullable), `status` (VARCHAR(50), not null), `created_at` (TIMESTAMP WITH TIME ZONE, not null)

Constraint: Infrastructure layer only
--------------------------------------

All persistence classes SHALL live in `com.vehiclerental.fleet.infrastructure.adapter.output.persistence` or its sub-packages.
