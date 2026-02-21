## Why

The payment-domain module is complete (Payment Aggregate Root with create/complete/fail/refund, 3 domain events, typed IDs, PaymentRepository port — 51 tests). The domain has no way to be invoked — there are no use cases, commands, or DTOs. The application layer is needed to bridge the domain with the outside world: receive primitive inputs, create the Payment aggregate, delegate charge processing to a gateway port (complete or fail based on gateway result), handle refunds as SAGA compensation, persist through the repository, publish domain events via outbox, and return responses. This is the second of three changes for Payment Service (domain -> **application** -> infrastructure+container).

Unlike Customer and Fleet — which have 5 use cases each for CRUD+lifecycle — Payment has 3 use cases driven by the SAGA flow: process a payment (create + charge via gateway), refund a completed payment (compensation), and query payment status. No use case is invoked by a human user directly via REST in the final system — the SAGA orchestrator will call them — but the application layer contracts are identical regardless of caller.

A key difference from Customer/Fleet is the introduction of a **PaymentGateway output port**. Customer and Fleet operations are deterministic — `suspend()` always works if the state is valid. Payment depends on an **external system** (the payment processor) whose outcome is non-deterministic. The gateway port abstracts this: the Application Service calls `gateway.charge(amount)` and routes to `complete()` or `fail()` based on the result. The infrastructure layer will provide a simulated implementation (always succeeds); SAGA tests can inject a stub that fails. This gives both paths (COMPLETED, FAILED) full test coverage already at the application layer.

## What Changes

### Included

- New Maven module `payment-service/payment-application` with dependency on `payment-domain` and `spring-tx` (for `@Transactional`)
- Root POM updated: add `payment-service/payment-application` to `<modules>` and `payment-application` to `<dependencyManagement>`
- Input port interfaces: one per use case (ProcessPayment, RefundPayment, GetPayment)
- Output port interfaces: `PaymentDomainEventPublisher` for event dispatch after persistence, `PaymentGateway` for charge processing delegation
- `PaymentGateway` interface with `charge(Money amount)` returning `PaymentGatewayResult` record (success boolean + failureMessages list) — decouples the Application Service from any specific payment processor
- Command records: `ProcessPaymentCommand` (reservationId, customerId, amount, currency — all primitives), `RefundPaymentCommand` (reservationId — lookup by reservation), `GetPaymentCommand` (paymentId)
- Response record: `PaymentResponse` (single response type — paymentId, reservationId, customerId, amount, currency, status, failureMessages, createdAt, updatedAt)
- Application Service implementing all input ports — orchestration only, zero business logic
- ProcessPayment flow: idempotency check via `findByReservationId` → `create()` → `paymentGateway.charge(amount)` → if success: `complete()`, if failure: `fail(messages)` → save → publish → clear → response
- Application-level exception for "not found" scenarios (`PaymentNotFoundException`)
- Manual domain-to-DTO mapper (plain Java, no MapStruct)
- Unit tests mocking output ports (repository, event publisher, **gateway**) to verify both happy path (charge succeeds → COMPLETED + PaymentCompletedEvent) and unhappy path (charge fails → FAILED + PaymentFailedEvent)

### Excluded

- Infrastructure layer (JPA entities, REST controllers, Flyway) — separate change `payment-infrastructure-and-container`
- `@Service` / `@Component` annotations — beans registered manually in future container module
- MapStruct — manual mapper is sufficient for the field count
- Real payment gateway integration — infrastructure change will provide `SimulatedPaymentGateway` (always succeeds); a real gateway adapter can replace it later
- SAGA step interface implementation — belongs in the SAGA orchestration change
- Retry logic on payment failure — the SAGA orchestrator handles retries by creating new Payments

## Capabilities

### New Capabilities

- `payment-application-ports`: Input port interfaces (ProcessPaymentUseCase, RefundPaymentUseCase, GetPaymentUseCase) and output ports (PaymentDomainEventPublisher for event dispatch, PaymentGateway for charge processing delegation with PaymentGatewayResult)
- `payment-application-dtos`: Command records (ProcessPaymentCommand, RefundPaymentCommand, GetPaymentCommand) and response record (PaymentResponse) for all Payment use cases
- `payment-application-service`: PaymentApplicationService implementing all input ports with @Transactional, orchestrating process (idempotency check → create → gateway.charge → complete/fail → save → publish → clear → response), refund (find by reservationId → refund → save → publish → clear → response), and get (find by id → not-found check → response) flows

### Modified Capabilities

- `multi-module-build`: Root POM adds `payment-service/payment-application` module declaration and dependencyManagement entry

## Impact

- **New module**: `payment-service/payment-application/` with its own `pom.xml`
- **Root POM**: New module in `<modules>` (after `payment-service/payment-domain`, before `payment-service/payment-infrastructure`) and `payment-application` in `<dependencyManagement>`
- **Dependencies**: `payment-domain` (compile), `spring-tx` (compile), `mockito` + `junit` (test)
- **Pattern consistency**: Follows the same structure as `customer-application`, `fleet-application`, and `reservation-application` — EventPublisher port, command records, manual mapper, NotFoundException in application. Adds PaymentGateway port (unique to Payment — Customer/Fleet don't need external system delegation)
- **Testability**: Both charge paths (success → COMPLETED, failure → FAILED) are testable by mocking the PaymentGateway port, without waiting for SAGA integration
- **No changes to existing code**: payment-domain, common, and all other services remain untouched
