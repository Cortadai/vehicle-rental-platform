# Vehicle Rental Platform — Project Context

## Overview

Microservices-based vehicle rental platform with 4 services communicating via events through RabbitMQ. The system implements SAGA Orchestration with automatic compensations and Outbox Pattern for eventual consistency.

**Purpose**: Learning POC for enterprise patterns (Hexagonal Architecture, SAGA, Outbox, DDD Tactical) + evaluation of OpenSpec as SDD tool.

## Tech Stack

| Technology | Version | Purpose |
|-----------|---------|---------|
| Java | 21 | Virtual Threads enabled |
| Spring Boot | 3.x (latest stable) | Base framework |
| Spring AMQP | managed by Boot | RabbitMQ integration |
| Spring Data JPA | managed by Boot | Persistence |
| PostgreSQL | 16+ | Database (single instance, multiple schemas) |
| RabbitMQ | 3.13+ | Message broker |
| Docker Compose | — | Local container orchestration |
| Flyway | managed by Boot | Database migrations |
| MapStruct | 1.5.x | DTO-Entity mapping |
| Lombok | managed by Boot | Boilerplate reduction |
| Testcontainers | managed by Boot | Integration testing |
| JUnit 5 + Mockito + AssertJ | managed by Boot | Testing |
| Maven | 3.9+ | Build tool (multi-module) |

## Architecture

### Hexagonal Architecture (Ports & Adapters)

Every microservice follows strict hexagonal architecture with Maven modules enforcing dependency direction:

```
service/
├── service-domain/           ← Pure Java, NO Spring dependencies
├── service-infrastructure/   ← Adapters (JPA, REST, Messaging)
└── service-container/        ← Spring Boot main class, BeanConfiguration
```

Dependencies: `container → infrastructure → domain` (always inward).

### Maven Multi-Module Structure

```
vehicle-rental-platform/
├── pom.xml                          ← Parent POM
├── common/                          ← Shared: BaseEntity, AggregateRoot, Money, ApiResponse
├── reservation-service/
│   ├── reservation-domain/
│   ├── reservation-infrastructure/
│   └── reservation-container/
├── customer-service/
│   ├── customer-domain/
│   ├── customer-infrastructure/
│   └── customer-container/
├── payment-service/
│   ├── payment-domain/
│   ├── payment-infrastructure/
│   └── payment-container/
├── fleet-service/
│   ├── fleet-domain/
│   ├── fleet-infrastructure/
│   └── fleet-container/
└── docker-compose.yml
```

### Package Structure per Service (inside each module)

```
domain/
├── model/                    ← Aggregate Root, Entities, Value Objects
│   └── vo/                   ← Typed IDs, Money, enums
├── event/                    ← Domain Events (past tense: ReservationCreated)
├── service/                  ← Domain Service (pure logic, no Spring)
└── exception/                ← Domain Exceptions (no HttpStatus)

application/
├── port/
│   ├── in/                   ← Input Ports (use case interfaces)
│   └── out/                  ← Output Ports (repository, publisher interfaces)
├── service/                  ← Application Services (@Transactional, orchestration only)
├── dto/                      ← Commands, Responses (records)
├── mapper/                   ← Domain ↔ DTO mappers
├── saga/                     ← SagaStep implementations
└── outbox/                   ← Outbox schedulers

adapter/
├── in/
│   └── web/                  ← REST Controllers (delegate to Input Ports only)
└── out/
    ├── persistence/          ← JPA Entities, Repositories, Adapters
    ├── messaging/            ← RabbitMQ publishers, listeners
    └── outbox/               ← OutboxJpaEntity, OutboxRepository
```

## The 4 Microservices

| Service | Role | Responsibility |
|---------|------|---------------|
| **Reservation Service** | SAGA Coordinator | REST entry point. Creates reservations, coordinates SAGA flow, manages full lifecycle |
| **Customer Service** | Validation | Verifies customer exists, active status, valid driver's license, eligibility |
| **Payment Service** | Payment processing | Charges customer. Supports compensation (refund) if later steps fail |
| **Fleet Service** | Fleet management | Confirms vehicle availability for requested dates, reserves vehicle |

## SAGA Flow (16 steps)

### Happy Path: PENDING → CUSTOMER_VALIDATED → PAID → CONFIRMED

1. Client POST → Reservation Service creates reservation (PENDING)
2. Reservation publishes ReservationCreatedEvent → Outbox → RabbitMQ
3. Customer Service validates → publishes CustomerValidatedEvent
4. Reservation updates to CUSTOMER_VALIDATED → publishes event
5. Payment Service charges → publishes PaymentCompletedEvent
6. Reservation updates to PAID → publishes event
7. Fleet Service confirms availability → publishes FleetConfirmedEvent
8. Reservation updates to CONFIRMED (final state)

### Compensation Flows

- **Customer validation fails**: PENDING → CANCELLED (no compensation needed)
- **Payment fails**: CUSTOMER_VALIDATED → CANCELLED (no compensation needed)
- **Fleet unavailable**: PAID → CANCELLING → Payment refund → CANCELLED

### SagaStep Interface

```java
public interface SagaStep<T> {
    void process(T data);
    void rollback(T data);
}
```

## Outbox Pattern

Every service uses an `outbox_events` table. Events are persisted in the same DB transaction as the business data (atomic). A scheduler polls and publishes to RabbitMQ. This eliminates the dual-write problem.

Flow: Business operation + INSERT outbox_event (same TX) → Scheduler polls → Publish to RabbitMQ → Mark as PUBLISHED.

## Database Strategy

Single PostgreSQL instance with **separate schemas** per service:

```
PostgreSQL Instance
├── schema: reservation
├── schema: customer
├── schema: payment
└── schema: fleet
```

Each schema has its own `outbox_events` table. Flyway manages migrations per service.

## RabbitMQ Topology

- One exchange per service: `reservation.exchange`, `customer.exchange`, `payment.exchange`, `fleet.exchange`
- Routing keys: `{service}.{event-type}` (e.g., `payment.completed`, `fleet.confirmation.failed`)
- Each service has its own DLQ: `{service}.dlq`
- JSON serialization with Jackson

## Testing Strategy

Test pyramid adapted for hexagonal + SAGA:

| Layer | Weight | Approach | Tools |
|-------|--------|----------|-------|
| Domain | 40% | Test-First (specs → tests) | JUnit 5, AssertJ |
| Application | 15% | Test-After, mock ports | JUnit 5, Mockito BDD |
| Infrastructure | 30% | Test-After, real infra | Testcontainers (PostgreSQL + RabbitMQ) |
| SAGA Flow | 15% | Test-After, multi-step | Testcontainers + Awaitility |

Conventions: `*Test.java` (unit), `*IT.java` (integration), `*SagaIT.java` (saga flow). Given-When-Then with comments. `@Nested` for scenarios. `@ActiveProfiles("test")` on all ITs.

## Implementation Phases

| Phase | Scope | Key Patterns |
|-------|-------|-------------|
| 1 | Reservation Service walking skeleton | Hexagonal, Domain model, REST, PostgreSQL |
| 2 | Customer Service + messaging | RabbitMQ, Outbox Pattern, inter-service events |
| 3 | Payment Service + SAGA steps | SagaStep interface, process/rollback, compensation |
| 4 | Fleet Service + complete SAGA | Full 16-step flow, end-to-end compensation |

## Conventions & Best Practices

**IMPORTANT**: Read the relevant best practice documents in `docs/` before implementing. These contain detailed patterns, code examples, and checklists:

| Document | When to read |
|----------|-------------|
| `docs/01-estructura-proyectos-spring-boot.md` | Project scaffolding, module layout |
| `docs/02-organizacion-paquetes-enterprise.md` | Package structure, hexagonal layout |
| `docs/03-manejo-excepciones-enterprise.md` | Exception hierarchy, @ControllerAdvice |
| `docs/04-testing-estrategia-completa.md` | Testing patterns, Testcontainers setup |
| `docs/05-convenciones-codigo-java-spring.md` | Naming, formatting, code style |
| `docs/06-configuracion-application-properties.md` | Spring config, profiles |
| `docs/07-dependencias-maven-gestion.md` | Maven multi-module, BOM, dependency management |
| `docs/08-apis-rest-best-practices.md` | REST design, DTOs, validation, pagination |
| `docs/11-migraciones-flyway.md` | Flyway naming, multi-schema migrations |
| `docs/13-mensajeria-rabbitmq-patterns.md` | RabbitMQ topology, Outbox entity, DLQ |
| `docs/14-resiliencia-circuit-breaker.md` | Retry, resilience patterns |
| `docs/16-virtual-threads-java21.md` | Virtual threads configuration |
| `docs/17-arquitectura-hexagonal-ddd-tactico.md` | Full hexagonal implementation guide with checklists |
| `docs/18-saga-orchestration-pattern.md` | SAGA design, SagaStep, compensation flows |
| `docs/19-docker-compose-multi-servicio.md` | Docker Compose for multi-service setup |

### Key Rules (always apply)

- Domain module: **zero Spring dependencies** in pom.xml. Pure Java only.
- Aggregate Root: factory method `create()`, no public constructors. `reconstruct()` for persistence rehydration.
- Value Objects: Java records with validation in compact constructor. Typed IDs always (never raw UUID/Long).
- Domain Events: past tense naming (ReservationCreated, NOT ReservationCreate). Accumulated in Aggregate Root, NOT published immediately.
- Application Service: implements Input Ports. `@Transactional`. **No business logic** — delegates to domain.
- JPA Entities: **separate classes** from Domain Entities. Mapper required between them.
- REST Controllers: delegate to Input Port only. No logic.
- BeanConfiguration in container module: registers domain/application beans manually (they have no Spring annotations).
- Outbox: event persisted in same TX as business data. Scheduler publishes to RabbitMQ.
- All code in English. Comments in English. Javadoc on public APIs.
