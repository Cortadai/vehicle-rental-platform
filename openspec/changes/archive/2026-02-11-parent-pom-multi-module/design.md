## Context

The vehicle-rental-platform is a greenfield learning POC with no code yet. The architecture (defined in `openspec/project.md`) calls for 4 microservices, each with 3 hexagonal Maven modules (domain, infrastructure, container), plus a `common` shared module — 13 modules total. Before any code can be written, we need the root POM that ties everything together.

## Goals / Non-Goals

**Goals:**
- Establish the single root POM from which all modules inherit
- Centralize all dependency and plugin versions so child modules never hardcode versions
- Enforce build quality gates (Java/Maven versions, dependency convergence, banned libraries)

**Non-Goals:**
- Creating child module POM files (each module will get its own POM in a future change)
- CI/CD pipeline configuration (profiles for CI exist in best practices but are out of scope)
- Docker/native image profiles (will be added when Docker Compose setup is implemented)

## Decisions

### Decision 1: Inherit from spring-boot-starter-parent (not BOM import)

**Choice**: Use `<parent>spring-boot-starter-parent</parent>` rather than importing `spring-boot-dependencies` as a BOM.

**Rationale**: We have no corporate parent POM to contend with. The starter-parent gives us automatic plugin configuration, resource filtering, encoding defaults, and managed plugin versions — all for free. The BOM approach is only needed when a corporate parent already occupies the `<parent>` slot.

**Alternative considered**: BOM import in `<dependencyManagement>`. Rejected because it requires manually configuring plugins that the starter-parent provides automatically.

### Decision 2: Flat module declaration with path-based references

**Choice**: Declare modules using path syntax like `reservation-service/reservation-domain` rather than nesting intermediate POM aggregators.

**Rationale**: This avoids creating intermediate `reservation-service/pom.xml` aggregator POMs that add complexity without value. The root POM directly references each leaf module. This is simpler and still allows `mvn -pl reservation-service/reservation-container` for targeted builds.

**Alternative considered**: Intermediate aggregator POMs per service (e.g., `reservation-service/pom.xml` listing its 3 sub-modules). Rejected for the POC to keep the module hierarchy flat and simple. Can be revisited if we need service-level builds.

### Decision 3: Lombok as global dependency, Spring Boot test as global test dependency

**Choice**: Declare `lombok` (optional) and `spring-boot-starter-test` (test scope) in the parent POM's `<dependencies>` section (not `<dependencyManagement>`), so they are inherited by all modules automatically.

**Rationale**: Every module in this project will use Lombok and JUnit 5 / AssertJ. Declaring them once in the parent avoids repeating them in 13 child POMs. Lombok is marked `<optional>true</optional>` so it doesn't leak into transitive dependencies.

**Caveat**: The domain modules are supposed to have zero Spring dependencies. However, `spring-boot-starter-test` is test-scoped and only brings JUnit 5 + AssertJ + Mockito at test time — it does NOT add Spring to the domain module's compile classpath. This is acceptable.

### Decision 4: Spring Boot version selection

**Choice**: Use Spring Boot 3.4.1 (latest stable in the 3.4.x line as referenced in best practice docs).

**Rationale**: The `project.md` specifies "3.x (latest stable)". The best practice documents were written against 3.4.1. Using the same version ensures the examples and patterns in `docs/` apply directly.

## Risks / Trade-offs

- **Modules declared but not yet created** → `mvn` commands will fail until child module POMs exist. This is expected — the parent POM is the foundation, child modules come in subsequent changes.
- **No intermediate aggregators** → Cannot run `mvn -pl reservation-service` to build all 3 sub-modules at once. Must list them individually. Acceptable for a POC. → Mitigation: can add aggregator POMs later if needed.
- **spring-boot-starter-test in domain modules** → Purists may object. → Mitigation: it's test-scoped only, brings no Spring to compile classpath.
