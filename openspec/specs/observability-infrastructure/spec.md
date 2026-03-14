# observability-infrastructure Specification

## Purpose
Stack Grafana completo (Loki + Tempo + Prometheus + Alloy + Grafana) desplegable con Docker Compose para observabilidad de los 4 microservicios.

## ADDED Requirements

### Requirement: Observability stack in Docker Compose
Docker Compose SHALL incluir los 5 servicios de observabilidad.

#### Scenario: All observability services running
- **WHEN** se ejecuta `docker compose up -d`
- **THEN** SHALL arrancar Grafana en puerto 3000
- **AND** SHALL arrancar Loki en puerto 3100
- **AND** SHALL arrancar Tempo en puerto 3200
- **AND** SHALL arrancar Prometheus en puerto 9090
- **AND** SHALL arrancar Alloy en puerto 12345

### Requirement: Alloy collects Docker logs
Alloy SHALL recoger logs de todos los contenedores Docker y enviarlos a Loki.

#### Scenario: Log collection via Docker socket
- **WHEN** un servicio escribe logs a stdout
- **THEN** Alloy SHALL recoger el log via Docker socket
- **AND** SHALL enviarlo a Loki con el nombre del contenedor como label

### Requirement: Alloy relays OTLP traces
Alloy SHALL actuar como receptor OTLP y reenviar traces a Tempo.

#### Scenario: OTLP trace relay
- **WHEN** un servicio envia traces via OTLP HTTP (puerto 4318) o gRPC (puerto 4317)
- **THEN** Alloy SHALL recibir los traces
- **AND** SHALL reenviarlos a Tempo

### Requirement: Prometheus scrapes microservices
Prometheus SHALL estar configurado para scrape de los 4 microservicios.

#### Scenario: Scrape configuration
- **WHEN** se inspecciona la configuracion de Prometheus
- **THEN** SHALL tener scrape configs para customer-service (8181), fleet-service (8182), reservation-service (8183), payment-service (8184)
- **AND** el metrics_path SHALL ser `/actuator/prometheus`

### Requirement: Grafana provisioned automatically
Grafana SHALL arrancar con datasources y dashboards pre-configurados.

#### Scenario: Auto-provisioned datasources
- **WHEN** Grafana arranca por primera vez
- **THEN** SHALL tener Prometheus como datasource (default)
- **AND** SHALL tener Loki como datasource
- **AND** SHALL tener Tempo como datasource

#### Scenario: Auto-provisioned dashboards
- **WHEN** Grafana arranca por primera vez
- **THEN** SHALL tener al menos un dashboard JVM/Spring Boot importado

### Requirement: Tempo accepts OTLP traces
Tempo SHALL estar configurado para recibir traces via OTLP.

#### Scenario: OTLP receiver configured
- **WHEN** se inspecciona la configuracion de Tempo
- **THEN** SHALL aceptar traces via OTLP HTTP y gRPC
