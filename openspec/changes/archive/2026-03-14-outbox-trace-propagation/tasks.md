## 1. Database Migration

- [x] 1.1 Create Flyway migration in all 4 services: `ALTER TABLE outbox_events ADD COLUMN trace_parent VARCHAR(256)` — customer V3, fleet V4, reservation V5, payment V4

## 2. Common Messaging Changes

- [x] 2.1 Add `traceParent` field to `OutboxEvent` entity + update `create()` factory method signature (7th param)
- [x] 2.2 Create `TraceContextHelper` utility class with `currentTraceparent(Tracer)` method + add `micrometer-tracing` dependency to common-messaging POM + add unit tests (TraceContextHelperTest)
- [x] 2.3 Update `OutboxPublisher.processEvent()` to inject `traceparent` header from OutboxEvent when not null

## 3. Publisher Updates (capture traceparent)

- [x] 3.1 Update `OutboxReservationDomainEventPublisher` — inject Tracer, pass traceparent to OutboxEvent.create()
- [x] 3.2 Update `OutboxSagaCommandPublisher` — inject Tracer, pass traceparent to OutboxEvent.create()
- [x] 3.3 Update `OutboxCustomerDomainEventPublisher` — inject Tracer, pass traceparent
- [x] 3.4 Update `OutboxFleetDomainEventPublisher` — inject Tracer, pass traceparent
- [x] 3.5 Update `OutboxPaymentDomainEventPublisher` — inject Tracer, pass traceparent

## 4. Verification

- [x] 4.1 Run `mvn test` — all tests pass (fixed OutboxEvent.create() callers in 5 test files + added TraceContextHelperTest for JaCoCo)
- [x] 4.2 Rebuild Docker images + restart platform
- [x] 4.3 Run Bruno E2E happy-path, then check Grafana Tempo — SAGA trace shows 4 services, 7 spans in single trace
- [x] 4.4 Take screenshot of multi-service trace as evidence (07-saga-trace-multi-service.png)

## 5. Additional Fix (discovered during verification)

- [x] 5.1 Enable `spring.rabbitmq.listener.simple.observation-enabled: true` in all 4 application.yml — required for Spring AMQP to extract traceparent from message headers
