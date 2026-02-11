## Why

The vehicle-rental-platform has architecture docs and an OpenSpec project definition, but no buildable code yet. Every Maven multi-module project starts with a parent POM that centralizes Java version, dependency versions, plugin configuration, and module declarations. Without it, nothing can compile.

## What Changes

- Create the root `pom.xml` as the parent POM for the entire platform
- Inherit from `spring-boot-starter-parent` (3.4.x)
- Configure Java 21 with parameter names retention
- Declare all 13 modules (common + 4 services x 3 submodules each)
- Centralize dependency versions via `<dependencyManagement>` (MapStruct, SpringDoc, Testcontainers, etc.)
- Configure `<pluginManagement>` for compiler (Lombok + MapStruct processors), Surefire, Failsafe, JaCoCo
- Add maven-enforcer-plugin to require Maven 3.9+ and Java 21+

## Capabilities

### New Capabilities
- `multi-module-build`: Root POM that enables building the entire platform from a single `mvn clean install` command, with all dependency and plugin versions centralized
- `build-enforcement`: Maven enforcer rules that guarantee minimum Maven 3.9+ and Java 21+ versions, dependency convergence, and banned legacy logging libraries

### Modified Capabilities
<!-- None — this is the first code artifact in the project -->

## Impact

- `pom.xml` (new): Root parent POM — single file, ~150 lines. All future modules will reference this as their parent.
