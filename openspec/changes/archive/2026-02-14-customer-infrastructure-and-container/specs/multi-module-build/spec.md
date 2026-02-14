multi-module-build
==================

## MODIFIED Requirements

### Requirement: All modules declared

The root POM SHALL declare all platform modules in a `<modules>` section: `common`, and for each service (reservation, customer, payment, fleet) its submodules. Customer service includes `-application`, `-infrastructure`, and `-container` modules following docs/17 four-module pattern.

#### Scenario: Module list includes customer-infrastructure and customer-container

- **WHEN** the root `pom.xml` is parsed
- **THEN** it SHALL declare `customer-service/customer-infrastructure` as a module
- **AND** it SHALL declare `customer-service/customer-container` as a module
- **AND** `customer-service/customer-infrastructure` SHALL appear after `customer-service/customer-application` and before `customer-service/customer-container`

#### Scenario: customer-infrastructure module directory exists

- **WHEN** the project directory structure is inspected
- **THEN** `customer-service/customer-infrastructure/` SHALL exist with a valid `pom.xml`
- **AND** the POM SHALL inherit from the root parent POM
- **AND** the POM SHALL depend on `customer-application` module

#### Scenario: customer-container module directory exists

- **WHEN** the project directory structure is inspected
- **THEN** `customer-service/customer-container/` SHALL exist with a valid `pom.xml`
- **AND** the POM SHALL inherit from the root parent POM
- **AND** the POM SHALL depend on `customer-infrastructure` module

#### Scenario: dependencyManagement includes new modules

- **WHEN** the root POM `<dependencyManagement>` is inspected
- **THEN** it SHALL declare `customer-infrastructure` with `${vehicle-rental.version}`
- **AND** it SHALL declare `customer-container` with `${vehicle-rental.version}`
