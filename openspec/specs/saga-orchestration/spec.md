## ADDED Requirements

### Requirement: SagaStep interface

SagaStep<T> SHALL be an interface in `reservation-application/saga/` with methods: `process(T data)`, `rollback(T data)`, `getName()` returning String, and `hasCompensation()` returning boolean. Steps with `hasCompensation() = false` SHALL be skipped during compensation.

#### Scenario: Interface defines all four methods

- **WHEN** SagaStep interface is inspected
- **THEN** it SHALL declare `process(T)`, `rollback(T)`, `getName()`, and `hasCompensation()`

### Requirement: ReservationSagaData record

ReservationSagaData SHALL be a Java record in `reservation-application/saga/` carrying all data needed by all steps: reservationId (UUID), customerId (UUID), vehicleId (UUID), totalAmount (BigDecimal), currency (String), pickupDate (LocalDate), returnDate (LocalDate). It SHALL be serializable to JSON for storage in saga_state.payload.

#### Scenario: Record contains all step-required fields

- **WHEN** ReservationSagaData is inspected
- **THEN** it SHALL have fields: reservationId, customerId, vehicleId, totalAmount, currency, pickupDate, returnDate

### Requirement: CustomerValidationStep

CustomerValidationStep SHALL implement `SagaStep<ReservationSagaData>`. Its `process()` SHALL publish a command to `customer.exchange` with routing key `customer.validate.command` containing customerId and reservationId. Its `hasCompensation()` SHALL return false. Its `getName()` SHALL return "CUSTOMER_VALIDATION".

#### Scenario: Process sends validate command

- **WHEN** `process(sagaData)` is called
- **THEN** a command SHALL be published via SagaCommandPublisher to exchange `customer.exchange` with routing key `customer.validate.command`
- **AND** the payload SHALL contain `customerId` and `reservationId` from sagaData

#### Scenario: Has no compensation

- **WHEN** `hasCompensation()` is called
- **THEN** it SHALL return false

#### Scenario: Rollback is no-op

- **WHEN** `rollback(sagaData)` is called
- **THEN** nothing SHALL happen (no command published)

### Requirement: PaymentStep

PaymentStep SHALL implement `SagaStep<ReservationSagaData>`. Its `process()` SHALL publish a command to `payment.exchange` with routing key `payment.process.command` containing reservationId, customerId, amount, and currency. Its `rollback()` SHALL publish a refund command to `payment.exchange` with routing key `payment.refund.command` containing reservationId. Its `hasCompensation()` SHALL return true. Its `getName()` SHALL return "PAYMENT".

#### Scenario: Process sends payment command

- **WHEN** `process(sagaData)` is called
- **THEN** a command SHALL be published via SagaCommandPublisher to exchange `payment.exchange` with routing key `payment.process.command`
- **AND** the payload SHALL contain `reservationId`, `customerId`, `amount` (from totalAmount), and `currency`

#### Scenario: Rollback sends refund command

- **WHEN** `rollback(sagaData)` is called
- **THEN** a command SHALL be published via SagaCommandPublisher to exchange `payment.exchange` with routing key `payment.refund.command`
- **AND** the payload SHALL contain `reservationId`

#### Scenario: Has compensation

- **WHEN** `hasCompensation()` is called
- **THEN** it SHALL return true

### Requirement: FleetConfirmationStep

FleetConfirmationStep SHALL implement `SagaStep<ReservationSagaData>`. Its `process()` SHALL publish a command to `fleet.exchange` with routing key `fleet.confirm.command` containing vehicleId, reservationId, pickupDate, and returnDate. Its `hasCompensation()` SHALL return false (Fleet is last step — if it rejects, nothing was confirmed). Its `getName()` SHALL return "FLEET_CONFIRMATION".

#### Scenario: Process sends fleet confirm command

- **WHEN** `process(sagaData)` is called
- **THEN** a command SHALL be published via SagaCommandPublisher to exchange `fleet.exchange` with routing key `fleet.confirm.command`
- **AND** the payload SHALL contain `vehicleId`, `reservationId`, `pickupDate`, and `returnDate`

#### Scenario: Has no compensation

- **WHEN** `hasCompensation()` is called
- **THEN** it SHALL return false

#### Scenario: Rollback is no-op

- **WHEN** `rollback(sagaData)` is called
- **THEN** nothing SHALL happen (fleet.release.command.queue not exercised by this SAGA)

### Requirement: ReservationSagaOrchestrator coordinates steps

ReservationSagaOrchestrator SHALL be a class in `reservation-application/saga/` that coordinates the 3 steps in order. Its constructor SHALL take 4 dependencies: `List<SagaStep<ReservationSagaData>>` (order: CustomerValidation, Payment, FleetConfirmation), `SagaStateRepository`, `ReservationRepository`, and `ObjectMapper`. It SHALL have no Spring annotations — registered as bean in BeanConfiguration. It does NOT need SagaCommandPublisher (steps encapsulate their own command publishing) or ReservationDomainEventPublisher (SAGA coordination uses commands, not domain events).

#### Scenario: start creates SagaState, transitions to PROCESSING, and executes first step

- **WHEN** `start(ReservationSagaData)` is called
- **THEN** a SagaState SHALL be created with sagaId = sagaData.reservationId, sagaType "RESERVATION_CREATION", totalSteps 3
- **AND** SagaState.beginProcessing() SHALL be called (STARTED → PROCESSING)
- **AND** sagaState SHALL be saved via SagaStateRepository
- **AND** Step[0] (CustomerValidation) SHALL be executed via `process(sagaData)`

#### Scenario: handleStepSuccess advances to next step

- **WHEN** `handleStepSuccess(reservationId, stepName)` is called and there are more steps
- **THEN** SagaState SHALL advance to the next step
- **AND** Reservation SHALL transition to the appropriate state (CUSTOMER_VALIDATED after step 0, PAID after step 1)
- **AND** the next step SHALL be executed via `process(sagaData)`

#### Scenario: handleStepSuccess on last step completes SAGA

- **WHEN** `handleStepSuccess(reservationId, "FLEET_CONFIRMATION")` is called (step 2, the last step)
- **THEN** SagaState SHALL be marked as SUCCEEDED
- **AND** Reservation SHALL transition to CONFIRMED

#### Scenario: handleStepFailure starts compensation

- **WHEN** `handleStepFailure(reservationId, stepName, failureMessages)` is called
- **THEN** SagaState SHALL transition to COMPENSATING with the failure reason
- **AND** Reservation SHALL transition appropriately (initCancel if PAID, cancel if earlier)
- **AND** compensation SHALL start from the step before the failed one, rolling back steps with `hasCompensation() = true`

#### Scenario: handleStepFailure on first step with no compensations

- **WHEN** `handleStepFailure(reservationId, "CUSTOMER_VALIDATION", messages)` is called (step 0 fails)
- **THEN** SagaState SHALL transition PROCESSING → COMPENSATING → FAILED (no actual compensation commands sent since step 0 has no compensation)
- **AND** Reservation SHALL transition to CANCELLED

#### Scenario: handleStepFailure on step 1 (Payment) skips Customer compensation

- **WHEN** `handleStepFailure(reservationId, "PAYMENT", messages)` is called (step 1 fails)
- **THEN** SagaState SHALL transition to COMPENSATING then immediately to FAILED
- **AND** CustomerValidationStep SHALL NOT be rolled back (hasCompensation = false)
- **AND** Reservation SHALL transition to CANCELLED

#### Scenario: handleStepFailure on step 2 (Fleet) compensates only Payment

- **WHEN** `handleStepFailure(reservationId, "FLEET_CONFIRMATION", messages)` is called (step 2 fails)
- **THEN** SagaState SHALL transition to COMPENSATING
- **AND** Reservation SHALL transition to CANCELLING
- **AND** PaymentStep.rollback() SHALL be called (sends refund command)
- **AND** FleetConfirmationStep.rollback() SHALL NOT be called (hasCompensation = false)

#### Scenario: handleCompensationComplete finishes compensation chain

- **WHEN** `handleCompensationComplete(reservationId, "PAYMENT")` is called after Payment refund
- **THEN** the orchestrator SHALL check for more steps to compensate
- **AND** since CustomerValidationStep has no compensation, SagaState SHALL transition to FAILED
- **AND** Reservation SHALL transition to CANCELLED

### Requirement: Orchestrator updates SagaState and Reservation atomically

All orchestrator methods that modify state SHALL be annotated with `@Transactional`. SagaState update, Reservation state transition, and next command publishing (via Outbox) SHALL happen in the same database transaction.

#### Scenario: start is transactional

- **WHEN** `start()` is called
- **THEN** SagaState creation, persistence, and first step command SHALL be in one transaction

#### Scenario: handleStepSuccess is transactional

- **WHEN** `handleStepSuccess()` is called
- **THEN** SagaState advance, Reservation transition, save, and next step command SHALL be in one transaction

#### Scenario: handleStepFailure is transactional

- **WHEN** `handleStepFailure()` is called
- **THEN** SagaState compensation start, Reservation transition, and rollback command SHALL be in one transaction
