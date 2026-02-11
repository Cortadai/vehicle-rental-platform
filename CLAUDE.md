# CLAUDE.md — Vehicle Rental Platform

## Project Context

This is a learning POC implementing enterprise microservice patterns. The primary goal is deep understanding of Hexagonal Architecture, SAGA Orchestration, Outbox Pattern, and DDD Tactical patterns — not just getting code to compile.

## How to Work in This Project

### Before Implementing ANY Code

1. Read `openspec/project.md` for architecture overview and conventions
2. Read the relevant `docs/*.md` best practice documents listed in project.md
3. Check `openspec/specs/` for current system state (source of truth)
4. Check `openspec/changes/` for the active change being implemented

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

## Build & Run

```bash
# Build all modules
mvn clean install

# Run unit tests only
mvn test

# Run unit + integration tests
mvn verify

# Start infrastructure (PostgreSQL + RabbitMQ)
docker-compose up -d

# Run a specific service
cd reservation-service/reservation-container
mvn spring-boot:run
```
