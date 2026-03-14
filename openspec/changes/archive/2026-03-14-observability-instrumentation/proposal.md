## Why

El stack Grafana (change #29) esta desplegado pero los 4 microservicios no emiten traces, metricas ni logs estructurados. Prometheus muestra los targets como "down" porque los servicios no exponen `/actuator/prometheus`. Los traces de la SAGA (el flujo mas valioso de observar) no llegan a Tempo. Este change instrumenta los servicios para conectar con la infraestructura de observabilidad.

## What Changes

- 3 nuevas dependencias Maven en los 4 container POMs: `micrometer-tracing-bridge-otel`, `opentelemetry-exporter-otlp`, `micrometer-registry-prometheus`
- Configuracion `application.yml` de los 4 servicios: tracing OTLP hacia Alloy, actuator prometheus, logging correlation con traceId/spanId
- Structured logging JSON (ECS) para produccion (Docker Compose)
- Propagacion automatica de trace context en mensajes RabbitMQ (via Spring AMQP + Micrometer Tracing)

## Capabilities

### New Capabilities
- `observability-instrumentation`: Instrumentacion de los 4 servicios con Micrometer Tracing, OpenTelemetry OTLP, Prometheus metrics y structured logging

### Modified Capabilities
(ninguna — los servicios mantienen su comportamiento funcional, solo anaden telemetria)

## Impact

- **POMs modificados**: 1 parent + 4 container POMs
- **application.yml**: 4 ficheros actualizados
- **Sin cambios en codigo Java**: toda la instrumentacion es via dependencias + configuracion
- **Sin impacto en tests**: las dependencias de tracing no afectan al comportamiento funcional
- **Prerequisito**: stack de observabilidad corriendo (change #29)
