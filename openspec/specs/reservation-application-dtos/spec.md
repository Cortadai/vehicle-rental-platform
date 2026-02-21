# reservation-application-dtos Specification

## Purpose
Command and Response records for Reservation use cases. Commands carry input data as primitives (Strings, BigDecimal, int). Responses carry output data for creation results and reservation tracking. All are Java records (immutable).

## Requirements

### Requirement: CreateReservationCommand carries creation data

CreateReservationCommand SHALL be a Java record with fields: customerId (String), pickupAddress (String), pickupCity (String), returnAddress (String), returnCity (String), pickupDate (String, ISO date format), returnDate (String, ISO date format), currency (String, currency code), items (List\<CreateReservationItemCommand\>).

#### Scenario: All fields accessible

* **WHEN** a CreateReservationCommand is constructed with valid data
* **THEN** `customerId()`, `pickupAddress()`, `pickupCity()`, `returnAddress()`, `returnCity()`, `pickupDate()`, `returnDate()`, `currency()` SHALL return the provided String values
* **AND** `items()` SHALL return the provided list of CreateReservationItemCommand

### Requirement: CreateReservationItemCommand carries item data

CreateReservationItemCommand SHALL be a Java record nested inside CreateReservationCommand with fields: vehicleId (String), dailyRate (BigDecimal), days (int).

#### Scenario: All fields accessible

* **WHEN** a CreateReservationItemCommand is constructed
* **THEN** `vehicleId()` SHALL return the provided String
* **AND** `dailyRate()` SHALL return the provided BigDecimal
* **AND** `days()` SHALL return the provided int

### Requirement: TrackReservationCommand carries tracking identity

TrackReservationCommand SHALL be a Java record with a single field: trackingId (String).

#### Scenario: TrackingId accessible

* **WHEN** a TrackReservationCommand is constructed with "some-uuid"
* **THEN** `trackingId()` SHALL return "some-uuid"

### Requirement: CreateReservationResponse carries creation result

CreateReservationResponse SHALL be a Java record with fields: trackingId (String), status (String).

#### Scenario: All fields accessible

* **WHEN** a CreateReservationResponse is constructed
* **THEN** `trackingId()` SHALL return the reservation's public-facing tracking ID
* **AND** `status()` SHALL return the reservation's status as a String (e.g., "PENDING")

### Requirement: TrackReservationResponse carries full reservation snapshot

TrackReservationResponse SHALL be a Java record with fields: trackingId (String), customerId (String), pickupAddress (String), pickupCity (String), returnAddress (String), returnCity (String), pickupDate (String), returnDate (String), status (String), totalPrice (BigDecimal), currency (String), items (List\<TrackReservationItemResponse\>), failureMessages (List\<String\>), createdAt (Instant).

#### Scenario: All fields accessible

* **WHEN** a TrackReservationResponse is constructed with full reservation data
* **THEN** all fields SHALL be accessible via record accessors

#### Scenario: Items list contains item details

* **WHEN** a TrackReservationResponse is constructed with 2 items
* **THEN** `items()` SHALL return a list of size 2
* **AND** each item SHALL have vehicleId, dailyRate, days, and subtotal

### Requirement: TrackReservationItemResponse carries item snapshot

TrackReservationItemResponse SHALL be a Java record nested inside TrackReservationResponse with fields: vehicleId (String), dailyRate (BigDecimal), days (int), subtotal (BigDecimal).

#### Scenario: All fields accessible

* **WHEN** a TrackReservationItemResponse is constructed
* **THEN** `vehicleId()` SHALL return the item's vehicle ID as String
* **AND** `dailyRate()` SHALL return the item's daily rate as BigDecimal
* **AND** `days()` SHALL return the item's rental days
* **AND** `subtotal()` SHALL return the item's subtotal as BigDecimal

### Requirement: Commands and Responses are plain Java records

All commands and responses SHALL be Java records with no Spring annotations and no validation annotations. They SHALL have zero framework dependencies.

#### Scenario: No framework imports

* **WHEN** any command or response class is inspected
* **THEN** it SHALL NOT import any type from `org.springframework.*` or `jakarta.*`

## Constraint: Zero Spring dependencies

No class in `com.vehiclerental.reservation.application.dto` SHALL import any type from `org.springframework.*`.
