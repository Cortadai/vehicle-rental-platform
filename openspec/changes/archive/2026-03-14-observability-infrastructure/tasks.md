## 1. Tempo Configuration

- [x] 1.1 Create `docker/tempo/tempo.yml` with OTLP receiver (HTTP + gRPC) and local storage backend

## 2. Loki Configuration

- [x] 2.1 Create `docker/loki/loki.yml` with local storage and minimal retention config

## 3. Prometheus Configuration

- [x] 3.1 Create `docker/prometheus/prometheus.yml` with scrape configs for 4 microservices on `/actuator/prometheus`

## 4. Alloy Configuration

- [x] 4.1 Create `docker/alloy/config.alloy` with Docker log collection (loki.source.docker → Loki) and OTLP trace relay (otelcol.receiver.otlp → Tempo)

## 5. Grafana Provisioning

- [x] 5.1 Create `docker/grafana/provisioning/datasources/datasources.yml` with Prometheus (default), Loki, Tempo
- [x] 5.2 Create `docker/grafana/provisioning/dashboards/dashboards.yml` with dashboard provider config
- [x] 5.3 Download and place JVM Micrometer dashboard JSON (ID 4701) in `docker/grafana/dashboards/` — fixed datasource UID (replaced `${DS_PROMETHEUS}` with `Prometheus`, removed `__inputs`/`__requires`)

## 6. Docker Compose

- [x] 6.1 Add 5 observability services to `docker-compose.yml`: grafana, loki, tempo, prometheus, alloy — with volumes, ports, depends_on, and config mounts

## 7. Verification

- [x] 7.1 Run `docker compose up -d` — all 11 containers running
- [x] 7.2 Verify Grafana accessible at http://localhost:3000 with 3 datasources provisioned (Prometheus, Loki, Tempo)
- [x] 7.3 Verify Prometheus accessible at http://localhost:9090 with 4 scrape targets listed (down expected — services not instrumented yet)
- [x] 7.4 Verify Alloy UI accessible at http://localhost:12345
