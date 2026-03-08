## Why

The 3 participant services (Customer, Fleet, Payment) can receive SAGA commands and publish response events, but there is no orchestrator coordinating the flow. Reservation Service creates reservations in PENDING status and publishes ReservationCreatedEvent, but nothing drives the state machine forward — all 7 response queues exist in RabbitMQ with no consumers. This change builds the central brain that ties the entire SAGA together.

## What Changes

- Add `SagaState` domain object with state machine (STARTED → PROCESSING → SUCCEEDED/COMPENSATING → FAILED), persisted via separate JPA entity in a new `saga_state` table
- Steps execute in order: Customer validation → Payment processing → Fleet confirmation. Compensation runs in reverse: Fleet release (no-op) → Payment refund → Customer rollback (no-op)
- Add `SagaStep<T>` interface and 3 concrete steps: CustomerValidationStep, PaymentStep, FleetConfirmationStep — each knows how to send its command and (if applicable) its compensation command
- Add `ReservationSagaOrchestrator` in application layer — coordinates steps sequentially, handles success/failure responses, drives compensation in reverse order
- Add `SagaCommandPublisher` output port with Outbox-based implementation — steps send commands through this port, never touching JPA directly
- Add 7 `@RabbitListener` response listeners in Reservation Service infrastructure that consume from the existing response queues and delegate to the orchestrator
- Extend `RabbitMQConfig` in Reservation Service to declare bindings for the 7 response queues
- Modify `CreateReservationUseCase` to start the SAGA after creating the reservation (ReservationCreatedEvent still published as before)
- Add `V3__create_saga_state_table.sql` Flyway migration
- Add SAGA integration tests with Testcontainers + Awaitility for happy path and compensation flows

## Capabilities

### New Capabilities
- `saga-state`: SagaState domain object, SagaStatus enum with transition rules, persistence via port/adapter pattern
- `saga-orchestration`: ReservationSagaOrchestrator, SagaStep interface, 3 concrete steps, ReservationSagaData, SagaCommandPublisher port
- `saga-response-listeners`: 7 RabbitMQ response listeners in Reservation Service that consume participant responses and feed the orchestrator
- `saga-command-publishing`: SagaCommandPublisher output port and its Outbox-based infrastructure implementation

### Modified Capabilities
- `reservation-application-service`: CreateReservationUseCase now also starts the SAGA orchestration after creating the reservation
- `reservation-rabbitmq-topology`: Extended with bindings for 7 response queues consumed by the orchestrator

## Impact

- **Reservation Service**: Major addition — new domain objects (saga/), new application layer (saga/), 7 new listeners, new persistence adapter, extended RabbitMQ config, modified BeanConfiguration, new Flyway migration
- **Customer/Fleet/Payment Services**: No changes — they already publish the response events this change consumes
- **Database**: New `saga_state` table in reservation schema with optimistic locking (`version` column)
- **RabbitMQ**: No new queues or exchanges — all 7 response queues already exist in definitions.json. Only new queue declarations and bindings in Reservation's RabbitMQConfig
- **API**: No REST API changes. Existing POST/GET endpoints unchanged
- **Testing**: New SAGA integration tests (happy path, customer rejection, payment failure, fleet rejection with compensation). Existing unit + integration tests unaffected
