# CLAUDE.md — Vehicle Rental Platform

## Project Context

This is a learning POC implementing enterprise microservice patterns. The primary goal is deep understanding of Hexagonal Architecture, SAGA Orchestration, Outbox Pattern, and DDD Tactical patterns — not just getting code to compile.

## How to Work in This Project

### Before Implementing ANY Code

1. Read `openspec/project.md` for architecture overview and conventions
2. Read the relevant `docs/*.md` best practice documents listed in project.md
3. Check `docs/roadmap.md` for the implementation roadmap and pending changes
4. Check `docs/journal.md` for the learning journal — update it after each `/opsx:archive`
5. Check `openspec/specs/` for current system state (source of truth)
6. Check `openspec/changes/` for the active change being implemented

### Best Practice Documents

The `docs/` folder contains 20 detailed best practice documents with code examples, checklists, and anti-patterns. **Always consult** the relevant document before generating code for a specific area. Key documents:

- **Hexagonal structure**: `docs/17-arquitectura-hexagonal-ddd-tactico.md` (includes full implementation checklist)
- **Package layout**: `docs/02-organizacion-paquetes-enterprise.md`
- **Maven multi-module**: `docs/07-dependencias-maven-gestion.md`
- **RabbitMQ + Outbox**: `docs/13-mensajeria-rabbitmq-patterns.md`
- **SAGA pattern**: `docs/18-saga-orchestration-pattern.md`
- **Testing strategy**: `docs/04-testing-estrategia-completa.md`
- **REST APIs**: `docs/08-apis-rest-best-practices.md`
- **Code conventions**: `docs/05-convenciones-codigo-java-spring.md`

### OpenSpec Workflow

Follow the OpenSpec spec-driven development workflow:

1. `/opsx:new <change-name>` — Create a new change
2. `/opsx:ff` — Fast-forward: generate proposal, specs, design, tasks
3. Review and iterate on generated artifacts before implementing
4. `/opsx:apply` — Implement tasks from tasks.md
5. `/opsx:archive <change-name>` — Merge specs into source of truth

### Testing Approach

- **Domain layer (40%)**: Test-First. Write tests FROM the OpenSpec scenarios BEFORE implementing.
- **Application layer (15%)**: Test-After. Mock output ports.
- **Infrastructure layer (30%)**: Test-After. Use Testcontainers for real PostgreSQL + RabbitMQ.
- **SAGA flows (15%)**: Test-After. Full flow with Testcontainers + Awaitility.

Naming: `*Test.java` (unit), `*IT.java` (integration), `*SagaIT.java` (saga).

## Critical Architecture Rules

- Domain module pom.xml must have ZERO Spring dependencies
- Never put business logic in Application Services — delegate to Domain
- JPA Entities and Domain Entities are SEPARATE classes with mappers
- Every event goes through Outbox (never publish directly to RabbitMQ)
- Aggregate Root accumulates domain events internally, published after persist
- Use typed IDs (ReservationId, CustomerId) — never raw UUID/Long
- BeanConfiguration in container module registers domain beans manually

## Mandatory Reads by Code Area

When implementing domain code: ALWAYS read docs/17 + docs/05 + docs/04 before writing any file.
When implementing infrastructure: ALWAYS read docs/17 + docs/02 + docs/06 + docs/08 before writing any file.
When implementing testing: ALWAYS read docs/04 + docs/03 before writing any test.
When modifying Maven POMs: ALWAYS read docs/07 before editing.



## Lessons Learned (from reservation-outbox-and-messaging)

### Cross-module JPA scanning
When a service imports `common-messaging`, Spring Boot does NOT auto-detect entities and repositories outside the service's base package. The service's main class MUST have all three:
```java
@SpringBootApplication(scanBasePackages = "com.vehiclerental")
@EntityScan(basePackages = "com.vehiclerental")
@EnableJpaRepositories(basePackages = "com.vehiclerental")
```

`scanBasePackages` alone is NOT enough — `@EntityScan` and `@EnableJpaRepositories` have their own independent scanning.

### All ITs need RabbitMQ after adding messaging

Once `common-messaging` is on the classpath, `OutboxPublisher` requires `RabbitTemplate`, which requires a RabbitMQ connection. ALL `@SpringBootTest` ITs in that service must declare a RabbitMQ Testcontainer:

```java
@Container
@ServiceConnection
static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.13-management-alpine");
```

Consider extracting a `BaseIT` class to avoid repeating this in every IT.

### Domain events are transient — publish from ORIGINAL aggregate

`reservationRepository.save(entity)` returns a NEW domain object reconstructed from JPA. Domain events do NOT survive the domain→JPA→domain mapper round-trip. Always publish from the ORIGINAL aggregate:

```java
// CORRECT
Reservation savedReservation = reservationRepository.save(reservation);
eventPublisher.publish(reservation.getDomainEvents());     // original
reservation.clearDomainEvents();                            // original

// WRONG — savedReservation has empty events
eventPublisher.publish(savedReservation.getDomainEvents());
```

This applies to ALL services: Reservation, Customer, Fleet, Payment.

## Build & Run

```bash
# Build all modules
mvn clean install

# Run unit tests only (includes JaCoCo coverage check)
mvn test

# Run unit + integration tests (includes JaCoCo coverage check on merged data)
mvn verify

# Build Paketo OCI images for all services
mvn spring-boot:build-image -DskipTests -pl customer-service/customer-container,fleet-service/fleet-container,reservation-service/reservation-container,payment-service/payment-container

# Start everything (infra + 4 services)
docker compose up -d

# Start only infrastructure (PostgreSQL + RabbitMQ)
docker compose up postgres rabbitmq -d

# Run a specific service locally (without Docker)
cd reservation-service/reservation-container
mvn spring-boot:run
```

JaCoCo is permanently active (no profile needed). Coverage thresholds: domain/common 80%, application 75%, infrastructure 60%. Container modules are excluded via `jacoco.skip`.

## Swagger UI

Each service exposes Swagger UI (springdoc-openapi, zero config):

| Service | Swagger UI |
|---------|-----------|
| Customer | http://localhost:8181/swagger-ui.html |
| Fleet | http://localhost:8182/swagger-ui.html |
| Reservation | http://localhost:8183/swagger-ui.html |
| Payment | http://localhost:8184/swagger-ui.html |

## Bruno E2E Tests

```bash
# Install Bruno CLI
npm install -g @usebruno/cli

# Run E2E SAGA happy path (requires docker compose up -d)
cd bruno
bru run --env local e2e/happy-path

# Run E2E compensation flow (fleet rejects → payment refund → CANCELLED)
bru run --env local e2e/compensation
```

The `bruno/` folder contains API requests for all 4 services (manual exploration) and `e2e/` subfolders with two SAGA flows: `happy-path/` (PENDING → CONFIRMED) and `compensation/` (fleet rejection → refund → CANCELLED).
