## MODIFIED Requirements

### Requirement: Create reservation orchestration

The create use case SHALL convert command fields to domain Value Objects and ReservationItems, call `Reservation.create()`, persist via ReservationRepository, publish domain events via ReservationDomainEventPublisher, clear domain events, start the SAGA via ReservationSagaOrchestrator, and return a CreateReservationResponse.

#### Scenario: Successful creation flow

* **WHEN** `execute(CreateReservationCommand)` is called with valid data
* **THEN** ReservationRepository.save() SHALL be called with the created Reservation
* **AND** ReservationDomainEventPublisher.publish() SHALL be called with the aggregate's domain events
* **AND** Reservation.clearDomainEvents() SHALL be called after publishing
* **AND** ReservationSagaOrchestrator.start() SHALL be called with a ReservationSagaData built from the reservation
* **AND** a CreateReservationResponse SHALL be returned with trackingId and status "PENDING"

#### Scenario: Items converted from command to domain

* **WHEN** `execute(CreateReservationCommand)` is called with 2 items
* **THEN** each CreateReservationItemCommand SHALL be converted to a ReservationItem using the command's vehicleId, dailyRate, days, and the command's currency
* **AND** the resulting ReservationItems SHALL be passed to `Reservation.create()`

#### Scenario: Currency applied to all items

* **WHEN** `execute(CreateReservationCommand)` is called with currency "EUR" and items with dailyRates
* **THEN** each item's dailyRate SHALL be converted to `Money(dailyRate, Currency.getInstance("EUR"))`

#### Scenario: SAGA data built from reservation

* **WHEN** the SAGA is started after reservation creation
* **THEN** ReservationSagaData SHALL contain the reservationId (from the saved reservation), customerId, vehicleId (from first item), totalAmount, currency, pickupDate, and returnDate

#### Scenario: ReservationCreatedEvent still published

* **WHEN** `execute(CreateReservationCommand)` is called
* **THEN** ReservationCreatedEvent SHALL be published via the event publisher before the SAGA starts
* **AND** the SAGA start SHALL NOT replace or prevent event publishing

### Requirement: Constructor injection with no Spring annotations

ReservationApplicationService SHALL use constructor injection with four dependencies: ReservationRepository, ReservationDomainEventPublisher, ReservationApplicationMapper, and ReservationSagaOrchestrator. It SHALL have no `@Autowired`, `@Service`, or `@Component` annotations. Bean registration happens in the container module's BeanConfiguration.

#### Scenario: No Spring annotations on class

* **WHEN** ReservationApplicationService is inspected
* **THEN** it SHALL NOT have `@Service`, `@Component`, or `@Autowired` annotations

#### Scenario: Constructor accepts four dependencies

* **WHEN** ReservationApplicationService constructor is inspected
* **THEN** it SHALL accept ReservationRepository, ReservationDomainEventPublisher, ReservationApplicationMapper, and ReservationSagaOrchestrator
