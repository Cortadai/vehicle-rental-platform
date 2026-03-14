## Context

La plataforma corre en Docker Compose con PostgreSQL, RabbitMQ y 4 microservicios Spring Boot (Paketo images). Necesitamos el stack de observabilidad Grafana (LGTM) como infraestructura base para que el change siguiente (observability-instrumentation) instrumente los servicios.

## Goals / Non-Goals

**Goals:**
- Stack Grafana completo desplegable con `docker compose up -d`
- Alloy como agente unificado: recoleccion de logs Docker + relay OTLP de traces
- Prometheus configurado para scrape de `/actuator/prometheus` de los 4 servicios
- Grafana provisionado automaticamente con datasources y dashboards (zero setup manual)
- Tempo listo para recibir traces OTLP via Alloy

**Non-Goals:**
- Instrumentacion de los servicios Spring Boot (change siguiente)
- Alertas de Grafana — solo dashboards y explore
- Persistencia de datos de observabilidad entre restarts (volumenes efimeros para POC)
- Alta disponibilidad de ningun componente

## Decisions

### D1: Alloy como intermediario para todo

**Decision:** Logs y traces pasan por Alloy. Los servicios no envian directamente a Loki/Tempo.

**Alternativa descartada:** Servicios envian traces directo a Tempo (mas simple). Pero Alloy como intermediario es el patron de produccion y aprenderemos el agente de recoleccion.

**Rationale:** En produccion real, los servicios no conocen los backends de observabilidad. Un agente como Alloy desacopla la instrumentacion del destino.

### D2: Logs via Docker socket, no OTLP logs

**Decision:** Alloy recoge logs de Docker via `loki.source.docker` (Unix socket), no via OTLP Log export desde los servicios.

**Rationale:** Las Paketo images no tienen shell utilities. Enviar logs via OTLP requeriria configurar un appender de Logback. Docker log collection es zero-config desde el lado del servicio — Alloy recoge lo que Docker captura de stdout.

### D3: Provisioning automatico de Grafana

**Decision:** Datasources y dashboards se pre-configuran via ficheros YAML y JSON en `docker/grafana/provisioning/`.

**Rationale:** Zero setup manual. Cualquiera que haga `docker compose up -d` tiene Grafana listo con los 3 datasources (Prometheus, Loki, Tempo) y dashboards JVM importados.

### D4: Prometheus scrape model (no OTLP metrics push)

**Decision:** Prometheus scrape los endpoints `/actuator/prometheus` de los 4 servicios. No usamos OTLP metrics push.

**Rationale:** Scrape es el modelo estandar de Prometheus y el mas compatible. OTLP metrics push es mas nuevo pero requiere configuracion adicional en Prometheus (remote write).

### D5: Tempo con storage local (no S3/GCS)

**Decision:** Tempo usa almacenamiento local (`/var/tempo`). Sin backend de objetos.

**Rationale:** Para un POC local no necesitamos persistencia duradera. Los traces se pierden al recrear el contenedor — aceptable para desarrollo.

### D6: Organizacion de ficheros de configuracion

**Decision:** Cada componente del stack tiene su carpeta bajo `docker/`:
```
docker/
├── grafana/provisioning/datasources/
├── grafana/provisioning/dashboards/
├── grafana/dashboards/          (JSON files)
├── prometheus/prometheus.yml
├── tempo/tempo.yml
├── loki/loki.yml
└── alloy/config.alloy
```

**Rationale:** Consistente con la estructura existente (`docker/postgres/`, `docker/rabbitmq/`). Cada componente autocontenido.

## Risks / Trade-offs

**[Memoria]** → El stack completo anade ~2GB de memoria. En maquinas con poca RAM, los contenedores pueden competir con los servicios.

**[Complejidad de red]** → 11 contenedores requieren conectividad entre ellos. Docker Compose default network maneja esto pero los nombres de host deben coincidir con los service names.

**[Alloy config syntax]** → Alloy usa su propio DSL (.alloy), no YAML. La curva de aprendizaje es real pero la documentacion es buena.
