multi-module-build
==================

## MODIFIED Requirements

### Requirement: All modules declared

The root POM SHALL declare all platform modules in a `<modules>` section. Fleet service includes a `-domain` module as the first submodule.

#### Scenario: Module list includes fleet-domain

- **WHEN** the root `pom.xml` is parsed
- **THEN** it SHALL declare `fleet-service/fleet-domain` as a module

#### Scenario: fleet-domain module directory exists

- **WHEN** the project directory structure is inspected
- **THEN** `fleet-service/fleet-domain/` SHALL exist with a valid `pom.xml`
- **AND** the POM SHALL inherit from the root parent POM
- **AND** the POM SHALL depend on `common` module
- **AND** the POM SHALL have zero Spring dependencies in compile scope

#### Scenario: dependencyManagement includes fleet-domain

- **WHEN** the root POM `<dependencyManagement>` is inspected
- **THEN** it SHALL declare `fleet-domain` with `${vehicle-rental.version}`
