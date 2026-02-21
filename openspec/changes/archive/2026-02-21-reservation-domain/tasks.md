## 1. Module Setup

- [x] 1.1 Add `reservation-domain` to root POM `<dependencyManagement>` with `${vehicle-rental.version}` and add `reservation-service/reservation-domain` to `<modules>` list (after `common`, before `reservation-service/reservation-infrastructure`)
- [x] 1.2 Create `reservation-service/reservation-domain/pom.xml` — inherits from root parent POM, depends on `common`, zero Spring dependencies in compile scope, packaging `jar`
- [x] 1.3 Create directory structure under `reservation-service/reservation-domain/src/main/java/com/vehiclerental/reservation/domain/` with subdirectories: `model/aggregate/`, `model/entity/`, `model/vo/`, `event/`, `exception/`, `port/output/`
- [x] 1.4 Create test directory structure under `reservation-service/reservation-domain/src/test/java/com/vehiclerental/reservation/domain/` with subdirectories: `model/aggregate/`, `model/entity/`, `model/vo/`, `event/`, `exception/`
- [x] 1.5 Verify module compiles: `mvn clean compile` from `reservation-service/reservation-domain/`

## 2. Domain Exception (first — needed by VOs, events, and aggregate)

- [x] 2.1 Create `ReservationDomainExceptionTest.java` — tests for: errorCode accessible, message accessible, constructor with cause, extends DomainException from common
- [x] 2.2 Create `ReservationDomainException.java` — extends DomainException from common, constructors requiring errorCode

## 3. Value Objects and Enum (Test-First)

- [x] 3.1 Create `ReservationIdTest.java` — tests for valid construction, null UUID rejected (throws ReservationDomainException), equality by value
- [x] 3.2 Create `ReservationId.java` — record wrapping UUID, null validation in compact constructor
- [x] 3.3 Create `TrackingIdTest.java` — tests for valid construction, null UUID rejected, equality by value
- [x] 3.4 Create `TrackingId.java` — record wrapping UUID, null validation in compact constructor
- [x] 3.5 Create `CustomerIdTest.java` — tests for valid construction, null UUID rejected, equality by value (local to reservation bounded context)
- [x] 3.6 Create `CustomerId.java` — record wrapping UUID, local to reservation domain, NOT imported from customer-domain
- [x] 3.7 Create `VehicleIdTest.java` — tests for valid construction, null UUID rejected, equality by value (local to reservation bounded context)
- [x] 3.8 Create `VehicleId.java` — record wrapping UUID, local to reservation domain, NOT imported from fleet-domain
- [x] 3.9 Create `DateRangeTest.java` — tests for: valid construction, getDays() returns correct count, single day range, null pickupDate rejected, null returnDate rejected, returnDate equals pickupDate rejected, returnDate before pickupDate rejected, past dates accepted (no future-date validation)
- [x] 3.10 Create `DateRange.java` — record with LocalDate pickupDate and returnDate, structural validation only, getDays() method
- [x] 3.11 Create `PickupLocationTest.java` — tests for: valid construction, null address rejected, blank address rejected, null city rejected, blank city rejected
- [x] 3.12 Create `PickupLocation.java` — record with String address and city, non-null and non-blank validation
- [x] 3.13 Create `ReservationStatus.java` — enum with PENDING, CUSTOMER_VALIDATED, PAID, CONFIRMED, CANCELLING, CANCELLED

## 4. Domain Events (Test-First)

- [x] 4.1 Create `ReservationItemSnapshot.java` — record with VehicleId vehicleId, Money dailyRate, int days, Money subtotal
- [x] 4.2 Create `ReservationDomainEventsTest.java` — tests for: ReservationCreatedEvent fields accessible (reservationId, trackingId, customerId, totalPrice, dateRange, pickupLocation, returnLocation, items), null eventId rejected, null occurredOn rejected, ReservationCancelledEvent fields accessible (reservationId, failureMessages), all events implement DomainEvent, events are records
- [x] 4.3 Create `ReservationCreatedEvent.java` — record implementing DomainEvent with eventId, occurredOn, reservationId, trackingId, customerId, totalPrice, dateRange, pickupLocation, returnLocation, List<ReservationItemSnapshot> items; null validation for eventId and occurredOn
- [x] 4.4 Create `ReservationCancelledEvent.java` — record implementing DomainEvent with eventId, occurredOn, reservationId, List<String> failureMessages; null validation for eventId and occurredOn

## 5. ReservationItem Inner Entity (Test-First)

- [x] 5.1 Create `ReservationItemTest.java` — tests for: successful creation (UUID generated, subtotal calculated as dailyRate × days), single day calculation, null vehicleId rejected, null dailyRate rejected, zero dailyRate amount rejected, zero days rejected, negative days rejected, reconstruct preserves all fields, no public constructors
- [x] 5.2 Create `ReservationItem.java` — inner entity extending BaseEntity<UUID>, private constructor, `create(VehicleId, Money, int)` factory method, `reconstruct(UUID, VehicleId, Money, int, Money)` factory method, getters only

## 6. Reservation Aggregate Root (Test-First)

- [x] 6.1 Create `ReservationTest.java` — tests for: successful creation (status PENDING, ReservationId and TrackingId generated, ReservationCreatedEvent emitted, totalPrice calculated from items), null customerId rejected, null pickupLocation rejected, null returnLocation rejected, null dateRange rejected, null items rejected, empty items rejected, fields accessible after creation, reconstruct does not emit events, reconstruct preserves items and failureMessages, no public constructors
- [x] 6.2 Create `ReservationLifecycleTest.java` — tests for: validateCustomer from PENDING (CUSTOMER_VALIDATED + updatedAt updated), validateCustomer from non-PENDING throws with RESERVATION_INVALID_STATE, pay from CUSTOMER_VALIDATED (PAID), pay from non-CUSTOMER_VALIDATED throws, confirm from PAID (CONFIRMED), confirm from non-PAID throws, initCancel from PAID (CANCELLING + failureMessages stored), initCancel from non-PAID throws, initCancel with null messages rejected, initCancel with empty messages rejected, cancel from PENDING (CANCELLED + ReservationCancelledEvent with empty failureMessages), cancel from CUSTOMER_VALIDATED (CANCELLED + event), cancel from CANCELLING (CANCELLED + event with preserved failureMessages), cancel from CONFIRMED rejected, cancel from PAID rejected, cancel from CANCELLED rejected
- [x] 6.3 Create `ReservationEventEmissionTest.java` — tests for: validateCustomer does not register new events, pay does not register events (after clearing), confirm does not register events, initCancel does not register events
- [x] 6.4 Create `Reservation.java` — Aggregate Root extending AggregateRoot<ReservationId>, private constructor, `create()` factory method (generates IDs, calculates totalPrice, registers ReservationCreatedEvent), `reconstruct()` factory method, `validateCustomer()`, `pay()`, `confirm()`, `initCancel(List<String>)`, `cancel()`, getters only

## 7. Output Port

- [x] 7.1 Create `ReservationRepository.java` — interface with `save(Reservation)` returning Reservation, `findById(ReservationId)` returning `Optional<Reservation>`, and `findByTrackingId(TrackingId)` returning `Optional<Reservation>`, domain types only

## 8. Verification

- [x] 8.1 Run `mvn clean install` from `reservation-service/reservation-domain/` — compile + all tests pass
- [x] 8.2 Verify zero Spring imports: grep `org.springframework` in `reservation-service/reservation-domain/src/main/` returns nothing
- [x] 8.3 Verify zero imports from customer-domain or fleet-domain: grep `com.vehiclerental.customer` and `com.vehiclerental.fleet` in `reservation-service/reservation-domain/src/main/` returns nothing
- [x] 8.4 Run `mvn clean install` from root — full platform build passes (common + customer modules + fleet modules + reservation-domain)
