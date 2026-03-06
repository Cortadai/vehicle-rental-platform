## ADDED Requirements

### Requirement: FleetConfirmedEvent carries confirmation success with SAGA correlation

FleetConfirmedEvent SHALL be a Java record in `com.vehiclerental.fleet.domain.event` implementing `DomainEvent`. Beyond the base contract (eventId, occurredOn), it SHALL carry `vehicleId` (VehicleId) and `reservationId` (UUID). The reservationId uses raw `java.util.UUID` (not a typed ID) to avoid cross-domain dependency.

#### Scenario: Confirmed event has all fields

- **WHEN** a FleetConfirmedEvent is constructed
- **THEN** `eventId()` SHALL return a non-null UUID
- **AND** `occurredOn()` SHALL return a non-null Instant
- **AND** `vehicleId()` SHALL return the vehicle's VehicleId
- **AND** `reservationId()` SHALL return the reservation's UUID for SAGA correlation

#### Scenario: Null eventId rejected

- **WHEN** a FleetConfirmedEvent is constructed with a null eventId
- **THEN** it SHALL throw FleetDomainException

#### Scenario: Null occurredOn rejected

- **WHEN** a FleetConfirmedEvent is constructed with a null occurredOn
- **THEN** it SHALL throw FleetDomainException

### Requirement: FleetRejectedEvent carries confirmation failure with SAGA correlation

FleetRejectedEvent SHALL be a Java record in `com.vehiclerental.fleet.domain.event` implementing `DomainEvent`. Beyond the base contract (eventId, occurredOn), it SHALL carry `vehicleId` (VehicleId), `reservationId` (UUID), and `failureMessages` (List<String>). The naming `Rejected` is consistent with CustomerRejectedEvent and enables routing key derivation to `fleet.rejected`.

#### Scenario: Rejected event has all fields

- **WHEN** a FleetRejectedEvent is constructed
- **THEN** `eventId()` SHALL return a non-null UUID
- **AND** `occurredOn()` SHALL return a non-null Instant
- **AND** `vehicleId()` SHALL return the vehicle's VehicleId
- **AND** `reservationId()` SHALL return the reservation's UUID for SAGA correlation
- **AND** `failureMessages()` SHALL return the list of failure reason strings

#### Scenario: Null eventId rejected

- **WHEN** a FleetRejectedEvent is constructed with a null eventId
- **THEN** it SHALL throw FleetDomainException

#### Scenario: Null occurredOn rejected

- **WHEN** a FleetRejectedEvent is constructed with a null occurredOn
- **THEN** it SHALL throw FleetDomainException

### Requirement: FleetReleasedEvent carries compensation acknowledgment with SAGA correlation

FleetReleasedEvent SHALL be a Java record in `com.vehiclerental.fleet.domain.event` implementing `DomainEvent`. Beyond the base contract (eventId, occurredOn), it SHALL carry `vehicleId` (VehicleId) and `reservationId` (UUID). It does NOT carry failureMessages — release (compensation) is always successful.

#### Scenario: Released event has all fields

- **WHEN** a FleetReleasedEvent is constructed
- **THEN** `eventId()` SHALL return a non-null UUID
- **AND** `occurredOn()` SHALL return a non-null Instant
- **AND** `vehicleId()` SHALL return the vehicle's VehicleId
- **AND** `reservationId()` SHALL return the reservation's UUID for SAGA correlation

#### Scenario: Null eventId rejected

- **WHEN** a FleetReleasedEvent is constructed with a null eventId
- **THEN** it SHALL throw FleetDomainException

#### Scenario: Null occurredOn rejected

- **WHEN** a FleetReleasedEvent is constructed with a null occurredOn
- **THEN** it SHALL throw FleetDomainException

### Requirement: SAGA events have zero Spring dependencies

FleetConfirmedEvent, FleetRejectedEvent, and FleetReleasedEvent SHALL NOT import any type from `org.springframework.*`. They SHALL reside in the domain layer alongside the 4 existing lifecycle events.

#### Scenario: No Spring imports in SAGA events

- **WHEN** the imports of FleetConfirmedEvent, FleetRejectedEvent, and FleetReleasedEvent are inspected
- **THEN** none SHALL import any type from `org.springframework.*`
