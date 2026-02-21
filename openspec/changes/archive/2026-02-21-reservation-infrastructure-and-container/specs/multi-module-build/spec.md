# multi-module-build Specification

## MODIFIED Requirements

### Requirement: All modules declared

The root POM SHALL declare all platform modules in a `<modules>` section: `common`, and for each service (reservation, customer, payment, fleet) its submodules. Customer and Fleet services include `-application`, `-infrastructure`, and `-container` modules following docs/17 four-module pattern. Reservation Service adds `-domain`, `-application`, `-infrastructure`, and `-container` modules.

#### Scenario: Module list is complete

- **WHEN** the root `pom.xml` is parsed
- **THEN** it SHALL declare `common`, `reservation-service/reservation-domain`, `reservation-service/reservation-application`, `reservation-service/reservation-infrastructure`, `reservation-service/reservation-container`, `customer-service/customer-domain`, `customer-service/customer-application`, `customer-service/customer-infrastructure`, `customer-service/customer-container`, `payment-service/payment-domain`, `payment-service/payment-infrastructure`, `payment-service/payment-container`, `fleet-service/fleet-domain`, `fleet-service/fleet-application`, `fleet-service/fleet-infrastructure`, `fleet-service/fleet-container`

#### Scenario: Module list includes reservation-application

* **WHEN** the root `pom.xml` is parsed
* **THEN** it SHALL declare `reservation-service/reservation-application` as a module

#### Scenario: reservation-application module directory exists

* **WHEN** the project directory structure is inspected
* **THEN** `reservation-service/reservation-application/` SHALL exist with a valid `pom.xml`
* **AND** the POM SHALL inherit from the root parent POM
* **AND** the POM SHALL depend on `reservation-domain` module
* **AND** the POM SHALL depend on `spring-tx`

#### Scenario: Module order for reservation-application

* **WHEN** the root `pom.xml` is parsed
* **THEN** `reservation-service/reservation-application` SHALL appear after `reservation-service/reservation-domain` and before `reservation-service/reservation-infrastructure`

#### Scenario: dependencyManagement includes reservation-application

* **WHEN** the root POM `<dependencyManagement>` is inspected
* **THEN** it SHALL declare `reservation-application` with `${vehicle-rental.version}`

#### Scenario: Module list includes reservation-infrastructure and reservation-container

- **WHEN** the root `pom.xml` is parsed
- **THEN** it SHALL declare `reservation-service/reservation-infrastructure` as a module
- **AND** it SHALL declare `reservation-service/reservation-container` as a module
- **AND** `reservation-service/reservation-infrastructure` SHALL appear after `reservation-service/reservation-application` and before `reservation-service/reservation-container`

#### Scenario: reservation-infrastructure module directory exists

- **WHEN** the project directory structure is inspected
- **THEN** `reservation-service/reservation-infrastructure/` SHALL exist with a valid `pom.xml`
- **AND** the POM SHALL inherit from the root parent POM
- **AND** the POM SHALL depend on `reservation-application` module

#### Scenario: reservation-container module directory exists

- **WHEN** the project directory structure is inspected
- **THEN** `reservation-service/reservation-container/` SHALL exist with a valid `pom.xml`
- **AND** the POM SHALL inherit from the root parent POM
- **AND** the POM SHALL depend on `reservation-infrastructure` module

#### Scenario: dependencyManagement includes reservation-infrastructure and reservation-container

- **WHEN** the root POM `<dependencyManagement>` is inspected
- **THEN** it SHALL declare `reservation-infrastructure` with `${vehicle-rental.version}`
- **AND** it SHALL declare `reservation-container` with `${vehicle-rental.version}`

#### Scenario: customer-domain module directory exists

- **WHEN** the project directory structure is inspected
- **THEN** `customer-service/customer-domain/` SHALL exist with a valid `pom.xml`
- **AND** the POM SHALL inherit from the root parent POM
- **AND** the POM SHALL depend on `common` module
- **AND** the POM SHALL have zero Spring dependencies in compile scope

#### Scenario: customer-application module directory exists

- **WHEN** the project directory structure is inspected
- **THEN** `customer-service/customer-application/` SHALL exist with a valid `pom.xml`
- **AND** the POM SHALL inherit from the root parent POM
- **AND** the POM SHALL depend on `customer-domain` module
- **AND** the POM SHALL depend on `spring-tx`

#### Scenario: Module order for customer-application

- **WHEN** the root `pom.xml` is parsed
- **THEN** `customer-service/customer-application` SHALL appear after `customer-service/customer-domain` and before `customer-service/customer-infrastructure`

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
* **THEN** `reservation-service/reservation-domain` SHALL appear after `common` and before `reservation-service/reservation-application`

#### Scenario: dependencyManagement includes reservation-domain

* **WHEN** the root POM `<dependencyManagement>` is inspected
* **THEN** it SHALL declare `reservation-domain` with `${vehicle-rental.version}`
