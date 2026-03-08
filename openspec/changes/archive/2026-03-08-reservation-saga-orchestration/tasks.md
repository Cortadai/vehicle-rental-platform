## 1. Domain Layer — SagaState and SagaStatus

- [x] 1.1 Create `SagaStatus` enum in `reservation-domain/model/saga/` with values STARTED, PROCESSING, COMPENSATING, SUCCEEDED, FAILED and `canTransitionTo()` validation
- [x] 1.2 Create `SagaState` domain object in `reservation-domain/model/saga/` — pure Java, factory methods `create()` and `reconstruct()`, state transition methods (`beginProcessing`, `advanceToNextStep`, `markAsSucceeded`, `startCompensation`, `decrementStep`, `markAsFailed`)
- [x] 1.3 Create `SagaStateRepository` output port interface in `reservation-domain/port/output/`
- [x] 1.4 Write unit tests for `SagaStatus` transitions (valid and invalid)
- [x] 1.5 Write unit tests for `SagaState` — create, reconstruct, all state transitions, rejection of invalid transitions

## 2. Infrastructure Layer — SagaState Persistence

- [x] 2.1 Create `V3__create_saga_state_table.sql` Flyway migration in `reservation-container/resources/db/migration/`
- [x] 2.2 Create `SagaStateJpaEntity` in `reservation-infrastructure/adapter/output/persistence/saga/` with `@Entity`, `@Table(name = "saga_state")`, `@Version` for optimistic locking
- [x] 2.3 Create `SagaStateJpaRepository` (Spring Data JPA interface)
- [x] 2.4 Create `SagaStatePersistenceMapper` — domain SagaState ↔ SagaStateJpaEntity conversion
- [x] 2.5 Create `SagaStateRepositoryAdapter` implementing `SagaStateRepository` — delegates to JPA repo via mapper

## 3. Application Layer — SagaStep Interface and SagaCommandPublisher Port

- [x] 3.1 Create `SagaStep<T>` interface in `reservation-application/saga/` — `process(T)`, `rollback(T)`, `getName()`, `hasCompensation()`
- [x] 3.2 Create `ReservationSagaData` record in `reservation-application/saga/` — reservationId, customerId, vehicleId, totalAmount, currency, pickupDate, returnDate
- [x] 3.3 Create `SagaCommandPublisher` output port interface in `reservation-application/port/output/`

## 4. Infrastructure Layer — SagaCommandPublisher Implementation

- [x] 4.1 Create `OutboxSagaCommandPublisher` in `reservation-infrastructure/adapter/output/messaging/saga/` — implements SagaCommandPublisher, writes OutboxEvent with aggregateType "SAGA"

## 5. Application Layer — Saga Steps

- [x] 5.1 Create `CustomerValidationStep` — process sends to `customer.exchange` / `customer.validate.command`, hasCompensation = false, rollback is no-op
- [x] 5.2 Create `PaymentStep` — process sends to `payment.exchange` / `payment.process.command`, rollback sends to `payment.exchange` / `payment.refund.command`, hasCompensation = true
- [x] 5.3 Create `FleetConfirmationStep` — process sends to `fleet.exchange` / `fleet.confirm.command`, hasCompensation = false, rollback is no-op
- [x] 5.4 Write unit tests for all 3 steps — verify correct exchange, routing key, payload fields, and hasCompensation value

## 6. Application Layer — ReservationSagaOrchestrator

- [x] 6.1 Create `ReservationSagaOrchestrator` in `reservation-application/saga/` — constructor takes List<SagaStep>, SagaStateRepository, ReservationRepository, ObjectMapper
- [x] 6.2 Implement `start(ReservationSagaData)` — create SagaState, beginProcessing, save, execute Step[0]
- [x] 6.3 Implement `handleStepSuccess(UUID reservationId, String stepName)` — load SagaState + Reservation, advance step, transition Reservation state, execute next step or mark SUCCEEDED
- [x] 6.4 Implement `handleStepFailure(UUID reservationId, String stepName, List<String> failureMessages)` — start compensation, transition Reservation (initCancel or cancel), rollback steps with hasCompensation=true in reverse order, skip no-ops
- [x] 6.5 Implement `handleCompensationComplete(UUID reservationId, String stepName)` — continue compensation chain or mark FAILED, transition Reservation to CANCELLED when done
- [x] 6.6 Write unit tests for orchestrator — mock SagaStateRepository, ReservationRepository, Steps. Test: happy path (3 successes → CONFIRMED), customer rejection (→ CANCELLED, no compensations), payment failure (→ CANCELLED, no compensations), fleet rejection (→ CANCELLING → refund → CANCELLED)

## 7. Infrastructure Layer — RabbitMQ Configuration

- [x] 7.1 Extend `RabbitMQConfig` in reservation-infrastructure — declare 3 participant exchanges (customer.exchange, payment.exchange, fleet.exchange) as TopicExchange beans
- [x] 7.2 Declare 7 response queues with DLQ routing (customer.validated, customer.rejected, payment.completed, payment.failed, payment.refunded, fleet.confirmed, fleet.rejected)
- [x] 7.3 Declare 7 bindings — each response queue bound to its participant exchange with appropriate routing key

## 8. Infrastructure Layer — Response Listeners

- [x] 8.1 Create `CustomerValidatedResponseListener` — listens on `customer.validated.queue`, parses reservationId, calls orchestrator.handleStepSuccess
- [x] 8.2 Create `CustomerRejectedResponseListener` — listens on `customer.rejected.queue`, parses reservationId + failureMessages, calls orchestrator.handleStepFailure
- [x] 8.3 Create `PaymentCompletedResponseListener` — listens on `payment.completed.queue`, calls orchestrator.handleStepSuccess
- [x] 8.4 Create `PaymentFailedResponseListener` — listens on `payment.failed.queue`, calls orchestrator.handleStepFailure
- [x] 8.5 Create `PaymentRefundedResponseListener` — listens on `payment.refunded.queue`, calls orchestrator.handleCompensationComplete
- [x] 8.6 Create `FleetConfirmedResponseListener` — listens on `fleet.confirmed.queue`, calls orchestrator.handleStepSuccess
- [x] 8.7 Create `FleetRejectedResponseListener` — listens on `fleet.rejected.queue`, calls orchestrator.handleStepFailure

## 9. Container Layer — Wiring

- [x] 9.1 Update `BeanConfiguration` — register SagaStatePersistenceMapper, SagaStateRepositoryAdapter, all 3 SagaSteps, ReservationSagaOrchestrator as beans
- [x] 9.2 Update `ReservationApplicationService` constructor — add ReservationSagaOrchestrator as 4th dependency
- [x] 9.3 Update `CreateReservationUseCase.execute()` — build ReservationSagaData from saved reservation, call `sagaOrchestrator.start(sagaData)` after event publishing

## 10. Integration Tests

- [x] 10.1 Create `SagaStateRepositoryAdapterIT` — verify save, findById, optimistic locking with Testcontainers PostgreSQL + RabbitMQ
- [x] 10.2 Create `ReservationSagaHappyPathIT` — full flow: POST reservation → Customer validated → Payment completed → Fleet confirmed → verify CONFIRMED status. Uses Testcontainers + Awaitility
- [x] 10.3 Create `ReservationSagaCustomerRejectionIT` — POST reservation → Customer rejected → verify CANCELLED, no compensation commands sent
- [x] 10.4 Create `ReservationSagaPaymentFailureIT` — POST reservation → Customer validated → Payment failed → verify CANCELLED, no compensation commands sent
- [x] 10.5 Create `ReservationSagaFleetRejectionIT` — POST reservation → Customer validated → Payment completed → Fleet rejected → Payment refunded → verify CANCELLED with compensation flow

## 11. Verify Existing Tests

- [x] 11.1 Run full `mvn verify` — ensure all existing 69 unit + 18 integration tests still pass with the new dependencies and modified CreateReservationUseCase
