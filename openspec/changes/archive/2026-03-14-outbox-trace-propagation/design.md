## Context

El OutboxPublisher corre en un @Scheduled thread cada 500ms, separado del thread HTTP que creo el evento. El trace context (traceId, spanId) vive en ThreadLocal durante el request HTTP pero no existe cuando el publisher recoge el evento. Necesitamos persisitir el trace context en la tabla outbox_events para reinyectarlo en el mensaje AMQP.

## Goals / Non-Goals

**Goals:**
- Trace completo de la SAGA visible en Grafana Tempo como un unico trace
- Compatibilidad con W3C Trace Context (header `traceparent`)
- Solucion en common-messaging (reutilizable por los 4 servicios)

**Non-Goals:**
- Propagar `tracestate` (solo `traceparent` es suficiente para linking)
- Modificar los listeners — Spring AMQP + Micrometer extraen el traceparent del header automaticamente
- Traces para el OutboxCleanupScheduler

## Decisions

### D1: Capturar traceparent via Micrometer Tracer

**Decision:** Inyectar `io.micrometer.tracing.Tracer` en los publishers y usar `tracer.currentSpan().context()` para obtener traceId y spanId. Construir el header W3C Trace Context manualmente: `00-{traceId}-{spanId}-01`.

**Alternativa descartada:** Usar OpenTelemetry SDK directamente (`Span.current().getSpanContext()`). Funcionaria pero acopla al SDK de OTel en vez de usar la abstraccion de Micrometer.

**Rationale:** Micrometer Tracer es la abstraccion que Spring Boot usa. Si en el futuro se cambia de OTel a Brave, el codigo sigue funcionando.

### D2: Columna nullable en outbox_events

**Decision:** `trace_parent VARCHAR(256) NULL` — nullable para backward compatibility. Eventos creados sin trace context (tests, schedulers) funcionan sin romper.

**Rationale:** Los tests unitarios y algunos schedulers no tienen trace context activo. Forzar NOT NULL romperia esos flujos.

### D3: Captura centralizada en OutboxEvent.create()

**Decision:** Cambiar la firma de `OutboxEvent.create()` para aceptar un parametro `traceParent` opcional. Los publishers pasan el valor; OutboxEvent lo almacena.

**Alternativa descartada:** Capturar el traceparent dentro de OutboxEvent.create() directamente. Romperia la separacion — OutboxEvent es una entidad JPA en common-messaging que no deberia depender de Micrometer Tracer.

**Rationale:** La responsabilidad de obtener el trace context es del publisher (infrastructure layer), no de la entidad.

### D4: Solo inyectar header traceparent en el mensaje

**Decision:** En `OutboxPublisher.processEvent()`, si el OutboxEvent tiene `traceParent` no null, anadir `.setHeader("traceparent", event.getTraceParent())` al MessageBuilder.

**Rationale:** Spring AMQP + Micrometer Tracing detecta automaticamente el header `traceparent` en el consumer y restaura el trace context. No necesitamos codigo manual en los listeners.

### D5: Helper method estatico para construir traceparent

**Decision:** Crear un utility class `TraceContextHelper` en common-messaging con un metodo estatico `currentTraceparent(Tracer)` que devuelve el traceparent string o null si no hay trace activo.

**Rationale:** Evita duplicar la logica de construccion del traceparent en 5 publishers diferentes. El helper esta en common-messaging, accesible por todos.

## Risks / Trade-offs

**[Micrometer Tracer en infrastructure]** → Los publishers (infrastructure layer) necesitan inyectar `Tracer`. Esto es aceptable — Tracer es una dependencia de observabilidad, no de negocio. No afecta domain ni application.

**[Tests existentes]** → Los tests unitarios de los publishers usan mocks. Necesitaran un mock de `Tracer` o pasar null. Con el campo nullable, pasar null es seguro.
