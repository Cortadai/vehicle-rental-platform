# reservation-application-service Specification

## Purpose
Application Service orchestrating Reservation use cases (create and track). Implements input ports, delegates business logic to the domain, persists through repository, publishes domain events, and returns responses. Contains zero business logic — pure orchestration.

## ADDED Requirements

### Requirement: ReservationApplicationService implements all input ports

ReservationApplicationService SHALL implement CreateReservationUseCase and TrackReservationUseCase.

#### Scenario: Service implements all use case interfaces

* **WHEN** ReservationApplicationService is inspected
* **THEN** it SHALL implement both CreateReservationUseCase and TrackReservationUseCase

### Requirement: Create reservation orchestration

The create use case SHALL convert command fields to domain Value Objects and ReservationItems, call `Reservation.create()`, persist via ReservationRepository, publish domain events via ReservationDomainEventPublisher, clear domain events, and return a CreateReservationResponse.

#### Scenario: Successful creation flow

* **WHEN** `execute(CreateReservationCommand)` is called with valid data
* **THEN** ReservationRepository.save() SHALL be called with the created Reservation
* **AND** ReservationDomainEventPublisher.publish() SHALL be called with the aggregate's domain events
* **AND** Reservation.clearDomainEvents() SHALL be called after publishing
* **AND** a CreateReservationResponse SHALL be returned with trackingId and status "PENDING"

#### Scenario: Items converted from command to domain

* **WHEN** `execute(CreateReservationCommand)` is called with 2 items
* **THEN** each CreateReservationItemCommand SHALL be converted to a ReservationItem using the command's vehicleId, dailyRate, days, and the command's currency
* **AND** the resulting ReservationItems SHALL be passed to `Reservation.create()`

#### Scenario: Currency applied to all items

* **WHEN** `execute(CreateReservationCommand)` is called with currency "EUR" and items with dailyRates
* **THEN** each item's dailyRate SHALL be converted to `Money(dailyRate, Currency.getInstance("EUR"))`

### Requirement: Track reservation orchestration

The track use case SHALL parse the tracking ID, look up the reservation by TrackingId, and return a TrackReservationResponse.

#### Scenario: Reservation found

* **WHEN** `execute(TrackReservationCommand)` is called with an existing tracking ID
* **THEN** a TrackReservationResponse SHALL be returned with the full reservation snapshot including items

#### Scenario: Reservation not found

* **WHEN** `execute(TrackReservationCommand)` is called with a non-existing tracking ID
* **THEN** it SHALL throw ReservationNotFoundException

#### Scenario: Invalid tracking ID format

* **WHEN** `execute(TrackReservationCommand)` is called with a string that is not a valid UUID
* **THEN** it SHALL throw an exception (IllegalArgumentException or similar)

### Requirement: Application Service has no business logic

ReservationApplicationService SHALL NOT contain business logic (validation rules, state transitions, price calculations). It SHALL only orchestrate: receive command, convert types, delegate to domain, persist, publish events, return response.

#### Scenario: Domain handles creation logic

* **WHEN** the create use case is executed
* **THEN** the totalPrice calculation, status initialization, and event registration SHALL be in `Reservation.create()`, not in the Application Service

### Requirement: ReservationNotFoundException for missing aggregates

ReservationNotFoundException SHALL extend RuntimeException directly. It is an application-level exception, NOT a domain exception. It SHALL carry the tracking ID that was not found and use a descriptive message. It lives in the application module under `exception/`.

#### Scenario: Exception carries tracking ID

* **WHEN** a ReservationNotFoundException is thrown for tracking ID "abc-123"
* **THEN** `getMessage()` SHALL contain "abc-123"

#### Scenario: Exception is not a domain exception

* **WHEN** ReservationNotFoundException is inspected
* **THEN** it SHALL NOT extend ReservationDomainException or DomainException

### Requirement: ReservationApplicationMapper converts domain to DTOs

ReservationApplicationMapper SHALL be a plain Java class that converts Reservation domain objects to response records. It SHALL have no Spring annotations.

#### Scenario: Maps Reservation to CreateReservationResponse

* **WHEN** `toCreateResponse(Reservation)` is called
* **THEN** it SHALL return a CreateReservationResponse with trackingId (UUID as String) and status (enum name as String)

#### Scenario: Maps Reservation to TrackReservationResponse

* **WHEN** `toTrackResponse(Reservation)` is called
* **THEN** it SHALL return a TrackReservationResponse with all fields mapped from the Reservation aggregate
* **AND** items SHALL be mapped to TrackReservationItemResponse records
* **AND** Money fields SHALL be mapped to BigDecimal (amount) and String (currency)
* **AND** DateRange SHALL be mapped to String pickupDate and String returnDate
* **AND** PickupLocation SHALL be mapped to String address and String city

### Requirement: Transaction boundaries

The create use case SHALL be annotated with `@Transactional`. The track use case SHALL be annotated with `@Transactional(readOnly = true)`.

#### Scenario: Create is transactional

* **WHEN** the execute(CreateReservationCommand) method is inspected
* **THEN** it SHALL be annotated with `@Transactional`

#### Scenario: Track is read-only transactional

* **WHEN** the execute(TrackReservationCommand) method is inspected
* **THEN** it SHALL be annotated with `@Transactional(readOnly = true)`

### Requirement: Constructor injection with no Spring annotations

ReservationApplicationService SHALL use constructor injection with three dependencies: ReservationRepository, ReservationDomainEventPublisher, ReservationApplicationMapper. It SHALL have no `@Autowired`, `@Service`, or `@Component` annotations. Bean registration happens in the container module's BeanConfiguration.

#### Scenario: No Spring annotations on class

* **WHEN** ReservationApplicationService is inspected
* **THEN** it SHALL NOT have `@Service`, `@Component`, or `@Autowired` annotations

#### Scenario: Constructor accepts three dependencies

* **WHEN** ReservationApplicationService constructor is inspected
* **THEN** it SHALL accept ReservationRepository, ReservationDomainEventPublisher, and ReservationApplicationMapper

## Constraint: Minimal Spring dependency

ReservationApplicationService SHALL only depend on `spring-tx` (for `@Transactional`). It SHALL NOT import types from `spring-context`, `spring-web`, `spring-data`, or any other Spring module.
