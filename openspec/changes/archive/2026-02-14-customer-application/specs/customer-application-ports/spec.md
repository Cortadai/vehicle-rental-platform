customer-application-ports
==========================

Purpose
-------

Input and output port interfaces for the Customer Service application layer. Input ports define use case contracts (one interface per use case). Output ports define what the application needs from external systems (persistence, event publishing).

ADDED Requirements
------------------

### Requirement: CreateCustomerUseCase input port

CreateCustomerUseCase SHALL be an interface with a single method `execute(CreateCustomerCommand)` returning `CustomerResponse`.

#### Scenario: Interface declares execute method

* **WHEN** the CreateCustomerUseCase interface is inspected
* **THEN** it SHALL declare `CustomerResponse execute(CreateCustomerCommand command)`

### Requirement: GetCustomerUseCase input port

GetCustomerUseCase SHALL be an interface with a single method `execute(GetCustomerCommand)` returning `CustomerResponse`.

#### Scenario: Interface declares execute method

* **WHEN** the GetCustomerUseCase interface is inspected
* **THEN** it SHALL declare `CustomerResponse execute(GetCustomerCommand command)`

### Requirement: SuspendCustomerUseCase input port

SuspendCustomerUseCase SHALL be an interface with a single method `execute(SuspendCustomerCommand)` returning void.

#### Scenario: Interface declares execute method

* **WHEN** the SuspendCustomerUseCase interface is inspected
* **THEN** it SHALL declare `void execute(SuspendCustomerCommand command)`

### Requirement: ActivateCustomerUseCase input port

ActivateCustomerUseCase SHALL be an interface with a single method `execute(ActivateCustomerCommand)` returning void.

#### Scenario: Interface declares execute method

* **WHEN** the ActivateCustomerUseCase interface is inspected
* **THEN** it SHALL declare `void execute(ActivateCustomerCommand command)`

### Requirement: DeleteCustomerUseCase input port

DeleteCustomerUseCase SHALL be an interface with a single method `execute(DeleteCustomerCommand)` returning void.

#### Scenario: Interface declares execute method

* **WHEN** the DeleteCustomerUseCase interface is inspected
* **THEN** it SHALL declare `void execute(DeleteCustomerCommand command)`

### Requirement: CustomerDomainEventPublisher output port

CustomerDomainEventPublisher SHALL be an interface with a single method `publish(List<DomainEvent>)` returning void. It SHALL use only domain types from common.

#### Scenario: Interface declares publish method

* **WHEN** the CustomerDomainEventPublisher interface is inspected
* **THEN** it SHALL declare `void publish(List<DomainEvent> events)`

#### Scenario: No framework types in signature

* **WHEN** the CustomerDomainEventPublisher interface is inspected
* **THEN** it SHALL NOT import any type from `org.springframework.*`

### Requirement: All ports use application-layer types only

Input port interfaces SHALL use Command and Response DTOs from the application layer, not raw primitives or domain types directly.

#### Scenario: Input ports reference application DTOs

* **WHEN** any input port interface is inspected
* **THEN** its method parameters SHALL be Command records from `dto.command`
* **AND** its return types SHALL be Response records from `dto.response` or void

Constraint: Zero Spring dependencies in port interfaces
-------------------------------------------------------

No interface in `com.vehiclerental.customer.application.port` SHALL import any type from `org.springframework.*`.
