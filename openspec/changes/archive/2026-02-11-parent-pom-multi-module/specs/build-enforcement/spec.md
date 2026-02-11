## ADDED Requirements

### Requirement: Maven version enforcement
The root POM SHALL enforce a minimum Maven version of 3.9.0 using maven-enforcer-plugin.

#### Scenario: Build with Maven < 3.9 fails
- **WHEN** a developer runs `mvn` with a Maven version below 3.9.0
- **THEN** the build SHALL fail with an enforcement error

### Requirement: Java version enforcement
The root POM SHALL enforce a minimum Java version of 21 using maven-enforcer-plugin.

#### Scenario: Build with Java < 21 fails
- **WHEN** a developer runs `mvn` with a Java version below 21
- **THEN** the build SHALL fail with an enforcement error

### Requirement: Dependency convergence
The root POM SHALL enforce dependency convergence and ban duplicate POM dependency versions.

#### Scenario: Conflicting transitive dependency versions fail the build
- **WHEN** two modules pull different versions of the same transitive dependency
- **THEN** the build SHALL fail with a convergence error

### Requirement: Legacy logging libraries banned
The root POM SHALL ban `commons-logging:commons-logging` and `log4j:log4j` via maven-enforcer-plugin's `bannedDependencies` rule, ensuring the platform uses only SLF4J + Logback (provided by Spring Boot).

#### Scenario: Module depending on commons-logging fails the build
- **WHEN** a module has a direct or transitive dependency on `commons-logging:commons-logging`
- **THEN** the build SHALL fail with a banned dependency error

#### Scenario: Module depending on log4j 1.x fails the build
- **WHEN** a module has a direct or transitive dependency on `log4j:log4j`
- **THEN** the build SHALL fail with a banned dependency error
