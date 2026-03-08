# jacoco-coverage-enforcement Specification

## Purpose
Defines JaCoCo as a permanent code coverage enforcement mechanism in the build, with differentiated thresholds per architectural layer and exclusions for JPA data holders.

## ADDED Requirements

### Requirement: JaCoCo permanently active in all modules
The root POM SHALL activate the JaCoCo Maven plugin in the `<plugins>` section (not in a profile), so that code coverage instrumentation and reporting runs on every `mvn test` and `mvn verify` invocation.

#### Scenario: JaCoCo runs without profile activation
- **WHEN** a developer runs `mvn verify` without any profile flags
- **THEN** JaCoCo SHALL instrument all modules (except those with `jacoco.skip=true`)
- **AND** coverage reports SHALL be generated in each module's `target/site/jacoco/`

#### Scenario: No coverage profile exists
- **WHEN** the root POM is inspected
- **THEN** there SHALL NOT be a profile with `<id>coverage</id>`

### Requirement: Coverage check with INSTRUCTION counter
The JaCoCo plugin SHALL include a `check` goal execution in the `verify` phase that validates coverage using the `INSTRUCTION` counter explicitly (not relying on defaults).

#### Scenario: Check uses INSTRUCTION counter explicitly
- **WHEN** the JaCoCo `check` execution configuration is inspected
- **THEN** the rule SHALL specify `<counter>INSTRUCTION</counter>` explicitly
- **AND** the rule SHALL specify `<value>COVEREDRATIO</value>`

#### Scenario: Build fails when coverage is below threshold
- **WHEN** `mvn verify` completes and a module's instruction coverage is below its configured threshold
- **THEN** the build SHALL fail with a JaCoCo coverage check error

### Requirement: Differentiated thresholds by architectural layer
The coverage check SHALL enforce different minimum thresholds depending on the module's architectural layer.

#### Scenario: Domain modules enforce 80% coverage
- **WHEN** a `*-domain` module is built with `mvn verify`
- **THEN** the JaCoCo check SHALL require a minimum of 0.80 (80%) instruction coverage

#### Scenario: Application modules enforce 75% coverage
- **WHEN** a `*-application` module is built with `mvn verify`
- **THEN** the JaCoCo check SHALL require a minimum of 0.75 (75%) instruction coverage

#### Scenario: Infrastructure modules enforce 60% coverage
- **WHEN** an `*-infrastructure` module is built with `mvn verify`
- **THEN** the JaCoCo check SHALL require a minimum of 0.60 (60%) instruction coverage

#### Scenario: Common modules enforce 80% coverage
- **WHEN** the `common` or `common-messaging` module is built with `mvn verify`
- **THEN** the JaCoCo check SHALL require a minimum of 0.80 (80%) instruction coverage

### Requirement: Threshold override in child module POMs
The default threshold (80%) SHALL be defined in the parent POM's `pluginManagement`. Modules that require a different threshold SHALL override the JaCoCo `check` configuration in their own POM with the appropriate minimum value.

#### Scenario: Parent POM defines default 80% threshold
- **WHEN** the root POM `pluginManagement` JaCoCo `check` execution is inspected
- **THEN** the default minimum SHALL be `0.80`

#### Scenario: Application module POMs override threshold to 75%
- **WHEN** any `*-application` module's POM is inspected
- **THEN** it SHALL contain a JaCoCo plugin `<configuration>` that overrides the `check` rule minimum to `0.75`

#### Scenario: Infrastructure module POMs override threshold to 60%
- **WHEN** any `*-infrastructure` module's POM is inspected
- **THEN** it SHALL contain a JaCoCo plugin `<configuration>` that overrides the `check` rule minimum to `0.60`

#### Scenario: Domain and common modules inherit default threshold
- **WHEN** any `*-domain`, `common`, or `common-messaging` module's POM is inspected
- **THEN** it SHALL NOT contain a JaCoCo plugin override (inherits 80% from parent)

### Requirement: Container modules excluded via jacoco.skip property
Container modules SHALL be excluded from JaCoCo instrumentation, reporting, and check by setting the `jacoco.skip` property.

#### Scenario: Parent POM defines jacoco.skip as false
- **WHEN** the root POM `<properties>` section is inspected
- **THEN** it SHALL contain `<jacoco.skip>false</jacoco.skip>`

#### Scenario: Container POMs override jacoco.skip to true
- **WHEN** any `*-container` module's POM `<properties>` section is inspected
- **THEN** it SHALL contain `<jacoco.skip>true</jacoco.skip>`

#### Scenario: Container module build skips JaCoCo
- **WHEN** `mvn verify` runs on a `*-container` module
- **THEN** JaCoCo SHALL NOT instrument classes
- **AND** JaCoCo SHALL NOT generate coverage reports
- **AND** JaCoCo SHALL NOT execute the coverage check

### Requirement: JPA entity classes excluded from coverage calculation
The JaCoCo check and report SHALL exclude JPA entity data holder classes from coverage calculation.

#### Scenario: JpaEntity classes excluded
- **WHEN** JaCoCo calculates coverage for any module
- **THEN** classes matching `**/entity/*JpaEntity.class` SHALL be excluded from the check and report

#### Scenario: OutboxEvent excluded
- **WHEN** JaCoCo calculates coverage for the `common-messaging` module
- **THEN** the class `**/outbox/OutboxEvent.class` SHALL be excluded from the check and report

#### Scenario: Manual mappers NOT excluded
- **WHEN** JaCoCo calculates coverage for any module
- **THEN** mapper classes (`*Mapper.java`) SHALL NOT be excluded (they contain real conversion logic)

### Requirement: Merged coverage data for check
The JaCoCo plugin SHALL merge unit test and integration test coverage data before running the check, so that modules whose tests are primarily ITs (like infrastructure) can meet their thresholds.

#### Scenario: Merge execution combines exec files
- **WHEN** `mvn verify` completes the `post-integration-test` phase
- **THEN** a `merge` execution SHALL combine `jacoco.exec` and `jacoco-it.exec` into `jacoco-merged.exec`

#### Scenario: Check runs on merged data
- **WHEN** the JaCoCo `check` execution runs in the `verify` phase
- **THEN** it SHALL use `jacoco-merged.exec` as the data file

#### Scenario: Missing exec file does not fail merge
- **WHEN** a module has no integration tests (no `jacoco-it.exec`)
- **THEN** the merge goal SHALL complete successfully using only `jacoco.exec`

### Requirement: Six JaCoCo executions in pluginManagement
The root POM `pluginManagement` SHALL define exactly six JaCoCo executions.

#### Scenario: All six executions are present
- **WHEN** the JaCoCo plugin configuration in `pluginManagement` is inspected
- **THEN** the following execution IDs SHALL be present: `prepare-agent`, `report`, `prepare-agent-integration`, `report-integration`, `merge-results`, `check`

#### Scenario: Execution phases are correct
- **WHEN** the execution phases are inspected
- **THEN** `prepare-agent` SHALL use the default phase
- **AND** `report` SHALL run in the `test` phase
- **AND** `prepare-agent-integration` SHALL use the default phase
- **AND** `report-integration` SHALL run in the `post-integration-test` phase
- **AND** `merge-results` SHALL run in the `verify` phase
- **AND** `check` SHALL run in the `verify` phase after `merge-results`
