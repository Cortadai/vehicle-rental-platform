## 1. RabbitMQConfig — Command Queues

- [x] 1.1 Add `payment.process.command.queue` bean with DLQ routing to `dlx.exchange` (routing key `payment.process.command.dlq`)
- [x] 1.2 Add `payment.refund.command.queue` bean with DLQ routing to `dlx.exchange` (routing key `payment.refund.command.dlq`)
- [x] 1.3 Add binding: `payment.process.command.queue` → `payment.exchange` with routing key `payment.process.command`
- [x] 1.4 Add binding: `payment.refund.command.queue` → `payment.exchange` with routing key `payment.refund.command`
- [x] 1.5 Add DLQ binding: `payment.dlq` → `dlx.exchange` with routing key `payment.process.command.dlq`
- [x] 1.6 Add DLQ binding: `payment.dlq` → `dlx.exchange` with routing key `payment.refund.command.dlq`

## 2. SAGA Listeners

- [x] 2.1 Create `PaymentProcessListener` in `com.vehiclerental.payment.infrastructure.adapter.input.messaging` — `@RabbitListener` on `payment.process.command.queue`, parses JSON (reservationId, customerId, amount, currency), calls `processPaymentUseCase.execute()`
- [x] 2.2 Create `PaymentRefundListener` in `com.vehiclerental.payment.infrastructure.adapter.input.messaging` — `@RabbitListener` on `payment.refund.command.queue`, parses JSON (reservationId), calls `refundPaymentUseCase.execute()`

## 3. Integration Tests

- [x] 3.1 Create `PaymentProcessListenerIT` — send message to `payment.process.command.queue`, verify Payment created in DB with correct status and outbox event published
- [x] 3.2 Create `PaymentRefundListenerIT` — create a Payment first, send refund message, verify Payment refunded in DB and outbox event published

## 4. Verification

- [x] 4.1 Run `mvn test` — all unit tests pass (no changes expected, but verify no regressions)
- [x] 4.2 Run `mvn verify` — all integration tests pass including new listener ITs
