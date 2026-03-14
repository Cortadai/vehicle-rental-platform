## Why

El Outbox Pattern desacopla intencionalmente la escritura en BD del envio del mensaje para garantizar consistencia. Pero ese desacoplamiento rompe la continuidad del trace: el `OutboxPublisher` corre en un `@Scheduled` thread 500ms despues, sin el trace context del request HTTP original. Los traces en Tempo muestran spans aislados — no se puede seguir el flujo SAGA completo como un unico trace.

La solucion: almacenar el `traceparent` (W3C Trace Context) en la tabla outbox_events durante la creacion del evento (cuando el trace esta activo), y reinyectarlo en los headers del mensaje AMQP cuando el OutboxPublisher lo envia.

## What Changes

- Nueva columna `trace_parent` en la tabla `outbox_events` (migracion Flyway en 4 servicios)
- `OutboxEvent` entity: nuevo campo `traceParent` + metodo `create()` actualizado
- Captura del traceparent actual en todos los publishers que crean OutboxEvents (5 publishers en 4 servicios + 1 SAGA command publisher)
- `OutboxPublisher`: inyecta el header `traceparent` en el mensaje AMQP desde el valor almacenado en el OutboxEvent

## Capabilities

### New Capabilities
- `outbox-trace-propagation`: Propagacion de trace context a traves del Outbox Pattern, permitiendo traces end-to-end de la SAGA en Tempo

### Modified Capabilities
- `outbox-pattern`: El OutboxEvent ahora almacena y propaga trace context

## Impact

- **Migraciones Flyway**: 1 nueva migracion por servicio (4 total) — `ALTER TABLE ADD COLUMN`
- **common-messaging**: OutboxEvent.java + OutboxPublisher.java modificados
- **4 servicios**: Cada DomainEventPublisher captura el traceparent actual
- **Sin cambio en listeners**: Spring AMQP + Micrometer extraen automaticamente el traceparent del header del mensaje
- **Backward compatible**: la columna `trace_parent` es nullable — eventos existentes sin trace funcionan igual
