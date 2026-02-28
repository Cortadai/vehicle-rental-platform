## ADDED Requirements

### Requirement: ValidateCustomerForReservationUseCase input port

ValidateCustomerForReservationUseCase SHALL be an interface in `com.vehiclerental.customer.application.port.input` declaring a single method `execute(ValidateCustomerCommand command)`.

#### Scenario: Interface declares execute method

- **WHEN** ValidateCustomerForReservationUseCase is inspected
- **THEN** it SHALL declare `void execute(ValidateCustomerCommand command)`

### Requirement: ValidateCustomerCommand carries correlation data

ValidateCustomerCommand SHALL be a Java record in `com.vehiclerental.customer.application.dto` with fields `customerId` (String) and `reservationId` (String), following the existing command naming convention.

#### Scenario: Command has required fields

- **WHEN** a ValidateCustomerCommand is constructed
- **THEN** `customerId()` SHALL return the customer identifier as String
- **AND** `reservationId()` SHALL return the reservation identifier as String for SAGA correlation

### Requirement: CustomerApplicationService implements validation use case

CustomerApplicationService SHALL implement `ValidateCustomerForReservationUseCase` in addition to the 5 existing use case interfaces. The `execute(ValidateCustomerCommand)` method SHALL be `@Transactional` (not readOnly, because outbox writes require a write transaction).

#### Scenario: Customer exists and is ACTIVE

- **WHEN** `execute(ValidateCustomerCommand)` is called with a customerId that exists in the repository and the customer has status ACTIVE
- **THEN** it SHALL publish a `CustomerValidatedEvent` with the customer's `CustomerId` and the command's `reservationId` as UUID
- **AND** it SHALL NOT call `customerRepository.save()` (no aggregate mutation)

#### Scenario: Customer not found

- **WHEN** `execute(ValidateCustomerCommand)` is called with a customerId that does not exist in the repository
- **THEN** it SHALL publish a `CustomerRejectedEvent` with failureMessages containing `"Customer not found: {customerId}"`
- **AND** the reservationId from the command SHALL be included in the event for SAGA correlation

#### Scenario: Customer exists but is not ACTIVE

- **WHEN** `execute(ValidateCustomerCommand)` is called with a customerId that exists but the customer's status is not ACTIVE (e.g., SUSPENDED or PENDING)
- **THEN** it SHALL publish a `CustomerRejectedEvent` with failureMessages containing `"Customer is not active, current status: {status}"`
- **AND** the reservationId from the command SHALL be included in the event for SAGA correlation

#### Scenario: Events are published directly, not via aggregate

- **WHEN** the validation use case executes
- **THEN** it SHALL create the response event directly and call `eventPublisher.publish(List.of(event))`
- **AND** it SHALL NOT call `customer.registerDomainEvent()` or `customer.clearDomainEvents()`

### Requirement: CustomerValidationListener receives commands from RabbitMQ

CustomerValidationListener SHALL be a Spring component in `com.vehiclerental.customer.infrastructure.adapter.input.messaging` annotated with `@RabbitListener(queues = "customer.validate.command.queue")`. It SHALL receive raw `Message` objects and parse JSON with `ObjectMapper`.

#### Scenario: Valid command message is processed

- **WHEN** a JSON message with fields `customerId` and `reservationId` arrives on `customer.validate.command.queue`
- **THEN** CustomerValidationListener SHALL parse the message body, construct a `ValidateCustomerCommand`, and call `validateCustomerUseCase.execute(command)`

#### Scenario: Business failure produces rejection event (message acked)

- **WHEN** the validation use case determines the customer is not found or not active
- **THEN** the use case SHALL publish a `CustomerRejectedEvent` via outbox
- **AND** the listener SHALL complete normally (message acknowledged)

#### Scenario: Infrastructure failure triggers Spring Retry

- **WHEN** an infrastructure failure occurs (database down, JSON parse error, outbox write failure)
- **THEN** the exception SHALL propagate from the listener
- **AND** Spring AMQP Retry SHALL attempt redelivery (3 attempts, exponential backoff)
- **AND** after exhausting retries, the message SHALL be sent to `customer.dlq`

### Requirement: BeanConfiguration registers validation use case

BeanConfiguration in customer-container SHALL register `ValidateCustomerForReservationUseCase` as a bean by injecting it into `CustomerApplicationService` constructor alongside existing dependencies.

#### Scenario: Validation use case bean is available

- **WHEN** the application context is loaded
- **THEN** a `ValidateCustomerForReservationUseCase` bean SHALL be available
- **AND** it SHALL resolve to the same `CustomerApplicationService` instance as other use case beans
