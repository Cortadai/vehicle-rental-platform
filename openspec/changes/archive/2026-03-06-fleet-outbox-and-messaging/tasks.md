## 1. Domain Layer — SAGA Events (Test-First)

- [x] 1.1 Write unit tests for FleetConfirmedEvent (fields: eventId, occurredOn, vehicleId, reservationId; null eventId/occurredOn rejection throws FleetDomainException)
- [x] 1.2 Write unit tests for FleetRejectedEvent (fields: eventId, occurredOn, vehicleId, reservationId, failureMessages; null eventId/occurredOn rejection throws FleetDomainException)
- [x] 1.3 Write unit tests for FleetReleasedEvent (fields: eventId, occurredOn, vehicleId, reservationId; null eventId/occurredOn rejection throws FleetDomainException)
- [x] 1.4 Implement FleetConfirmedEvent record in fleet-domain event package (vehicleId: VehicleId, reservationId: UUID, zero Spring imports)
- [x] 1.5 Implement FleetRejectedEvent record in fleet-domain event package (vehicleId: VehicleId, reservationId: UUID, failureMessages: List<String>, zero Spring imports)
- [x] 1.6 Implement FleetReleasedEvent record in fleet-domain event package (vehicleId: VehicleId, reservationId: UUID, zero Spring imports)

## 2. Application Layer — SAGA Use Cases (Test-First for service logic)

- [x] 2.1 Create ConfirmFleetAvailabilityCommand record in fleet-application dto.command (vehicleId: String, reservationId: String, pickupDate: String, returnDate: String)
- [x] 2.2 Create ReleaseFleetReservationCommand record in fleet-application dto.command (vehicleId: String, reservationId: String)
- [x] 2.3 Create ConfirmFleetAvailabilityUseCase interface in fleet-application port.input
- [x] 2.4 Create ReleaseFleetReservationUseCase interface in fleet-application port.input
- [x] 2.5 Write unit tests for FleetApplicationService.execute(ConfirmFleetAvailabilityCommand) — 3 scenarios: vehicle ACTIVE → FleetConfirmedEvent, vehicle not found → FleetRejectedEvent with "Vehicle not found: {id}", vehicle not ACTIVE → FleetRejectedEvent with "Vehicle is not available, current status: {status}". Verify events published directly via eventPublisher.publish(List.of(event)), NOT via aggregate
- [x] 2.6 Write unit tests for FleetApplicationService.execute(ReleaseFleetReservationCommand) — 2 scenarios: vehicle exists → FleetReleasedEvent with vehicle's VehicleId, vehicle not found → FleetReleasedEvent with VehicleId constructed from command String. Verify no save/clearDomainEvents
- [x] 2.7 Implement ConfirmFleetAvailabilityUseCase in FleetApplicationService (6th interface, @Transactional not readOnly, no save/clearDomainEvents)
- [x] 2.8 Implement ReleaseFleetReservationUseCase in FleetApplicationService (7th interface, @Transactional not readOnly, idempotent — always publishes FleetReleasedEvent)

## 3. Infrastructure — POM and Dependencies

- [x] 3.1 Add common-messaging dependency to fleet-infrastructure pom.xml
- [x] 3.2 Add test dependencies to fleet-container pom.xml (spring-boot-testcontainers, testcontainers-rabbitmq, testcontainers-postgresql, testcontainers-junit-jupiter, awaitility) — consistent with Customer/Payment pattern, NO spring-rabbit-test

## 4. Infrastructure — Outbox Publisher

- [x] 4.1 Delete FleetDomainEventPublisherAdapter (logger no-op)
- [x] 4.2 Create OutboxFleetDomainEventPublisher @Component in adapter.output.event — static Map<Class, String> for 7 routing keys, 7 instanceof checks for aggregateId extraction via vehicleId().value().toString(), JSON serialization via ObjectMapper, save to OutboxEventRepository, exchange = "fleet.exchange", aggregateType = "FLEET"

## 5. Infrastructure — RabbitMQ Topology

- [x] 5.1 Create RabbitMQConfig @Configuration in fleet-infrastructure config — fleet.exchange (TopicExchange), 2 event queues (confirmed, rejected) with per-queue DLQ routing keys, 2 command queues (confirm.command, release.command) with per-queue DLQ routing keys, fleet.dlq, dlx.exchange (DirectExchange), 5 queue-to-exchange bindings, 5 DLQ bindings to shared fleet.dlq

## 6. Infrastructure — SAGA Listeners

- [x] 6.1 Create FleetConfirmationListener @Component in adapter.input.messaging — @RabbitListener on fleet.confirm.command.queue, receives raw Message, parses JSON with ObjectMapper (vehicleId, reservationId, pickupDate, returnDate), calls confirmFleetAvailabilityUseCase.execute()
- [x] 6.2 Create FleetReleaseListener @Component in adapter.input.messaging — @RabbitListener on fleet.release.command.queue, receives raw Message, parses JSON with ObjectMapper (vehicleId, reservationId), calls releaseFleetReservationUseCase.execute()

## 7. Container Assembly

- [x] 7.1 Add scanning annotations to FleetServiceApplication (@SpringBootApplication scanBasePackages = "com.vehiclerental", @EntityScan basePackages = "com.vehiclerental", @EnableJpaRepositories basePackages = "com.vehiclerental")
- [x] 7.2 Add RabbitMQ connection and listener retry properties to application.yml (host, port, username, password, listener.simple.retry with enabled: true, initial-interval: 1000, max-attempts: 3, multiplier: 2.0)
- [x] 7.3 Create Flyway migration V2__create_outbox_events_table.sql (identical to customer/payment/reservation)
- [x] 7.4 Update BeanConfiguration to register ConfirmFleetAvailabilityUseCase and ReleaseFleetReservationUseCase — add 6th and 7th interfaces to FleetApplicationService bean

## 8. Platform Topology — definitions.json

- [x] 8.1 Add fleet.release.command.queue to definitions.json (durable, x-dead-letter-exchange: dlx.exchange, x-dead-letter-routing-key: fleet.release.command.dlq)
- [x] 8.2 Add fleet.release.command.queue binding to fleet.exchange with routing key fleet.release.command
- [x] 8.3 Add DLQ binding from dlx.exchange to fleet.dlq with routing key fleet.release.command.dlq

## 9. Integration Tests — Fix Existing ITs

- [x] 9.1 Add @Container @ServiceConnection RabbitMQContainer to FleetServiceApplicationIT
- [x] 9.2 Add @Container @ServiceConnection RabbitMQContainer to VehicleControllerIT
- [x] 9.3 Add @Container @ServiceConnection RabbitMQContainer to VehicleRepositoryAdapterIT

## 10. Integration Tests — New Outbox ITs

- [x] 10.1 Create OutboxPublisherIT — verify PENDING OutboxEvent transitions to PUBLISHED after scheduler runs (Awaitility + RabbitMQ Testcontainer)
- [x] 10.2 Create OutboxAtomicityIT — verify vehicle entity + outbox event persist atomically in same transaction

## 11. Verification

- [x] 11.1 Run mvn test — all unit tests pass including new domain event tests and application service SAGA tests
- [x] 11.2 Run mvn verify — all compile, unit tests pass, integration tests pass with Testcontainers
