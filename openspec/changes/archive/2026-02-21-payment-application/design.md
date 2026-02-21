## Context

The payment-domain module is complete with: Payment aggregate root (create, reconstruct, complete, fail, refund — 4 states: PENDING, COMPLETED, FAILED, REFUNDED), typed Value Objects (PaymentId, ReservationId, CustomerId — all local to the bounded context), PaymentStatus enum, 3 domain events (PaymentCompletedEvent, PaymentFailedEvent, PaymentRefundedEvent), PaymentDomainException with error codes, Money reused from common, and PaymentRepository output port (save, findById, findByReservationId). The domain has 51 passing tests and zero Spring dependencies.

Customer-application (17 tests, 13 classes), fleet-application (17 tests, 13 classes), and reservation-application (18 tests, 10 classes) established the application layer pattern: one input port per use case (ISP), command/response records with primitives, a DomainEventPublisher output port, a single Application Service implementing all input ports with `@Transactional`, manual mapper, and NotFoundException extending RuntimeException.

Payment's application layer differs from Customer/Fleet/Reservation in one fundamental way: it introduces a **PaymentGateway output port** to delegate charge processing to an external system. Customer and Fleet operations are deterministic — `suspend()` always works if the state is valid. Payment depends on a non-deterministic external system (the payment processor), so the Application Service needs a seam to route between `complete()` and `fail()` based on the gateway result. This gives both paths full test coverage at the application layer by mocking the gateway.

## Goals / Non-Goals

**Goals:**

- Replicate the application layer pattern established by customer/fleet/reservation-application
- Define input ports: ProcessPaymentUseCase, RefundPaymentUseCase, GetPaymentUseCase (one interface per use case)
- Define output ports: PaymentDomainEventPublisher for event dispatch, PaymentGateway for charge processing delegation
- Add PaymentGatewayResult record as the return type for PaymentGateway.charge() — carries success boolean + failure messages
- Add command records with primitives: ProcessPaymentCommand (reservationId, customerId, amount, currency), RefundPaymentCommand (reservationId), GetPaymentCommand (paymentId)
- Add PaymentResponse as single response type for all use cases (paymentId, reservationId, customerId, amount, currency, status, failureMessages, createdAt, updatedAt)
- Implement PaymentApplicationService as pure orchestrator with idempotency check, gateway delegation, save → publish → clearDomainEvents cycle
- Add PaymentApplicationMapper (manual, plain Java) for domain ↔ DTO translation
- Add PaymentNotFoundException for "not found" scenarios (application-level exception)
- Unit test the application service with mocked output ports (repository, event publisher, gateway), covering both charge success and failure paths
- Update root POM to include payment-application module

**Non-Goals:**

- Infrastructure layer (JPA entities, REST controllers, Flyway) — separate change `payment-infrastructure-and-container`
- `@Service` / `@Component` annotations — beans registered manually in future container module
- SimulatedPaymentGateway implementation — belongs in infrastructure (always returns success)
- Real payment gateway integration — out of scope for POC
- SAGA step interface implementation — belongs in the SAGA orchestration change
- Retry logic on payment failure — the SAGA orchestrator handles retries by creating new Payments
- MapStruct mapper — manual mapper is sufficient for the field count
- Input validation on commands — validation happens in domain Value Objects (PaymentId, ReservationId, CustomerId constructors) and aggregate factory method (amount > 0)
- Relocating PaymentRepository — the output port stays in domain where it was placed in payment-domain change

## Decisions

### Decision 1: PaymentGateway output port — the key difference from Customer/Fleet/Reservation

**Choice**: Define `PaymentGateway` interface in `application/port/output/` with `PaymentGatewayResult charge(Money amount)`. The Application Service calls `gateway.charge(amount)` after `create()` and routes to `complete()` or `fail(messages)` based on the result.

**Rationale**: Customer/Fleet operations are deterministic (state transitions always succeed if preconditions are met). Payment depends on an external payment processor whose outcome is non-deterministic. Without the gateway port, the Application Service would either: (a) always call `complete()` with no way to test the failure path, or (b) embed a `simulateFailure` flag in the command, leaking test infrastructure into the application contract. The gateway port is the hexagonal solution: the Application Service depends on an abstraction, and the implementation (simulated, real Stripe, test stub) is injected at the infrastructure/container level.

**Alternative rejected**: `simulateFailure` flag in ProcessPaymentCommand. Pollutes the domain contract with testing concerns. Also rejected: always calling `complete()` — would leave `fail()` untested until SAGA integration.

### Decision 2: PaymentGatewayResult as a record, not a boolean

**Choice**: `PaymentGatewayResult(boolean success, List<String> failureMessages)` record returned by `PaymentGateway.charge()`.

**Rationale**: A boolean would suffice for success/failure, but the domain's `fail(List<String> failureMessages)` requires failure reasons. The gateway result must carry these messages so the Application Service can pass them to the aggregate. This mirrors how a real payment processor returns decline reasons ("Card declined", "Insufficient funds"). The record lives alongside the gateway interface in `port/output/`.

**Alternative rejected**: Boolean return + separate `getLastFailureMessages()` method on the gateway. Stateful, thread-unsafe, anti-pattern.

### Decision 3: ProcessPayment includes idempotency check via findByReservationId

**Choice**: Before calling `Payment.create()`, the Application Service calls `paymentRepository.findByReservationId(reservationId)`. If a payment already exists, it returns the existing payment as-is regardless of its status (PENDING, COMPLETED, FAILED, or REFUNDED) instead of creating a duplicate.

**Rationale**: The SAGA orchestrator may retry the ProcessPayment command if it doesn't receive a response (network timeout, crash recovery). Without idempotency, a retry would create a second payment for the same reservation. This check is an application-layer orchestration concern (as stated in the payment-domain design), not a domain invariant. `findByReservationId` was added to the domain's PaymentRepository port specifically for this purpose.

**Note on FAILED payments**: The idempotency check returns a FAILED payment as-is without retrying the charge. This is correct because the SAGA orchestrator does not retry the same `reservationId` — it creates an entirely new reservation (with a new `reservationId`) for retry attempts. Therefore, a FAILED payment for a given `reservationId` is a terminal state. If this behavior needs to evolve (e.g., retry with the same `reservationId`), the idempotency check would need to filter by status or allow re-processing of FAILED payments.

**Alternative rejected**: Unique constraint in database. Would throw an infrastructure exception instead of returning the existing payment gracefully. Both mechanisms can coexist (belt + suspenders), but the application-level check provides a better developer experience.

### Decision 4: RefundPaymentCommand carries reservationId, not paymentId

**Choice**: `RefundPaymentCommand(String reservationId)`. The Application Service uses `findByReservationId()` to locate the payment, then calls `refund()`.

**Rationale**: The SAGA orchestrator knows which reservation triggered the compensation flow, but doesn't necessarily track the internal paymentId. Using reservationId as the lookup key aligns with the compensation flow: "refund the payment for reservation X". This is consistent with the idempotency design — reservationId is the correlation key throughout the SAGA.

**Alternative rejected**: `RefundPaymentCommand(String paymentId)`. Would require the SAGA to track paymentId from the ProcessPayment response, adding unnecessary coupling.

### Decision 5: Single PaymentResponse for all use cases — no CQS split

**Choice**: All three use cases return `PaymentResponse` with the same fields (paymentId, reservationId, customerId, amount, currency, status, failureMessages, createdAt, updatedAt).

**Rationale**: Unlike Reservation — where create returns a lean response (trackingId + status) and track returns a full snapshot (14 fields + items) — Payment has no inner entities and no field asymmetry between use cases. A single response type with ~9 fields is sufficient. The caller always needs the same information: "What happened with this payment?"

**Alternative rejected**: Separate `ProcessPaymentResponse` and `GetPaymentResponse`. Would be identical in structure — pure duplication.

### Decision 6: PaymentRepository stays in domain

**Choice**: Leave `PaymentRepository` in `payment-domain/port/output/`. The application module depends on payment-domain and uses the port directly.

**Rationale**: Same decision as customer/fleet/reservation-application. The repository interface uses only domain types (`Payment`, `PaymentId`, `ReservationId`). Consistent with the established pattern.

**Alternative rejected**: Move to `payment-application/port/output/`. Unnecessary — the interface is framework-free regardless of location.

### Decision 7: One Application Service implementing all input ports

**Choice**: A single `PaymentApplicationService` implements `ProcessPaymentUseCase`, `RefundPaymentUseCase`, and `GetPaymentUseCase`.

**Rationale**: Three use cases with straightforward orchestration don't justify separate service classes. Each use case has its own interface (ISP), so extraction is trivial if needed. When SAGA steps are added, they'll likely use the same use case interfaces — the SAGA listener will call `ProcessPaymentUseCase.execute()`.

**Alternative rejected**: One service class per use case. Over-engineering for 3 methods.

### Decision 8: ProcessPaymentCommand carries primitives

**Choice**: `ProcessPaymentCommand(String reservationId, String customerId, BigDecimal amount, String currency)`. The Application Service converts to domain types: `new ReservationId(UUID.fromString(...))`, `new CustomerId(UUID.fromString(...))`, `new Money(amount, Currency.getInstance(currency))`.

**Rationale**: Same boundary principle as all other services. Commands carry primitives from the outside world, the application layer translates to typed domain objects. BigDecimal for amount (not String) because it's already a precise numeric type — parsing it from String would add unnecessary conversion.

**Alternative rejected**: Commands with domain types (ReservationId, CustomerId, Money). Would leak domain types into infrastructure.

### Decision 9: PaymentNotFoundException extends RuntimeException

**Choice**: `PaymentNotFoundException` extends `RuntimeException` directly, NOT `PaymentDomainException`. Lives in the application module under `exception/`. Carries the identifier as a String for diagnostic purposes.

**Rationale**: Same reasoning as customer/fleet/reservation-application. "Not found" is an application-level concern. Allows GlobalExceptionHandler to map: `PaymentDomainException → 422`, `PaymentNotFoundException → 404`.

**Alternative rejected**: Extend `PaymentDomainException`. Conflates domain invariant violations with application-level errors.

### Decision 10: Manual mapper, no MapStruct

**Choice**: `PaymentApplicationMapper` is a plain Java class with `toResponse(Payment)`. Maps ~9 fields.

**Rationale**: Same reasoning as all other services. Manual mapping is ~15 lines. MapStruct overhead not justified.

### Decision 11: Single test file for Application Service

**Choice**: `PaymentApplicationServiceTest` covering all 3 use cases in one file, plus `PaymentApplicationMapperTest` and `PaymentNotFoundExceptionTest`.

**Rationale**: Unlike Reservation (which split tests by use case due to create's complexity with items), Payment's use cases are compact. ProcessPayment has ~5 tests (happy path, charge fails, idempotent return, null checks), RefundPayment ~3 tests (happy path, not found, invalid state), GetPayment ~2 tests (happy path, not found). Total ~10-12 tests in one file is manageable. If the gateway delegation adds complexity, splitting is still possible.

**Alternative rejected**: Split by use case. Unnecessary for the expected test count.

## Package Structure

```
payment-service/payment-application/src/main/java/com/vehiclerental/payment/application/
├── port/
│   ├── input/
│   │   ├── ProcessPaymentUseCase.java
│   │   ├── RefundPaymentUseCase.java
│   │   └── GetPaymentUseCase.java
│   └── output/
│       ├── PaymentDomainEventPublisher.java
│       ├── PaymentGateway.java
│       └── PaymentGatewayResult.java
├── dto/
│   ├── command/
│   │   ├── ProcessPaymentCommand.java
│   │   ├── RefundPaymentCommand.java
│   │   └── GetPaymentCommand.java
│   └── response/
│       └── PaymentResponse.java
├── service/
│   └── PaymentApplicationService.java
├── mapper/
│   └── PaymentApplicationMapper.java
└── exception/
    └── PaymentNotFoundException.java
```

## Risks / Trade-offs

- **spring-tx dependency in application** — Same trade-off as all other services. `@Transactional` is a single annotation import. Pragmatic choice over a TransactionPort abstraction.
- **PaymentGateway adds an output port not present in other services** — This is a genuine architectural addition, not boilerplate. It introduces a new dependency to mock in tests and a new bean to wire in the container module. Mitigation: the gateway interface is minimal (one method), and the simulated implementation will be ~5 lines.
- **Idempotency via findByReservationId is application-level only** — A concurrent race between two ProcessPayment calls could still create duplicates if both pass the check simultaneously. Mitigation: the infrastructure layer should add a unique constraint on reservation_id in the payments table as belt-and-suspenders. The application-level check handles the normal case gracefully.
- **Single PaymentResponse for all use cases** — If use cases diverge in the future (e.g., ProcessPayment needs to return gateway transaction details), a split may be needed. Mitigation: the response is returned through typed use case interfaces, so adding a new response type is backwards-compatible.
- **RefundPayment by reservationId assumes one payment per reservation** — If the business allows multiple payments (partial payments, retry with new payment), this lookup would need to change. Mitigation: the SAGA flow creates one payment per reservation attempt (documented in payment-domain design Decision 6).
