## MODIFIED Requirements

### Requirement: All modules declared

The root POM SHALL declare all platform modules in a `<modules>` section: `common`, `common-messaging`, and for each service (reservation, customer, payment, fleet) its submodules. Customer and Fleet services include `-domain`, `-application`, `-infrastructure`, and `-container` modules following docs/17 four-module pattern. Reservation Service includes the same four-module pattern. Payment Service includes `-domain`, `-application`, `-infrastructure`, and `-container` modules.

#### Scenario: Module list is complete

- **WHEN** the root `pom.xml` is parsed
- **THEN** it SHALL declare `common`, `common-messaging`, `reservation-service/reservation-domain`, `reservation-service/reservation-application`, `reservation-service/reservation-infrastructure`, `reservation-service/reservation-container`, `customer-service/customer-domain`, `customer-service/customer-application`, `customer-service/customer-infrastructure`, `customer-service/customer-container`, `payment-service/payment-domain`, `payment-service/payment-application`, `payment-service/payment-infrastructure`, `payment-service/payment-container`, `fleet-service/fleet-domain`, `fleet-service/fleet-application`, `fleet-service/fleet-infrastructure`, `fleet-service/fleet-container`

#### Scenario: Module list includes payment-infrastructure and payment-container

- **WHEN** the root `pom.xml` is parsed
- **THEN** it SHALL declare `payment-service/payment-infrastructure` as a module
- **AND** it SHALL declare `payment-service/payment-container` as a module
- **AND** `payment-service/payment-infrastructure` SHALL appear after `payment-service/payment-application` and before `payment-service/payment-container`

#### Scenario: payment-infrastructure module directory exists

- **WHEN** the project directory structure is inspected
- **THEN** `payment-service/payment-infrastructure/` SHALL exist with a valid `pom.xml`
- **AND** the POM SHALL inherit from the root parent POM
- **AND** the POM SHALL depend on `payment-application` module

#### Scenario: payment-container module directory exists

- **WHEN** the project directory structure is inspected
- **THEN** `payment-service/payment-container/` SHALL exist with a valid `pom.xml`
- **AND** the POM SHALL inherit from the root parent POM
- **AND** the POM SHALL depend on `payment-infrastructure` module

#### Scenario: dependencyManagement includes payment-infrastructure and payment-container

- **WHEN** the root POM `<dependencyManagement>` is inspected
- **THEN** it SHALL declare `payment-infrastructure` with `${vehicle-rental.version}`
- **AND** it SHALL declare `payment-container` with `${vehicle-rental.version}`
