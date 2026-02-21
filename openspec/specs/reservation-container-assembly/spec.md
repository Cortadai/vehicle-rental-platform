# reservation-container-assembly Specification

## Purpose

Spring Boot assembly module for Reservation Service. Contains the main application class, manual bean registration for domain and application layers, configuration files (application.yml with reservation_db datasource and port 8183), and integration tests with Testcontainers PostgreSQL. This is the only module with `spring-boot-maven-plugin` (executable JAR).

## Requirements

### Requirement: ReservationServiceApplication is the Spring Boot entry point

ReservationServiceApplication SHALL be annotated with `@SpringBootApplication` and SHALL have a standard `main()` method.

#### Scenario: Application starts successfully

- **WHEN** `ReservationServiceApplication.main()` is invoked with a configured PostgreSQL database
- **THEN** the Spring Boot application context SHALL start without errors

### Requirement: BeanConfiguration registers domain and application beans

BeanConfiguration SHALL be a `@Configuration` class that manually creates beans for classes that have no Spring annotations (domain and application layer classes).

#### Scenario: ReservationPersistenceMapper is registered as a bean

- **WHEN** the application context is loaded
- **THEN** a `ReservationPersistenceMapper` bean SHALL be available

#### Scenario: ReservationApplicationMapper is registered as a bean

- **WHEN** the application context is loaded
- **THEN** a `ReservationApplicationMapper` bean SHALL be available

#### Scenario: ReservationApplicationService is registered as a bean

- **WHEN** the application context is loaded
- **THEN** a `ReservationApplicationService` bean SHALL be available
- **AND** it SHALL be injected with `ReservationRepository`, `ReservationDomainEventPublisher`, and `ReservationApplicationMapper`

#### Scenario: Input ports are exposed as beans

- **WHEN** the application context is loaded
- **THEN** beans for `CreateReservationUseCase` and `TrackReservationUseCase` SHALL be available
- **AND** they SHALL all resolve to the same `ReservationApplicationService` instance

### Requirement: application.yml configures datasource and JPA

The base `application.yml` SHALL configure Spring datasource, JPA, and Flyway for PostgreSQL with reservation_db database on port 8183.

#### Scenario: Datasource uses environment variables with defaults

- **WHEN** application.yml is loaded without environment variables
- **THEN** the datasource URL SHALL default to `jdbc:postgresql://localhost:5432/reservation_db`
- **AND** the username SHALL default to `postgres`
- **AND** the password SHALL default to `postgres`

#### Scenario: Server port is 8183

- **WHEN** application.yml is loaded
- **THEN** `server.port` SHALL be `8183`

#### Scenario: JPA is configured for validation mode

- **WHEN** application.yml is loaded
- **THEN** `spring.jpa.hibernate.ddl-auto` SHALL be `validate`
- **AND** `spring.jpa.open-in-view` SHALL be `false`

#### Scenario: Flyway is enabled

- **WHEN** application.yml is loaded
- **THEN** Flyway SHALL be enabled with default migration location (`classpath:db/migration`)

### Requirement: Test configuration uses Testcontainers

An `application-test.yml` or test configuration SHALL configure the application for integration tests using Testcontainers PostgreSQL.

#### Scenario: Test profile overrides datasource

- **WHEN** the test profile is active
- **THEN** the datasource SHALL be configured to use a Testcontainers PostgreSQL instance

### Requirement: Container module is the only executable module

The reservation-container POM SHALL include `spring-boot-maven-plugin`. No other reservation-service module SHALL include this plugin.

#### Scenario: Container POM has spring-boot-maven-plugin

- **WHEN** `reservation-service/reservation-container/pom.xml` is inspected
- **THEN** it SHALL declare `spring-boot-maven-plugin` in `<build><plugins>`

#### Scenario: Infrastructure POM does not have spring-boot-maven-plugin

- **WHEN** `reservation-service/reservation-infrastructure/pom.xml` is inspected
- **THEN** it SHALL NOT declare `spring-boot-maven-plugin`

### Requirement: Integration tests verify persistence and REST endpoints

Integration tests SHALL verify the full stack: context loading, repository round-trip with parent-child persistence, and controller endpoints.

#### Scenario: Context loads smoke test

- **WHEN** the Spring Boot application context is loaded with Testcontainers PostgreSQL
- **THEN** it SHALL start without errors

#### Scenario: Repository adapter round-trip with items

- **WHEN** a Reservation with ReservationItems is saved via the adapter
- **THEN** `findById` SHALL return the reservation with all items intact
- **AND** `findByTrackingId` SHALL return the same reservation
- **AND** all value objects (Money, DateRange, PickupLocation, typed IDs) SHALL be correctly reconstructed

#### Scenario: Create reservation endpoint integration

- **WHEN** `POST /api/v1/reservations` is called with a valid request body containing items
- **THEN** the response SHALL have HTTP status 201
- **AND** the response body SHALL contain a trackingId and status "PENDING"

#### Scenario: Track reservation endpoint integration

- **WHEN** a reservation is created and then tracked via `GET /api/v1/reservations/{trackingId}`
- **THEN** the response SHALL have HTTP status 200
- **AND** the response body SHALL contain the full reservation snapshot including items

### Requirement: Inner modules compile independently after change

Adding infrastructure and container modules SHALL NOT break the independent compilation of reservation-domain and reservation-application. The full platform build (`mvn clean install` from root) SHALL pass green.

#### Scenario: reservation-domain compiles independently

- **WHEN** `mvn compile` is executed on reservation-domain in isolation
- **THEN** the build SHALL succeed with zero Spring dependencies in compile scope

#### Scenario: reservation-application compiles independently

- **WHEN** `mvn compile` is executed on reservation-application in isolation
- **THEN** the build SHALL succeed with only `spring-tx` as Spring dependency

#### Scenario: Full platform build passes

- **WHEN** `mvn clean install` is executed from the root POM
- **THEN** all modules (including reservation-infrastructure and reservation-container) SHALL build successfully

## Constraint: Container is the assembly layer

BeanConfiguration SHALL live in `com.vehiclerental.reservation.config`. ReservationServiceApplication SHALL live in `com.vehiclerental.reservation`. No business logic SHALL exist in the container module.
