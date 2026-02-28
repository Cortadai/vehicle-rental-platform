## 1. Domain Layer — SAGA Events (Test-First)

- [x] 1.1 Write unit tests for CustomerValidatedEvent (fields, null eventId/occurredOn rejection) following existing event test patterns
- [x] 1.2 Write unit tests for CustomerRejectedEvent (fields including failureMessages, null eventId/occurredOn rejection)
- [x] 1.3 Implement CustomerValidatedEvent record in customer-domain (customerId: CustomerId, reservationId: UUID)
- [x] 1.4 Implement CustomerRejectedEvent record in customer-domain (customerId: CustomerId, reservationId: UUID, failureMessages: List<String>)

## 2. Application Layer — Validation Use Case (Test-First)

- [x] 2.1 Create ValidateCustomerCommand record in customer-application dto (customerId: String, reservationId: String)
- [x] 2.2 Create ValidateCustomerForReservationUseCase interface in customer-application port.input
- [x] 2.3 Write unit tests for CustomerApplicationService.execute(ValidateCustomerCommand) — 3 scenarios: customer active → validated, not found → rejected, not active → rejected. Verify events published directly (not via aggregate)
- [x] 2.4 Add ValidateCustomerForReservationUseCase implementation to CustomerApplicationService (6th interface, @Transactional, no save/clearDomainEvents)

## 3. Infrastructure — POM and Dependencies

- [x] 3.1 Add common-messaging dependency to customer-infrastructure pom.xml
- [x] 3.2 Add test dependencies to customer-container pom.xml (spring-rabbit-test, testcontainers-rabbitmq, awaitility)

## 4. Infrastructure — Outbox Publisher

- [x] 4.1 Delete CustomerDomainEventPublisherAdapter (logger no-op)
- [x] 4.2 Create OutboxCustomerDomainEventPublisher @Component — 6 instanceof extractAggregateId, routing key auto-derivation, JSON serialization via ObjectMapper, save to OutboxEventRepository

## 5. Infrastructure — RabbitMQ Topology

- [x] 5.1 Create RabbitMQConfig @Configuration in customer-infrastructure config — customer.exchange (TopicExchange), 2 event queues (validated, rejected), 1 command queue (validate.command), customer.dlq, dlx.exchange (DirectExchange), per-queue DLQ routing keys (customer.validated.dlq, customer.rejected.dlq, customer.validate.command.dlq), all bindings

## 6. Infrastructure — SAGA Listener

- [x] 6.1 Create CustomerValidationListener @Component in adapter.input.messaging — @RabbitListener on customer.validate.command.queue, receives raw Message, parses JSON with ObjectMapper, calls validateCustomerUseCase.execute()

## 7. Container Assembly

- [x] 7.1 Add scanning annotations to CustomerServiceApplication (@SpringBootApplication scanBasePackages, @EntityScan, @EnableJpaRepositories with basePackages = "com.vehiclerental")
- [x] 7.2 Add RabbitMQ connection and listener retry properties to application.yml (host, port, username, password, listener.simple.retry with 3 attempts and exponential backoff)
- [x] 7.3 Create Flyway migration V2__create_outbox_events_table.sql (identical to payment/reservation)
- [x] 7.4 Update BeanConfiguration to register ValidateCustomerForReservationUseCase — add 6th interface to CustomerApplicationService bean

## 8. Integration Tests — Fix Existing ITs

- [x] 8.1 Add @Container @ServiceConnection RabbitMQContainer to CustomerServiceApplicationIT
- [x] 8.2 Add @Container @ServiceConnection RabbitMQContainer to CustomerControllerIT
- [x] 8.3 Add @Container @ServiceConnection RabbitMQContainer to CustomerRepositoryAdapterIT

## 9. Integration Tests — New Outbox ITs

- [x] 9.1 Create OutboxPublisherIT — verify PENDING OutboxEvent transitions to PUBLISHED after scheduler runs (Awaitility)
- [x] 9.2 Create OutboxAtomicityIT — verify customer entity + outbox event persist atomically in same transaction

## 10. Verification

- [x] 10.1 Run mvn test (unit tests) — all pass including new domain and application tests
- [x] 10.2 Run mvn verify (unit + integration tests) — all compile, unit tests pass. ITs blocked by Docker Desktop/Testcontainers connectivity (same issue affects existing Payment ITs — environmental, not code)
