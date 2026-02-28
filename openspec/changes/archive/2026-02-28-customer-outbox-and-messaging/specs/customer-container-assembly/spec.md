## MODIFIED Requirements

### Requirement: CustomerServiceApplication is the Spring Boot entry point

CustomerServiceApplication SHALL be annotated with `@SpringBootApplication(scanBasePackages = "com.vehiclerental")`, `@EntityScan(basePackages = "com.vehiclerental")`, and `@EnableJpaRepositories(basePackages = "com.vehiclerental")`. These annotations are required for Spring Boot to detect entities and repositories from `common-messaging` (specifically `OutboxEvent` and `OutboxEventRepository`).

#### Scenario: Application starts successfully

- **WHEN** `CustomerServiceApplication.main()` is invoked with a configured PostgreSQL database and RabbitMQ
- **THEN** the Spring Boot application context SHALL start without errors
- **AND** `OutboxEvent` entity and `OutboxEventRepository` from `common-messaging` SHALL be detected

#### Scenario: Cross-module JPA scanning is configured

- **WHEN** CustomerServiceApplication annotations are inspected
- **THEN** `scanBasePackages` SHALL include `com.vehiclerental`
- **AND** `@EntityScan` basePackages SHALL include `com.vehiclerental`
- **AND** `@EnableJpaRepositories` basePackages SHALL include `com.vehiclerental`

### Requirement: BeanConfiguration registers domain and application beans

BeanConfiguration SHALL be a `@Configuration` class that manually creates beans for classes that have no Spring annotations (domain and application layer classes). It SHALL register `ValidateCustomerForReservationUseCase` in addition to the 5 existing use case beans.

#### Scenario: CustomerApplicationMapper is registered as a bean

- **WHEN** the application context is loaded
- **THEN** a `CustomerApplicationMapper` bean SHALL be available

#### Scenario: CustomerApplicationService is registered as a bean

- **WHEN** the application context is loaded
- **THEN** a `CustomerApplicationService` bean SHALL be available
- **AND** it SHALL be injected with `CustomerRepository`, `CustomerDomainEventPublisher`, and `CustomerApplicationMapper`

#### Scenario: Input ports are exposed as beans

- **WHEN** the application context is loaded
- **THEN** beans for `CreateCustomerUseCase`, `GetCustomerUseCase`, `SuspendCustomerUseCase`, `ActivateCustomerUseCase`, `DeleteCustomerUseCase`, and `ValidateCustomerForReservationUseCase` SHALL be available
- **AND** they SHALL all resolve to the same `CustomerApplicationService` instance

### Requirement: application.yml configures datasource, JPA, and RabbitMQ

The base `application.yml` SHALL configure Spring datasource, JPA, Flyway for PostgreSQL, and RabbitMQ connection and retry settings.

#### Scenario: Datasource uses environment variables with defaults

- **WHEN** application.yml is loaded without environment variables
- **THEN** the datasource URL SHALL default to `jdbc:postgresql://localhost:5432/customer_db`
- **AND** the username SHALL default to `postgres`
- **AND** the password SHALL default to `postgres`

#### Scenario: JPA is configured for validation mode

- **WHEN** application.yml is loaded
- **THEN** `spring.jpa.hibernate.ddl-auto` SHALL be `validate`
- **AND** `spring.jpa.open-in-view` SHALL be `false`

#### Scenario: Flyway is enabled

- **WHEN** application.yml is loaded
- **THEN** Flyway SHALL be enabled with default migration location (`classpath:db/migration`)

#### Scenario: RabbitMQ connection is configured

- **WHEN** application.yml is loaded
- **THEN** `spring.rabbitmq.host` SHALL default to `localhost`
- **AND** `spring.rabbitmq.port` SHALL default to `5672`
- **AND** `spring.rabbitmq.username` SHALL default to `guest`
- **AND** `spring.rabbitmq.password` SHALL default to `guest`

#### Scenario: Spring AMQP retry is configured for listeners

- **WHEN** application.yml is loaded
- **THEN** `spring.rabbitmq.listener.simple.retry.enabled` SHALL be `true`
- **AND** `spring.rabbitmq.listener.simple.retry.initial-interval` SHALL be `1000`
- **AND** `spring.rabbitmq.listener.simple.retry.max-attempts` SHALL be `3`
- **AND** `spring.rabbitmq.listener.simple.retry.multiplier` SHALL be `2.0`

### Requirement: Test configuration uses Testcontainers

An `application-test.yml` or test configuration SHALL configure the application for integration tests using Testcontainers PostgreSQL.

#### Scenario: Test profile overrides datasource

- **WHEN** the test profile is active
- **THEN** the datasource SHALL be configured to use a Testcontainers PostgreSQL instance

### Requirement: All ITs declare RabbitMQ Testcontainer

All `@SpringBootTest` integration tests SHALL declare a `@Container @ServiceConnection static RabbitMQContainer` to provide RabbitMQ connectivity. This is required because `common-messaging` on the classpath causes `OutboxPublisher` to autowire `RabbitTemplate`.

#### Scenario: Existing ITs start with RabbitMQ container

- **WHEN** `CustomerServiceApplicationIT`, `CustomerControllerIT`, or `CustomerRepositoryAdapterIT` is executed
- **THEN** a RabbitMQ Testcontainer SHALL be started and connected via `@ServiceConnection`

### Requirement: Flyway V2 migration creates outbox_events table

A migration file `V2__create_outbox_events_table.sql` SHALL create the `outbox_events` table with columns: id (BIGSERIAL PK), aggregate_type, aggregate_id, event_type, payload (TEXT), routing_key, exchange, status (default PENDING), retry_count (default 0), created_at (TIMESTAMPTZ default NOW()), published_at (TIMESTAMPTZ nullable). An index `idx_outbox_events_status_created` SHALL be created on (status, created_at).

#### Scenario: Outbox table exists after migration

- **WHEN** Flyway runs V2 migration
- **THEN** the `outbox_events` table SHALL exist with all required columns and the status index

### Requirement: Container module test dependencies

The customer-container POM SHALL include test dependencies for `org.testcontainers:rabbitmq` and `org.awaitility:awaitility`.

#### Scenario: Test dependencies are declared

- **WHEN** `customer-service/customer-container/pom.xml` is inspected
- **THEN** it SHALL declare `org.testcontainers:rabbitmq` and `org.awaitility:awaitility` with scope `test`

### Requirement: OutboxPublisherIT verifies outbox publishing

OutboxPublisherIT SHALL verify that the `OutboxPublisher` scheduler picks up PENDING outbox events and publishes them to RabbitMQ, transitioning their status to PUBLISHED.

#### Scenario: PENDING event becomes PUBLISHED

- **WHEN** an OutboxEvent with status PENDING is saved to the database
- **AND** the OutboxPublisher scheduler runs
- **THEN** the event status SHALL transition to PUBLISHED within a reasonable timeout (using Awaitility)

### Requirement: OutboxAtomicityIT verifies transactional consistency

OutboxAtomicityIT SHALL verify that customer entity persistence and outbox event insertion occur atomically within the same transaction.

#### Scenario: Customer creation produces outbox event atomically

- **WHEN** a customer is created via the application service
- **THEN** both the customer entity and the corresponding outbox event SHALL be persisted
- **AND** if the transaction fails, neither SHALL be persisted

### Requirement: Container module is the only executable module

The customer-container POM SHALL include `spring-boot-maven-plugin`. No other customer-service module SHALL include this plugin.

#### Scenario: Container POM has spring-boot-maven-plugin

- **WHEN** `customer-service/customer-container/pom.xml` is inspected
- **THEN** it SHALL declare `spring-boot-maven-plugin` in `<build><plugins>`

#### Scenario: Infrastructure POM does not have spring-boot-maven-plugin

- **WHEN** `customer-service/customer-infrastructure/pom.xml` is inspected
- **THEN** it SHALL NOT declare `spring-boot-maven-plugin`
