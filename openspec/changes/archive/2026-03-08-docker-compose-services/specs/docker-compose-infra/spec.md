# docker-compose-infra Specification (Delta)

## MODIFIED Requirements

### Requirement: Docker Compose uses profile for infrastructure

Infrastructure containers SHALL NOT use Docker Compose profiles — they start with a plain `docker compose up -d` alongside the microservices.

#### Scenario: No profile required

- **WHEN** `docker compose up -d` is executed without any flags
- **THEN** both PostgreSQL and RabbitMQ containers SHALL start

#### Scenario: Infrastructure-only startup

- **WHEN** a developer wants only infrastructure without microservices
- **THEN** they SHALL run `docker compose up postgres rabbitmq -d`
