## Context

Reservation Service currently creates reservations (PENDING) and publishes ReservationCreatedEvent via Outbox, but nothing drives the state machine forward. The 3 participant services (Customer, Fleet, Payment) already have command listeners and publish response events to queues that exist but have no consumers. This change adds the SAGA orchestrator inside Reservation Service to coordinate the full flow.

All 10 architectural decisions are documented in `analisis.md` (decisions #1–#10). This design document captures the technical approach that implements those decisions.

### Current state

- Reservation aggregate has 6 states: PENDING → CUSTOMER_VALIDATED → PAID → CONFIRMED, with CANCELLING and CANCELLED for failures
- 5 command queues exist and have listeners in participant services
- 7 response queues exist in definitions.json with no consumers
- Outbox pattern is operational in all 4 services

## Goals / Non-Goals

**Goals:**
- Implement the SAGA orchestrator that drives reservation state from PENDING to CONFIRMED (or CANCELLED)
- Persist SAGA coordination state in a dedicated `saga_state` table for resilience
- Handle all 4 flows: happy path, customer rejection, payment failure, fleet rejection with compensation
- Use the existing Outbox mechanism for all command publishing (zero dual-write risk)
- Maintain hexagonal architecture consistency: domain objects + JPA entities separated, ports + adapters

**Non-Goals:**
- SAGA recovery/retry for stuck SAGAs (post-SAGA change)
- MDC/correlationId propagation (deferred — roadmap lists it but it can be added later without architectural changes)
- Monitoring dashboard or metrics for SAGA states
- Timeout handling for unresponsive participants
- Generic/reusable SAGA framework — this is a specific `ReservationSagaOrchestrator`

## Decisions

### D1: Step execution order

Steps execute in fixed order: **Step 0: Customer validation → Step 1: Payment processing → Step 2: Fleet confirmation**.

Compensation runs in reverse, skipping no-ops:
- Fleet rejection at Step 2 → compensate Step 1 (Payment refund) → skip Step 0 (Customer is no-op) → FAILED
- Payment failure at Step 1 → skip Step 0 (no-op) → FAILED
- Customer rejection at Step 0 → nothing to compensate → FAILED

**Why this order**: Validate cheapest operation first (Customer = read-only check), then commit money (Payment), then allocate physical resource (Fleet). This minimizes the chance of needing expensive compensations.

### D2: reservationId as sagaId

Use the reservation's UUID directly as the saga_state primary key. No separate sagaId.

**Why**: Every response event already carries `reservationId`. There is exactly one SAGA per reservation. Adding a separate sagaId would require either a lookup table or passing sagaId through all participant services (which would require modifying their events). Using reservationId eliminates all of this.

**Trade-off**: If we ever needed multiple SAGAs per reservation (e.g., modification SAGA), we'd need to change this. Acceptable for this POC.

### D3: SagaState as domain object with separate JPA entity (Decision #10)

```
reservation-domain/
  model/saga/
    SagaState.java        ← pure Java, no JPA annotations
    SagaStatus.java       ← enum with transition validation
  port/output/
    SagaStateRepository.java

reservation-infrastructure/
  adapter/output/persistence/saga/
    SagaStateJpaEntity.java
    SagaStateJpaRepository.java
    SagaStateRepositoryAdapter.java
    SagaStatePersistenceMapper.java
```

**Why**: Consistent with Reservation, Customer, Payment, Vehicle — all follow domain object + JPA entity + mapper + adapter. Anyone reading the codebase expects this boundary everywhere.

**Alternative considered**: SagaState directly in application layer (doc 18 approach). Rejected because it breaks the visual consistency of the project, which is a primary learning goal.

### D4: SagaCommandPublisher output port (Decision #8)

```java
// In reservation-application
public interface SagaCommandPublisher {
    void publish(String exchange, String routingKey, String payload);
}

// In reservation-infrastructure
@Component
public class OutboxSagaCommandPublisher implements SagaCommandPublisher {
    // Writes to OutboxEvent table — same mechanism as domain events
}
```

**Why**: Steps live in application layer. They must not touch `OutboxEventRepository` (JPA) directly. The port keeps the hexagonal boundary clean.

**Alternative considered**: Steps write to OutboxEventRepository directly (doc 18 approach). Rejected — couples application to infrastructure.

### D5: Listeners normalize responses before calling orchestrator

Each of the 7 response listeners parses its specific event shape and calls the orchestrator with a normalized result:

```java
// Infrastructure listener parses specific event
CustomerValidatedResponseListener:
  json = parse(message)
  reservationId = json.get("reservationId")
  orchestrator.handleStepSuccess(reservationId, "CUSTOMER_VALIDATION")

// Orchestrator receives uniform calls
ReservationSagaOrchestrator:
  handleStepSuccess(reservationId, stepName)
  handleStepFailure(reservationId, stepName, failureMessages)
  handleCompensationComplete(reservationId, stepName)
```

**Why**: The orchestrator stays clean — no JSON parsing, no knowledge of event shapes. Infrastructure handles the messy parsing (same pattern as existing command listeners in Customer/Fleet/Payment).

### D6: Orchestrator updates both SagaState and Reservation in same transaction

When the orchestrator receives a step result, it:
1. Loads SagaState by reservationId
2. Loads Reservation by reservationId
3. Updates SagaState (advance step / start compensation / mark terminal)
4. Transitions Reservation state (validateCustomer / pay / confirm / initCancel / cancel)
5. Publishes next command via SagaCommandPublisher (if more steps)
6. Saves both in same `@Transactional` — atomic with the Outbox write

**Why**: Both state changes and the next command must be atomic. If the service crashes between updating SagaState and sending the next command, we'd have a stuck SAGA. Same-TX guarantees consistency.

### D7: Customer rollback is a no-op skip

When compensating, the orchestrator checks if a step's rollback is a no-op before sending a command. CustomerValidationStep declares itself as no-op (`hasCompensation() = false`). The orchestrator skips it and moves to the next compensation or marks FAILED.

```java
public interface SagaStep<T> {
    void process(T data);
    void rollback(T data);
    String getName();
    boolean hasCompensation();  // false = skip during rollback
}
```

FleetConfirmationStep.hasCompensation() = false as well. Fleet is the last step — if it rejects, nothing has been confirmed at Fleet so there is nothing to release. `fleet.release.command.queue` is available for future scenarios (e.g., modification SAGA) but is not exercised by this flow.

**Why**: Sending a rollback command and waiting for a response that does nothing adds latency and complexity for zero value. Customer validation is read-only — there's nothing to undo. Fleet rejection means Fleet never reserved anything.

### D8: ReservationSagaData carries all data needed by all steps

```java
public record ReservationSagaData(
    UUID reservationId,
    UUID customerId,
    UUID vehicleId,
    BigDecimal totalAmount,
    String currency,
    LocalDate pickupDate,
    LocalDate returnDate
) {}
```

Serialized as JSON in `saga_state.payload`. Each step extracts what it needs:
- CustomerValidationStep → customerId, reservationId
- PaymentStep → reservationId, customerId, totalAmount, currency
- FleetConfirmationStep → vehicleId, reservationId, pickupDate, returnDate

**Why**: A single immutable record is simpler than step-specific payloads. All data is known at SAGA creation time.

**Note**: `vehicleId` comes from the first ReservationItem. For this POC, reservations have one vehicle. Multi-vehicle would require a different approach.

### D9: saga_state table schema

```sql
CREATE TABLE saga_state (
    saga_id         UUID PRIMARY KEY,          -- = reservationId
    saga_type       VARCHAR(50) NOT NULL,       -- 'RESERVATION_CREATION'
    status          VARCHAR(20) NOT NULL,       -- SagaStatus enum
    current_step    INT NOT NULL DEFAULT 0,
    total_steps     INT NOT NULL,
    payload         TEXT NOT NULL,              -- JSON (ReservationSagaData)
    failure_reason  TEXT,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    version         BIGINT NOT NULL DEFAULT 0   -- optimistic locking
);

CREATE INDEX idx_saga_state_status ON saga_state(status);
```

Optimistic locking via `version` prevents concurrent processing of the same SAGA from duplicate messages.

### D10: How CreateReservationUseCase changes

```
Current flow:
  1. Reservation.create() → PENDING + ReservationCreatedEvent
  2. reservationRepository.save()
  3. eventPublisher.publish(ReservationCreatedEvent)  ← kept
  4. clearDomainEvents()

New flow (additions):
  1. Reservation.create() → PENDING + ReservationCreatedEvent
  2. reservationRepository.save()
  3. eventPublisher.publish(ReservationCreatedEvent)  ← kept
  4. clearDomainEvents()
  5. sagaOrchestrator.start(sagaData)                 ← NEW
     └── Creates SagaState + executes Step[0] (CustomerValidation)
         └── Writes OutboxEvent: customer.validate.command
```

All in the same `@Transactional`. ReservationCreatedEvent is still published — it's a domain event that may have other consumers in the future.

## Risks / Trade-offs

**[Dual state machine sync]** → SagaState and ReservationStatus must stay in sync. Mitigation: both updated in the same transaction by the orchestrator. No separate processes update them independently.

**[Single vehicle per reservation]** → ReservationSagaData carries one vehicleId. Mitigation: acceptable for POC scope. Multi-vehicle would require iterating fleet confirmation per item.

**[No SAGA timeout/recovery]** → If a participant never responds, the SAGA stays in PROCESSING forever. Mitigation: deferred to post-SAGA change. Can be added as a scheduled job that queries `findStuckSagas()` without architectural changes.

**[No idempotency in orchestrator]** → Duplicate response messages could advance the SAGA twice. Mitigation: optimistic locking on `saga_state.version` — second concurrent update throws `OptimisticLockException`, message goes to DLQ or is retried. The step check (current step name matches expected) provides a second guard.

**[7 listeners is many classes]** → Could be consolidated into fewer listeners. Trade-off: one-class-per-queue follows the existing pattern in Customer/Fleet/Payment services and keeps each listener simple and independently testable.
