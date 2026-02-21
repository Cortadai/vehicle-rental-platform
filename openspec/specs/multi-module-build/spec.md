# multi-module-build Specification

## Purpose
TBD - created by archiving change parent-pom-multi-module. Update Purpose after archive.
## Requirements
### Requirement: Root POM inherits from Spring Boot starter parent
The root `pom.xml` SHALL inherit from `spring-boot-starter-parent` 3.4.x with `<relativePath/>` to resolve from Maven Central.

#### Scenario: POM declares correct parent
- **WHEN** the root `pom.xml` is parsed
- **THEN** the parent groupId SHALL be `org.springframework.boot`
- **AND** the parent artifactId SHALL be `spring-boot-starter-parent`
- **AND** the parent version SHALL be 3.4.x (latest stable)

### Requirement: Project coordinates and packaging
The root POM SHALL use groupId `com.vehiclerental`, artifactId `vehicle-rental-platform`, version `1.0.0-SNAPSHOT`, and packaging `pom`.

#### Scenario: POM has correct coordinates
- **WHEN** the root `pom.xml` is parsed
- **THEN** the groupId SHALL be `com.vehiclerental`
- **AND** the artifactId SHALL be `vehicle-rental-platform`
- **AND** the packaging SHALL be `pom`

### Requirement: All modules declared

The root POM SHALL declare all platform modules in a `<modules>` section: `common`, `common-messaging`, and for each service (reservation, customer, payment, fleet) its submodules. Customer and Fleet services include `-domain`, `-application`, `-infrastructure`, and `-container` modules following docs/17 four-module pattern. Reservation Service includes the same four-module pattern. Payment Service includes `-domain`, `-infrastructure`, and `-container` modules (payment-application will be added in a future change).

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

### Requirement: Java 21 configuration
The root POM SHALL configure Java 21 as the source, target, and release version with `-parameters` compiler flag for method parameter name retention.

#### Scenario: Java version is 21
- **WHEN** the effective POM properties are evaluated
- **THEN** `java.version` SHALL be `21`

### Requirement: Dependency version centralization
The root POM SHALL centralize all non-Spring-Boot-managed dependency versions in `<properties>` and declare them in `<dependencyManagement>`.

#### Scenario: Internal modules managed
- **WHEN** a child module depends on `common`
- **THEN** it SHALL NOT need to specify a version (managed by parent)

#### Scenario: MapStruct version managed
- **WHEN** a child module uses MapStruct
- **THEN** the version SHALL be centralized in the parent POM properties

### Requirement: Plugin configuration centralized in pluginManagement
The root POM SHALL configure Surefire, Failsafe, maven-compiler-plugin (with Lombok + MapStruct annotation processors), and spring-boot-maven-plugin in `<pluginManagement>`.

#### Scenario: Surefire runs only unit tests
- **WHEN** `mvn test` is executed
- **THEN** Surefire SHALL include `**/*Test.java` and exclude `**/*IT.java`

#### Scenario: Failsafe runs only integration tests
- **WHEN** `mvn verify` is executed
- **THEN** Failsafe SHALL include `**/*IT.java`

