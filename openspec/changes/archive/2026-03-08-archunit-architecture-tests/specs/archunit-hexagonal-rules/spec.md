# archunit-hexagonal-rules Specification

## Purpose
Defines ArchUnit rules that enforce hexagonal architecture boundaries across all services, ensuring domain purity, application isolation, and inward-only dependency flow.

## ADDED Requirements

### Requirement: Architecture tests module exists
The project SHALL contain a dedicated `architecture-tests` module at the root level with only test sources (`src/test/java`), no production code.

#### Scenario: Module structure
- **WHEN** the `architecture-tests/` directory is inspected
- **THEN** it SHALL contain a valid `pom.xml` inheriting from the root parent POM
- **AND** it SHALL contain `src/test/java/com/vehiclerental/architecture/` with test classes
- **AND** it SHALL NOT contain `src/main/java/`

#### Scenario: Module dependencies
- **WHEN** the `architecture-tests/pom.xml` is inspected
- **THEN** it SHALL depend on `customer-infrastructure`, `fleet-infrastructure`, `reservation-infrastructure`, and `payment-infrastructure`
- **AND** it SHALL depend on `common-messaging`
- **AND** it SHALL depend on `archunit-junit5` with scope `test`

#### Scenario: JaCoCo skipped
- **WHEN** the `architecture-tests/pom.xml` properties are inspected
- **THEN** it SHALL contain `<jacoco.skip>true</jacoco.skip>`

#### Scenario: Not in dependencyManagement
- **WHEN** the root POM `<dependencyManagement>` is inspected
- **THEN** it SHALL NOT declare `architecture-tests`

### Requirement: Architecture tests always active
The architecture tests SHALL run on every `mvn verify` invocation without requiring a profile or flag.

#### Scenario: Tests run without profile
- **WHEN** a developer runs `mvn verify` without any profile flags
- **THEN** ArchUnit tests in `architecture-tests` SHALL execute as part of the Surefire test phase
- **AND** build SHALL fail if any architecture rule is violated

### Requirement: Full codebase scan
All ArchUnit tests SHALL analyze classes from the complete `com.vehiclerental` package, covering all 4 services plus common modules.

#### Scenario: Scan scope
- **WHEN** ArchUnit tests execute
- **THEN** they SHALL analyze classes from `com.vehiclerental` package (all subpackages)
- **AND** this SHALL include customer, fleet, reservation, payment service classes
- **AND** this SHALL include common and common-messaging classes

### Requirement: Domain purity rules
ArchUnit SHALL enforce that domain layer classes have zero dependencies on Spring, JPA, infrastructure, application, and common-messaging packages.

#### Scenario: Domain does not depend on Spring
- **WHEN** ArchUnit analyzes classes in `com.vehiclerental..domain..`
- **THEN** none of those classes SHALL depend on classes in `org.springframework..`

#### Scenario: Domain does not depend on JPA
- **WHEN** ArchUnit analyzes classes in `com.vehiclerental..domain..`
- **THEN** none of those classes SHALL depend on classes in `jakarta.persistence..`

#### Scenario: Domain does not depend on application layer
- **WHEN** ArchUnit analyzes classes in `com.vehiclerental..domain..`
- **THEN** none of those classes SHALL depend on classes in `com.vehiclerental..application..`

#### Scenario: Domain does not depend on infrastructure layer
- **WHEN** ArchUnit analyzes classes in `com.vehiclerental..domain..`
- **THEN** none of those classes SHALL depend on classes in `com.vehiclerental..infrastructure..`

#### Scenario: Domain does not depend on common-messaging
- **WHEN** ArchUnit analyzes classes in `com.vehiclerental..domain..`
- **THEN** none of those classes SHALL depend on classes in `com.vehiclerental.common.messaging..`

### Requirement: Application isolation rules
ArchUnit SHALL enforce that application layer classes only depend on an explicit allowlist of packages: domain, common, java, lombok, and `org.springframework.transaction`.

#### Scenario: Application only depends on allowed packages
- **WHEN** ArchUnit analyzes classes in `com.vehiclerental..application..`
- **THEN** those classes SHALL only depend on classes in:
  - `com.vehiclerental..domain..`
  - `com.vehiclerental.common..`
  - `java..`
  - `lombok..`
  - `org.slf4j..`
  - `com.fasterxml.jackson..`
  - `org.springframework.transaction..`
- **AND** application classes SHALL NOT need to be explicitly listed in the allowlist (ArchUnit permits intra-package dependencies implicitly)

#### Scenario: Application does not depend on infrastructure
- **WHEN** ArchUnit analyzes classes in `com.vehiclerental..application..`
- **THEN** none of those classes SHALL depend on classes in `com.vehiclerental..infrastructure..`

#### Scenario: Application does not depend on Spring (except transaction)
- **WHEN** ArchUnit analyzes classes in `com.vehiclerental..application..`
- **THEN** none of those classes SHALL depend on Spring classes outside `org.springframework.transaction..`

### Requirement: Dependency flow rules
ArchUnit SHALL enforce that dependencies between architectural layers flow exclusively inward: infrastructure may depend on application and domain, application may depend on domain, but never the reverse.

#### Scenario: Domain does not depend on outer layers
- **WHEN** ArchUnit analyzes dependency flow
- **THEN** classes in `..domain..` SHALL NOT depend on classes in `..application..` or `..infrastructure..`

#### Scenario: Application does not depend on infrastructure
- **WHEN** ArchUnit analyzes dependency flow
- **THEN** classes in `..application..` SHALL NOT depend on classes in `..infrastructure..`

#### Scenario: Infrastructure may depend on application and domain
- **WHEN** ArchUnit analyzes dependency flow
- **THEN** classes in `..infrastructure..` SHALL be allowed to depend on classes in `..application..` and `..domain..`

### Requirement: Test organization by rule category
ArchUnit tests SHALL be organized into 3 test classes by rule category, not by service.

#### Scenario: Three test classes exist
- **WHEN** the `architecture-tests` test sources are inspected
- **THEN** the following test classes SHALL exist:
  - `DomainPurityTest` — validates domain layer isolation rules
  - `ApplicationIsolationTest` — validates application layer allowlist
  - `DependencyFlowTest` — validates inward-only dependency flow

#### Scenario: Test naming convention
- **WHEN** ArchUnit test class names are inspected
- **THEN** all test classes SHALL end with `Test` suffix (Surefire convention)
- **AND** all test classes SHALL reside in `com.vehiclerental.architecture` package
