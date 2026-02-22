payment-container-assembly
==========================

Purpose
-------

Spring Boot assembly module for Payment Service. Contains the main application class, manual bean registration for domain and application layers, and configuration files. This is the only payment module with `spring-boot-maven-plugin` (executable JAR).

## ADDED Requirements

### Requirement: PaymentServiceApplication is the Spring Boot entry point

PaymentServiceApplication SHALL be annotated with `@SpringBootApplication` and SHALL have a standard `main()` method.

#### Scenario: Application starts successfully

- **WHEN** `PaymentServiceApplication.main()` is invoked with a configured PostgreSQL database
- **THEN** the Spring Boot application context SHALL start without errors

### Requirement: BeanConfiguration registers domain and application beans

BeanConfiguration SHALL be a `@Configuration` class that manually creates beans for classes that have no Spring annotations (domain and application layer classes).

#### Scenario: PaymentApplicationMapper is registered as a bean

- **WHEN** the application context is loaded
- **THEN** a `PaymentApplicationMapper` bean SHALL be available

#### Scenario: PaymentApplicationService is registered as a bean

- **WHEN** the application context is loaded
- **THEN** a `PaymentApplicationService` bean SHALL be available
- **AND** it SHALL be injected with `PaymentRepository`, `PaymentDomainEventPublisher`, `PaymentGateway`, and `PaymentApplicationMapper`

#### Scenario: Input ports are exposed as beans

- **WHEN** the application context is loaded
- **THEN** beans for `ProcessPaymentUseCase`, `RefundPaymentUseCase`, and `GetPaymentUseCase` SHALL be available
- **AND** they SHALL all resolve to the same `PaymentApplicationService` instance

### Requirement: application.yml configures datasource, JPA, and Flyway

The base `application.yml` SHALL configure Spring datasource, JPA, and Flyway for PostgreSQL.

#### Scenario: Datasource uses environment variables with defaults

- **WHEN** application.yml is loaded without environment variables
- **THEN** the datasource URL SHALL default to `jdbc:postgresql://localhost:5432/payment_db`
- **AND** the username SHALL default to `postgres`
- **AND** the password SHALL default to `postgres`

#### Scenario: JPA is configured for validation mode

- **WHEN** application.yml is loaded
- **THEN** `spring.jpa.hibernate.ddl-auto` SHALL be `validate`
- **AND** `spring.jpa.open-in-view` SHALL be `false`

#### Scenario: Flyway is enabled with default schema

- **WHEN** application.yml is loaded
- **THEN** Flyway SHALL be enabled
- **AND** `spring.flyway.default-schema` SHALL be `payment`

#### Scenario: Server port is 8184

- **WHEN** application.yml is loaded without environment variables
- **THEN** `server.port` SHALL default to `8184`

### Requirement: Test configuration uses Testcontainers

An `application-test.yml` or test configuration SHALL configure the application for integration tests using Testcontainers PostgreSQL.

#### Scenario: Test profile overrides datasource

- **WHEN** the test profile is active
- **THEN** the datasource SHALL be configured to use a Testcontainers PostgreSQL instance

### Requirement: Container module is the only executable module

The payment-container POM SHALL include `spring-boot-maven-plugin`. No other payment-service module SHALL include this plugin.

#### Scenario: Container POM has spring-boot-maven-plugin

- **WHEN** `payment-service/payment-container/pom.xml` is inspected
- **THEN** it SHALL declare `spring-boot-maven-plugin` in `<build><plugins>`

#### Scenario: Infrastructure POM does not have spring-boot-maven-plugin

- **WHEN** `payment-service/payment-infrastructure/pom.xml` is inspected
- **THEN** it SHALL NOT declare `spring-boot-maven-plugin`

Constraint: Container is the assembly layer
--------------------------------------------

BeanConfiguration SHALL live in `com.vehiclerental.payment.config`. PaymentServiceApplication SHALL live in `com.vehiclerental.payment`. No business logic SHALL exist in the container module.
