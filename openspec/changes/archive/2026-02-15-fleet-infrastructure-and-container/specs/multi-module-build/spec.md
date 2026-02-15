multi-module-build
==================

## MODIFIED Requirements

### Requirement: All modules declared

The root POM SHALL declare all platform modules in a `<modules>` section: `common`, and for each service (reservation, customer, payment, fleet) its submodules. Customer and Fleet services include `-application`, `-infrastructure`, and `-container` modules following docs/17 four-module pattern.

#### Scenario: dependencyManagement includes fleet-infrastructure and fleet-container

- **WHEN** the root POM `<dependencyManagement>` is inspected
- **THEN** it SHALL declare `fleet-infrastructure` with `${vehicle-rental.version}`
- **AND** it SHALL declare `fleet-container` with `${vehicle-rental.version}`

#### Scenario: fleet-infrastructure module directory exists

- **WHEN** the project directory structure is inspected
- **THEN** `fleet-service/fleet-infrastructure/` SHALL exist with a valid `pom.xml`
- **AND** the POM SHALL inherit from the root parent POM
- **AND** the POM SHALL depend on `fleet-application` module

#### Scenario: fleet-container module directory exists

- **WHEN** the project directory structure is inspected
- **THEN** `fleet-service/fleet-container/` SHALL exist with a valid `pom.xml`
- **AND** the POM SHALL inherit from the root parent POM
- **AND** the POM SHALL depend on `fleet-infrastructure` module
