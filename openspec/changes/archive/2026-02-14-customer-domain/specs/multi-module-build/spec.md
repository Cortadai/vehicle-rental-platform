# multi-module-build

## MODIFIED Requirements

### Requirement: All 13 modules declared

The root POM SHALL declare all platform modules in a `<modules>` section: `common`, and for each service (reservation, customer, payment, fleet) its three submodules (`-domain`, `-infrastructure`, `-container`).

#### Scenario: Module list is complete

- **WHEN** the root `pom.xml` is parsed
- **THEN** it SHALL declare exactly 13 modules: `common`, `reservation-service/reservation-domain`, `reservation-service/reservation-infrastructure`, `reservation-service/reservation-container`, `customer-service/customer-domain`, `customer-service/customer-infrastructure`, `customer-service/customer-container`, `payment-service/payment-domain`, `payment-service/payment-infrastructure`, `payment-service/payment-container`, `fleet-service/fleet-domain`, `fleet-service/fleet-infrastructure`, `fleet-service/fleet-container`

#### Scenario: customer-domain module directory exists

- **WHEN** the project directory structure is inspected
- **THEN** `customer-service/customer-domain/` SHALL exist with a valid `pom.xml`
- **AND** the POM SHALL inherit from the root parent POM
- **AND** the POM SHALL depend on `common` module
- **AND** the POM SHALL have zero Spring dependencies in compile scope
