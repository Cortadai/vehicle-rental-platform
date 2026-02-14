# customer-repository-port

## Purpose

Output port interface defining the persistence contract for Customer aggregate. Pure Java interface with no framework dependencies — the implementation lives in the infrastructure layer.

## ADDED Requirements

### Requirement: CustomerRepository defines persistence contract

CustomerRepository SHALL be a Java interface declaring methods for persisting and retrieving Customer aggregates.

#### Scenario: Save method declared

- **WHEN** the CustomerRepository interface is inspected
- **THEN** it SHALL declare a `save(Customer customer)` method that returns Customer

#### Scenario: FindById method declared

- **WHEN** the CustomerRepository interface is inspected
- **THEN** it SHALL declare a `findById(CustomerId id)` method that returns `Optional<Customer>`

### Requirement: CustomerRepository uses domain types only

CustomerRepository SHALL use only domain types (Customer, CustomerId) and standard Java types (Optional). It SHALL NOT reference any framework-specific types (JPA, Spring Data, etc.).

#### Scenario: No framework types in signature

- **WHEN** the CustomerRepository interface is inspected
- **THEN** it SHALL NOT import any type from `org.springframework.*` or `jakarta.persistence.*`

## Constraint: Zero Spring dependencies

No class in `com.vehiclerental.customer.domain.port.output` SHALL import any type from `org.springframework.*`.
