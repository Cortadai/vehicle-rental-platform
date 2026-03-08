# actuator-health Specification

## Purpose
Minimal Spring Boot Actuator configuration for Docker Compose healthchecks — only the health endpoint exposed.

## ADDED Requirements

### Requirement: Actuator dependency in container modules
Each service container module SHALL include `spring-boot-starter-actuator` as a dependency.

#### Scenario: Actuator dependency present
- **WHEN** the POM of each `*-container` module is inspected
- **THEN** it SHALL declare a dependency on `org.springframework.boot:spring-boot-starter-actuator`

### Requirement: Minimal actuator configuration
Each service SHALL configure Actuator to expose only the health endpoint.

#### Scenario: Only health endpoint exposed
- **WHEN** the `application.yml` of each service is inspected
- **THEN** `management.endpoints.web.exposure.include` SHALL be set to `health`

#### Scenario: Health endpoint accessible
- **WHEN** a service is running
- **THEN** `GET /actuator/health` SHALL return HTTP 200 with status UP when the service is healthy
- **AND** no other actuator endpoints SHALL be accessible via HTTP
