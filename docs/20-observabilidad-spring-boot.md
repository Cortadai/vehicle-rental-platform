# Area 20: Observabilidad - Spring Boot + Stack Grafana

> **Audiencia**: Desarrolladores junior/mid (guia detallada) + Seniors (referencia rapida)
> **Stack**: Spring Boot 3.4.x, Java 21, Micrometer Tracing, OpenTelemetry, Grafana + Loki + Tempo + Prometheus + Alloy

---

## 1. Los Tres Pilares de la Observabilidad

### Referencia Rapida (Seniors)

```
Logs    → SLF4J/Logback → JSON (ECS) → Alloy → Loki → Grafana
Traces  → Micrometer Tracing → OTel Bridge → OTLP → Alloy → Tempo → Grafana
Metrics → Micrometer Core → Prometheus Registry → /actuator/prometheus → Prometheus → Grafana

MDC: traceId + spanId inyectados automaticamente por Micrometer Tracing
Spring AMQP: propagacion de trace context automatica en mensajes RabbitMQ
```

### Guia Detallada (Junior/Mid)

#### ¿Que es observabilidad?

```
┌────────────────────────────────────────────────────────────────┐
│                     OBSERVABILIDAD                             │
├────────────────┬───────────────────┬───────────────────────────┤
│     LOGS       │     TRACES        │       METRICS             │
│                │                   │                           │
│  ¿Que paso?    │  ¿Por donde paso? │  ¿Cuanto esta pasando?    │
│                │                   │                           │
│  Texto/JSON    │  Spans + TraceId  │  Contadores, Gauges,      │
│  con contexto  │  entre servicios  │  Histogramas              │
│                │                   │                           │
│  Loki          │  Tempo            │  Prometheus               │
└────────────────┴───────────────────┴───────────────────────────┘
```

- **Logs**: Registros de eventos discretos. Con structured logging (JSON), se pueden filtrar y correlacionar.
- **Traces**: Seguimiento de una peticion a traves de multiples servicios. Un trace contiene multiples spans (uno por operacion).
- **Metrics**: Valores numericos agregados en el tiempo. Tasa de peticiones, latencia P99, uso de memoria JVM.

#### La correlacion es la clave

Sin correlacion, tienes 3 silos independientes. Con `traceId` en los logs, puedes:
1. Ver un error en los logs de Loki
2. Copiar el traceId
3. Ir a Tempo y ver el trace completo cruzando los 4 servicios
4. Ir a Prometheus y ver las metricas del momento exacto

---

## 2. Stack Grafana (LGTM)

### Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                        GRAFANA (:3000)                          │
│              Dashboards + Explore + Correlacion                 │
├──────────────┬──────────────────┬───────────────────────────────┤
│  LOKI        │    TEMPO         │    PROMETHEUS                 │
│  (logs)      │    (traces)      │    (metrics)                  │
│  :3100       │    :3200         │    :9090                      │
├──────────────┴──────────────────┴───────────────────────────────┤
│                        ALLOY (:12345)                           │
│      Log collector (Docker) + OTLP relay (traces)               │
│      discovery.docker → loki.source.docker → Loki               │
│      otelcol.receiver.otlp → otelcol.exporter.otlphttp → Tempo │
├─────────────────────────────────────────────────────────────────┤
│  Microservicios Spring Boot                                     │
│  micrometer-tracing-bridge-otel → OTLP → Alloy                 │
│  micrometer-registry-prometheus → /actuator/prometheus          │
│  Logback JSON → stdout → Docker → Alloy → Loki                 │
└─────────────────────────────────────────────────────────────────┘
```

### Componentes

| Componente | Funcion | Puerto | Docker Image |
|-----------|---------|--------|-------------|
| **Grafana** | Visualizacion, dashboards, alertas | 3000 | `grafana/grafana:latest` |
| **Loki** | Almacen de logs (como Prometheus pero para logs) | 3100 | `grafana/loki:latest` |
| **Tempo** | Almacen de traces distribuidos | 3200 | `grafana/tempo:latest` |
| **Prometheus** | Almacen de metricas (scrape model) | 9090 | `prom/prometheus:latest` |
| **Alloy** | Agente unificado (sucesor de Promtail) | 12345 | `grafana/alloy:latest` |

### ¿Por que Alloy y no Promtail?

Promtail entro en LTS el 13 de febrero de 2025 y su EOL es febrero de 2026. Grafana Alloy es el sucesor oficial:
- Agente unificado para logs, traces, metricas y profiles
- Compatible 100% con OTLP
- Pipelines programables
- Es lo que usarias en produccion hoy

---

## 3. Dependencias Maven (Spring Boot 3.4.x)

### Referencia Rapida

```xml
<!-- Parent dependencyManagement — versiones gestionadas por Spring Boot BOM -->
<!-- No pinear versiones manualmente -->

<!-- Container POMs (runtime) -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### Guia Detallada

#### ¿Que hace cada dependencia?

| Dependencia | Capa | Funcion |
|------------|------|---------|
| `spring-boot-starter-actuator` | Container | Endpoints de gestion (health, metrics, prometheus). Ya incluye Micrometer Core. |
| `micrometer-tracing-bridge-otel` | Container | Puente entre Micrometer Observation API y OpenTelemetry SDK. Crea spans, inyecta MDC. |
| `opentelemetry-exporter-otlp` | Container | Exporta traces via protocolo OTLP (HTTP o gRPC) al backend (Tempo via Alloy). |
| `micrometer-registry-prometheus` | Container | Expone metricas en formato Prometheus en `/actuator/prometheus`. |

#### ¿Por que en container y no en infrastructure?

Las dependencias de observabilidad son concerns de **deployment**, no de logica de negocio. El container module es donde viven las dependencias de runtime. Los modulos domain, application e infrastructure no necesitan saber que existe Prometheus.

#### Versiones — NO pinear

Spring Boot 3.4.x gestiona las versiones de Micrometer y OpenTelemetry a traves de su BOM. Si pineas versiones manualmente, puedes tener conflictos. Deja que el parent las gestione:

```xml
<!-- CORRECTO — sin version -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>

<!-- INCORRECTO — version manual -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
    <version>1.3.0</version> <!-- NO hacer esto -->
</dependency>
```

---

## 4. Configuracion application.yml

### Configuracion Completa

```yaml
spring:
  application:
    name: reservation-service  # Aparece en logs, traces y metricas

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  tracing:
    sampling:
      probability: 1.0  # 100% para desarrollo, 0.1 (10%) en produccion
  otlp:
    tracing:
      endpoint: http://alloy:4318/v1/traces  # OTLP HTTP → Alloy

logging:
  pattern:
    correlation: "[${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

### Desglose

#### Tracing

```yaml
management:
  tracing:
    sampling:
      probability: 1.0
```

- `1.0` = tracear el 100% de las peticiones (desarrollo/POC)
- `0.1` = tracear el 10% (produccion — reduce volumen)
- Con `micrometer-tracing-bridge-otel` en el classpath, esto es todo lo que necesitas

#### Exportacion OTLP

```yaml
management:
  otlp:
    tracing:
      endpoint: http://alloy:4318/v1/traces
```

- Puerto 4318 = OTLP HTTP (4317 = gRPC)
- Los traces se envian automaticamente al endpoint configurado
- Spring Boot autoconfigura el exporter — no necesitas beans manuales

#### Actuator

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

- `prometheus` expone `/actuator/prometheus` con todas las metricas en formato Prometheus
- Prometheus scrape este endpoint periodicamente (cada 15s por defecto)

---

## 5. MDC y Correlation IDs

### Como Funciona (automatico)

Con `micrometer-tracing-bridge-otel` en el classpath, Spring Boot 3.4.x **automaticamente**:

1. Crea un span para cada peticion HTTP entrante
2. Inyecta `traceId` y `spanId` en el MDC de SLF4J
3. Los incluye en el patron de logging

**No necesitas codigo manual.** Ni `MDC.put()`, ni filtros, ni interceptores.

### Formato de Logs

```
2026-03-14T10:23:45.123Z  INFO [reservation-service,803B448A0489F84084905D3093480352,3425F23BB2432450] --- ReservationController : Creating reservation
```

Los campos `traceId` y `spanId` aparecen automaticamente entre corchetes.

### Structured Logging (JSON)

Para entornos con contenedores, JSON es el formato estandar. Spring Boot 3.4.x soporta structured logging nativo:

```yaml
logging:
  structured:
    format:
      console: ecs  # Elastic Common Schema — JSON automatico
```

Esto produce logs como:

```json
{
  "@timestamp": "2026-03-14T10:23:45.123Z",
  "log.level": "INFO",
  "service.name": "reservation-service",
  "trace.id": "803B448A0489F84084905D3093480352",
  "span.id": "3425F23BB2432450",
  "message": "Creating reservation",
  "ecs.version": "1.2.0"
}
```

Loki parsea JSON automaticamente — los campos son filtrables sin regex.

---

## 6. Propagacion de Traces en RabbitMQ

### Automatica con Spring AMQP

Spring AMQP + Micrometer Tracing propaga trace context **automaticamente** en mensajes RabbitMQ:

```
Reservation Service                    Customer Service
┌────────────────────┐                ┌────────────────────┐
│ POST /reservations │                │ @RabbitListener     │
│                    │                │                    │
│ Span A (HTTP)      │                │ Span B (AMQP)      │
│  └─ Span B (send)  │  ──message──▶ │  traceId = X       │
│     traceId = X    │   + headers    │  parentSpanId = B   │
│     traceparent    │                │                    │
└────────────────────┘                └────────────────────┘
```

**Como funciona:**
1. `RabbitTemplate.send()` automaticamente anade headers `traceparent` y `tracestate` al mensaje AMQP
2. `@RabbitListener` automaticamente extrae los headers y continua el trace
3. Ambos spans quedan vinculados bajo el mismo `traceId`

**En el contexto de la SAGA:**

```
POST /reservations
  └─ Span: customer.validate.command (send)
       └─ Span: CustomerValidationListener (receive)
            └─ Span: customer.validated (send)
                 └─ Span: CustomerValidatedResponseListener (receive)
                      └─ Span: payment.process.command (send)
                           └─ ... (Payment → Fleet → Confirmed)
```

**Todo el flujo SAGA aparece como un unico trace en Tempo** con spans por cada servicio y operacion.

**Requisito**: Solo necesitas `micrometer-tracing-bridge-otel` en el classpath. Spring Boot autoconfigura la instrumentacion de Spring AMQP.

---

## 7. Configuracion de Grafana Alloy

### Recoleccion de Logs de Docker

```alloy
// Descubrir contenedores Docker
discovery.docker "containers" {
    host = "unix:///var/run/docker.sock"
}

// Extraer nombre del contenedor como label
discovery.relabel "containers" {
    targets = discovery.docker.containers.targets

    rule {
        source_labels = ["__meta_docker_container_name"]
        target_label  = "container"
    }
}

// Recoger logs de los contenedores
loki.source.docker "logs" {
    host       = "unix:///var/run/docker.sock"
    targets    = discovery.relabel.containers.output
    forward_to = [loki.process.process.receiver]
}

// Procesar logs (parsear formato Docker)
loki.process "process" {
    stage.docker {}
    forward_to = [loki.write.loki.receiver]
}

// Enviar a Loki
loki.write "loki" {
    endpoint {
        url = "http://loki:3100/loki/api/v1/push"
    }
}
```

### Receptor OTLP para Traces

```alloy
// Recibir traces OTLP de los servicios Spring Boot
otelcol.receiver.otlp "traces" {
    grpc { endpoint = "0.0.0.0:4317" }
    http { endpoint = "0.0.0.0:4318" }

    output {
        traces = [otelcol.processor.batch.default.input]
    }
}

// Agrupar en batches para eficiencia
otelcol.processor.batch "default" {
    output {
        traces = [otelcol.exporter.otlphttp.tempo.input]
    }
}

// Exportar traces a Tempo
otelcol.exporter.otlphttp "tempo" {
    client {
        endpoint = "http://tempo:4318"
    }
}
```

---

## 8. Configuracion de Prometheus

### prometheus.yml

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'customer-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['customer-service:8181']

  - job_name: 'fleet-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['fleet-service:8182']

  - job_name: 'reservation-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['reservation-service:8183']

  - job_name: 'payment-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['payment-service:8184']
```

### Metricas de Spring Boot disponibles

Con `micrometer-registry-prometheus`, Spring Boot expone automaticamente:

| Metrica | Tipo | Descripcion |
|---------|------|-------------|
| `jvm_memory_used_bytes` | Gauge | Memoria JVM usada por area (heap, non-heap) |
| `jvm_threads_live_threads` | Gauge | Threads activos |
| `jvm_gc_pause_seconds` | Summary | Pausas de GC |
| `http_server_requests_seconds` | Histogram | Latencia de peticiones HTTP |
| `spring_rabbitmq_listener_seconds` | Histogram | Latencia de procesamiento de mensajes |
| `process_cpu_usage` | Gauge | Uso de CPU del proceso |

---

## 9. Grafana Dashboards

### Dashboards Preconstruidos

Importar en Grafana (+ → Import → pegar ID):

| ID | Nombre | Contenido |
|----|--------|-----------|
| **4701** | JVM (Micrometer) | Memoria JVM, GC, threads — punto de partida recomendado |
| **21308** | Spring Boot HTTP (3.x) | Request rates, latencias, status codes |

### Provisioning Automatico

Para que Grafana arranque configurado (datasources + dashboards), se usan ficheros YAML de provisioning:

```yaml
# provisioning/datasources/datasources.yml
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
  - name: Loki
    type: loki
    access: proxy
    url: http://loki:3100
  - name: Tempo
    type: tempo
    access: proxy
    url: http://tempo:3200
```

---

## 10. Checklist de Implementacion

### Infraestructura (Docker Compose)

- [ ] `docker-compose.yml` con Grafana, Loki, Tempo, Prometheus, Alloy
- [ ] `docker/prometheus/prometheus.yml` con scrape configs de los 4 servicios
- [ ] `docker/tempo/tempo.yml` con receptor OTLP
- [ ] `docker/loki/loki.yml` con configuracion local
- [ ] `docker/alloy/config.alloy` con log collection + OTLP relay
- [ ] `docker/grafana/provisioning/datasources/datasources.yml` con los 3 datasources
- [ ] `docker/grafana/provisioning/dashboards/` con dashboards JSON preconstruidos
- [ ] Verificar: `docker compose up -d` → Grafana accesible en :3000

### Instrumentacion (Spring Boot)

- [ ] Parent POM: 3 dependencias en `dependencyManagement`
- [ ] 4 container POMs: declarar las 3 dependencias
- [ ] 4 `application.yml`: tracing + OTLP endpoint + actuator + logging correlation
- [ ] Verificar: `mvn test` pasa (ArchUnit OK)
- [ ] Verificar: traces en Tempo, logs en Loki, metricas en Prometheus

### Validacion E2E

- [ ] Ejecutar `bru run --env local e2e/happy-path`
- [ ] Ir a Grafana Tempo → ver trace completo de la SAGA (4 servicios, multiples spans)
- [ ] Ir a Grafana Loki → filtrar por traceId → ver logs correlacionados
- [ ] Ir a Grafana dashboards → ver metricas JVM de los 4 servicios

---

## 11. Anti-Patterns

### NO hacer

```yaml
# NO: Sampling al 100% en produccion
management:
  tracing:
    sampling:
      probability: 1.0  # Genera volumen excesivo

# NO: Pinear versiones de Micrometer/OTel
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
    <version>1.3.0</version>  # Conflictos con Spring Boot BOM
</dependency>

# NO: MDC manual con Micrometer Tracing
MDC.put("traceId", tracer.currentSpan().context().traceId());  # Innecesario — es automatico
```

### SI hacer

```yaml
# SI: Sampling reducido en produccion
management:
  tracing:
    sampling:
      probability: 0.1  # 10% es suficiente para diagnostico

# SI: Dejar que Spring Boot gestione versiones
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
    <!-- Sin version — gestionada por spring-boot-starter-parent -->
</dependency>
```

---

## 12. Decisiones Arquitectonicas del Proyecto

| Decision | Rationale |
|----------|-----------|
| Alloy como intermediario (no directo a Tempo) | Es lo que usarias en produccion. Aprenderemos el agente de recoleccion. |
| JSON logs (ECS) en produccion, patron legible en desarrollo | Loki parsea JSON automaticamente. Terminal se lee mejor con texto. |
| Provisioning automatico de Grafana | Zero setup manual — blueprint reutilizable. Arranca y funciona. |
| Dos changes separados (infra + instrumentacion) | Reducir scope por change. Infra primero, instrumentacion despues. |
| Sin @Schema en DTOs de observabilidad | Mismo criterio que OpenAPI — mantener pureza de capas. |
| micrometer-registry-prometheus (no OTLP metrics) | Prometheus scrape es el modelo estandar y mas compatible. |

---

## 13. Referencias

- [Spring Boot 3.4 — Tracing](https://docs.spring.io/spring-boot/3.4/reference/actuator/tracing)
- [Spring Boot 3.4 — Structured Logging](https://spring.io/blog/2024/08/23/structured-logging-in-spring-boot-3-4/)
- [Micrometer Tracing — Observation API](https://micrometer.io/docs/tracing)
- [Grafana Alloy — Documentation](https://grafana.com/docs/alloy/latest/)
- [Grafana Alloy — Docker Log Collection](https://grafana.com/docs/alloy/latest/tutorials/processing-logs/)
- [Grafana Alloy — OpenTelemetry to LGTM Stack](https://grafana.com/docs/alloy/latest/collect/opentelemetry-to-lgtm-stack/)
- [Spring AMQP — Micrometer Observation](https://docs.spring.io/spring-amqp/reference/amqp/receiving-messages/micrometer-observation.html)
- [Grafana Dashboard 4701 — JVM Micrometer](https://grafana.com/grafana/dashboards/4701-jvm-micrometer/)
