# Área 6: Configuración Application Properties/YAML

> **Audiencia**: Desarrolladores junior/mid (guía detallada) + Seniors (referencia rápida)
> **Stack**: Spring Boot 3.4.x, Java 17+, PostgreSQL, RabbitMQ, Kubernetes

---

## 1. YAML vs Properties

### Referencia Rápida (Seniors)

| Aspecto | YAML | Properties |
|---------|------|------------|
| Jerarquía | ✅ Nativa | ❌ Prefijos repetidos |
| Listas | ✅ Sintaxis clara | ⚠️ Índices [0], [1] |
| Multi-documento | ✅ Con `---` | ❌ No soportado |
| Errores de indentación | ⚠️ Comunes | ✅ No aplica |
| IDE support | ✅ Bueno | ✅ Excelente |

### Guía Detallada (Junior/Mid)

✅ **Recomendación**: Usar YAML para proyectos enterprise

```yaml
# application.yml - Más legible para configuraciones complejas
spring:
  application:
    name: pedido-service
  datasource:
    url: jdbc:postgresql://localhost:5432/pedidos
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

server:
  port: 8080
  shutdown: graceful

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

Equivalente en properties (más verboso):
```properties
# application.properties
spring.application.name=pedido-service
spring.datasource.url=jdbc:postgresql://localhost:5432/pedidos
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true
server.port=8080
server.shutdown=graceful
management.endpoints.web.exposure.include=health,info,prometheus
```

---

## 2. Estructura de Perfiles

### Referencia Rápida

```
src/main/resources/
├── application.yml           ← Configuración base (común)
├── application-dev.yml       ← Desarrollo local
├── application-test.yml      ← Tests de integración
├── application-staging.yml   ← Pre-producción
└── application-prod.yml      ← Producción
```

### Guía Detallada

#### application.yml (Base - Común a todos los entornos)

```yaml
spring:
  application:
    name: pedido-service

  # Configuración JPA común
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        jdbc:
          batch_size: 50
        order_inserts: true
        order_updates: true

  # Jackson
  jackson:
    default-property-inclusion: non_null
    serialization:
      write-dates-as-timestamps: false
    deserialization:
      fail-on-unknown-properties: false

  # Lifecycle
  lifecycle:
    timeout-per-shutdown-phase: 30s

server:
  port: 8080
  shutdown: graceful
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/xml,text/plain

# Actuator común
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
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true

# Propiedades custom de la aplicación
app:
  name: ${spring.application.name}
  version: @project.version@
```

#### application-dev.yml (Desarrollo Local)

```yaml
spring:
  # Base de datos local
  datasource:
    url: jdbc:postgresql://localhost:5432/pedidos_dev
    username: postgres
    password: postgres

  # JPA para desarrollo
  jpa:
    hibernate:
      ddl-auto: update  # Solo en dev
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  # RabbitMQ local
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

  # DevTools
  devtools:
    restart:
      enabled: true
    livereload:
      enabled: true

# Logging verbose en dev
logging:
  level:
    com.acme: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE

# Actuator expuesto completamente en dev
management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always

# Config de app para dev
app:
  feature-flags:
    new-checkout: true
  external-services:
    inventario:
      url: http://localhost:8081
      timeout: 10s
```

#### application-test.yml (Tests de Integración)

```yaml
spring:
  # Testcontainers configura la conexión automáticamente con @ServiceConnection
  # Estas propiedades son fallback si no se usa Testcontainers

  datasource:
    url: jdbc:tc:postgresql:15-alpine:///testdb
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

  rabbitmq:
    host: localhost
    port: 5672

  # Desactivar caché en tests
  cache:
    type: none

# Logging reducido en tests
logging:
  level:
    com.acme: INFO
    org.springframework: WARN
    org.testcontainers: INFO

# Config de app para tests
app:
  external-services:
    inventario:
      url: http://localhost:${wiremock.server.port:8089}
      timeout: 5s
```

#### application-prod.yml (Producción)

```yaml
spring:
  # Conexión desde variables de entorno (Kubernetes Secrets)
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: ${DB_POOL_SIZE:20}
      minimum-idle: ${DB_POOL_MIN:5}
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

  jpa:
    hibernate:
      ddl-auto: none  # NUNCA auto-modificar en prod
    show-sql: false
    properties:
      hibernate:
        generate_statistics: false

  # RabbitMQ desde variables de entorno
  rabbitmq:
    host: ${RABBITMQ_HOST}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}
    virtual-host: ${RABBITMQ_VHOST:/}

# Logging estructurado para ELK/agregadores
logging:
  level:
    root: WARN
    com.acme: INFO
  pattern:
    console: '{"timestamp":"%d{ISO8601}","level":"%level","service":"${spring.application.name}","trace":"%X{traceId:-}","span":"%X{spanId:-}","message":"%msg"}%n'

# Actuator restringido en prod
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: when_authorized

# Config de app para prod
app:
  external-services:
    inventario:
      url: ${INVENTARIO_SERVICE_URL}
      timeout: ${INVENTARIO_TIMEOUT:5s}
```

#### Activar perfiles

```bash
# Variable de entorno (recomendado para K8s)
export SPRING_PROFILES_ACTIVE=prod

# Línea de comandos
java -jar app.jar --spring.profiles.active=prod

# En application.yml (NO recomendado para prod)
spring:
  profiles:
    active: dev
```

---

## 3. @ConfigurationProperties vs @Value

### Referencia Rápida

| Característica | @ConfigurationProperties | @Value |
|----------------|--------------------------|--------|
| Type-safe | ✅ Sí | ❌ No |
| Validación | ✅ Con @Validated | ⚠️ Limitada |
| Relaxed binding | ✅ Completo | ⚠️ Limitado |
| IDE autocomplete | ✅ Con metadata | ❌ No |
| SpEL | ❌ No | ✅ Sí |
| Uso recomendado | Grupos de propiedades | Valores individuales |

### Guía Detallada

#### ✅ Usar @ConfigurationProperties (Recomendado)

```java
// Clase de configuración type-safe
@ConfigurationProperties(prefix = "app.external-services.inventario")
@Validated
public class InventarioClientProperties {

    @NotBlank(message = "La URL del servicio de inventario es requerida")
    private String url;

    @NotNull
    @DurationMin(millis = 100)
    @DurationMax(seconds = 30)
    private Duration timeout = Duration.ofSeconds(5);

    @Min(1)
    @Max(10)
    private int maxRetries = 3;

    @NotNull
    private RetryConfig retry = new RetryConfig();

    // Getters y Setters
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public RetryConfig getRetry() { return retry; }
    public void setRetry(RetryConfig retry) { this.retry = retry; }

    // Clase anidada para configuración de retry
    public static class RetryConfig {
        private Duration initialInterval = Duration.ofMillis(500);
        private Duration maxInterval = Duration.ofSeconds(5);
        private double multiplier = 2.0;

        // Getters y Setters
        public Duration getInitialInterval() { return initialInterval; }
        public void setInitialInterval(Duration initialInterval) { this.initialInterval = initialInterval; }

        public Duration getMaxInterval() { return maxInterval; }
        public void setMaxInterval(Duration maxInterval) { this.maxInterval = maxInterval; }

        public double getMultiplier() { return multiplier; }
        public void setMultiplier(double multiplier) { this.multiplier = multiplier; }
    }
}
```

**Con Records (Java 17+ - Inmutable)**:

```java
// Usar @DefaultValue de Spring Boot para valores por defecto en records (no se pueden reasignar en compact constructors)
@ConfigurationProperties(prefix = "app.external-services.inventario")
@Validated
public record InventarioClientProperties(
        @NotBlank String url,
        @DefaultValue("5s") Duration timeout,
        @DefaultValue("3") @Min(1) @Max(10) int maxRetries,
        @DefaultValue RetryConfig retry
) {
    public record RetryConfig(
            @DefaultValue("500ms") Duration initialInterval,
            @DefaultValue("5s") Duration maxInterval,
            @DefaultValue("2.0") double multiplier
    ) {}
}
```

**Habilitar ConfigurationProperties**:

```java
@Configuration
@EnableConfigurationProperties({
        InventarioClientProperties.class,
        AppProperties.class
})
public class AppConfig {
}

// O en la clase principal
@SpringBootApplication
@ConfigurationPropertiesScan("com.acme.ecommerce.config")
public class Application {
}
```

**Uso en servicios**:

```java
@Service
@RequiredArgsConstructor
public class InventarioClient {

    private final InventarioClientProperties properties;
    private final RestClient restClient;

    public InventarioClient(InventarioClientProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder
                .baseUrl(properties.url())
                .requestFactory(createRequestFactory())
                .build();
    }

    private ClientHttpRequestFactory createRequestFactory() {
        // Usar properties.timeout(), properties.retry(), etc.
    }
}
```

**application.yml correspondiente**:

```yaml
app:
  external-services:
    inventario:
      url: http://inventario-service:8080
      timeout: 5s
      max-retries: 3
      retry:
        initial-interval: 500ms
        max-interval: 5s
        multiplier: 2.0
```

#### ⚠️ Usar @Value solo para valores simples/individuales

```java
@Service
public class NotificacionService {

    // OK para valores simples que no se agrupan
    @Value("${app.notifications.enabled:true}")
    private boolean notificationsEnabled;

    @Value("${app.name}")
    private String appName;

    // OK para SpEL (no soportado en @ConfigurationProperties)
    @Value("#{${app.feature-flags.new-checkout:false} and ${app.notifications.enabled:true}}")
    private boolean enableNewCheckoutNotifications;

    // OK para valores con expresiones
    @Value("${app.admin.emails:admin@example.com,ops@example.com}")
    private List<String> adminEmails;
}
```

❌ **Evitar @Value para grupos de propiedades relacionadas**:

```java
// ❌ MAL: Muchos @Value relacionados
@Service
public class InventarioClient {
    @Value("${app.external-services.inventario.url}")
    private String url;

    @Value("${app.external-services.inventario.timeout:5s}")
    private Duration timeout;

    @Value("${app.external-services.inventario.max-retries:3}")
    private int maxRetries;

    // Difícil de mantener, sin validación, sin autocomplete
}

// ✅ BIEN: @ConfigurationProperties
@Service
@RequiredArgsConstructor
public class InventarioClient {
    private final InventarioClientProperties properties;
    // Type-safe, validado, con autocomplete
}
```

---

## 4. Configuración para Kubernetes

### Referencia Rápida

```yaml
# ConfigMap → Configuración no sensible
# Secret → Credenciales y datos sensibles
# Variables de entorno → Inyección en pods
```

### Guía Detallada

#### application-prod.yml preparado para K8s

```yaml
spring:
  application:
    name: pedido-service

  config:
    import:
      - optional:configtree:/etc/config/  # Monta ConfigMaps/Secrets como archivos

  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:pedidos}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: ${DB_POOL_SIZE:10}
      connection-timeout: ${DB_CONNECTION_TIMEOUT:30000}

  rabbitmq:
    addresses: ${RABBITMQ_ADDRESSES:localhost:5672}
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}

server:
  port: ${SERVER_PORT:8080}

management:
  server:
    port: ${MANAGEMENT_PORT:8081}  # Puerto separado para probes
  endpoints:
    web:
      exposure:
        include: health,prometheus
```

#### Kubernetes ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: pedido-service-config
  namespace: ecommerce
data:
  # Variables de entorno
  DB_HOST: "postgres.database.svc.cluster.local"
  DB_PORT: "5432"
  DB_NAME: "pedidos"
  DB_POOL_SIZE: "20"
  RABBITMQ_ADDRESSES: "rabbitmq.messaging.svc.cluster.local:5672"
  SERVER_PORT: "8080"
  MANAGEMENT_PORT: "8081"

  # O como archivo application.yml completo
  application.yml: |
    app:
      feature-flags:
        new-checkout: true
      external-services:
        inventario:
          url: http://inventario-service.ecommerce.svc.cluster.local:8080
          timeout: 5s
```

#### Kubernetes Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: pedido-service-secrets
  namespace: ecommerce
type: Opaque
data:
  # Valores en base64
  DB_USERNAME: cGVkaWRvc191c2Vy  # pedidos_user
  DB_PASSWORD: c3VwZXJzZWNyZXQ=  # supersecret
  RABBITMQ_USERNAME: cGVkaWRvcw==
  RABBITMQ_PASSWORD: cmFiYml0cGFzcw==
```

#### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: pedido-service
  namespace: ecommerce
spec:
  replicas: 3
  selector:
    matchLabels:
      app: pedido-service
  template:
    metadata:
      labels:
        app: pedido-service
    spec:
      containers:
        - name: pedido-service
          image: acme/pedido-service:1.0.0
          ports:
            - containerPort: 8080
              name: http
            - containerPort: 8081
              name: management

          # Variables de entorno desde ConfigMap
          envFrom:
            - configMapRef:
                name: pedido-service-config

          # Variables de entorno desde Secret
          env:
            - name: DB_USERNAME
              valueFrom:
                secretKeyRef:
                  name: pedido-service-secrets
                  key: DB_USERNAME
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: pedido-service-secrets
                  key: DB_PASSWORD
            - name: RABBITMQ_USERNAME
              valueFrom:
                secretKeyRef:
                  name: pedido-service-secrets
                  key: RABBITMQ_USERNAME
            - name: RABBITMQ_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: pedido-service-secrets
                  key: RABBITMQ_PASSWORD
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"

          # Probes usando puerto de management
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: management
            initialDelaySeconds: 30
            periodSeconds: 10

          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: management
            initialDelaySeconds: 10
            periodSeconds: 5

          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "1000m"

          # Montar ConfigMap como archivo
          volumeMounts:
            - name: config-volume
              mountPath: /etc/config
              readOnly: true

      volumes:
        - name: config-volume
          configMap:
            name: pedido-service-config
```

---

## 5. Configuración de Conexiones HTTP

### Referencia Rápida

```yaml
spring:
  http:
    client:
      connect-timeout: 2s
      read-timeout: 5s
```

### Guía Detallada

#### Configuración global de clientes HTTP

```yaml
# application.yml
spring:
  http:
    client:
      connect-timeout: 2s
      read-timeout: 5s
      redirects: dont-follow  # follow, dont-follow

# Configuración específica por servicio
app:
  http-clients:
    inventario:
      url: ${INVENTARIO_URL:http://localhost:8081}
      connect-timeout: 2s
      read-timeout: 10s
      max-connections: 50
      max-connections-per-route: 20

    pagos:
      url: ${PAGOS_URL:http://localhost:8082}
      connect-timeout: 5s
      read-timeout: 30s  # Operaciones más lentas
      max-connections: 20

    notificaciones:
      url: ${NOTIFICACIONES_URL:http://localhost:8083}
      connect-timeout: 1s
      read-timeout: 3s
      max-connections: 100  # Alto volumen
```

#### ConfigurationProperties para HTTP clients

```java
@ConfigurationProperties(prefix = "app.http-clients")
@Validated
public class HttpClientsProperties {

    @NestedConfigurationProperty
    @NotNull
    private ServiceClientConfig inventario;

    @NestedConfigurationProperty
    @NotNull
    private ServiceClientConfig pagos;

    @NestedConfigurationProperty
    private ServiceClientConfig notificaciones;

    // Getters y setters...

    @Validated
    public static class ServiceClientConfig {
        @NotBlank
        private String url;

        @NotNull
        private Duration connectTimeout = Duration.ofSeconds(2);

        @NotNull
        private Duration readTimeout = Duration.ofSeconds(5);

        @Min(1)
        private int maxConnections = 50;

        @Min(1)
        private int maxConnectionsPerRoute = 20;

        // Getters y setters...
    }
}
```

#### Configuración de RestClient con properties

```java
@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    private final HttpClientsProperties httpClientsProperties;

    @Bean
    public RestClient inventarioRestClient(RestClient.Builder builder) {
        var config = httpClientsProperties.getInventario();
        return builder
                .baseUrl(config.getUrl())
                .requestFactory(createRequestFactory(config))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public RestClient pagosRestClient(RestClient.Builder builder) {
        var config = httpClientsProperties.getPagos();
        return builder
                .baseUrl(config.getUrl())
                .requestFactory(createRequestFactory(config))
                .build();
    }

    private ClientHttpRequestFactory createRequestFactory(
            HttpClientsProperties.ServiceClientConfig config) {

        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(config.getMaxConnections())
                .setMaxConnPerRoute(config.getMaxConnectionsPerRoute())
                .build();

        var httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

        var requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setConnectTimeout(config.getConnectTimeout());
        requestFactory.setReadTimeout(config.getReadTimeout());

        return requestFactory;
    }
}
```

---

## 6. Configuración de Base de Datos (HikariCP)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:pedidos}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver

    hikari:
      # Pool sizing
      maximum-pool-size: ${DB_POOL_MAX:20}
      minimum-idle: ${DB_POOL_MIN:5}

      # Timeouts
      connection-timeout: 30000      # 30s - Tiempo máximo para obtener conexión
      idle-timeout: 600000           # 10min - Tiempo máximo de conexión idle
      max-lifetime: 1800000          # 30min - Tiempo máximo de vida de conexión
      validation-timeout: 5000       # 5s - Tiempo máximo para validar conexión

      # Configuración de pool
      pool-name: PedidoServicePool
      auto-commit: false             # Mejor rendimiento con @Transactional
      leak-detection-threshold: 60000  # 1min - Detectar connection leaks

      # Propiedades de conexión PostgreSQL
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 250
        prepStmtCacheSqlLimit: 2048
        useServerPrepStmts: true

  jpa:
    hibernate:
      ddl-auto: ${JPA_DDL_AUTO:validate}
    open-in-view: false  # SIEMPRE false en producción
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          batch_size: 50
          fetch_size: 100
        order_inserts: true
        order_updates: true
        generate_statistics: ${HIBERNATE_STATS:false}
        format_sql: ${HIBERNATE_FORMAT_SQL:false}
```

---

## 7. Configuración de RabbitMQ

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    virtual-host: ${RABBITMQ_VHOST:/}

    # Configuración de conexión
    connection-timeout: 30s
    channel-rpc-timeout: 10s

    # SSL (si aplica)
    ssl:
      enabled: ${RABBITMQ_SSL_ENABLED:false}

    # Listener configuration
    listener:
      simple:
        acknowledge-mode: auto
        concurrency: ${RABBITMQ_CONCURRENCY:3}
        max-concurrency: ${RABBITMQ_MAX_CONCURRENCY:10}
        prefetch: 10

        # Retry configuration
        retry:
          enabled: true
          initial-interval: 1s
          max-attempts: 3
          max-interval: 10s
          multiplier: 2.0

        # Dead letter handling
        default-requeue-rejected: false

    # Template configuration
    template:
      retry:
        enabled: true
        initial-interval: 1s
        max-attempts: 3
        max-interval: 10s
        multiplier: 2.0
```

---

## 8. Logging Configuration

```yaml
logging:
  # Nivel por paquete
  level:
    root: ${LOG_LEVEL_ROOT:INFO}
    com.acme: ${LOG_LEVEL_APP:INFO}
    org.springframework.web: ${LOG_LEVEL_WEB:INFO}
    org.springframework.security: ${LOG_LEVEL_SECURITY:INFO}
    org.hibernate.SQL: ${LOG_LEVEL_SQL:WARN}
    org.hibernate.type.descriptor.sql.BasicBinder: ${LOG_LEVEL_SQL_PARAMS:WARN}

  # Patrón para desarrollo (legible)
  pattern:
    console: "%d{HH:mm:ss.SSS} %highlight(%-5level) [%thread] %cyan(%logger{36}) - %msg%n"

  # Para producción, usar JSON estructurado (ver logback-spring.xml)

  # Archivo de log (opcional, preferir stdout en K8s)
  file:
    name: ${LOG_FILE:/var/log/app/application.log}
    max-size: 100MB
    max-history: 7
```

#### logback-spring.xml para JSON en producción

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <!-- Desarrollo: formato legible -->
    <springProfile name="dev,local">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss.SSS} %highlight(%-5level) [%thread] %cyan(%logger{36}) - %msg%n</pattern>
            </encoder>
        </appender>
    </springProfile>

    <!-- Producción: JSON estructurado para ELK -->
    <springProfile name="prod,staging">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LogstashEncoder">
                <includeMdcKeyName>traceId</includeMdcKeyName>
                <includeMdcKeyName>spanId</includeMdcKeyName>
                <includeMdcKeyName>userId</includeMdcKeyName>
                <customFields>{"service":"${spring.application.name}"}</customFields>
            </encoder>
        </appender>
    </springProfile>

    <root level="${LOG_LEVEL_ROOT:-INFO}">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

---

## 9. Propiedades Custom de Aplicación

```yaml
# application.yml
app:
  # Información de la aplicación
  name: ${spring.application.name}
  version: @project.version@
  environment: ${ENVIRONMENT:local}

  # Feature flags
  feature-flags:
    new-checkout: ${FEATURE_NEW_CHECKOUT:false}
    async-notifications: ${FEATURE_ASYNC_NOTIFICATIONS:true}
    cache-enabled: ${FEATURE_CACHE:true}

  # Configuración de negocio
  business:
    max-items-per-order: ${MAX_ITEMS:100}
    order-expiration-hours: ${ORDER_EXPIRATION:24}
    default-currency: ${DEFAULT_CURRENCY:EUR}

  # Servicios externos (ya cubierto arriba)
  external-services:
    inventario:
      url: ${INVENTARIO_URL}
      timeout: 5s
    pagos:
      url: ${PAGOS_URL}
      timeout: 30s

  # Seguridad
  security:
    jwt:
      secret: ${JWT_SECRET}
      expiration: ${JWT_EXPIRATION:3600}
    cors:
      allowed-origins: ${CORS_ORIGINS:http://localhost:3000}
```

```java
@ConfigurationProperties(prefix = "app")
@Validated
public record AppProperties(
        String name,
        String version,
        String environment,
        FeatureFlags featureFlags,
        BusinessConfig business,
        SecurityConfig security
) {
    public record FeatureFlags(
            boolean newCheckout,
            boolean asyncNotifications,
            boolean cacheEnabled
    ) {}

    public record BusinessConfig(
            @Min(1) @Max(1000) int maxItemsPerOrder,
            @Min(1) int orderExpirationHours,
            @NotBlank String defaultCurrency
    ) {}

    public record SecurityConfig(
            JwtConfig jwt,
            CorsConfig cors
    ) {
        public record JwtConfig(
                @NotBlank String secret,
                @Min(60) int expiration
        ) {}

        public record CorsConfig(
                List<String> allowedOrigins
        ) {}
    }
}
```

---

## Checklist de Configuración

| Aspecto | ✅ Correcto | ❌ Incorrecto |
|---------|------------|---------------|
| Formato | YAML para enterprise | Properties para config compleja |
| Perfiles | dev, test, staging, prod | Solo application.yml |
| Secrets | Variables de entorno / K8s Secrets | Hardcoded en yml |
| Propiedades agrupadas | @ConfigurationProperties | Múltiples @Value |
| Validación | @Validated en properties | Sin validación |
| Base de datos prod | ddl-auto: validate/none | ddl-auto: update |
| open-in-view | false | true (default) |
| Logging prod | JSON estructurado | Texto plano |
| Timeouts | Configurados explícitamente | Defaults del framework |

---

## Próximos Pasos

Este documento cubre el **Área 6: Configuración Application Properties/YAML**.

Cuando estés listo, continuamos con el **Área 7: Dependencias Maven y Gestión** que incluirá:
- Spring Boot BOM
- Estructura multi-módulo
- Plugins esenciales
- Gestión de versiones

---

*Documento generado con Context7 - Spring Boot 3.4.x*
