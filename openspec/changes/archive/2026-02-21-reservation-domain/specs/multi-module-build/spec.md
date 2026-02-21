## MODIFIED Requirements

### Requirement: All modules declared

The root POM SHALL declare all platform modules in a `<modules>` section: `common`, and for each service (reservation, customer, payment, fleet) its submodules. Customer and Fleet services include `-application`, `-infrastructure`, and `-container` modules following docs/17 four-module pattern. Reservation Service adds `-domain` module.

#### Scenario: Module list includes reservation-domain

* **WHEN** the root `pom.xml` is parsed
* **THEN** it SHALL declare `reservation-service/reservation-domain` as a module

#### Scenario: reservation-domain module directory exists

* **WHEN** the project directory structure is inspected
* **THEN** `reservation-service/reservation-domain/` SHALL exist with a valid `pom.xml`
* **AND** the POM SHALL inherit from the root parent POM
* **AND** the POM SHALL depend on `common` module
* **AND** the POM SHALL have zero Spring dependencies in compile scope

#### Scenario: Module order for reservation-domain

* **WHEN** the root `pom.xml` is parsed
* **THEN** `reservation-service/reservation-domain` SHALL appear after `common` and before `reservation-service/reservation-infrastructure`

#### Scenario: dependencyManagement includes reservation-domain

* **WHEN** the root POM `<dependencyManagement>` is inspected
* **THEN** it SHALL declare `reservation-domain` with `${vehicle-rental.version}`
