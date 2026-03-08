## 1. Actuator Dependency & Config

- [x] 1.1 Add `spring-boot-starter-actuator` dependency to `customer-service/customer-container/pom.xml`
- [x] 1.2 Add `spring-boot-starter-actuator` dependency to `fleet-service/fleet-container/pom.xml`
- [x] 1.3 Add `spring-boot-starter-actuator` dependency to `reservation-service/reservation-container/pom.xml`
- [x] 1.4 Add `spring-boot-starter-actuator` dependency to `payment-service/payment-container/pom.xml`
- [x] 1.5 Add `management.endpoints.web.exposure.include: health` to all 4 `application.yml` files

## 2. Paketo Image Names

- [x] 2.1 Configure `<image><name>vehicle-rental/customer-service:latest</name></image>` in customer-container `spring-boot-maven-plugin`
- [x] 2.2 Configure `<image><name>vehicle-rental/fleet-service:latest</name></image>` in fleet-container `spring-boot-maven-plugin`
- [x] 2.3 Configure `<image><name>vehicle-rental/reservation-service:latest</name></image>` in reservation-container `spring-boot-maven-plugin`
- [x] 2.4 Configure `<image><name>vehicle-rental/payment-service:latest</name></image>` in payment-container `spring-boot-maven-plugin`

## 3. Docker Compose Changes

- [x] 3.1 Remove `profiles: [infra]` from postgres and rabbitmq services in `docker-compose.yml`
- [x] 3.2 Add customer-service block (image, ports 8181:8181, environment, depends_on)
- [x] 3.3 Add fleet-service block (image, ports 8182:8182, environment, depends_on)
- [x] 3.4 Add reservation-service block (image, ports 8183:8183, environment, depends_on)
- [x] 3.5 Add payment-service block (image, ports 8184:8184, environment, depends_on)

## 4. Build & Verification

- [x] 4.1 Run `mvn verify` — full build green (actuator doesn't break existing tests)
- [x] 4.2 Build Paketo images: `mvn spring-boot:build-image -DskipTests` for all 4 containers
- [x] 4.3 Run `docker compose up -d` — all 6 containers start and run
- [x] 4.4 Verify each service responds: curl healthcheck on ports 8181-8184
- [x] 4.5 Run `docker compose down` — clean shutdown

## 5. Documentation

- [x] 5.1 Update `CLAUDE.md` Build & Run section with image build and compose commands
