# multi-module-build Specification (delta)

## MODIFIED Requirements

### Requirement: All modules declared

The root POM SHALL declare all platform modules in a `<modules>` section: `common`, `common-messaging`, and for each service (reservation, customer, payment, fleet) its submodules. Customer and Fleet services include `-domain`, `-application`, `-infrastructure`, and `-container` modules following docs/17 four-module pattern. Reservation Service includes the same four-module pattern. Payment Service includes `-domain`, `-infrastructure`, and `-container` modules (payment-application will be added in a future change).

#### Scenario: Module list includes payment-domain

* **WHEN** the root `pom.xml` is parsed
* **THEN** it SHALL declare `payment-service/payment-domain` as a module

#### Scenario: payment-domain module directory exists

* **WHEN** the project directory structure is inspected
* **THEN** `payment-service/payment-domain/` SHALL exist with a valid `pom.xml`
* **AND** the POM SHALL inherit from the root parent POM
* **AND** the POM SHALL depend on `common` module
* **AND** the POM SHALL have zero Spring dependencies in compile scope

#### Scenario: Module order for payment-domain

* **WHEN** the root `pom.xml` is parsed
* **THEN** `payment-service/payment-domain` SHALL appear after `common` and before `payment-service/payment-infrastructure`

#### Scenario: dependencyManagement includes payment-domain

* **WHEN** the root POM `<dependencyManagement>` is inspected
* **THEN** it SHALL declare `payment-domain` with `${vehicle-rental.version}`
