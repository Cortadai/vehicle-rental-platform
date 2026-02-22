## MODIFIED Requirements

### Requirement: ReservationPersistenceMapper converts bidirectionally with parent-child reconstruct

ReservationPersistenceMapper SHALL be a plain Java class that receives an `ObjectMapper` via constructor injection. It SHALL convert between domain `Reservation` (with `ReservationItem` children) and `ReservationJpaEntity` (with `ReservationItemJpaEntity` children) in both directions. The JPA-to-domain direction SHALL use `Reservation.reconstruct()` and `ReservationItem.reconstruct()` to rebuild the aggregate with its children.

#### Scenario: Mapper requires ObjectMapper via constructor

- **WHEN** `ReservationPersistenceMapper` is instantiated
- **THEN** it SHALL require an `ObjectMapper` parameter in its constructor
- **AND** it SHALL store the `ObjectMapper` as a private final field
- **AND** `BeanConfiguration` SHALL pass Spring's auto-configured `ObjectMapper` to the factory method

#### Scenario: Domain to JPA entity — failureMessages serialized as JSON

- **WHEN** `toJpaEntity(Reservation)` is called with a reservation that has `failureMessages = ["msg1", "msg2"]`
- **THEN** the `failureMessages` field in the JPA entity SHALL be the JSON string `["msg1","msg2"]` produced by `objectMapper.writeValueAsString()`
- **AND** if `failureMessages` is null or empty, the field SHALL be null

#### Scenario: JPA to domain entity — failureMessages deserialized from JSON

- **WHEN** `toDomainEntity(ReservationJpaEntity)` is called with a JPA entity that has `failureMessages = '["msg1","msg2"]'`
- **THEN** the domain entity SHALL have `failureMessages = List.of("msg1", "msg2")` produced by `objectMapper.readValue()` with `TypeReference<List<String>>`
- **AND** if `failureMessages` is null or blank, the domain entity SHALL have an empty list

#### Scenario: Serialization failure throws RuntimeException

- **WHEN** `objectMapper.writeValueAsString()` throws `JsonProcessingException` during serialization
- **THEN** the mapper SHALL wrap and rethrow it as `RuntimeException("Failed to serialize failureMessages")`

#### Scenario: Deserialization failure throws RuntimeException

- **WHEN** `objectMapper.readValue()` throws `JsonProcessingException` during deserialization
- **THEN** the mapper SHALL wrap and rethrow it as `RuntimeException("Failed to deserialize failureMessages")`
