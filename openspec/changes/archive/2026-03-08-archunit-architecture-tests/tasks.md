## 1. Module Setup

- [x] 1.1 Create `architecture-tests/` directory with `pom.xml` (parent POM, jacoco.skip=true, dependencies: 4x *-infrastructure + common-messaging + archunit-junit5 test)
- [x] 1.2 Create `architecture-tests/src/test/java/com/vehiclerental/architecture/` package
- [x] 1.3 Add `<module>architecture-tests</module>` to root POM `<modules>` after all service containers

## 2. Domain Purity Tests

- [x] 2.1 Create `DomainPurityTest.java` with `@AnalyzeClasses(packages = "com.vehiclerental")`
- [x] 2.2 Rule: domain classes SHALL NOT depend on `org.springframework..`
- [x] 2.3 Rule: domain classes SHALL NOT depend on `jakarta.persistence..`
- [x] 2.4 Rule: domain classes SHALL NOT depend on `..application..`
- [x] 2.5 Rule: domain classes SHALL NOT depend on `..infrastructure..`
- [x] 2.6 Rule: domain classes SHALL NOT depend on `com.vehiclerental.common.messaging..`

## 3. Application Isolation Tests

- [x] 3.1 Create `ApplicationIsolationTest.java` with `@AnalyzeClasses(packages = "com.vehiclerental")`
- [x] 3.2 Rule: application classes SHALL only depend on allowlist (`..domain..`, `com.vehiclerental.common..`, `java..`, `lombok..`, `org.slf4j..`, `com.fasterxml.jackson..`, `org.springframework.transaction..`)

## 4. Dependency Flow Tests

- [x] 4.1 Create `DependencyFlowTest.java` with `@AnalyzeClasses(packages = "com.vehiclerental")`
- [x] 4.2 Rule: domain SHALL NOT depend on application or infrastructure
- [x] 4.3 Rule: application SHALL NOT depend on infrastructure

## 5. Build Verification

- [x] 5.1 Run `mvn verify` — all architecture tests pass, full build green
- [x] 5.2 Verify JaCoCo skips `architecture-tests` module (no reports generated)
