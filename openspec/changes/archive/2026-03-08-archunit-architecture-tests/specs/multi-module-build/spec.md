# multi-module-build Specification (Delta)

## MODIFIED Requirements

### Requirement: All modules declared

The root POM SHALL declare all platform modules in a `<modules>` section: `common`, `common-messaging`, for each service (reservation, customer, payment, fleet) its submodules (`-domain`, `-application`, `-infrastructure`, `-container`), and the `architecture-tests` module.

#### Scenario: Module list is complete

- **WHEN** the root `pom.xml` is parsed
- **THEN** it SHALL declare `common`, `common-messaging`, `reservation-service/reservation-domain`, `reservation-service/reservation-application`, `reservation-service/reservation-infrastructure`, `reservation-service/reservation-container`, `customer-service/customer-domain`, `customer-service/customer-application`, `customer-service/customer-infrastructure`, `customer-service/customer-container`, `payment-service/payment-domain`, `payment-service/payment-application`, `payment-service/payment-infrastructure`, `payment-service/payment-container`, `fleet-service/fleet-domain`, `fleet-service/fleet-application`, `fleet-service/fleet-infrastructure`, `fleet-service/fleet-container`, `architecture-tests`

#### Scenario: Module list includes architecture-tests

- **WHEN** the root `pom.xml` is parsed
- **THEN** it SHALL declare `architecture-tests` as a module
- **AND** `architecture-tests` SHALL appear after all service container modules
