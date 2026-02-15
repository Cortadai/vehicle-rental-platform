multi-module-build
==================

## MODIFIED Requirements

### Requirement: All modules declared

The root POM SHALL declare all platform modules in a `<modules>` section. Fleet service includes `-domain` and `-application` modules following docs/17 four-module pattern.

#### Scenario: Module list includes fleet-application

- **WHEN** the root `pom.xml` is parsed
- **THEN** it SHALL declare `fleet-service/fleet-application` as a module

#### Scenario: fleet-application module directory exists

- **WHEN** the project directory structure is inspected
- **THEN** `fleet-service/fleet-application/` SHALL exist with a valid `pom.xml`
- **AND** the POM SHALL inherit from the root parent POM
- **AND** the POM SHALL depend on `fleet-domain` module
- **AND** the POM SHALL depend on `spring-tx`

#### Scenario: Module order for fleet-application

- **WHEN** the root `pom.xml` is parsed
- **THEN** `fleet-service/fleet-application` SHALL appear after `fleet-service/fleet-domain` and before `fleet-service/fleet-infrastructure`

#### Scenario: dependencyManagement includes fleet-application

- **WHEN** the root POM `<dependencyManagement>` is inspected
- **THEN** it SHALL declare `fleet-application` with `${vehicle-rental.version}`
