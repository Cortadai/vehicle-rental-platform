## REMOVED Requirements

### Requirement: Coverage profile
**Reason**: JaCoCo is now permanently active in the build via the `<plugins>` section. The `coverage` profile is no longer needed.
**Migration**: Remove `-Pcoverage` from any CI/CD scripts or documentation. `mvn verify` now always includes JaCoCo instrumentation, reporting, and coverage checks.
