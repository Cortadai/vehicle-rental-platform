# Área 9: Spring Boot + Kubernetes Specifics

## Referencia Rápida (Senior Developers)

### Health Probes - Configuración Mínima

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: when_authorized
      probes:
        enabled: true
      group:
        liveness:
          include: livenessState
        readiness:
          include: readinessState,db,rabbit
  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true
```

### Kubernetes Probes

```yaml
# deployment.yaml
spec:
  containers:
    - name: app
      livenessProbe:
        httpGet:
          path: /actuator/health/liveness
          port: 8080
        initialDelaySeconds: 10
        periodSeconds: 10
        failureThreshold: 3
      readinessProbe:
        httpGet:
          path: /actuator/health/readiness
          port: 8080
        initialDelaySeconds: 5
        periodSeconds: 5
        failureThreshold: 3
      startupProbe:
        httpGet:
          path: /actuator/health/liveness
          port: 8080
        initialDelaySeconds: 0
        periodSeconds: 5
        failureThreshold: 30  # 30 * 5s = 150s max startup
```

### Graceful Shutdown

```yaml
# application.yml
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
server:
  shutdown: graceful
```

```yaml
# deployment.yaml
spec:
  terminationGracePeriodSeconds: 45  # > timeout-per-shutdown-phase
  containers:
    - lifecycle:
        preStop:
          exec:
            command: ["sh", "-c", "sleep 5"]  # Tiempo para deregistrar del LB
```

### Métricas Prometheus

```yaml
# Dependencias en pom.xml
# - micrometer-registry-prometheus
# - spring-boot-starter-actuator

management:
  prometheus:
    metrics:
      export:
        enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
      environment: ${ENVIRONMENT:local}
```

### Structured Logging (JSON)

```yaml
logging:
  structured:
    format:
      console: ecs  # o logstash
    ecs:
      service:
        name: ${spring.application.name}
        version: ${APP_VERSION:unknown}
        environment: ${ENVIRONMENT:local}
```

---

## Guía Detallada (Junior/Mid Developers)

### 1. Health Checks: Liveness vs Readiness vs Startup

#### Conceptos Fundamentales

| Probe | Propósito | Qué Pasa si Falla | Qué Verificar |
|-------|-----------|-------------------|---------------|
| **Startup** | App está iniciando | Reinicia el pod | JVM arrancó, contexto Spring cargado |
| **Liveness** | App está "viva" | Reinicia el pod | No deadlocks, app responde |
| **Readiness** | App puede recibir tráfico | Remueve del Service | DB conectada, dependencias OK |

#### Dependencias Necesarias

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

#### Configuración Completa de Health

```yaml
# application.yml
management:
  server:
    port: 8081  # Puerto separado para actuator (opcional pero recomendado)

  endpoints:
    web:
      base-path: /actuator
      exposure:
        include: health,info,prometheus,metrics,env,loggers

  endpoint:
    health:
      show-details: when_authorized  # never | when_authorized | always
      show-components: when_authorized
      probes:
        enabled: true  # Habilita /health/liveness y /health/readiness
      group:
        # Grupo liveness: solo verifica que la app responde
        liveness:
          include: livenessState
          show-details: always
        # Grupo readiness: verifica dependencias externas
        readiness:
          include: readinessState,db,rabbit,redis,diskSpace
          show-details: always
        # Grupo custom para checks internos
        internal:
          include: db,rabbit,redis,diskSpace,custom
          show-details: always

  health:
    # Habilitar indicadores de estado
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true
    # Configurar health indicators específicos
    db:
      enabled: true
    rabbit:
      enabled: true
    redis:
      enabled: true
    diskspace:
      enabled: true
      threshold: 10MB
```

#### Custom Health Indicator

```java
package com.acme.sales.infrastructure.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("externalPaymentGateway")
@RequiredArgsConstructor
@Slf4j
public class PaymentGatewayHealthIndicator implements HealthIndicator {

    private final PaymentGatewayClient paymentClient;

    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();
            boolean isHealthy = paymentClient.healthCheck();
            long responseTime = System.currentTimeMillis() - startTime;

            if (isHealthy) {
                return Health.up()
                        .withDetail("service", "payment-gateway")
                        .withDetail("responseTimeMs", responseTime)
                        .build();
            } else {
                return Health.down()
                        .withDetail("service", "payment-gateway")
                        .withDetail("error", "Health check returned false")
                        .build();
            }
        } catch (Exception e) {
            log.warn("Payment gateway health check failed", e);
            return Health.down()
                    .withDetail("service", "payment-gateway")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
```

#### Health Indicator para Readiness con Circuit Breaker

```java
@Component("databaseReadiness")
@RequiredArgsConstructor
public class DatabaseReadinessIndicator implements HealthIndicator {

    private final DataSource dataSource;

    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) {  // 2 segundos timeout
                return Health.up()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("validConnection", true)
                        .build();
            }
            return Health.down()
                    .withDetail("error", "Connection not valid")
                    .build();
        } catch (SQLException e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}
```

### 2. Kubernetes Deployment Completo

```yaml
# k8s/base/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: sales-service
  labels:
    app: sales-service
    version: v1
spec:
  replicas: 3
  selector:
    matchLabels:
      app: sales-service
  template:
    metadata:
      labels:
        app: sales-service
        version: v1
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8081"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      # Tiempo total para graceful shutdown
      terminationGracePeriodSeconds: 45

      # Service Account para RBAC
      serviceAccountName: sales-service

      containers:
        - name: sales-service
          image: acme/sales-service:latest
          imagePullPolicy: Always

          ports:
            - name: http
              containerPort: 8080
              protocol: TCP
            - name: actuator
              containerPort: 8081
              protocol: TCP

          # Variables de entorno desde ConfigMap y Secret
          envFrom:
            - configMapRef:
                name: sales-service-config
            - secretRef:
                name: sales-service-secrets

          env:
            - name: JAVA_OPTS
              value: "-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"
            - name: POD_NAME
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
            - name: POD_NAMESPACE
              valueFrom:
                fieldRef:
                  fieldPath: metadata.namespace

          # Recursos (QoS: Guaranteed si limits == requests)
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "1000m"

          # Startup Probe: permite tiempo de arranque largo
          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: actuator
            initialDelaySeconds: 0
            periodSeconds: 5
            timeoutSeconds: 3
            failureThreshold: 30  # 30 * 5s = 150s máximo para iniciar

          # Liveness Probe: la app está viva?
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: actuator
            initialDelaySeconds: 0  # startupProbe se encarga del delay
            periodSeconds: 10
            timeoutSeconds: 3
            failureThreshold: 3

          # Readiness Probe: puede recibir tráfico?
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: actuator
            initialDelaySeconds: 0
            periodSeconds: 5
            timeoutSeconds: 3
            failureThreshold: 3

          # Lifecycle hooks para graceful shutdown
          lifecycle:
            preStop:
              exec:
                # Esperar a que el LB deregistre el pod
                command: ["sh", "-c", "sleep 5"]

          # Volume mounts
          volumeMounts:
            - name: tmp
              mountPath: /tmp
            - name: config-volume
              mountPath: /config
              readOnly: true

      volumes:
        - name: tmp
          emptyDir: {}
        - name: config-volume
          configMap:
            name: sales-service-files

      # Topology spread para alta disponibilidad
      topologySpreadConstraints:
        - maxSkew: 1
          topologyKey: topology.kubernetes.io/zone
          whenUnsatisfiable: ScheduleAnyway
          labelSelector:
            matchLabels:
              app: sales-service
```

### 3. ConfigMap y Secrets

#### ConfigMap para Configuración

```yaml
# k8s/base/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: sales-service-config
data:
  # Variables de entorno
  SPRING_PROFILES_ACTIVE: "kubernetes"
  SERVER_PORT: "8080"
  MANAGEMENT_SERVER_PORT: "8081"
  ENVIRONMENT: "production"

  # Configuración de base de datos (sin secretos)
  SPRING_DATASOURCE_URL: "jdbc:postgresql://postgres-service:5432/sales"
  SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE: "10"
  SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE: "5"

  # RabbitMQ
  SPRING_RABBITMQ_HOST: "rabbitmq-service"
  SPRING_RABBITMQ_PORT: "5672"

  # Logging
  LOGGING_LEVEL_ROOT: "INFO"
  LOGGING_LEVEL_COM_ACME: "DEBUG"
```

#### Secret para Credenciales

```yaml
# k8s/base/secret.yaml (usar Sealed Secrets o External Secrets en producción)
apiVersion: v1
kind: Secret
metadata:
  name: sales-service-secrets
type: Opaque
stringData:
  SPRING_DATASOURCE_USERNAME: "sales_user"
  SPRING_DATASOURCE_PASSWORD: "super-secret-password"
  SPRING_RABBITMQ_USERNAME: "sales"
  SPRING_RABBITMQ_PASSWORD: "rabbit-secret"
  JWT_SECRET: "jwt-signing-secret-key"
```

#### Usar Config Tree para Secrets Montados

```yaml
# application-kubernetes.yml
spring:
  config:
    import:
      - "optional:configtree:/run/secrets/"  # Docker secrets
      - "optional:configtree:/etc/secrets/"  # Kubernetes secrets montados
```

```yaml
# deployment.yaml - montar secrets como archivos
volumes:
  - name: db-credentials
    secret:
      secretName: db-credentials
containers:
  - volumeMounts:
      - name: db-credentials
        mountPath: /etc/secrets/db
        readOnly: true
```

```properties
# El archivo /etc/secrets/db/password se convierte en:
# db.password=valor-del-secret
```

### 4. Graceful Shutdown

#### Configuración de Spring Boot

```yaml
# application.yml
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s  # Tiempo para completar requests en vuelo

server:
  shutdown: graceful  # immediate | graceful
```

#### Secuencia de Shutdown

```
1. Kubernetes envía SIGTERM al pod
2. preStop hook se ejecuta (sleep 5s para deregistrar del LB)
3. Spring Boot recibe SIGTERM
4. Se deja de aceptar nuevas conexiones
5. Se esperan hasta 30s para requests en vuelo
6. Se cierran conexiones a DB, RabbitMQ, etc.
7. JVM termina
8. Si excede terminationGracePeriodSeconds, Kubernetes envía SIGKILL
```

#### Listener para Cleanup Custom

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class GracefulShutdownListener {

    private final RabbitTemplate rabbitTemplate;
    private final CacheManager cacheManager;

    @PreDestroy
    public void onShutdown() {
        log.info("Application shutting down, cleaning up resources...");

        // Completar mensajes pendientes
        try {
            // Esperar a que los mensajes en vuelo se procesen
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Limpiar caches si es necesario
        cacheManager.getCacheNames()
                .forEach(name -> {
                    var cache = cacheManager.getCache(name);
                    if (cache != null) {
                        cache.clear();
                    }
                });

        log.info("Cleanup completed");
    }

    @EventListener(ContextClosedEvent.class)
    public void onContextClosed(ContextClosedEvent event) {
        log.info("Spring context closed");
    }
}
```

### 5. Observabilidad: Métricas con Micrometer

#### Dependencias

```xml
<!-- pom.xml -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
</dependencies>
```

#### Configuración de Métricas

```yaml
# application.yml
management:
  prometheus:
    metrics:
      export:
        enabled: true

  metrics:
    enable:
      all: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
      slo:
        http.server.requests: 50ms, 100ms, 200ms, 500ms
    tags:
      application: ${spring.application.name}
      environment: ${ENVIRONMENT:local}
      version: ${APP_VERSION:unknown}
```

#### Métricas Custom

```java
@Component
@RequiredArgsConstructor
public class OrderMetrics {

    private final MeterRegistry meterRegistry;

    private Counter ordersCreatedCounter;
    private Timer orderProcessingTimer;
    private AtomicInteger activeOrders;

    @PostConstruct
    public void init() {
        // Contador de órdenes creadas
        ordersCreatedCounter = Counter.builder("orders.created.total")
                .description("Total orders created")
                .tags("type", "all")
                .register(meterRegistry);

        // Timer para procesamiento de órdenes
        orderProcessingTimer = Timer.builder("orders.processing.duration")
                .description("Time to process an order")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry);

        // Gauge para órdenes activas
        activeOrders = new AtomicInteger(0);
        Gauge.builder("orders.active", activeOrders, AtomicInteger::get)
                .description("Number of orders being processed")
                .register(meterRegistry);
    }

    public void recordOrderCreated(OrderType type) {
        ordersCreatedCounter.increment();
        Counter.builder("orders.created.total")
                .tags("type", type.name())
                .register(meterRegistry)
                .increment();
    }

    public Timer.Sample startOrderProcessing() {
        activeOrders.incrementAndGet();
        return Timer.start(meterRegistry);
    }

    public void stopOrderProcessing(Timer.Sample sample) {
        sample.stop(orderProcessingTimer);
        activeOrders.decrementAndGet();
    }
}
```

#### Uso de Métricas en Service

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMetrics metrics;

    public Order createOrder(CreateOrderRequest request) {
        Timer.Sample sample = metrics.startOrderProcessing();

        try {
            Order order = orderMapper.toEntity(request);
            Order saved = orderRepository.save(order);

            metrics.recordOrderCreated(order.getType());

            return saved;
        } finally {
            metrics.stopOrderProcessing(sample);
        }
    }
}
```

### 6. Tracing Distribuido

#### Dependencias para OpenTelemetry

```xml
<dependencies>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-tracing-bridge-otel</artifactId>
    </dependency>
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-exporter-otlp</artifactId>
    </dependency>
</dependencies>
```

#### Configuración de Tracing

```yaml
# application.yml
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0  # 100% en dev, reducir en prod (0.1 = 10%)
    propagation:
      type: W3C  # W3C Trace Context

  otlp:
    tracing:
      endpoint: http://otel-collector:4318/v1/traces

# Propagación de trace ID en logs
logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

#### Spans Custom

```java
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final Tracer tracer;  // io.micrometer.tracing.Tracer
    private final PaymentGatewayClient paymentClient;

    public PaymentResult processPayment(PaymentRequest request) {
        // Crear span custom
        Span span = tracer.nextSpan().name("process-payment");

        try (Tracer.SpanInScope ws = tracer.withSpan(span.start())) {
            span.tag("payment.amount", request.amount().toString());
            span.tag("payment.currency", request.currency());
            span.tag("payment.method", request.method().name());

            PaymentResult result = paymentClient.charge(request);

            span.tag("payment.status", result.status().name());
            span.event("payment-completed");

            return result;
        } catch (PaymentException e) {
            span.error(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
```

### 7. Structured Logging (JSON)

#### Configuración ECS (Elastic Common Schema)

```yaml
# application.yml
logging:
  structured:
    format:
      console: ecs  # Formato ECS para Elasticsearch/Kibana
    ecs:
      service:
        name: ${spring.application.name}
        version: ${APP_VERSION:unknown}
        environment: ${ENVIRONMENT:local}
        node-name: ${POD_NAME:unknown}
    json:
      add:
        kubernetes.namespace: ${POD_NAMESPACE:unknown}
        kubernetes.pod: ${POD_NAME:unknown}
```

#### Configuración Logstash Format

```yaml
# application.yml
logging:
  structured:
    format:
      console: logstash
    json:
      include: timestamp,level,logger,message,mdc,markers,stacktrace
      exclude: log.level  # Evitar duplicados
      rename:
        "@timestamp": "timestamp"
        "process.id": "pid"
```

#### Uso de MDC para Contexto

```java
@Component
@Slf4j
public class RequestContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // Añadir contexto al MDC
            MDC.put("requestId", getOrGenerateRequestId(request));
            MDC.put("userId", extractUserId(request));
            MDC.put("clientIp", request.getRemoteAddr());
            MDC.put("userAgent", request.getHeader("User-Agent"));
            MDC.put("path", request.getRequestURI());
            MDC.put("method", request.getMethod());

            filterChain.doFilter(request, response);

        } finally {
            MDC.clear();
        }
    }

    private String getOrGenerateRequestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        return requestId != null ? requestId : UUID.randomUUID().toString();
    }

    private String extractUserId(HttpServletRequest request) {
        // Extraer del token JWT o sesión
        return Optional.ofNullable(request.getUserPrincipal())
                .map(Principal::getName)
                .orElse("anonymous");
    }
}
```

#### Logging Estructurado en Código

```java
@Service
@Slf4j
public class OrderService {

    public Order createOrder(CreateOrderRequest request) {
        // Usando SLF4J fluent API para añadir datos estructurados
        log.atInfo()
            .addKeyValue("customerId", request.customerId())
            .addKeyValue("itemCount", request.items().size())
            .addKeyValue("totalAmount", calculateTotal(request))
            .log("Creating new order");

        // ... lógica de negocio

        log.atInfo()
            .addKeyValue("orderId", order.getId())
            .addKeyValue("status", order.getStatus())
            .log("Order created successfully");

        return order;
    }
}
```

#### Ejemplo de Output JSON (ECS)

```json
{
  "@timestamp": "2024-01-15T10:30:00.123Z",
  "log.level": "INFO",
  "message": "Creating new order",
  "service.name": "sales-service",
  "service.version": "1.2.3",
  "service.environment": "production",
  "trace.id": "abc123def456",
  "span.id": "789xyz",
  "customerId": "CUST-001",
  "itemCount": 3,
  "totalAmount": 150.00,
  "requestId": "req-abc-123",
  "userId": "user@example.com",
  "kubernetes.namespace": "production",
  "kubernetes.pod": "sales-service-abc123-xyz"
}
```

### 8. Service y NetworkPolicy

```yaml
# k8s/base/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: sales-service
  labels:
    app: sales-service
spec:
  type: ClusterIP
  ports:
    - name: http
      port: 80
      targetPort: http
      protocol: TCP
    - name: actuator
      port: 8081
      targetPort: actuator
      protocol: TCP
  selector:
    app: sales-service
---
# NetworkPolicy para seguridad
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: sales-service-network-policy
spec:
  podSelector:
    matchLabels:
      app: sales-service
  policyTypes:
    - Ingress
    - Egress
  ingress:
    # Solo permitir tráfico del ingress controller y prometheus
    - from:
        - namespaceSelector:
            matchLabels:
              name: ingress-nginx
        - namespaceSelector:
            matchLabels:
              name: monitoring
      ports:
        - protocol: TCP
          port: 8080
        - protocol: TCP
          port: 8081
  egress:
    # Permitir tráfico a PostgreSQL, RabbitMQ y DNS
    - to:
        - podSelector:
            matchLabels:
              app: postgres
      ports:
        - protocol: TCP
          port: 5432
    - to:
        - podSelector:
            matchLabels:
              app: rabbitmq
      ports:
        - protocol: TCP
          port: 5672
    - to:  # DNS
        - namespaceSelector: {}
          podSelector:
            matchLabels:
              k8s-app: kube-dns
      ports:
        - protocol: UDP
          port: 53
```

### 9. HorizontalPodAutoscaler

```yaml
# k8s/base/hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: sales-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: sales-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
    # Métrica custom de Prometheus
    - type: Pods
      pods:
        metric:
          name: http_requests_per_second
        target:
          type: AverageValue
          averageValue: "100"
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300  # 5 min estabilización
      policies:
        - type: Percent
          value: 10
          periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
        - type: Percent
          value: 100
          periodSeconds: 15
        - type: Pods
          value: 4
          periodSeconds: 15
      selectPolicy: Max
```

### 10. PodDisruptionBudget

```yaml
# k8s/base/pdb.yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: sales-service-pdb
spec:
  minAvailable: 2  # O usar maxUnavailable: 1
  selector:
    matchLabels:
      app: sales-service
```

---

## ✅ Hacer

1. **Separar puertos** - Actuator en puerto diferente (8081) al de la app
2. **Usar los 3 probes** - Startup, Liveness, Readiness con propósitos distintos
3. **Graceful shutdown** - Configurar `terminationGracePeriodSeconds` > `timeout-per-shutdown-phase`
4. **preStop hook** - Sleep para permitir deregistro del load balancer
5. **Structured logging** - JSON con ECS o Logstash format
6. **Propagar trace ID** - En logs y responses para correlación
7. **Métricas con tags** - application, environment, version
8. **Secrets como archivos** - No como environment variables (más seguro)
9. **Resource limits** - Definir requests y limits para QoS Guaranteed
10. **PDB y HPA** - Para alta disponibilidad y auto-scaling

## ❌ Evitar

1. **DB check en liveness** - Puede causar reinicio innecesario si DB está lenta
2. **initialDelaySeconds alto** - Usar startupProbe en su lugar
3. **Logs en texto plano** - Difíciles de parsear en sistemas de logging
4. **Secrets en ConfigMap** - Usar Secrets o External Secrets
5. **Ignorar terminationGracePeriodSeconds** - Puede causar SIGKILL prematuro
6. **100% sampling en prod** - Demasiado overhead, usar 1-10%
7. **Health check sin timeout** - Puede bloquear el probe indefinidamente
8. **Un solo pod** - Sin réplicas no hay alta disponibilidad
9. **Exponer actuator públicamente** - Solo accesible internamente
10. **Logs sin request ID** - Imposible correlacionar requests

---

## Checklist de Implementación

### Health Checks
- [ ] `spring-boot-starter-actuator` como dependencia
- [ ] Puerto de management separado (8081)
- [ ] Probes habilitados: startup, liveness, readiness
- [ ] Health groups configurados correctamente
- [ ] Custom health indicators para servicios críticos
- [ ] Timeout en health checks externos

### Kubernetes Manifests
- [ ] Deployment con los 3 probes configurados
- [ ] terminationGracePeriodSeconds > shutdown timeout
- [ ] preStop hook con sleep
- [ ] Resource requests y limits definidos
- [ ] ConfigMap para configuración no sensible
- [ ] Secret para credenciales (o External Secrets)
- [ ] Service con puertos correctos
- [ ] PodDisruptionBudget para HA
- [ ] HorizontalPodAutoscaler configurado

### Observabilidad
- [ ] `micrometer-registry-prometheus` configurado
- [ ] Métricas custom para operaciones de negocio
- [ ] Tracing con OpenTelemetry
- [ ] Sampling rate apropiado para el ambiente
- [ ] Structured logging en JSON
- [ ] MDC con requestId, userId, traceId
- [ ] Prometheus scrape annotations en pods
