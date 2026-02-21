# docker-compose-infra Specification

## Purpose

Local development infrastructure via Docker Compose — PostgreSQL (single instance, schema-per-service) and RabbitMQ (with management plugin and pre-loaded topology).

## ADDED Requirements

### Requirement: Docker Compose provides PostgreSQL container

A `docker-compose.yml` at the project root SHALL define a PostgreSQL 16 container for local development.

#### Scenario: PostgreSQL container configuration

- **WHEN** `docker compose --profile infra up -d` is executed
- **THEN** a container named `vehicle-rental-postgres` SHALL start with PostgreSQL 16, exposed on port `5432`, with a named volume for data persistence and a health check using `pg_isready`

#### Scenario: Four schemas created on init

- **WHEN** the PostgreSQL container starts for the first time
- **THEN** the init script `docker/postgres/init-schemas.sql` SHALL create four schemas: `reservation`, `customer`, `payment`, `fleet`

#### Scenario: Dedicated users per schema

- **WHEN** the init script runs
- **THEN** it SHALL create four database users (e.g., `reservation_user`, `customer_user`, `payment_user`, `fleet_user`), each with `search_path` set to their respective schema and permissions restricted to that schema only

### Requirement: Docker Compose provides RabbitMQ container

The `docker-compose.yml` SHALL define a RabbitMQ 3.13 container with the management plugin.

#### Scenario: RabbitMQ container configuration

- **WHEN** `docker compose --profile infra up -d` is executed
- **THEN** a container named `vehicle-rental-rabbitmq` SHALL start with RabbitMQ 3.13 management image, exposed on ports `5672` (AMQP) and `15672` (Management UI), with a health check using `rabbitmq-diagnostics -q ping`

#### Scenario: Management UI accessible

- **WHEN** the RabbitMQ container is healthy
- **THEN** the Management UI SHALL be accessible at `http://localhost:15672` with credentials `guest/guest`

#### Scenario: Definitions loaded at startup

- **WHEN** the RabbitMQ container starts
- **THEN** `docker/rabbitmq/definitions.json` SHALL be mounted and loaded via `docker/rabbitmq/rabbitmq.conf`, creating all exchanges, queues, and bindings

### Requirement: Docker Compose uses profile for infrastructure

Infrastructure containers SHALL be gated behind a Docker Compose profile to prevent accidental startup.

#### Scenario: Profile name

- **WHEN** `docker compose up -d` is executed without `--profile`
- **THEN** no infrastructure containers SHALL start

#### Scenario: Profile activation

- **WHEN** `docker compose --profile infra up -d` is executed
- **THEN** both PostgreSQL and RabbitMQ containers SHALL start

### Requirement: Init scripts for PostgreSQL schema setup

The `docker/postgres/init-schemas.sql` script SHALL create the multi-schema structure.

#### Scenario: Idempotent schema creation

- **WHEN** `init-schemas.sql` runs
- **THEN** it SHALL use idempotent SQL constructs (e.g., `CREATE SCHEMA IF NOT EXISTS`, PL/pgSQL `DO` blocks with role existence checks) to be safe for repeated container restarts with persisted volumes

### Requirement: Makefile provides helper targets

A `Makefile` at the project root SHALL provide convenience targets for Docker Compose operations.

#### Scenario: Available targets

- **WHEN** `make` is invoked
- **THEN** the following targets SHALL be available: `infra-up` (starts infrastructure), `infra-down` (stops infrastructure), `infra-reset` (removes volumes and restarts), `infra-status` (shows container status), `infra-logs` (tails container logs)

### Requirement: Test configuration for Testcontainers RabbitMQ

Integration tests SHALL use Testcontainers for RabbitMQ, consistent with the existing PostgreSQL test pattern. PostgreSQL already uses the `jdbc:tc:` URL scheme in `application-test.yml`. For RabbitMQ, since there is no equivalent JDBC URL scheme, tests SHALL use `@ServiceConnection` with `RabbitMQContainer` to auto-configure connection properties.

#### Scenario: Testcontainers RabbitMQ available in tests

- **WHEN** integration tests that require RabbitMQ are executed
- **THEN** a `RabbitMQContainer` SHALL be declared with `@Container` and `@ServiceConnection` annotations, which auto-configures `spring.rabbitmq.host`, `spring.rabbitmq.port`, `spring.rabbitmq.username`, and `spring.rabbitmq.password` without manual properties in `application-test.yml`

#### Scenario: Testcontainers RabbitMQ dependency added to POM

- **WHEN** the reservation-container POM is inspected
- **THEN** it SHALL include `org.testcontainers:rabbitmq` as a test-scoped dependency

## Constraint: Project root location

`docker-compose.yml`, `Makefile`, and the `docker/` directory SHALL live at the project root (`vehicle-rental-platform/`).
