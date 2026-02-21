# payment-application-ports Specification

## Purpose
Input and output port interfaces for the Payment Service application layer. Input ports define use case contracts (one interface per use case, ISP). Output ports define what the application needs from external systems: persistence (PaymentRepository, defined in domain), event publishing (PaymentDomainEventPublisher), and charge processing delegation (PaymentGateway with PaymentGatewayResult).

## ADDED Requirements

### Requirement: ProcessPaymentUseCase input port

ProcessPaymentUseCase SHALL be an interface with a single method `execute(ProcessPaymentCommand)` returning `PaymentResponse`.

#### Scenario: Interface declares execute method

* **WHEN** the ProcessPaymentUseCase interface is inspected
* **THEN** it SHALL declare `PaymentResponse execute(ProcessPaymentCommand command)`

### Requirement: RefundPaymentUseCase input port

RefundPaymentUseCase SHALL be an interface with a single method `execute(RefundPaymentCommand)` returning `PaymentResponse`.

#### Scenario: Interface declares execute method

* **WHEN** the RefundPaymentUseCase interface is inspected
* **THEN** it SHALL declare `PaymentResponse execute(RefundPaymentCommand command)`

### Requirement: GetPaymentUseCase input port

GetPaymentUseCase SHALL be an interface with a single method `execute(GetPaymentCommand)` returning `PaymentResponse`.

#### Scenario: Interface declares execute method

* **WHEN** the GetPaymentUseCase interface is inspected
* **THEN** it SHALL declare `PaymentResponse execute(GetPaymentCommand command)`

### Requirement: PaymentDomainEventPublisher output port

PaymentDomainEventPublisher SHALL be an interface with a single method `publish(List<DomainEvent>)` returning void. It SHALL use only domain types from common.

#### Scenario: Interface declares publish method

* **WHEN** the PaymentDomainEventPublisher interface is inspected
* **THEN** it SHALL declare `void publish(List<DomainEvent> events)`

#### Scenario: No framework types in signature

* **WHEN** the PaymentDomainEventPublisher interface is inspected
* **THEN** it SHALL NOT import any type from `org.springframework.*`

### Requirement: PaymentGateway output port

PaymentGateway SHALL be an interface with a single method `charge(Money amount)` returning `PaymentGatewayResult`. It abstracts the external payment processor, allowing the Application Service to route between `complete()` and `fail()` based on the gateway result.

#### Scenario: Interface declares charge method

* **WHEN** the PaymentGateway interface is inspected
* **THEN** it SHALL declare `PaymentGatewayResult charge(Money amount)`

#### Scenario: No framework types in signature

* **WHEN** the PaymentGateway interface is inspected
* **THEN** it SHALL NOT import any type from `org.springframework.*`

### Requirement: PaymentGatewayResult record

PaymentGatewayResult SHALL be a Java record with fields: `success` (boolean) and `failureMessages` (List<String>). It carries the outcome of a charge attempt from the gateway to the Application Service.

#### Scenario: Success result has no failure messages

* **WHEN** a PaymentGatewayResult is constructed with `success=true` and an empty list
* **THEN** `success()` SHALL return true
* **AND** `failureMessages()` SHALL return an empty list

#### Scenario: Failure result carries decline reasons

* **WHEN** a PaymentGatewayResult is constructed with `success=false` and messages ["Card declined"]
* **THEN** `success()` SHALL return false
* **AND** `failureMessages()` SHALL contain "Card declined"

### Requirement: All ports use application-layer types only

Input port interfaces SHALL use Command and Response DTOs from the application layer, not raw primitives or domain types directly.

#### Scenario: Input ports reference application DTOs

* **WHEN** any input port interface is inspected
* **THEN** its method parameters SHALL be Command records from `dto.command`
* **AND** its return types SHALL be Response records from `dto.response`

## Constraints

### Constraint: Zero Spring dependencies in port interfaces

No interface in `com.vehiclerental.payment.application.port` SHALL import any type from `org.springframework.*`.
