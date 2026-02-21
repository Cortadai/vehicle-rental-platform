# reservation-application-ports Specification

## Purpose
Input and output port interfaces for the Reservation Service application layer. Input ports define use case contracts (one interface per use case). Output port defines the domain event publishing contract.

## ADDED Requirements

### Requirement: CreateReservationUseCase input port

CreateReservationUseCase SHALL be an interface with a single method `execute(CreateReservationCommand)` returning `CreateReservationResponse`.

#### Scenario: Interface declares execute method

* **WHEN** the CreateReservationUseCase interface is inspected
* **THEN** it SHALL declare `CreateReservationResponse execute(CreateReservationCommand command)`

### Requirement: TrackReservationUseCase input port

TrackReservationUseCase SHALL be an interface with a single method `execute(TrackReservationCommand)` returning `TrackReservationResponse`.

#### Scenario: Interface declares execute method

* **WHEN** the TrackReservationUseCase interface is inspected
* **THEN** it SHALL declare `TrackReservationResponse execute(TrackReservationCommand command)`

### Requirement: ReservationDomainEventPublisher output port

ReservationDomainEventPublisher SHALL be an interface with a single method `publish(List<DomainEvent>)` returning void. It SHALL use only the `DomainEvent` type from common.

#### Scenario: Interface declares publish method

* **WHEN** the ReservationDomainEventPublisher interface is inspected
* **THEN** it SHALL declare `void publish(List<DomainEvent> domainEvents)`

#### Scenario: No framework types in signature

* **WHEN** the ReservationDomainEventPublisher interface is inspected
* **THEN** it SHALL NOT import any type from `org.springframework.*`

### Requirement: All ports use application-layer types only

Input port interfaces SHALL use Command and Response DTOs from the application layer, not raw primitives or domain types directly.

#### Scenario: Input ports reference application DTOs

* **WHEN** any input port interface is inspected
* **THEN** its method parameters SHALL be Command records from `dto.command`
* **AND** its return types SHALL be Response records from `dto.response`

## Constraint: Zero Spring dependencies in port interfaces

No interface in `com.vehiclerental.reservation.application.port` SHALL import any type from `org.springframework.*`.
