## 1. Module Setup

- [x] 1.1 Add `payment-domain` to root POM `<dependencyManagement>` with `${vehicle-rental.version}` (the stub POM already exists in `<modules>`, verify it is present and ordered after `common`, before `payment-service/payment-infrastructure`)
- [x] 1.2 Update `payment-service/payment-domain/pom.xml` — replace stub with real POM: inherits from root parent, depends on `common`, zero Spring dependencies in compile scope, packaging `jar`
- [x] 1.3 Create directory structure under `payment-service/payment-domain/src/main/java/com/vehiclerental/payment/domain/` with subdirectories: `model/aggregate/`, `model/vo/`, `event/`, `exception/`, `port/output/`
- [x] 1.4 Create test directory structure under `payment-service/payment-domain/src/test/java/com/vehiclerental/payment/domain/` with subdirectories: `model/aggregate/`, `model/vo/`, `event/`, `exception/`
- [x] 1.5 Verify module compiles: `mvn clean compile` from `payment-service/payment-domain/`

## 2. Domain Exception (first — needed by VOs, events, and aggregate)

- [x] 2.1 Create `PaymentDomainExceptionTest.java` — tests for: errorCode accessible, message accessible, constructor with cause, extends DomainException from common
- [x] 2.2 Create `PaymentDomainException.java` — extends DomainException from common, constructors requiring errorCode

## 3. Value Objects and Enum (Test-First)

- [x] 3.1 Create `PaymentIdTest.java` — tests for valid construction, null UUID rejected (throws PaymentDomainException), equality by value
- [x] 3.2 Create `PaymentId.java` — record wrapping UUID, null validation in compact constructor
- [x] 3.3 Create `ReservationIdTest.java` — tests for valid construction, null UUID rejected, equality by value (local to payment bounded context, NOT imported from reservation-domain)
- [x] 3.4 Create `ReservationId.java` — record wrapping UUID, local to payment domain
- [x] 3.5 Create `CustomerIdTest.java` — tests for valid construction, null UUID rejected, equality by value (local to payment bounded context, NOT imported from customer-domain)
- [x] 3.6 Create `CustomerId.java` — record wrapping UUID, local to payment domain
- [x] 3.7 Create `PaymentStatus.java` — enum with PENDING, COMPLETED, FAILED, REFUNDED

## 4. Domain Events (Test-First)

- [x] 4.1 Create `PaymentDomainEventsTest.java` — tests for: PaymentCompletedEvent fields accessible (paymentId, reservationId, customerId, amount), null eventId rejected, null occurredOn rejected; PaymentFailedEvent fields accessible (paymentId, reservationId, failureMessages); PaymentRefundedEvent fields accessible (paymentId, reservationId, amount); all events implement DomainEvent; events are records
- [x] 4.2 Create `PaymentCompletedEvent.java` — record implementing DomainEvent with eventId, occurredOn, paymentId, reservationId, customerId, amount; null validation for eventId and occurredOn
- [x] 4.3 Create `PaymentFailedEvent.java` — record implementing DomainEvent with eventId, occurredOn, paymentId, reservationId, failureMessages; null validation for eventId and occurredOn
- [x] 4.4 Create `PaymentRefundedEvent.java` — record implementing DomainEvent with eventId, occurredOn, paymentId, reservationId, amount; null validation for eventId and occurredOn

## 5. Payment Aggregate Root (Test-First)

- [x] 5.1 Create `PaymentTest.java` — tests for: successful creation (status PENDING, PaymentId generated, NO domain event emitted), null reservationId rejected (PAYMENT_RESERVATION_ID_REQUIRED), null customerId rejected (PAYMENT_CUSTOMER_ID_REQUIRED), null amount rejected (PAYMENT_AMOUNT_REQUIRED), zero amount rejected (PAYMENT_AMOUNT_INVALID), fields accessible after creation, reconstruct does not emit events, reconstruct preserves failureMessages, no public constructors
- [x] 5.2 Create `PaymentLifecycleTest.java` — tests for: complete from PENDING (COMPLETED + PaymentCompletedEvent emitted + updatedAt updated), complete from COMPLETED throws PAYMENT_INVALID_STATE, complete from FAILED throws, complete from REFUNDED throws; fail from PENDING with messages (FAILED + failureMessages stored + PaymentFailedEvent emitted + updatedAt updated), fail from COMPLETED throws, fail from FAILED throws, fail from REFUNDED throws, fail with null messages rejected (PAYMENT_FAILURE_MESSAGES_REQUIRED), fail with empty messages rejected (PAYMENT_FAILURE_MESSAGES_REQUIRED); refund from COMPLETED (REFUNDED + PaymentRefundedEvent emitted + updatedAt updated), refund from PENDING throws, refund from FAILED throws, refund from REFUNDED throws
- [x] 5.3 Create `Payment.java` — Aggregate Root extending AggregateRoot<PaymentId>, private constructor, `create(ReservationId, CustomerId, Money)` factory method (generates PaymentId, sets PENDING, validates amount > 0, NO event), `reconstruct(PaymentId, ReservationId, CustomerId, Money, PaymentStatus, List<String>, Instant, Instant)` factory method, `complete()` (PENDING→COMPLETED, registers PaymentCompletedEvent), `fail(List<String>)` (PENDING→FAILED, stores messages, registers PaymentFailedEvent), `refund()` (COMPLETED→REFUNDED, registers PaymentRefundedEvent), getters only

## 6. Output Port

- [x] 6.1 Create `PaymentRepository.java` — interface with `save(Payment)` returning Payment, `findById(PaymentId)` returning `Optional<Payment>`, and `findByReservationId(ReservationId)` returning `Optional<Payment>`, domain types only

## 7. Verification

- [x] 7.1 Run `mvn clean install` from `payment-service/payment-domain/` — compile + all tests pass
- [x] 7.2 Verify zero Spring imports: grep `org.springframework` in `payment-service/payment-domain/src/main/` returns nothing
- [x] 7.3 Verify zero imports from other domains: grep `com.vehiclerental.customer`, `com.vehiclerental.fleet`, and `com.vehiclerental.reservation` in `payment-service/payment-domain/src/main/` returns nothing
- [x] 7.4 Run `mvn clean install` from root — full platform build passes
