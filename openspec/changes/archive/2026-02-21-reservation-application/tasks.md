## 1. Module Setup

- [x] 1.1 Create `reservation-service/reservation-application/pom.xml` — inherits from root parent POM, depends on `reservation-domain` and `spring-tx`, packaging `jar`

- [x] 1.2 Add `reservation-service/reservation-application` to root POM modules list (after `reservation-domain`, before `reservation-infrastructure`)

- [x] 1.3 Add `reservation-application` to root POM `<dependencyManagement>` with `${vehicle-rental.version}`

- [x] 1.4 Create source directory structure under `reservation-service/reservation-application/src/main/java/com/vehiclerental/reservation/application/` with subdirectories: `port/input/`, `port/output/`, `service/`, `dto/command/`, `dto/response/`, `mapper/`, `exception/`

- [x] 1.5 Create test directory structure under `reservation-service/reservation-application/src/test/java/com/vehiclerental/reservation/application/` with subdirectories: `service/`, `mapper/`, `exception/`

## 2. DTOs (Commands and Responses)

- [x] 2.1 Create `CreateReservationCommand.java` — record with customerId, pickupAddress, pickupCity, returnAddress, returnCity, pickupDate, returnDate, currency (all String), items (List\<CreateReservationItemCommand\>); inner record `CreateReservationItemCommand` with vehicleId (String), dailyRate (BigDecimal), days (int)

- [x] 2.2 Create `TrackReservationCommand.java` — record with trackingId (String)

- [x] 2.3 Create `CreateReservationResponse.java` — record with trackingId (String), status (String)

- [x] 2.4 Create `TrackReservationResponse.java` — record with trackingId, customerId, pickupAddress, pickupCity, returnAddress, returnCity, pickupDate, returnDate, status (all String), totalPrice (BigDecimal), currency (String), items (List\<TrackReservationItemResponse\>), failureMessages (List\<String\>), createdAt (Instant); inner record `TrackReservationItemResponse` with vehicleId (String), dailyRate (BigDecimal), days (int), subtotal (BigDecimal)

## 3. Exception

- [x] 3.1 Write `ReservationNotFoundExceptionTest.java` — tests for: message contains tracking ID, extends RuntimeException (NOT ReservationDomainException)

- [x] 3.2 Create `ReservationNotFoundException.java` — extends RuntimeException, message includes tracking ID, lives in `application/exception/`

## 4. Output Port

- [x] 4.1 Create `ReservationDomainEventPublisher.java` — interface with `void publish(List<DomainEvent> domainEvents)` method

## 5. Input Ports

- [x] 5.1 Create `CreateReservationUseCase.java` — interface with `CreateReservationResponse execute(CreateReservationCommand command)`

- [x] 5.2 Create `TrackReservationUseCase.java` — interface with `TrackReservationResponse execute(TrackReservationCommand command)`

## 6. Mapper

- [x] 6.1 Write `ReservationApplicationMapperTest.java` — tests for: toCreateResponse maps trackingId and status from Reservation, toTrackResponse maps all fields including items (vehicleId, dailyRate, days, subtotal), Money mapped to BigDecimal + String, DateRange mapped to String dates, PickupLocation mapped to String address + city, failureMessages preserved

- [x] 6.2 Create `ReservationApplicationMapper.java` — plain Java class, `toCreateResponse(Reservation)` returns CreateReservationResponse, `toTrackResponse(Reservation)` returns TrackReservationResponse with item mapping

## 7. Application Service

- [x] 7.1 Write `ReservationApplicationServiceCreateTest.java` — tests for: create flow (convert command → domain objects → Reservation.create() → save → publish → clearDomainEvents → return CreateReservationResponse with trackingId and status PENDING), items converted with currency from command, repository save() called once, eventPublisher publish() called with domain events, clearDomainEvents() called after publish, no @Service or @Component annotations on class, @Transactional on create method

- [x] 7.2 Write `ReservationApplicationServiceTrackTest.java` — tests for: track found returns TrackReservationResponse with full snapshot including items, track not found throws ReservationNotFoundException, invalid trackingId format throws exception, @Transactional(readOnly=true) on track method

- [x] 7.3 Create `ReservationApplicationService.java` — implements CreateReservationUseCase and TrackReservationUseCase, constructor injection (reservationRepository, eventPublisher, mapper), `@Transactional` on create, `@Transactional(readOnly = true)` on track, zero business logic, save → publish → clearDomainEvents cycle on create

## 8. Verification

- [x] 8.1 Run `mvn clean install` from `reservation-service/reservation-application/` — compile + all tests pass
- [x] 8.2 Verify no `@Service` or `@Component` annotations: `grep -r "@Service\|@Component" reservation-service/reservation-application/src/main/` returns nothing
- [x] 8.3 Verify `reservation-domain` still compiles cleanly: `mvn clean compile` from `reservation-service/reservation-domain/`
- [x] 8.4 Run `mvn clean install` from root — full platform build (all modules) passes green
