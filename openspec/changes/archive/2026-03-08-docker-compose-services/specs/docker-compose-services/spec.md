# docker-compose-services Specification

## Purpose
Defines the 4 microservice containers in Docker Compose — Paketo images, environment configuration, healthchecks, and startup ordering.

## ADDED Requirements

### Requirement: Four microservices defined in Docker Compose
The `docker-compose.yml` SHALL define service blocks for customer-service, fleet-service, reservation-service, and payment-service.

#### Scenario: Service images use Paketo-built OCI images
- **WHEN** the `docker-compose.yml` is inspected
- **THEN** each service SHALL reference a Paketo-built image with naming convention `vehicle-rental/<service-name>:latest`
- **AND** the 4 images SHALL be: `vehicle-rental/customer-service:latest`, `vehicle-rental/fleet-service:latest`, `vehicle-rental/reservation-service:latest`, `vehicle-rental/payment-service:latest`

#### Scenario: Each service exposes its configured port
- **WHEN** the services are running
- **THEN** customer-service SHALL be accessible on port 8181
- **AND** fleet-service SHALL be accessible on port 8182
- **AND** reservation-service SHALL be accessible on port 8183
- **AND** payment-service SHALL be accessible on port 8184

#### Scenario: Services have no Docker Compose profile
- **WHEN** `docker compose up -d` is executed without any profile flags
- **THEN** all 4 microservices SHALL start

### Requirement: Services connect to shared PostgreSQL with dedicated users
Each microservice SHALL connect to `vehicle_rental_db` using its dedicated schema user, configured via environment variables.

#### Scenario: Database environment variables
- **WHEN** the compose environment for each service is inspected
- **THEN** each service SHALL set `DB_HOST=postgres`, `DB_PORT=5432`, `DB_NAME=vehicle_rental_db`
- **AND** customer-service SHALL set `DB_USERNAME=customer_user` and `DB_PASSWORD=customer_pass`
- **AND** fleet-service SHALL set `DB_USERNAME=fleet_user` and `DB_PASSWORD=fleet_pass`
- **AND** reservation-service SHALL set `DB_USERNAME=reservation_user` and `DB_PASSWORD=reservation_pass`
- **AND** payment-service SHALL set `DB_USERNAME=payment_user` and `DB_PASSWORD=payment_pass`

### Requirement: Services connect to RabbitMQ
Each microservice SHALL connect to the RabbitMQ container via environment variables.

#### Scenario: RabbitMQ environment variables
- **WHEN** the compose environment for each service is inspected
- **THEN** each service SHALL set `RABBITMQ_HOST=rabbitmq`, `RABBITMQ_PORT=5672`, `RABBITMQ_USERNAME=guest`, `RABBITMQ_PASSWORD=guest`

### Requirement: Services wait for healthy infrastructure
Each microservice SHALL wait for PostgreSQL and RabbitMQ to be healthy before starting.

#### Scenario: Startup ordering with health conditions
- **WHEN** the compose `depends_on` for each service is inspected
- **THEN** each service SHALL declare `depends_on` on `postgres` and `rabbitmq` with `condition: service_healthy`

### Requirement: Services health verified externally
Paketo images are minimal (no curl, wget, or bash) so Docker Compose healthchecks cannot run inside the container. Health is verified externally.

#### Scenario: External health verification
- **WHEN** the services are running
- **THEN** each service SHALL respond to `GET /actuator/health` from the host with `{"status":"UP"}`
- **AND** Docker Compose SHALL NOT declare healthcheck blocks for microservices (only infra has healthchecks)

### Requirement: Paketo image build via Maven
The 4 container modules SHALL support building OCI images with `mvn spring-boot:build-image`.

#### Scenario: Image name configured in container POMs
- **WHEN** `mvn spring-boot:build-image` is executed for a container module
- **THEN** it SHALL produce an image named `vehicle-rental/<service-name>:latest`
- **AND** this SHALL be configured via `<image><name>` in the `spring-boot-maven-plugin` configuration
