## ADDED Requirements

### Requirement: OutboxFleetDomainEventPublisher implements FleetDomainEventPublisher

OutboxFleetDomainEventPublisher SHALL be a Spring `@Component` in `com.vehiclerental.fleet.infrastructure.adapter.output.event` that implements `FleetDomainEventPublisher`. It SHALL persist each domain event as an `OutboxEvent` via `OutboxEventRepository` in the same transaction as the business operation.

#### Scenario: Publishes lifecycle events to outbox

- **WHEN** `publish(List<DomainEvent>)` is called with a list containing VehicleRegisteredEvent, VehicleSentToMaintenanceEvent, VehicleActivatedEvent, or VehicleRetiredEvent
- **THEN** it SHALL create one `OutboxEvent` per event with aggregateType `FLEET`, the serialized event as payload, the derived routing key, and exchange `fleet.exchange`
- **AND** each OutboxEvent SHALL be saved via `OutboxEventRepository.save()`

#### Scenario: Publishes SAGA response events to outbox

- **WHEN** `publish(List<DomainEvent>)` is called with a list containing FleetConfirmedEvent, FleetRejectedEvent, or FleetReleasedEvent
- **THEN** it SHALL create one `OutboxEvent` per event with aggregateType `FLEET`, the serialized event as payload, the derived routing key, and exchange `fleet.exchange`

#### Scenario: Empty event list does nothing

- **WHEN** `publish(List<DomainEvent>)` is called with an empty list
- **THEN** it SHALL NOT save any OutboxEvent and SHALL NOT throw an exception

### Requirement: Routing key derivation via explicit Map

OutboxFleetDomainEventPublisher SHALL derive routing keys using a static `Map<Class, String>` mapping each event class to its routing key. This is necessary because Fleet's lifecycle events use the `Vehicle*` prefix while SAGA events use the `Fleet*` prefix, preventing uniform auto-derivation.

#### Scenario: Lifecycle events derive correct routing keys

- **WHEN** a VehicleRegisteredEvent is published
- **THEN** the routing key SHALL be `fleet.registered`
- **AND** when a VehicleSentToMaintenanceEvent is published, the routing key SHALL be `fleet.senttomaintenance`
- **AND** when a VehicleActivatedEvent is published, the routing key SHALL be `fleet.activated`
- **AND** when a VehicleRetiredEvent is published, the routing key SHALL be `fleet.retired`

#### Scenario: SAGA events derive correct routing keys

- **WHEN** a FleetConfirmedEvent is published
- **THEN** the routing key SHALL be `fleet.confirmed`
- **AND** when a FleetRejectedEvent is published, the routing key SHALL be `fleet.rejected`
- **AND** when a FleetReleasedEvent is published, the routing key SHALL be `fleet.released`

### Requirement: Aggregate ID extraction for all 7 event types

OutboxFleetDomainEventPublisher SHALL extract the aggregate ID from each event using `vehicleId().value().toString()`. It SHALL support all 7 event types via instanceof checks.

#### Scenario: All event types return vehicleId as aggregate ID

- **WHEN** any of the 7 fleet domain events is published
- **THEN** the OutboxEvent aggregateId SHALL be the string representation of the event's `vehicleId().value()`

#### Scenario: Unknown event type fails loudly

- **WHEN** a DomainEvent not matching any of the 7 known types is published
- **THEN** it SHALL throw `IllegalArgumentException`

### Requirement: Event serialization to JSON

OutboxFleetDomainEventPublisher SHALL serialize each domain event to JSON using Jackson `ObjectMapper`.

#### Scenario: Successful serialization

- **WHEN** a domain event is serialized
- **THEN** the OutboxEvent payload SHALL contain a valid JSON string representing the event

#### Scenario: Serialization failure throws IllegalStateException

- **WHEN** `ObjectMapper.writeValueAsString()` throws JsonProcessingException
- **THEN** OutboxFleetDomainEventPublisher SHALL throw `IllegalStateException` wrapping the original exception
