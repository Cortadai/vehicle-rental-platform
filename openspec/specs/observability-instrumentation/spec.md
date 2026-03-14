# observability-instrumentation Specification

## Purpose
Instrumentar los 4 microservicios con Micrometer Tracing, OpenTelemetry OTLP y Prometheus metrics para conectar con el stack Grafana.

## ADDED Requirements

### Requirement: Distributed tracing via OTLP
Los 4 servicios SHALL exportar traces via OTLP HTTP a Alloy.

#### Scenario: Traces exported to Alloy
- **WHEN** un servicio recibe una peticion HTTP o un mensaje RabbitMQ
- **THEN** SHALL crear un span con traceId y spanId
- **AND** SHALL exportar el span via OTLP HTTP al endpoint configurado

### Requirement: Prometheus metrics endpoint
Los 4 servicios SHALL exponer metricas en formato Prometheus.

#### Scenario: Actuator prometheus endpoint
- **WHEN** se accede a `/actuator/prometheus` de cualquier servicio
- **THEN** SHALL responder con metricas en formato Prometheus (text/plain)
- **AND** SHALL incluir metricas JVM (memoria, GC, threads) y HTTP (request count, latencia)

### Requirement: Logging correlation with traceId
Los 4 servicios SHALL incluir traceId y spanId en los logs.

#### Scenario: MDC auto-injection
- **WHEN** un servicio procesa una peticion con un trace activo
- **THEN** los logs SHALL incluir traceId y spanId en el patron de salida

### Requirement: RabbitMQ trace propagation
Los traces SHALL propagarse automaticamente entre servicios via mensajes RabbitMQ.

#### Scenario: SAGA trace continuity
- **WHEN** el Reservation Service envia un comando SAGA via RabbitMQ
- **AND** el Customer/Payment/Fleet Service recibe y procesa el comando
- **THEN** ambos spans SHALL compartir el mismo traceId
- **AND** SHALL ser visibles como parte del mismo trace en Tempo

### Requirement: Dependencies managed by Spring Boot BOM
Las dependencias de observabilidad SHALL usar versiones gestionadas por Spring Boot.

#### Scenario: No explicit versions
- **WHEN** se inspecciona el parent POM
- **THEN** las dependencias de micrometer y opentelemetry SHALL no tener version explicita
