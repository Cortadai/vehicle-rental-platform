## ADDED Requirements

### Requirement: SagaStatus enum with transition validation

SagaStatus SHALL be an enum in `reservation-domain` with values: STARTED, PROCESSING, COMPENSATING, SUCCEEDED, FAILED. It SHALL enforce valid transitions via a `canTransitionTo(SagaStatus)` method.

#### Scenario: Valid forward transitions

- **WHEN** SagaStatus transitions are checked
- **THEN** STARTED → PROCESSING SHALL be valid
- **AND** PROCESSING → SUCCEEDED SHALL be valid
- **AND** PROCESSING → COMPENSATING SHALL be valid
- **AND** COMPENSATING → FAILED SHALL be valid

#### Scenario: Terminal states reject all transitions

- **WHEN** SUCCEEDED.canTransitionTo(any) or FAILED.canTransitionTo(any) is called
- **THEN** it SHALL return false for all targets

#### Scenario: Invalid transitions rejected

- **WHEN** STARTED.canTransitionTo(SUCCEEDED) is called
- **THEN** it SHALL return false
- **AND** COMPENSATING.canTransitionTo(SUCCEEDED) SHALL return false

### Requirement: SagaState domain object

SagaState SHALL be a pure Java class in `reservation-domain/model/saga/` with no JPA or Spring annotations. It SHALL use a UUID identifier (the reservationId), and track sagaType, status (SagaStatus), currentStep, totalSteps, payload (String), failureReason, createdAt, updatedAt, and version (Long for optimistic locking).

#### Scenario: Create factory method

- **WHEN** `SagaState.create(sagaId, sagaType, totalSteps, payload)` is called
- **THEN** a new SagaState SHALL be returned with status STARTED, currentStep 0, version 0, createdAt and updatedAt set to now, and failureReason null

#### Scenario: Reconstruct factory method

- **WHEN** `SagaState.reconstruct(...)` is called with all fields
- **THEN** a SagaState SHALL be returned with all fields set exactly as provided, no events registered

### Requirement: SagaState state transitions

SagaState SHALL expose methods for state transitions that validate the transition is legal before applying it.

#### Scenario: beginProcessing transitions STARTED to PROCESSING

- **WHEN** `beginProcessing()` is called on a SagaState with status STARTED
- **THEN** status SHALL transition to PROCESSING and updatedAt SHALL be updated

#### Scenario: advanceToNextStep on PROCESSING

- **WHEN** `advanceToNextStep()` is called on a SagaState with status PROCESSING
- **THEN** currentStep SHALL increment by 1 (status stays PROCESSING)

#### Scenario: advanceToNextStep rejects non-PROCESSING state

- **WHEN** `advanceToNextStep()` is called on a SagaState with status STARTED
- **THEN** it SHALL throw an exception (must call beginProcessing first)

#### Scenario: markAsSucceeded

- **WHEN** `markAsSucceeded()` is called on a SagaState with status PROCESSING
- **THEN** status SHALL transition to SUCCEEDED and updatedAt SHALL be updated

#### Scenario: markAsSucceeded from wrong state

- **WHEN** `markAsSucceeded()` is called on a SagaState with status STARTED
- **THEN** it SHALL throw an exception

#### Scenario: startCompensation

- **WHEN** `startCompensation(reason)` is called on a SagaState with status PROCESSING
- **THEN** status SHALL transition to COMPENSATING, failureReason SHALL be set, and updatedAt SHALL be updated

#### Scenario: markAsFailed

- **WHEN** `markAsFailed()` is called on a SagaState with status COMPENSATING
- **THEN** status SHALL transition to FAILED and updatedAt SHALL be updated

#### Scenario: decrementStep

- **WHEN** `decrementStep()` is called on a SagaState with currentStep 2
- **THEN** currentStep SHALL become 1 and updatedAt SHALL be updated

### Requirement: SagaStateRepository output port

SagaStateRepository SHALL be an interface in `reservation-domain/port/output/` with methods: `save(SagaState)`, `findById(UUID sagaId)` returning `Optional<SagaState>`.

#### Scenario: Port is a plain Java interface

- **WHEN** SagaStateRepository is inspected
- **THEN** it SHALL have no Spring or JPA annotations
- **AND** it SHALL live in `reservation-domain`

### Requirement: SagaState JPA persistence

SagaStateJpaEntity SHALL be a JPA entity in `reservation-infrastructure` mapped to table `saga_state`. SagaStateRepositoryAdapter SHALL implement SagaStateRepository, using SagaStatePersistenceMapper to convert between domain and JPA objects.

#### Scenario: JPA entity maps to saga_state table

- **WHEN** SagaStateJpaEntity is inspected
- **THEN** it SHALL be annotated with `@Entity` and `@Table(name = "saga_state")`
- **AND** it SHALL have a `@Version` field for optimistic locking

#### Scenario: Adapter saves via mapper

- **WHEN** `sagaStateRepository.save(sagaState)` is called
- **THEN** the adapter SHALL convert the domain SagaState to SagaStateJpaEntity via mapper, persist it, and return the converted result

#### Scenario: Adapter finds by ID

- **WHEN** `sagaStateRepository.findById(sagaId)` is called
- **THEN** the adapter SHALL query by UUID and convert the JPA entity back to domain SagaState via mapper

### Requirement: Flyway migration V3 creates saga_state table

V3__create_saga_state_table.sql SHALL create the `saga_state` table with columns: saga_id (UUID PK), saga_type (VARCHAR 50), status (VARCHAR 20), current_step (INT default 0), total_steps (INT), payload (TEXT), failure_reason (TEXT), created_at (TIMESTAMPTZ), updated_at (TIMESTAMPTZ), version (BIGINT default 0). An index on status SHALL be created.

#### Scenario: Table created with correct schema

- **WHEN** Flyway migration V3 runs
- **THEN** table `saga_state` SHALL exist with all specified columns and types
- **AND** `saga_id` SHALL be the primary key
- **AND** index `idx_saga_state_status` SHALL exist on the `status` column
