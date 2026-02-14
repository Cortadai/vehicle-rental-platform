multi-module-build
==================

MODIFIED Requirements
---------------------

### Requirement: All 13 modules declared

The root POM SHALL declare all platform modules in a `<modules>` section: `common`, and for each service (reservation, customer, payment, fleet) its submodules. Customer service includes a `-application` module following docs/17 four-module pattern.

#### Scenario: Module list includes customer-application

- **WHEN** the root `pom.xml` is parsed
- **THEN** it SHALL declare `customer-service/customer-application` as a module
- **AND** it SHALL appear after `customer-service/customer-domain` and before `customer-service/customer-infrastructure`

#### Scenario: customer-application module directory exists

- **WHEN** the project directory structure is inspected
- **THEN** `customer-service/customer-application/` SHALL exist with a valid `pom.xml`
- **AND** the POM SHALL inherit from the root parent POM
- **AND** the POM SHALL depend on `customer-domain` module
- **AND** the POM SHALL depend on `spring-tx`
