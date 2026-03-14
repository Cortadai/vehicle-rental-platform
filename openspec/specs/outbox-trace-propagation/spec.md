# outbox-trace-propagation Specification

## Purpose
Propagar trace context a traves del Outbox Pattern para que los traces de la SAGA sean visibles end-to-end en Grafana Tempo.

## ADDED Requirements

### Requirement: OutboxEvent stores trace context
La tabla outbox_events SHALL tener una columna para almacenar el trace context.

#### Scenario: trace_parent column exists
- **WHEN** se inspecciona el schema de la tabla outbox_events
- **THEN** SHALL existir una columna `trace_parent` de tipo VARCHAR nullable

### Requirement: Trace context captured during event creation
Los publishers SHALL capturar el traceparent actual al crear un OutboxEvent.

#### Scenario: Publisher captures active trace
- **WHEN** un publisher crea un OutboxEvent durante un request HTTP con trace activo
- **THEN** el OutboxEvent SHALL contener el traceparent del trace actual en formato W3C (`00-{traceId}-{spanId}-01`)

#### Scenario: Publisher handles missing trace
- **WHEN** un publisher crea un OutboxEvent sin trace activo (tests, schedulers)
- **THEN** el OutboxEvent SHALL tener trace_parent como null
- **AND** la creacion SHALL no fallar

### Requirement: OutboxPublisher injects trace header
El OutboxPublisher SHALL inyectar el traceparent almacenado en los headers del mensaje AMQP.

#### Scenario: Message includes traceparent header
- **WHEN** el OutboxPublisher envia un mensaje con trace_parent no null
- **THEN** el mensaje AMQP SHALL incluir un header `traceparent` con el valor almacenado

#### Scenario: Message without traceparent
- **WHEN** el OutboxPublisher envia un mensaje con trace_parent null
- **THEN** el mensaje AMQP SHALL no incluir header traceparent
- **AND** el envio SHALL funcionar normalmente

### Requirement: End-to-end SAGA trace in Tempo
El flujo SAGA completo SHALL ser visible como un unico trace en Grafana Tempo.

#### Scenario: Full SAGA trace
- **WHEN** se ejecuta el Bruno E2E happy path y se busca el trace en Tempo
- **THEN** el trace SHALL contener spans de multiples servicios (reservation, customer, payment, fleet)
