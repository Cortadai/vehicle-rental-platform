## ADDED Requirements

### Requirement: ConfirmFleetAvailabilityUseCase input port

ConfirmFleetAvailabilityUseCase SHALL be an interface in `com.vehiclerental.fleet.application.port.input` declaring a single method `execute(ConfirmFleetAvailabilityCommand command)`.

#### Scenario: Interface declares execute method

- **WHEN** ConfirmFleetAvailabilityUseCase is inspected
- **THEN** it SHALL declare `void execute(ConfirmFleetAvailabilityCommand command)`

### Requirement: ConfirmFleetAvailabilityCommand carries correlation data and dates

ConfirmFleetAvailabilityCommand SHALL be a Java record in `com.vehiclerental.fleet.application.dto.command` with fields `vehicleId` (String), `reservationId` (String), `pickupDate` (String), and `returnDate` (String). Dates are ISO-8601 strings for simplicity — not used in current logic but included in the command contract for future availability-by-date validation.

#### Scenario: Command has required fields

- **WHEN** a ConfirmFleetAvailabilityCommand is constructed
- **THEN** `vehicleId()` SHALL return the vehicle identifier as String
- **AND** `reservationId()` SHALL return the reservation identifier as String for SAGA correlation
- **AND** `pickupDate()` SHALL return the pickup date as ISO-8601 String
- **AND** `returnDate()` SHALL return the return date as ISO-8601 String

### Requirement: ReleaseFleetReservationUseCase input port

ReleaseFleetReservationUseCase SHALL be an interface in `com.vehiclerental.fleet.application.port.input` declaring a single method `execute(ReleaseFleetReservationCommand command)`.

#### Scenario: Interface declares execute method

- **WHEN** ReleaseFleetReservationUseCase is inspected
- **THEN** it SHALL declare `void execute(ReleaseFleetReservationCommand command)`

### Requirement: ReleaseFleetReservationCommand carries correlation data

ReleaseFleetReservationCommand SHALL be a Java record in `com.vehiclerental.fleet.application.dto.command` with fields `vehicleId` (String) and `reservationId` (String). No dates needed — release does not require date context.

#### Scenario: Command has required fields

- **WHEN** a ReleaseFleetReservationCommand is constructed
- **THEN** `vehicleId()` SHALL return the vehicle identifier as String
- **AND** `reservationId()` SHALL return the reservation identifier as String for SAGA correlation

### Requirement: FleetApplicationService implements confirmation use case

FleetApplicationService SHALL implement `ConfirmFleetAvailabilityUseCase` in addition to the 5 existing use case interfaces. The `execute(ConfirmFleetAvailabilityCommand)` method SHALL be `@Transactional` (not readOnly, because outbox writes require a write transaction).

#### Scenario: Vehicle exists and is ACTIVE

- **WHEN** `execute(ConfirmFleetAvailabilityCommand)` is called with a vehicleId that exists in the repository and the vehicle has status ACTIVE
- **THEN** it SHALL publish a `FleetConfirmedEvent` with the vehicle's `VehicleId` and the command's `reservationId` as UUID
- **AND** it SHALL NOT call `vehicleRepository.save()` (no aggregate mutation)

#### Scenario: Vehicle not found

- **WHEN** `execute(ConfirmFleetAvailabilityCommand)` is called with a vehicleId that does not exist in the repository
- **THEN** it SHALL publish a `FleetRejectedEvent` with failureMessages containing `"Vehicle not found: {vehicleId}"`
- **AND** the reservationId from the command SHALL be included in the event for SAGA correlation

#### Scenario: Vehicle exists but is not ACTIVE

- **WHEN** `execute(ConfirmFleetAvailabilityCommand)` is called with a vehicleId that exists but the vehicle's status is not ACTIVE (e.g., UNDER_MAINTENANCE or RETIRED)
- **THEN** it SHALL publish a `FleetRejectedEvent` with failureMessages containing `"Vehicle is not available, current status: {status}"`
- **AND** the reservationId from the command SHALL be included in the event for SAGA correlation

#### Scenario: Events are published directly, not via aggregate

- **WHEN** the confirmation use case executes
- **THEN** it SHALL create the response event directly and call `eventPublisher.publish(List.of(event))`
- **AND** it SHALL NOT call `vehicle.registerDomainEvent()` or `vehicle.clearDomainEvents()`

### Requirement: FleetApplicationService implements release use case

FleetApplicationService SHALL implement `ReleaseFleetReservationUseCase` in addition to the other use case interfaces (total: 7). The `execute(ReleaseFleetReservationCommand)` method SHALL be `@Transactional` (not readOnly, because outbox writes require a write transaction).

#### Scenario: Vehicle exists — release acknowledged

- **WHEN** `execute(ReleaseFleetReservationCommand)` is called with a vehicleId that exists in the repository
- **THEN** it SHALL publish a `FleetReleasedEvent` with the vehicle's `VehicleId` and the command's `reservationId` as UUID
- **AND** it SHALL NOT call `vehicleRepository.save()` (no aggregate mutation — no vehicle_reservations model yet)

#### Scenario: Vehicle not found — release still acknowledged

- **WHEN** `execute(ReleaseFleetReservationCommand)` is called with a vehicleId that does not exist in the repository
- **THEN** it SHALL still publish a `FleetReleasedEvent` with a `VehicleId` constructed from the command's vehicleId String (`new VehicleId(UUID.fromString(command.vehicleId()))`) and the reservationId as UUID
- **AND** the compensation SHALL NOT fail — compensations MUST be idempotent and always succeed

#### Scenario: Events are published directly, not via aggregate

- **WHEN** the release use case executes
- **THEN** it SHALL create the response event directly and call `eventPublisher.publish(List.of(event))`
- **AND** it SHALL NOT call `vehicle.registerDomainEvent()` or `vehicle.clearDomainEvents()`

### Requirement: FleetConfirmationListener receives confirmation commands from RabbitMQ

FleetConfirmationListener SHALL be a Spring component in `com.vehiclerental.fleet.infrastructure.adapter.input.messaging` annotated with `@RabbitListener(queues = "fleet.confirm.command.queue")`. It SHALL receive raw `Message` objects and parse JSON with `ObjectMapper`.

#### Scenario: Valid confirm command message is processed

- **WHEN** a JSON message with fields `vehicleId`, `reservationId`, `pickupDate`, and `returnDate` arrives on `fleet.confirm.command.queue`
- **THEN** FleetConfirmationListener SHALL parse the message body, construct a `ConfirmFleetAvailabilityCommand`, and call `confirmFleetAvailabilityUseCase.execute(command)`

#### Scenario: Infrastructure failure triggers Spring Retry

- **WHEN** an infrastructure failure occurs (database down, JSON parse error, outbox write failure)
- **THEN** the exception SHALL propagate from the listener
- **AND** Spring AMQP Retry SHALL attempt redelivery (3 attempts, exponential backoff)
- **AND** after exhausting retries, the message SHALL be sent to `fleet.dlq`

### Requirement: FleetReleaseListener receives compensation commands from RabbitMQ

FleetReleaseListener SHALL be a Spring component in `com.vehiclerental.fleet.infrastructure.adapter.input.messaging` annotated with `@RabbitListener(queues = "fleet.release.command.queue")`. It SHALL receive raw `Message` objects and parse JSON with `ObjectMapper`.

#### Scenario: Valid release command message is processed

- **WHEN** a JSON message with fields `vehicleId` and `reservationId` arrives on `fleet.release.command.queue`
- **THEN** FleetReleaseListener SHALL parse the message body, construct a `ReleaseFleetReservationCommand`, and call `releaseFleetReservationUseCase.execute(command)`

#### Scenario: Infrastructure failure triggers Spring Retry

- **WHEN** an infrastructure failure occurs (database down, JSON parse error, outbox write failure)
- **THEN** the exception SHALL propagate from the listener
- **AND** Spring AMQP Retry SHALL attempt redelivery (3 attempts, exponential backoff)
- **AND** after exhausting retries, the message SHALL be sent to `fleet.dlq`

### Requirement: BeanConfiguration registers both SAGA use cases

BeanConfiguration in fleet-container SHALL register `ConfirmFleetAvailabilityUseCase` and `ReleaseFleetReservationUseCase` as beans pointing to the same `FleetApplicationService` instance.

#### Scenario: Confirmation use case bean is available

- **WHEN** the application context is loaded
- **THEN** a `ConfirmFleetAvailabilityUseCase` bean SHALL be available
- **AND** it SHALL resolve to the same `FleetApplicationService` instance as other use case beans

#### Scenario: Release use case bean is available

- **WHEN** the application context is loaded
- **THEN** a `ReleaseFleetReservationUseCase` bean SHALL be available
- **AND** it SHALL resolve to the same `FleetApplicationService` instance as other use case beans
