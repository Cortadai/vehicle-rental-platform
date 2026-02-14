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

