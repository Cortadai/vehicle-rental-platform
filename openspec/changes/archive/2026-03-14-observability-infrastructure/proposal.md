## Why

La plataforma tiene 4 microservicios con SAGA orchestration pero cero observabilidad — sin logs centralizados, sin traces distribuidos, sin metricas exportadas. Para un blueprint reutilizable necesitamos el stack de observabilidad mas usado en produccion con contenedores: Grafana (LGTM). Este primer change monta la infraestructura completa; el segundo (observability-instrumentation) instrumentara los servicios.

## What Changes

- 5 nuevos servicios en Docker Compose: Grafana, Loki, Tempo, Prometheus, Alloy
- Ficheros de configuracion para cada componente del stack
- Grafana provisionado automaticamente con datasources (Prometheus, Loki, Tempo) y dashboards (JVM Micrometer)
- Alloy configurado como recolector de logs de Docker y relay OTLP para traces
- Prometheus configurado para scrape de los 4 microservicios

## Capabilities

### New Capabilities
- `observability-infrastructure`: Stack Grafana completo (Loki + Tempo + Prometheus + Alloy) desplegable con Docker Compose y Grafana provisionado automaticamente

### Modified Capabilities
(ninguna — este change solo anade infraestructura, no modifica los servicios)

## Impact

- **Docker Compose**: 5 nuevos servicios (11 total: 2 infra existentes + 4 microservicios + 5 observabilidad)
- **Ficheros nuevos**: ~10 ficheros de configuracion en `docker/`
- **Sin cambios en codigo Java**: los servicios se instrumentaran en el change siguiente
- **Puertos nuevos**: 3000 (Grafana), 3100 (Loki), 3200 (Tempo), 9090 (Prometheus), 12345 (Alloy UI)
- **Prerequisito**: Docker Compose con suficiente memoria (~2GB extra para el stack)
