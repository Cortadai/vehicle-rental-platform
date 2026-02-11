## 1. POM Skeleton

- [x] 1.1 Create root `pom.xml` with XML declaration, `<project>` namespace, and `<modelVersion>`
- [x] 1.2 Add `<parent>` block inheriting from `spring-boot-starter-parent` 3.4.1
- [x] 1.3 Add project coordinates: groupId `com.vehiclerental`, artifactId `vehicle-rental-platform`, version `1.0.0-SNAPSHOT`, packaging `pom`

## 2. Modules

- [x] 2.1 Add `<modules>` section declaring all 13 modules (common + 4 services x 3 submodules each)

## 3. Properties

- [x] 3.1 Add `<properties>` with `java.version=21`, encoding UTF-8, and internal module version
- [x] 3.2 Add external dependency version properties (MapStruct, SpringDoc, ArchUnit, Lombok-MapStruct binding)

## 4. Dependency Management

- [x] 4.1 Add `<dependencyManagement>` with internal module (`common`) and external libraries (MapStruct, SpringDoc, ArchUnit)

## 5. Global Dependencies

- [x] 5.1 Add `<dependencies>` section with Lombok (optional) and spring-boot-starter-test (test scope)

## 6. Plugin Management

- [x] 6.1 Configure `maven-compiler-plugin` with `-parameters` flag and Lombok + MapStruct annotation processors
- [x] 6.2 Configure `spring-boot-maven-plugin` to exclude Lombok from final JAR
- [x] 6.3 Configure `maven-surefire-plugin` to include `*Test.java` and exclude `*IT.java`
- [x] 6.4 Configure `maven-failsafe-plugin` to include `*IT.java` with integration-test and verify goals

## 7. Build Enforcement

- [x] 7.1 Add `maven-enforcer-plugin` in `<plugins>` (not pluginManagement) with Maven 3.9+, Java 21+, dependency convergence, ban duplicate versions, and banned logging libraries (commons-logging, log4j)

## 8. Verify

- [x] 8.1 Validate the POM is well-formed XML (no syntax errors)
