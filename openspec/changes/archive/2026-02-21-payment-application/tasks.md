## 1. Module Setup

- [x] 1.1 Add `payment-service/payment-application` module to root POM `<modules>` (after `payment-service/payment-domain`, before `payment-service/payment-infrastructure`) and add `payment-application` to `<dependencyManagement>` with `${vehicle-rental.version}`
- [x] 1.2 Create `payment-service/payment-application/pom.xml` inheriting from root parent, with dependencies: `payment-domain` (compile), `spring-tx` (compile), `junit-jupiter` + `mockito-core` + `mockito-junit-jupiter` (test)
- [x] 1.3 Create directory structure: `src/main/java/com/vehiclerental/payment/application/{port/{input,output},dto/{command,response},service,mapper,exception}` and `src/test/java/com/vehiclerental/payment/application/{dto,mapper,exception,service}`
- [x] 1.4 Run `mvn compile -pl payment-service/payment-application` to verify module compiles with empty sources
- [x] 1.5 Run `mvn compile` from root to verify full platform builds with the new module

## 2. DTOs (Commands and Responses)

- [x] 2.1 Create `ProcessPaymentCommand` record with fields: `reservationId` (String), `customerId` (String), `amount` (BigDecimal), `currency` (String)
- [x] 2.2 Create `RefundPaymentCommand` record with single field: `reservationId` (String)
- [x] 2.3 Create `GetPaymentCommand` record with single field: `paymentId` (String)
- [x] 2.4 Create `PaymentResponse` record with fields: `paymentId` (String), `reservationId` (String), `customerId` (String), `amount` (BigDecimal), `currency` (String), `status` (String), `failureMessages` (List<String>), `createdAt` (Instant), `updatedAt` (Instant)

## 3. Exception

- [x] 3.1 Write `PaymentNotFoundExceptionTest` — verify extends RuntimeException (not DomainException), message contains the identifier, and both constructor forms work
- [x] 3.2 Implement `PaymentNotFoundException` extending RuntimeException with constructor `(String identifier)` and descriptive message containing the identifier

## 4. Output Ports

- [x] 4.1 Create `PaymentDomainEventPublisher` interface in `port/output/` with `void publish(List<DomainEvent> events)` — zero Spring imports
- [x] 4.2 Create `PaymentGateway` interface in `port/output/` with `PaymentGatewayResult charge(Money amount)` — zero Spring imports
- [x] 4.3 Create `PaymentGatewayResult` record in `port/output/` with fields: `success` (boolean), `failureMessages` (List<String>)

## 5. Input Ports

- [x] 5.1 Create `ProcessPaymentUseCase` interface in `port/input/` with `PaymentResponse execute(ProcessPaymentCommand command)`
- [x] 5.2 Create `RefundPaymentUseCase` interface in `port/input/` with `PaymentResponse execute(RefundPaymentCommand command)`
- [x] 5.3 Create `GetPaymentUseCase` interface in `port/input/` with `PaymentResponse execute(GetPaymentCommand command)`

## 6. Mapper

- [x] 6.1 Write `PaymentApplicationMapperTest` — verify `toResponse(Payment)` maps all 9 fields correctly, verify failureMessages is empty list (not null) for non-FAILED payments
- [x] 6.2 Implement `PaymentApplicationMapper` as plain Java class with `toResponse(Payment)` — no Spring annotations

## 7. Application Service

- [x] 7.1 Write `PaymentApplicationServiceTest` with mocked output ports (PaymentRepository, PaymentDomainEventPublisher, PaymentGateway) covering: processPayment happy path (charge succeeds → COMPLETED + PaymentCompletedEvent), processPayment charge fails (→ FAILED + PaymentFailedEvent), processPayment idempotent return (existing payment returned as-is), refundPayment happy path (COMPLETED → REFUNDED + PaymentRefundedEvent), refundPayment not found (throws PaymentNotFoundException), getPayment happy path, getPayment not found (throws PaymentNotFoundException), events published from original aggregate (not from save result) — mock `repository.save()` to return a `Payment.reconstruct(...)` with empty events so the test distinguishes original vs saved; use `ArgumentCaptor` on `publish()` to verify events come from the original aggregate, clearDomainEvents called after publish
- [x] 7.2 Implement `PaymentApplicationService` implementing ProcessPaymentUseCase, RefundPaymentUseCase, GetPaymentUseCase — constructor injection (repository, eventPublisher, gateway, mapper), `@Transactional` on write methods, `@Transactional(readOnly = true)` on get, no `@Service`/`@Component`/`@Autowired`

## 8. Verification

- [x] 8.1 Run `mvn test -pl payment-service/payment-application` — all tests pass
- [x] 8.2 Verify zero `@Service`, `@Component`, `@Autowired` annotations in main sources via grep
- [x] 8.3 Verify only `spring-tx` Spring import in main sources (no spring-context, spring-web, spring-data) via grep
- [x] 8.4 Run `mvn clean install` from root — full platform builds with all modules
