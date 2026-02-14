customer-jpa-persistence
========================

Purpose
-------

JPA persistence layer for Customer Service. Includes a JPA entity separate from the domain entity, a Spring Data repository, a persistence adapter implementing the domain output port, a bidirectional mapper, and the Flyway migration for the customers table.

## ADDED Requirements

### Requirement: CustomerJpaEntity is separate from domain Customer

CustomerJpaEntity SHALL be a JPA entity class (`@Entity`, `@Table`) in the infrastructure layer. It SHALL NOT extend or reference the domain `Customer` class. It SHALL have a protected no-arg constructor, getters, setters, and JPA annotations.

#### Scenario: JPA entity has required JPA annotations

- **WHEN** CustomerJpaEntity is inspected
- **THEN** it SHALL be annotated with `@Entity` and `@Table(name = "customers")`
- **AND** it SHALL have a field `id` of type `UUID` annotated with `@Id`

#### Scenario: JPA entity has all customer fields

- **WHEN** CustomerJpaEntity is inspected
- **THEN** it SHALL have fields: `id` (UUID), `firstName` (String), `lastName` (String), `email` (String), `phone` (String, nullable), `status` (String), `createdAt` (Instant)

#### Scenario: JPA entity has no domain imports

- **WHEN** CustomerJpaEntity imports are inspected
- **THEN** it SHALL NOT import any type from `com.vehiclerental.customer.domain.*`

### Requirement: CustomerJpaRepository is a Spring Data interface

CustomerJpaRepository SHALL extend `JpaRepository<CustomerJpaEntity, UUID>`. It SHALL have no custom query methods beyond what JPA provides.

#### Scenario: Repository extends JpaRepository

- **WHEN** CustomerJpaRepository is inspected
- **THEN** it SHALL extend `JpaRepository<CustomerJpaEntity, UUID>`

### Requirement: CustomerRepositoryAdapter implements domain output port

CustomerRepositoryAdapter SHALL implement `CustomerRepository` from the domain layer. It SHALL use `CustomerJpaRepository` for persistence and `CustomerPersistenceMapper` for conversion.

#### Scenario: Save converts domain to JPA and persists

- **WHEN** `save(Customer)` is called on the adapter
- **THEN** it SHALL convert the domain Customer to CustomerJpaEntity via the mapper
- **AND** it SHALL call `CustomerJpaRepository.save()` with the JPA entity
- **AND** it SHALL convert the saved JPA entity back to a domain Customer via the mapper
- **AND** it SHALL return the domain Customer

#### Scenario: FindById converts JPA to domain

- **WHEN** `findById(CustomerId)` is called with an existing customer ID
- **THEN** it SHALL call `CustomerJpaRepository.findById()` with the UUID value
- **AND** it SHALL convert the JPA entity to a domain Customer via the mapper
- **AND** it SHALL return `Optional.of(customer)`

#### Scenario: FindById returns empty for non-existing customer

- **WHEN** `findById(CustomerId)` is called with a non-existing customer ID
- **THEN** it SHALL return `Optional.empty()`

#### Scenario: Adapter is a Spring component

- **WHEN** CustomerRepositoryAdapter is inspected
- **THEN** it SHALL be annotated with `@Component`

### Requirement: CustomerPersistenceMapper converts bidirectionally

CustomerPersistenceMapper SHALL convert between domain `Customer` and `CustomerJpaEntity` in both directions. It SHALL be a plain Java class with no Spring annotations.

#### Scenario: Domain to JPA entity mapping

- **WHEN** `toJpaEntity(Customer)` is called
- **THEN** it SHALL return a CustomerJpaEntity with all fields mapped from the domain Customer
- **AND** `id` SHALL be mapped from `customer.getId().value()`
- **AND** `status` SHALL be mapped from `customer.getStatus().name()`

#### Scenario: JPA entity to domain mapping

- **WHEN** `toDomainEntity(CustomerJpaEntity)` is called
- **THEN** it SHALL return a domain Customer using `Customer.reconstruct()`
- **AND** it SHALL convert `id` to `CustomerId`, `email` to `Email`, and `phone` to `PhoneNumber` (or null)
- **AND** it SHALL convert `status` string to `CustomerStatus` enum

### Requirement: Flyway migration creates customers table

A Flyway migration `V1__create_customer_table.sql` SHALL create the `customers` table with the correct schema.

#### Scenario: Table schema matches JPA entity

- **WHEN** the migration is executed
- **THEN** the `customers` table SHALL be created with columns: `id` (UUID, primary key), `first_name` (VARCHAR, not null), `last_name` (VARCHAR, not null), `email` (VARCHAR, not null, unique), `phone` (VARCHAR, nullable), `status` (VARCHAR, not null), `created_at` (TIMESTAMP WITH TIME ZONE, not null)

Constraint: Infrastructure layer only
--------------------------------------

All persistence classes SHALL live in `com.vehiclerental.customer.infrastructure.adapter.output.persistence` or its sub-packages.
