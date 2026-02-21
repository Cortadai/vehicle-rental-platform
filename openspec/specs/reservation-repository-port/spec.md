# reservation-repository-port Specification

## Purpose
ReservationRepository output port interface with save, findById, and findByTrackingId operations.

## Requirements

### Requirement: ReservationRepository defines save, findById, and findByTrackingId

ReservationRepository SHALL be a Java interface with three methods:
- `Reservation save(Reservation reservation)` — persists a Reservation and returns the persisted instance
- `Optional<Reservation> findById(ReservationId reservationId)` — retrieves a Reservation by its internal typed ID
- `Optional<Reservation> findByTrackingId(TrackingId trackingId)` — retrieves a Reservation by its public-facing tracking ID

#### Scenario: Save method signature

* **WHEN** the ReservationRepository interface is inspected
* **THEN** it SHALL declare a `save` method accepting `Reservation` and returning `Reservation`

#### Scenario: FindById method signature

* **WHEN** the ReservationRepository interface is inspected
* **THEN** it SHALL declare a `findById` method accepting `ReservationId` and returning `Optional<Reservation>`

#### Scenario: FindByTrackingId method signature

* **WHEN** the ReservationRepository interface is inspected
* **THEN** it SHALL declare a `findByTrackingId` method accepting `TrackingId` and returning `Optional<Reservation>`

### Requirement: ReservationRepository uses only domain types

ReservationRepository SHALL NOT reference any Spring, JPA, or infrastructure types. Method signatures SHALL use only `Reservation`, `ReservationId`, `TrackingId`, and `java.util.Optional`.

#### Scenario: No framework imports

* **WHEN** the ReservationRepository source file is inspected
* **THEN** it SHALL contain no imports from `org.springframework.*`
* **AND** it SHALL contain no imports from `jakarta.persistence.*`

## Constraint: Zero Spring dependencies

No class in `com.vehiclerental.reservation.domain.port.output` SHALL import any type from `org.springframework.*`.
