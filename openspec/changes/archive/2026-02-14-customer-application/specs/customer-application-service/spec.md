customer-application-service
============================

Purpose
-------

Application Service orchestrating all Customer use cases. Implements input ports, delegates business logic to the domain, persists through output ports, and dispatches domain events. Contains zero business logic — pure orchestration.

### ADDED Requirements

### Requirement: CustomerApplicationService implements all input ports

CustomerApplicationService SHALL implement CreateCustomerUseCase, GetCustomerUseCase, SuspendCustomerUseCase, ActivateCustomerUseCase, and DeleteCustomerUseCase.

#### Scenario: Service implements all use case interfaces

* **WHEN** CustomerApplicationService is inspected
* **THEN** it SHALL implement all five use case interfaces

### Requirement: Create customer orchestration

The create use case SHALL convert command fields to domain Value Objects, call `Customer.create()`, persist via CustomerRepository, publish domain events via CustomerDomainEventPublisher, and return a CustomerResponse.

#### Scenario: Successful creation flow

* **WHEN** `execute(CreateCustomerCommand)` is called with valid data
* **THEN** CustomerRepository.save() SHALL be called with the created Customer
* **AND** CustomerDomainEventPublisher.publish() SHALL be called with the aggregate's domain events
* **AND** Customer.clearDomainEvents() SHALL be called after publishing
* **AND** a CustomerResponse SHALL be returned with the new customer's data

### Requirement: Get customer orchestration

The get use case SHALL load the customer by ID and return a CustomerResponse.

#### Scenario: Customer found

* **WHEN** `execute(GetCustomerCommand)` is called with an existing customer ID
* **THEN** a CustomerResponse SHALL be returned with the customer's data

#### Scenario: Customer not found

* **WHEN** `execute(GetCustomerCommand)` is called with a non-existing customer ID
* **THEN** it SHALL throw CustomerNotFoundException

### Requirement: Suspend customer orchestration

The suspend use case SHALL load the customer, call `customer.suspend()`, persist, and publish events.

#### Scenario: Successful suspension

* **WHEN** `execute(SuspendCustomerCommand)` is called with an active customer's ID
* **THEN** CustomerRepository.save() SHALL be called
* **AND** CustomerDomainEventPublisher.publish() SHALL be called with the aggregate's domain events
* **AND** Customer.clearDomainEvents() SHALL be called after publishing

#### Scenario: Customer not found for suspension

* **WHEN** `execute(SuspendCustomerCommand)` is called with a non-existing ID
* **THEN** it SHALL throw CustomerNotFoundException

### Requirement: Activate customer orchestration

The activate use case SHALL load the customer, call `customer.activate()`, persist, and publish events.

#### Scenario: Successful activation

* **WHEN** `execute(ActivateCustomerCommand)` is called with a suspended customer's ID
* **THEN** CustomerRepository.save() SHALL be called
* **AND** CustomerDomainEventPublisher.publish() SHALL be called with the aggregate's domain events
* **AND** Customer.clearDomainEvents() SHALL be called after publishing

#### Scenario: Customer not found for activation

* **WHEN** `execute(ActivateCustomerCommand)` is called with a non-existing ID
* **THEN** it SHALL throw CustomerNotFoundException

### Requirement: Delete customer orchestration

The delete use case SHALL load the customer, call `customer.delete()`, persist, and publish events.

#### Scenario: Successful deletion

* **WHEN** `execute(DeleteCustomerCommand)` is called with an active or suspended customer's ID
* **THEN** CustomerRepository.save() SHALL be called
* **AND** CustomerDomainEventPublisher.publish() SHALL be called with the aggregate's domain events
* **AND** Customer.clearDomainEvents() SHALL be called after publishing

#### Scenario: Customer not found for deletion

* **WHEN** `execute(DeleteCustomerCommand)` is called with a non-existing ID
* **THEN** it SHALL throw CustomerNotFoundException

### Requirement: Application Service has no business logic

CustomerApplicationService SHALL NOT contain business logic (validation rules, state transitions, calculations). It SHALL only orchestrate: receive command, convert types, delegate to domain, persist, publish events, return response.

#### Scenario: Domain handles state transitions

* **WHEN** the suspend use case is executed
* **THEN** the state transition logic SHALL be in `Customer.suspend()`, not in the Application Service

### Requirement: CustomerNotFoundException for missing aggregates

CustomerNotFoundException SHALL extend RuntimeException directly. It is an application-level exception, NOT a domain exception. It SHALL carry the customer ID that was not found and use a descriptive message. It lives in the application module under `exception/`.

#### Scenario: Exception carries customer ID

* **WHEN** a CustomerNotFoundException is thrown for customer ID "abc-123"
* **THEN** `getMessage()` SHALL contain "abc-123"

#### Scenario: Exception is not a domain exception

* **WHEN** CustomerNotFoundException is inspected
* **THEN** it SHALL NOT extend CustomerDomainException or DomainException

### Requirement: CustomerApplicationMapper converts domain to DTOs

CustomerApplicationMapper SHALL be a plain Java class that converts Customer domain objects to CustomerResponse records. It SHALL have no Spring annotations.

#### Scenario: Maps Customer to CustomerResponse

* **WHEN** `toResponse(Customer)` is called
* **THEN** it SHALL return a CustomerResponse with all fields mapped from the Customer aggregate

### Requirement: Transaction boundaries on write operations

Write use cases (create, suspend, activate, delete) SHALL be annotated with `@Transactional`. Read use cases (get) SHALL be annotated with `@Transactional(readOnly = true)`.

#### Scenario: Create is transactional

* **WHEN** the create method is inspected
* **THEN** it SHALL be annotated with `@Transactional`

#### Scenario: Get is read-only transactional

* **WHEN** the get method is inspected
* **THEN** it SHALL be annotated with `@Transactional(readOnly = true)`

### Requirement: Constructor injection with no Spring annotations

CustomerApplicationService SHALL use constructor injection with no `@Autowired` or `@Service` annotations. Bean registration happens in the container module's BeanConfiguration.

#### Scenario: No Spring annotations on class

* **WHEN** CustomerApplicationService is inspected
* **THEN** it SHALL NOT have `@Service`, `@Component`, or `@Autowired` annotations

Constraint: Minimal Spring dependency
-------------------------------------

CustomerApplicationService SHALL only depend on `spring-tx` (for `@Transactional`). It SHALL NOT import types from `spring-context`, `spring-web`, `spring-data`, or any other Spring module.
