# payment-application-service Specification

## Purpose
Application Service orchestrating all Payment use cases. Implements input ports, delegates business logic to the domain, persists through output ports, dispatches domain events, and routes between complete/fail based on PaymentGateway result. Contains zero business logic — pure orchestration.

## ADDED Requirements

### Requirement: PaymentApplicationService implements all input ports

PaymentApplicationService SHALL implement ProcessPaymentUseCase, RefundPaymentUseCase, and GetPaymentUseCase.

#### Scenario: Service implements all use case interfaces

* **WHEN** PaymentApplicationService is inspected
* **THEN** it SHALL implement all three use case interfaces

### Requirement: Process payment orchestration

The process payment use case SHALL: (1) check idempotency via `findByReservationId` — if a payment already exists, return it as-is; (2) convert command primitives to domain Value Objects; (3) call `Payment.create()`; (4) call `paymentGateway.charge(amount)`; (5) if charge succeeds, call `payment.complete()`; if charge fails, call `payment.fail(failureMessages)`; (6) save via repository; (7) publish domain events from the ORIGINAL aggregate; (8) clear domain events; (9) return PaymentResponse.

#### Scenario: Successful payment processing (charge succeeds)

* **WHEN** `execute(ProcessPaymentCommand)` is called with valid data
* **AND** PaymentGateway.charge() returns success
* **THEN** PaymentRepository.save() SHALL be called with a Payment in COMPLETED status
* **AND** PaymentDomainEventPublisher.publish() SHALL be called with the aggregate's domain events
* **AND** the published events SHALL include a PaymentCompletedEvent
* **AND** a PaymentResponse SHALL be returned with status "COMPLETED"

#### Scenario: Failed payment processing (charge fails)

* **WHEN** `execute(ProcessPaymentCommand)` is called with valid data
* **AND** PaymentGateway.charge() returns failure with messages ["Card declined"]
* **THEN** PaymentRepository.save() SHALL be called with a Payment in FAILED status
* **AND** PaymentDomainEventPublisher.publish() SHALL be called with the aggregate's domain events
* **AND** the published events SHALL include a PaymentFailedEvent
* **AND** a PaymentResponse SHALL be returned with status "FAILED"
* **AND** the response failureMessages SHALL contain "Card declined"

#### Scenario: Idempotent return for existing payment

* **WHEN** `execute(ProcessPaymentCommand)` is called with a reservationId that already has a payment
* **THEN** it SHALL return the existing payment as a PaymentResponse regardless of its status (COMPLETED, FAILED, or REFUNDED)
* **AND** PaymentGateway.charge() SHALL NOT be called
* **AND** PaymentRepository.save() SHALL NOT be called again

#### Scenario: Idempotent return for FAILED payment does not retry charge

* **WHEN** `execute(ProcessPaymentCommand)` is called with a reservationId that has a FAILED payment
* **THEN** it SHALL return the FAILED payment as-is without retrying the charge
* **AND** this is correct because the SAGA creates a new reservation (new reservationId) for retry attempts

#### Scenario: Domain events published from original aggregate

* **WHEN** a payment is processed (create + complete or fail)
* **THEN** events SHALL be published from the ORIGINAL Payment aggregate, not the one returned by save()
* **AND** clearDomainEvents() SHALL be called on the original aggregate after publishing

### Requirement: Refund payment orchestration

The refund use case SHALL load the payment by reservationId via `findByReservationId()`, call `payment.refund()`, persist, publish events from the original aggregate, clear events, and return PaymentResponse.

#### Scenario: Successful refund

* **WHEN** `execute(RefundPaymentCommand)` is called with a reservationId that has a COMPLETED payment
* **THEN** PaymentRepository.save() SHALL be called with a Payment in REFUNDED status
* **AND** PaymentDomainEventPublisher.publish() SHALL be called with the aggregate's domain events
* **AND** the published events SHALL include a PaymentRefundedEvent
* **AND** a PaymentResponse SHALL be returned with status "REFUNDED"

#### Scenario: Payment not found for refund

* **WHEN** `execute(RefundPaymentCommand)` is called with a reservationId that has no payment
* **THEN** it SHALL throw PaymentNotFoundException

### Requirement: Get payment orchestration

The get use case SHALL load the payment by ID and return a PaymentResponse.

#### Scenario: Payment found

* **WHEN** `execute(GetPaymentCommand)` is called with an existing payment ID
* **THEN** a PaymentResponse SHALL be returned with the payment's data

#### Scenario: Payment not found

* **WHEN** `execute(GetPaymentCommand)` is called with a non-existing payment ID
* **THEN** it SHALL throw PaymentNotFoundException

### Requirement: Application Service has no business logic

PaymentApplicationService SHALL NOT contain business logic (validation rules, state transitions, calculations). It SHALL only orchestrate: receive command, convert types, delegate to domain and gateway, persist, publish events, return response.

#### Scenario: Domain handles state transitions

* **WHEN** the refund use case is executed
* **THEN** the state transition logic SHALL be in `Payment.refund()`, not in the Application Service

#### Scenario: Gateway handles charge decision

* **WHEN** the process payment use case is executed
* **THEN** the charge outcome SHALL come from `PaymentGateway.charge()`, not from Application Service logic

### Requirement: PaymentNotFoundException for missing aggregates

PaymentNotFoundException SHALL extend RuntimeException directly. It is an application-level exception, NOT a domain exception. It SHALL carry the identifier that was not found and use a descriptive message. It lives in the application module under `exception/`.

#### Scenario: Exception carries identifier

* **WHEN** a PaymentNotFoundException is thrown for payment ID "abc-123"
* **THEN** `getMessage()` SHALL contain "abc-123"

#### Scenario: Exception is not a domain exception

* **WHEN** PaymentNotFoundException is inspected
* **THEN** it SHALL NOT extend PaymentDomainException or DomainException

### Requirement: PaymentApplicationMapper converts domain to DTOs

PaymentApplicationMapper SHALL be a plain Java class that converts Payment domain objects to PaymentResponse records. It SHALL have no Spring annotations.

#### Scenario: Maps Payment to PaymentResponse

* **WHEN** `toResponse(Payment)` is called
* **THEN** it SHALL return a PaymentResponse with all fields mapped from the Payment aggregate
* **AND** failureMessages SHALL be an empty list if the payment has no failure messages (not null)

### Requirement: Transaction boundaries on write operations

Write use cases (processPayment, refundPayment) SHALL be annotated with `@Transactional`. Read use cases (getPayment) SHALL be annotated with `@Transactional(readOnly = true)`.

#### Scenario: ProcessPayment is transactional

* **WHEN** the processPayment method is inspected
* **THEN** it SHALL be annotated with `@Transactional`

#### Scenario: RefundPayment is transactional

* **WHEN** the refundPayment method is inspected
* **THEN** it SHALL be annotated with `@Transactional`

#### Scenario: GetPayment is read-only transactional

* **WHEN** the getPayment method is inspected
* **THEN** it SHALL be annotated with `@Transactional(readOnly = true)`

### Requirement: Constructor injection with no Spring annotations

PaymentApplicationService SHALL use constructor injection with no `@Autowired` or `@Service` annotations. Bean registration happens in the container module's BeanConfiguration.

#### Scenario: No Spring annotations on class

* **WHEN** PaymentApplicationService is inspected
* **THEN** it SHALL NOT have `@Service`, `@Component`, or `@Autowired` annotations

#### Scenario: Constructor receives all output ports

* **WHEN** PaymentApplicationService is constructed
* **THEN** it SHALL receive PaymentRepository, PaymentDomainEventPublisher, PaymentGateway, and PaymentApplicationMapper via constructor

## Constraints

### Constraint: Minimal Spring dependency

PaymentApplicationService SHALL only depend on `spring-tx` (for `@Transactional`). It SHALL NOT import types from `spring-context`, `spring-web`, `spring-data`, or any other Spring module.
