## 1. Parent POM — pluginManagement JaCoCo

- [x] 1.1 Add `<jacoco.skip>false</jacoco.skip>` to parent POM `<properties>`
- [x] 1.2 Update JaCoCo `pluginManagement` block: add `merge-results` execution (goal `merge`, phase `verify`, merges `jacoco.exec` + `jacoco-it.exec` into `jacoco-merged.exec`)
- [x] 1.3 Add `check` execution to `pluginManagement` (goal `check`, phase `verify`, dataFile `jacoco-merged.exec`, counter `INSTRUCTION`, value `COVEREDRATIO`, minimum `0.80`)
- [x] 1.4 Add `<excludes>` to check and report executions: `**/entity/*JpaEntity.class` and `**/outbox/OutboxEvent.class`

## 2. Parent POM — Activate plugin and remove profile

- [x] 2.1 Add `jacoco-maven-plugin` to parent `<plugins>` section (alongside enforcer)
- [x] 2.2 Remove the `<profile><id>coverage</id>` block entirely

## 3. Container POMs — Skip JaCoCo

- [x] 3.1 Add `<jacoco.skip>true</jacoco.skip>` to `customer-container` POM `<properties>`
- [x] 3.2 Add `<jacoco.skip>true</jacoco.skip>` to `fleet-container` POM `<properties>`
- [x] 3.3 Add `<jacoco.skip>true</jacoco.skip>` to `reservation-container` POM `<properties>`
- [x] 3.4 Add `<jacoco.skip>true</jacoco.skip>` to `payment-container` POM `<properties>`

## 4. Application POMs — Override threshold to 75%

- [x] 4.1 Add JaCoCo plugin config override with minimum `0.75` to `customer-application` POM
- [x] 4.2 Add JaCoCo plugin config override with minimum `0.75` to `fleet-application` POM
- [x] 4.3 Add JaCoCo plugin config override with minimum `0.75` to `reservation-application` POM
- [x] 4.4 Add JaCoCo plugin config override with minimum `0.75` to `payment-application` POM

## 5. Infrastructure POMs — Override threshold to 60%

- [x] 5.1 Add JaCoCo plugin config override with minimum `0.60` to `customer-infrastructure` POM
- [x] 5.2 Add JaCoCo plugin config override with minimum `0.60` to `fleet-infrastructure` POM
- [x] 5.3 Add JaCoCo plugin config override with minimum `0.60` to `reservation-infrastructure` POM
- [x] 5.4 Add JaCoCo plugin config override with minimum `0.60` to `payment-infrastructure` POM

## 6. Verification

- [x] 6.1 Run `mvn verify` — all 18 modules build successfully with JaCoCo check passing
- [x] 6.2 Verify coverage reports generated in domain/application/infrastructure/common modules (target/site/jacoco/)
- [x] 6.3 Verify no coverage reports generated in container modules (jacoco.skip=true)
- [x] 6.4 Update CLAUDE.md Build & Run section: remove `-Pcoverage` reference, document JaCoCo is always active
