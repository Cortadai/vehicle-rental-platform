## 1. POM Dependencies

- [x] 1.1 Add `common-messaging` dependency to `payment-service/payment-infrastructure/pom.xml`
- [x] 1.2 Add `testcontainers-rabbitmq` and `awaitility` test dependencies to `payment-service/payment-container/pom.xml`

## 2. Outbox Event Publisher

- [x] 2.1 Create `OutboxPaymentDomainEventPublisher` in `com.vehiclerental.payment.infrastructure.adapter.output.event` — implements `PaymentDomainEventPublisher`, handles PaymentCompletedEvent/PaymentFailedEvent/PaymentRefundedEvent, derives routing keys, serializes to JSON via ObjectMapper
- [x] 2.2 Delete `PaymentDomainEventPublisherAdapter` (logger no-op)

## 3. RabbitMQ Topology

- [x] 3.1 Create `RabbitMQConfig` in `com.vehiclerental.payment.infrastructure.config` — declares payment.exchange (TopicExchange), dlx.exchange (DirectExchange), 3 queues (completed, failed, refunded) with DLQ routing, payment.dlq, and all bindings
- [x] 3.2 Update `docker/rabbitmq/definitions.json` — add payment.refunded.queue, its binding to payment.exchange, and its DLQ binding to dlx.exchange

## 4. Container Configuration

- [x] 4.1 Modify `PaymentServiceApplication` — add `@SpringBootApplication(scanBasePackages = "com.vehiclerental")`, `@EntityScan(basePackages = "com.vehiclerental")`, `@EnableJpaRepositories(basePackages = "com.vehiclerental")`
- [x] 4.2 Add RabbitMQ connection properties to `application.yml` (`spring.rabbitmq.host/port/username/password` with env var defaults)
- [x] 4.3 Create Flyway migration `V2__create_outbox_events_table.sql` in `payment-service/payment-container/src/main/resources/db/migration/` — same schema as Reservation's V2

## 5. Fix Existing Integration Tests

- [x] 5.1 Add `@Container @ServiceConnection RabbitMQContainer` to `PaymentRepositoryAdapterIT`, `PaymentControllerIT`, and `PaymentServiceApplicationIT` — required because common-messaging puts OutboxPublisher (which needs RabbitTemplate) on the classpath

## 6. New Integration Tests

- [x] 6.1 Create `OutboxAtomicityIT` — verify payment + outbox event persist in same transaction, verify both roll back on domain failure
- [x] 6.2 Create `OutboxPublisherIT` — insert PENDING OutboxEvent, wait for scheduler to publish, verify status PUBLISHED + message in RabbitMQ queue

## 7. Verification

- [x] 7.1 Run `mvn clean verify` from payment-container — all existing + new ITs pass
- [x] 7.2 Verify `PaymentDomainEventPublisherAdapter` no longer exists in the source tree
