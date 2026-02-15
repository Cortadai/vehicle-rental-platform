fleet-container-assembly
========================

Purpose
-------

Spring Boot assembly module for Fleet Service. Contains the main application class, manual bean registration for domain and application layers, and configuration files. This is the only module with `spring-boot-maven-plugin` (executable JAR).

## ADDED Requirements

### Requirement: FleetServiceApplication is the Spring Boot entry point

FleetServiceApplication SHALL be annotated with `@SpringBootApplication` and SHALL have a standard `main()` method.

#### Scenario: Application starts successfully

- **WHEN** `FleetServiceApplication.main()` is invoked with a configured PostgreSQL database
- **THEN** the Spring Boot application context SHALL start without errors

### Requirement: BeanConfiguration registers domain and application beans

BeanConfiguration SHALL be a `@Configuration` class that manually creates beans for classes that have no Spring annotations (domain and application layer classes).

#### Scenario: FleetApplicationMapper is registered as a bean

- **WHEN** the application context is loaded
- **THEN** a `FleetApplicationMapper` bean SHALL be available

#### Scenario: FleetApplicationService is registered as a bean

- **WHEN** the application context is loaded
- **THEN** a `FleetApplicationService` bean SHALL be available
- **AND** it SHALL be injected with `VehicleRepository`, `FleetDomainEventPublisher`, and `FleetApplicationMapper`

#### Scenario: Input ports are exposed as beans

- **WHEN** the application context is loaded
- **THEN** beans for `RegisterVehicleUseCase`, `GetVehicleUseCase`, `SendToMaintenanceUseCase`, `ActivateVehicleUseCase`, and `RetireVehicleUseCase` SHALL be available
- **AND** they SHALL all resolve to the same `FleetApplicationService` instance

### Requirement: application.yml configures datasource and JPA

The base `application.yml` SHALL configure Spring datasource, JPA, and Flyway for PostgreSQL.

#### Scenario: Datasource uses environment variables with defaults

- **WHEN** application.yml is loaded without environment variables
- **THEN** the datasource URL SHALL default to `jdbc:postgresql://localhost:5432/fleet_db`
- **AND** the username SHALL default to `postgres`
- **AND** the password SHALL default to `postgres`

#### Scenario: JPA is configured for validation mode

- **WHEN** application.yml is loaded
- **THEN** `spring.jpa.hibernate.ddl-auto` SHALL be `validate`
- **AND** `spring.jpa.open-in-view` SHALL be `false`

#### Scenario: Flyway is enabled

- **WHEN** application.yml is loaded
- **THEN** Flyway SHALL be enabled with default migration location (`classpath:db/migration`)

#### Scenario: Server port is 8182

- **WHEN** application.yml is loaded without environment variables
- **THEN** `server.port` SHALL default to `8182`

### Requirement: Test configuration uses Testcontainers

An `application-test.yml` or test configuration SHALL configure the application for integration tests using Testcontainers PostgreSQL.

#### Scenario: Test profile overrides datasource

- **WHEN** the test profile is active
- **THEN** the datasource SHALL be configured to use a Testcontainers PostgreSQL instance

### Requirement: Container module is the only executable module

The fleet-container POM SHALL include `spring-boot-maven-plugin`. No other fleet-service module SHALL include this plugin.

#### Scenario: Container POM has spring-boot-maven-plugin

- **WHEN** `fleet-service/fleet-container/pom.xml` is inspected
- **THEN** it SHALL declare `spring-boot-maven-plugin` in `<build><plugins>`

#### Scenario: Infrastructure POM does not have spring-boot-maven-plugin

- **WHEN** `fleet-service/fleet-infrastructure/pom.xml` is inspected
- **THEN** it SHALL NOT declare `spring-boot-maven-plugin`

Constraint: Container is the assembly layer
--------------------------------------------

BeanConfiguration SHALL live in `com.vehiclerental.fleet.config`. FleetServiceApplication SHALL live in `com.vehiclerental.fleet`. No business logic SHALL exist in the container module.
