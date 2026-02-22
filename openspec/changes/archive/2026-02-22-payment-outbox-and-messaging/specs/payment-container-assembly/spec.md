## MODIFIED Requirements

### Requirement: PaymentServiceApplication is the Spring Boot entry point

PaymentServiceApplication SHALL be annotated with `@SpringBootApplication(scanBasePackages = "com.vehiclerental")`, `@EntityScan(basePackages = "com.vehiclerental")`, and `@EnableJpaRepositories(basePackages = "com.vehiclerental")`. It SHALL have a standard `main()` method.

#### Scenario: Application starts successfully

- **WHEN** `PaymentServiceApplication.main()` is invoked with a configured PostgreSQL database and RabbitMQ
- **THEN** the Spring Boot application context SHALL start without errors

#### Scenario: Cross-module JPA scanning is configured

- **WHEN** PaymentServiceApplication is inspected
- **THEN** it SHALL have `@SpringBootApplication(scanBasePackages = "com.vehiclerental")`
- **AND** it SHALL have `@EntityScan(basePackages = "com.vehiclerental")`
- **AND** it SHALL have `@EnableJpaRepositories(basePackages = "com.vehiclerental")`

### Requirement: application.yml configures datasource, JPA, Flyway, and RabbitMQ

The base `application.yml` SHALL configure Spring datasource, JPA, Flyway for PostgreSQL, and RabbitMQ connection properties.

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

#### Scenario: RabbitMQ connection uses environment variables with defaults

- **WHEN** application.yml is loaded without environment variables
- **THEN** `spring.rabbitmq.host` SHALL default to `localhost`
- **AND** `spring.rabbitmq.port` SHALL default to `5672`
- **AND** `spring.rabbitmq.username` SHALL default to `guest`
- **AND** `spring.rabbitmq.password` SHALL default to `guest`

### Requirement: Test configuration uses Testcontainers

An `application-test.yml` or test configuration SHALL configure the application for integration tests using Testcontainers PostgreSQL. All `@SpringBootTest` integration tests SHALL declare a RabbitMQ Testcontainer.

#### Scenario: Test profile overrides datasource

- **WHEN** the test profile is active
- **THEN** the datasource SHALL be configured to use a Testcontainers PostgreSQL instance

#### Scenario: All integration tests declare RabbitMQ container

- **WHEN** any `@SpringBootTest` integration test class is inspected
- **THEN** it SHALL declare a `RabbitMQContainer` with `@Container` and `@ServiceConnection`

## ADDED Requirements

### Requirement: Flyway V2 migration creates outbox_events table

A Flyway migration `V2__create_outbox_events_table.sql` SHALL create the `outbox_events` table in the payment schema.

#### Scenario: Outbox table schema matches OutboxEvent entity

- **WHEN** the V2 migration is executed
- **THEN** the `outbox_events` table SHALL be created with columns: `id` (BIGSERIAL, primary key), `aggregate_type` (VARCHAR(50), not null), `aggregate_id` (VARCHAR(50), not null), `event_type` (VARCHAR(100), not null), `payload` (TEXT, not null), `routing_key` (VARCHAR(100), not null), `exchange` (VARCHAR(100), not null), `status` (VARCHAR(20), not null, default 'PENDING'), `retry_count` (INTEGER, not null, default 0), `created_at` (TIMESTAMPTZ, not null, default NOW()), `published_at` (TIMESTAMPTZ, nullable)

#### Scenario: Outbox table has composite index

- **WHEN** the V2 migration is executed
- **THEN** the `outbox_events` table SHALL have an index on `(status, created_at)`

### Requirement: Container POM includes messaging test dependencies

The payment-container POM SHALL include `testcontainers-rabbitmq` and `awaitility` as test dependencies.

#### Scenario: RabbitMQ Testcontainer dependency exists

- **WHEN** `payment-service/payment-container/pom.xml` is inspected
- **THEN** it SHALL declare `org.testcontainers:rabbitmq` with scope `test`

#### Scenario: Awaitility dependency exists

- **WHEN** `payment-service/payment-container/pom.xml` is inspected
- **THEN** it SHALL declare `org.awaitility:awaitility` with scope `test`

### Requirement: payment-infrastructure POM depends on common-messaging

The payment-infrastructure POM SHALL declare a dependency on `common-messaging`.

#### Scenario: common-messaging dependency exists

- **WHEN** `payment-service/payment-infrastructure/pom.xml` is inspected
- **THEN** it SHALL declare `com.vehiclerental:common-messaging` as a dependency
