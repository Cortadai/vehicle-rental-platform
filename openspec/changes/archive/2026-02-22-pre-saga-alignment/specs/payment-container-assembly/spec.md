## MODIFIED Requirements

### Requirement: application.yml configures datasource, JPA, Flyway, and RabbitMQ

The base `application.yml` SHALL configure Spring datasource, JPA, Flyway for PostgreSQL, and RabbitMQ connection properties.

#### Scenario: Flyway is enabled without default schema

- **WHEN** application.yml is loaded
- **THEN** Flyway SHALL be enabled (`spring.flyway.enabled: true`)
- **AND** `spring.flyway.default-schema` SHALL NOT be defined
- **AND** Flyway SHALL use the default `public` schema, consistent with Customer, Fleet, and Reservation services
