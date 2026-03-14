## Context

4 microservicios Spring Boot 3.4.13 con Actuator (solo /health expuesto). Stack Grafana desplegado (change #29) con Alloy como receptor OTLP en puerto 4318 y Prometheus scrapeando `/actuator/prometheus`. Los servicios necesitan emitir traces, metricas y logs estructurados.

## Goals / Non-Goals

**Goals:**
- Traces OTLP enviados a Alloy (→ Tempo) con 100% sampling para desarrollo
- Metricas Prometheus expuestas en `/actuator/prometheus`
- Logging correlation con traceId/spanId en MDC (automatico con Micrometer Tracing)
- Propagacion automatica de trace context en mensajes RabbitMQ
- Trace completo de la SAGA visible en Grafana Tempo

**Non-Goals:**
- Structured logging JSON (ECS) — evaluado y diferido, requiere Spring Boot 3.4.4+ y puede complicar la legibilidad en terminal
- Custom metrics o spans manuales — la instrumentacion automatica es suficiente
- Alertas de Grafana — solo visualizacion

## Decisions

### D1: Dependencias solo en container POMs, versiones gestionadas por BOM

**Decision:** Declarar las 3 dependencias en parent `dependencyManagement` (sin version) y en los 4 container POMs. Spring Boot BOM gestiona las versiones.

**Rationale:** Consistente con el patron existente (actuator, springdoc). No pinear versiones evita conflictos con el BOM.

### D2: OTLP HTTP endpoint apuntando a Alloy

**Decision:** `management.otlp.tracing.endpoint: http://alloy:4318/v1/traces` en Docker Compose. Para desarrollo local sin Docker, un perfil o variable de entorno.

**Rationale:** Los servicios no conocen Tempo directamente — solo hablan con Alloy. Desacopla instrumentacion del backend.

### D3: Logging correlation via patron, no structured JSON

**Decision:** Usar `logging.pattern.correlation` para incluir traceId/spanId en el patron de logging estandar. No activar structured logging JSON (ECS) por ahora.

**Alternativa considerada:** `logging.structured.format.console: ecs` para JSON logs. Pero requiere Spring Boot 3.4.4+ (tenemos 3.4.13, compatible), y los logs JSON son ilegibles en terminal durante desarrollo.

**Rationale:** El patron `[service,traceId,spanId]` es legible en terminal y Loki los indexa igualmente via Alloy. Si en el futuro se necesita JSON, es un cambio de una linea en application.yml.

### D4: Sampling al 100% para desarrollo

**Decision:** `management.tracing.sampling.probability: 1.0` — tracear todas las peticiones.

**Rationale:** POC de aprendizaje con volumen minimo. En produccion se reduciria a 0.1 (10%).

### D5: Endpoint OTLP configurable via variable de entorno

**Decision:** Usar variable de entorno `OTEL_EXPORTER_OTLP_ENDPOINT` en Docker Compose que Spring Boot lee automaticamente, y un valor por defecto en application.yml para desarrollo local.

**Rationale:** Permite ejecutar los servicios tanto en Docker Compose (apuntando a `alloy:4318`) como en local sin Docker (apuntando a `localhost:4318` o deshabilitado).

## Risks / Trade-offs

**[Volumen de traces]** → Con 100% sampling y 4 servicios + SAGA, el volumen de traces es alto. Aceptable para POC local.

**[RabbitMQ trace propagation]** → Spring AMQP propaga trace context automaticamente, pero requiere que `micrometer-tracing-bridge-otel` este en el classpath de TODOS los servicios participantes. Si un servicio no lo tiene, la cadena se rompe.
