## 1. Unify failureMessages serialization to JSON

- [x] 1.1 Modify `ReservationPersistenceMapper` — add `ObjectMapper` constructor parameter, replace `String.join(",")` / `split(",")` with `objectMapper.writeValueAsString()` / `objectMapper.readValue()` using `TypeReference<List<String>>`. Pattern identical to `PaymentPersistenceMapper`.
- [x] 1.2 Modify `BeanConfiguration` in reservation-container — update `reservationPersistenceMapper()` factory method to accept and pass `ObjectMapper` parameter.

## 2. Remove Flyway default-schema from Payment

- [x] 2.1 Modify `application.yml` in payment-container — remove `default-schema: payment` from `spring.flyway` section, keep only `enabled: true`. Aligns with Customer, Fleet, and Reservation which all use `public` schema by default.

## 3. Add OutboxPublisher unit tests

- [x] 3.1 Add `spring-boot-starter-test` dependency (test scope) to `common-messaging/pom.xml`
- [x] 3.2 Create `OutboxPublisherTest` in `common-messaging/src/test/java/com/vehiclerental/common/messaging/outbox/` — 5 unit tests with Mockito: no-op with empty pending list, successful publish marks PUBLISHED, send failure increments retry count, max retries (5) marks FAILED, AMQP message has correct headers (X-Aggregate-Type, X-Aggregate-Id, messageId, contentType)

## 4. Add command queues to RabbitMQ topology

- [x] 4.1 Update `docker/rabbitmq/definitions.json` — add 4 command queues: `customer.validate.command.queue`, `payment.process.command.queue`, `payment.refund.command.queue`, `fleet.confirm.command.queue`. Each with DLQ routing to the receiver service's DLQ.
- [x] 4.2 Add 8 bindings to `definitions.json` — 4 work queue bindings (command queue → receiver's exchange with routing key `{service}.{action}.command`) and 4 DLQ bindings (`dlx.exchange` → receiver's DLQ with routing key `{service}.{action}.command.dlq`)

## 5. Verification

- [x] 5.1 Run `mvn clean verify` from project root — 397 tests pass, BUILD SUCCESS (5 new tests from OutboxPublisherTest added to the previous 392)
