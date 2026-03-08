## MODIFIED Requirements

### Requirement: Plugin configuration centralized in pluginManagement
The root POM SHALL configure Surefire, Failsafe, maven-compiler-plugin (with Lombok + MapStruct annotation processors), spring-boot-maven-plugin, and JaCoCo (with prepare-agent, report, prepare-agent-integration, report-integration, merge-results, and check executions) in `<pluginManagement>`.

#### Scenario: JaCoCo configured in pluginManagement
- **WHEN** the root POM `<pluginManagement>` is inspected
- **THEN** it SHALL contain a `jacoco-maven-plugin` configuration with six executions: `prepare-agent`, `report`, `prepare-agent-integration`, `report-integration`, `merge-results`, and `check`
