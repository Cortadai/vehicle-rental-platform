## 1. Maven Dependencies

- [x] 1.1 Add `micrometer-tracing-bridge-otel`, `opentelemetry-exporter-otlp`, `micrometer-registry-prometheus` to parent POM `dependencyManagement` — REMOVED (Spring Boot BOM manages versions directly, declaring in dependencyManagement without versions shadows the BOM)
- [x] 1.2 Add the 3 dependencies to `customer-service/customer-container/pom.xml`
- [x] 1.3 Add the 3 dependencies to `fleet-service/fleet-container/pom.xml`
- [x] 1.4 Add the 3 dependencies to `reservation-service/reservation-container/pom.xml`
- [x] 1.5 Add the 3 dependencies to `payment-service/payment-container/pom.xml`

## 2. Application Configuration

- [x] 2.1 Update `customer-service` application.yml: tracing (sampling 1.0, OTLP endpoint), actuator (expose prometheus), logging correlation
- [x] 2.2 Update `fleet-service` application.yml: same config
- [x] 2.3 Update `reservation-service` application.yml: same config
- [x] 2.4 Update `payment-service` application.yml: same config

## 3. Docker Compose Environment

- [x] 3.1 Add `OTEL_EXPORTER_OTLP_ENDPOINT` environment variable to the 4 microservices in docker-compose.yml pointing to `http://alloy:4318`

## 4. Verification

- [x] 4.1 Run `mvn test` — all tests pass
- [x] 4.2 Rebuild Docker images with `mvn spring-boot:build-image -DskipTests`
- [x] 4.3 Run `docker compose up -d` — all 11 containers healthy, Prometheus targets UP (4/4)
- [x] 4.4 Run Bruno E2E happy-path, then check Grafana Tempo for SAGA traces — traces from all services arriving
- [x] 4.5 Update README.md and CLAUDE.md with observability section
