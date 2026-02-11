# Área 14: Resiliencia - Circuit Breaker y Fault Tolerance

## Referencia Rápida (Senior Developers)

> **Stack**: Spring Boot 3.4.x, Java 17+, Resilience4j 2.x
> **Audiencia**: Desarrolladores backend que integran servicios externos (inventario, pagos, notificaciones)

### Dependencias Esenciales

```xml
<!-- Resilience4j Spring Boot 3 Starter -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>

<!-- Si usas WebFlux -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-reactor</artifactId>
</dependency>

<!-- AOP requerido para anotaciones -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>

<!-- Actuator para métricas y health indicators -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### Configuración Mínima CircuitBreaker

```yaml
resilience4j:
  circuitbreaker:
    instances:
      inventarioService:
        registerHealthIndicator: true
        slidingWindowSize: 100
        minimumNumberOfCalls: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
```

### Uso con Anotaciones

```java
@Service
@RequiredArgsConstructor
public class InventarioClient {

    private final RestClient restClient;

    @CircuitBreaker(name = "inventarioService", fallbackMethod = "checkStockFallback")
    @Retry(name = "inventarioService")
    public InventarioResponse checkStock(Long productId) {
        return restClient.get()
                .uri("/api/v1/inventario/products/{id}/stock", productId)
                .retrieve()
                .body(InventarioResponse.class);
    }

    private InventarioResponse checkStockFallback(Long productId, Exception ex) {
        log.warn("Fallback activado para inventario del producto {}: {}", productId, ex.getMessage());
        return InventarioResponse.unavailable(productId);
    }
}
```

### Orden de Ejecución de Decoradores

```
Retry ( CircuitBreaker ( RateLimiter ( Bulkhead ( TimeLimiter ( función ) ) ) ) )
```

---

## Guía Detallada (Junior/Mid Developers)

### 1. Conceptos de Resiliencia

En arquitecturas de microservicios, los servicios dependen unos de otros. Cuando un servicio externo falla o se vuelve lento, debemos proteger nuestro sistema para evitar fallos en cascada.

#### Patrones de Resiliencia

| Patrón | Propósito | Cuándo Usar |
|--------|-----------|-------------|
| CircuitBreaker | Evitar llamadas a servicios caídos | Llamadas HTTP a servicios externos |
| Retry | Reintentar operaciones con fallos transitorios | Timeouts, errores 503, connection refused |
| RateLimiter | Limitar tasa de llamadas salientes | Proteger APIs externas con rate limits |
| Bulkhead | Aislar recursos entre consumidores | Evitar que un servicio lento agote threads |
| TimeLimiter | Limitar tiempo de ejecución | Operaciones async con deadline |

#### Diagrama de Flujo: Llamada Resiliente

```
Cliente → [Retry] → [CircuitBreaker] → [RateLimiter] → [Bulkhead] → Servicio Externo
                          │                                              │
                          │ (OPEN)                                       │ (Fallo)
                          ↓                                              ↓
                     Fallback ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← Exception
```

### 2. Configuración de Dependencias

```xml
<properties>
    <resilience4j.version>2.2.0</resilience4j.version>
</properties>

<dependencies>
    <!-- Resilience4j Spring Boot 3 Starter (incluye todos los módulos) -->
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-spring-boot3</artifactId>
    </dependency>

    <!-- Necesario para @CircuitBreaker, @Retry, etc. -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>

    <!-- Actuator para health indicators y métricas -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Si usas WebFlux / Reactor -->
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-reactor</artifactId>
    </dependency>
</dependencies>
```

#### Estructura de Paquetes

```
com.acme.sales.infrastructure.resilience/
├── config/
│   └── ResilienceConfig.java
├── client/
│   ├── InventarioClient.java
│   └── PaymentGatewayClient.java
└── fallback/
    └── InventarioFallbackHandler.java
```

### 3. CircuitBreaker

El Circuit Breaker previene llamadas repetidas a un servicio que está fallando. Funciona como un interruptor eléctrico:

#### Estados del Circuit Breaker

```
     Llamadas exitosas
          ↓
    ┌──────────┐    Tasa de fallos >= umbral   ┌──────────┐
    │  CLOSED  │ ────────────────────────────→  │   OPEN   │
    │(Normal)  │                                │(Rechaza) │
    └──────────┘                                └──────────┘
         ↑                                           │
         │         Espera waitDuration               │
         │                                           ↓
         │                                    ┌───────────┐
         │    Tasa de fallos < umbral         │ HALF_OPEN │
         └──────────────────────────────────  │ (Prueba)  │
                                              └───────────┘
```

- **CLOSED**: Estado normal. Las llamadas pasan al servicio. Se monitorean fallos.
- **OPEN**: El circuito está abierto. Todas las llamadas se rechazan inmediatamente con `CallNotPermittedException`. Se ejecuta el fallback.
- **HALF_OPEN**: Después de `waitDurationInOpenState`, se permiten unas pocas llamadas de prueba. Si tienen éxito, vuelve a CLOSED. Si fallan, vuelve a OPEN.

#### Configuración Completa en application.yml

```yaml
resilience4j:
  circuitbreaker:
    configs:
      # Configuración por defecto para todos los circuit breakers
      default:
        registerHealthIndicator: true
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 100
        minimumNumberOfCalls: 10
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 10s
        failureRateThreshold: 50
        slowCallRateThreshold: 100
        slowCallDurationThreshold: 3s
        recordExceptions:
          - java.io.IOException
          - java.net.ConnectException
          - java.net.SocketTimeoutException
          - org.springframework.web.client.HttpServerErrorException
        ignoreExceptions:
          - com.acme.sales.domain.exception.BusinessException
    instances:
      inventarioService:
        baseConfig: default
        slidingWindowSize: 50
        waitDurationInOpenState: 15s
        failureRateThreshold: 60
      paymentGateway:
        baseConfig: default
        slidingWindowSize: 20
        minimumNumberOfCalls: 5
        waitDurationInOpenState: 30s
        failureRateThreshold: 40
        slowCallDurationThreshold: 5s
```

#### Parámetros Clave Explicados

| Parámetro | Descripción | Valor Recomendado |
|-----------|-------------|-------------------|
| `slidingWindowType` | COUNT_BASED o TIME_BASED | COUNT_BASED |
| `slidingWindowSize` | Tamaño de la ventana para calcular tasa de error | 50-100 |
| `minimumNumberOfCalls` | Llamadas mínimas antes de calcular tasa | 10-20 |
| `failureRateThreshold` | % de fallos para abrir el circuito | 50 |
| `waitDurationInOpenState` | Tiempo en OPEN antes de pasar a HALF_OPEN | 10-30s |
| `permittedNumberOfCallsInHalfOpenState` | Llamadas de prueba en HALF_OPEN | 3-5 |
| `slowCallDurationThreshold` | Umbral para considerar llamada "lenta" | 2-5s |
| `recordExceptions` | Excepciones que cuentan como fallo | IOExceptions |
| `ignoreExceptions` | Excepciones que NO cuentan como fallo | BusinessException |

#### Uso con Anotación y Fallbacks

```java
package com.acme.sales.infrastructure.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventarioClient {

    private final RestClient restClient;

    @CircuitBreaker(name = "inventarioService", fallbackMethod = "checkStockFallback")
    public StockResponse checkStock(Long productId) {
        log.debug("Consultando stock para producto: {}", productId);
        return restClient.get()
                .uri("/api/v1/inventario/products/{id}/stock", productId)
                .retrieve()
                .body(StockResponse.class);
    }

    @CircuitBreaker(name = "inventarioService", fallbackMethod = "reserveStockFallback")
    public ReservationResponse reserveStock(StockReservationRequest request) {
        return restClient.post()
                .uri("/api/v1/inventario/reservations")
                .body(request)
                .retrieve()
                .body(ReservationResponse.class);
    }

    // --- Fallback Methods ---
    // IMPORTANTE: La firma debe coincidir con el método original + Exception como último parámetro.

    private StockResponse checkStockFallback(Long productId, Exception ex) {
        log.warn("CircuitBreaker fallback para checkStock(productId={}): {}", productId, ex.getMessage());
        return StockResponse.builder()
                .productId(productId)
                .available(true)
                .quantity(-1)  // -1 indica que no se pudo verificar
                .source("FALLBACK")
                .build();
    }

    private ReservationResponse reserveStockFallback(StockReservationRequest request, Exception ex) {
        log.warn("CircuitBreaker fallback para reserveStock: {}", ex.getMessage());
        return ReservationResponse.builder()
                .status(ReservationStatus.PENDING_VERIFICATION)
                .message("Reserva en cola - servicio de inventario temporalmente no disponible")
                .build();
    }
}
```

#### Reglas para Métodos Fallback

```java
// El fallback DEBE tener la misma firma + Exception al final
@CircuitBreaker(name = "svc", fallbackMethod = "fb")
public ResponseType method(ParamA a, ParamB b) { }

// Fallback correcto:
private ResponseType fb(ParamA a, ParamB b, Exception ex) { }

// Fallback con excepción específica (se prefiere sobre Exception genérica):
private ResponseType fb(ParamA a, ParamB b, CallNotPermittedException ex) { }
```

### 4. Retry

El patrón Retry reintenta automáticamente una operación que falló por un error transitorio.

#### Configuración en application.yml

```yaml
resilience4j:
  retry:
    configs:
      default:
        maxAttempts: 3
        waitDuration: 500ms
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
        # Con exponential backoff: 500ms -> 1000ms -> 2000ms
        retryExceptions:
          - java.io.IOException
          - java.net.SocketTimeoutException
          - org.springframework.web.client.HttpServerErrorException
        ignoreExceptions:
          - com.acme.sales.domain.exception.BusinessException
          - com.acme.sales.domain.exception.ResourceNotFoundException
    instances:
      inventarioService:
        baseConfig: default
        maxAttempts: 3
        waitDuration: 500ms
      paymentGateway:
        baseConfig: default
        maxAttempts: 2
        waitDuration: 1000ms
        exponentialBackoffMultiplier: 3
      notificationService:
        maxAttempts: 5
        waitDuration: 200ms
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
```

#### Uso con Anotación

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentGatewayClient {

    private final RestClient restClient;

    @Retry(name = "paymentGateway", fallbackMethod = "processPaymentFallback")
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Procesando pago por ${} para orden {}", request.amount(), request.orderId());
        return restClient.post()
                .uri("/api/v1/payments")
                .body(request)
                .retrieve()
                .body(PaymentResponse.class);
    }

    private PaymentResponse processPaymentFallback(PaymentRequest request, Exception ex) {
        log.error("Todos los reintentos fallaron para pago de orden {}: {}",
                request.orderId(), ex.getMessage());
        return PaymentResponse.builder()
                .orderId(request.orderId())
                .status(PaymentStatus.FAILED)
                .errorMessage("Servicio de pagos no disponible. Intente más tarde.")
                .build();
    }
}
```

#### Monitoreo de Eventos de Retry

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class RetryEventListener {

    private final RetryRegistry retryRegistry;

    @PostConstruct
    public void registerEventListeners() {
        retryRegistry.getAllRetries().forEach(retry ->
            retry.getEventPublisher()
                .onRetry(event -> log.warn("Retry #{} para '{}': {}",
                    event.getNumberOfRetryAttempts(),
                    event.getName(),
                    event.getLastThrowable().getMessage()))
                .onSuccess(event -> {
                    if (event.getNumberOfRetryAttempts() > 0) {
                        log.info("'{}' exitoso después de {} reintentos",
                                event.getName(), event.getNumberOfRetryAttempts());
                    }
                })
                .onError(event -> log.error("Reintentos agotados para '{}' después de {} intentos",
                    event.getName(), event.getNumberOfRetryAttempts()))
        );
    }
}
```

### 5. RateLimiter

El RateLimiter controla la tasa de llamadas salientes para no exceder los límites de APIs externas.

```yaml
resilience4j:
  ratelimiter:
    instances:
      paymentGateway:
        limitForPeriod: 10
        limitRefreshPeriod: 1s
        timeoutDuration: 5s
        # Máximo 10 llamadas por segundo al gateway de pago
      notificationService:
        limitForPeriod: 100
        limitRefreshPeriod: 1s
        timeoutDuration: 2s
      inventarioService:
        limitForPeriod: 30
        limitRefreshPeriod: 1s
        timeoutDuration: 3s
```

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationClient {

    private final RestClient restClient;

    @RateLimiter(name = "notificationService", fallbackMethod = "sendNotificationFallback")
    public NotificationResponse sendNotification(NotificationRequest request) {
        return restClient.post()
                .uri("/api/v1/notifications")
                .body(request)
                .retrieve()
                .body(NotificationResponse.class);
    }

    private NotificationResponse sendNotificationFallback(NotificationRequest request, RequestNotPermitted ex) {
        log.warn("Rate limit excedido para notificaciones. Encolando para envío posterior.");
        messageQueue.enqueue(request);
        return NotificationResponse.builder()
                .status(NotificationStatus.QUEUED)
                .message("Notificación encolada por rate limit")
                .build();
    }
}
```

### 6. Bulkhead

El Bulkhead aísla llamadas a diferentes servicios para evitar que un servicio lento agote todos los recursos.

| Tipo | Mecanismo | Uso |
|------|-----------|-----|
| Semaphore (default) | Limita llamadas concurrentes con semáforo | Llamadas síncronas |
| ThreadPool | Pool de threads aislado | Llamadas asíncronas |

```yaml
resilience4j:
  bulkhead:
    instances:
      inventarioService:
        maxConcurrentCalls: 25
        maxWaitDuration: 500ms
      paymentGateway:
        maxConcurrentCalls: 10
        maxWaitDuration: 1000ms

  # Thread Pool Bulkhead (para operaciones async)
  thread-pool-bulkhead:
    instances:
      reportService:
        maxThreadPoolSize: 10
        coreThreadPoolSize: 5
        queueCapacity: 20
        keepAliveDuration: 100ms
```

```java
@Bulkhead(name = "inventarioService", fallbackMethod = "checkStockBulkheadFallback")
public StockResponse checkStock(Long productId) {
    return restClient.get()
            .uri("/api/v1/inventario/products/{id}/stock", productId)
            .retrieve()
            .body(StockResponse.class);
}

private StockResponse checkStockBulkheadFallback(Long productId, BulkheadFullException ex) {
    log.warn("Bulkhead lleno para inventario. Llamadas concurrentes excedidas.");
    return StockResponse.degraded(productId, "BULKHEAD_FALLBACK");
}
```

### 7. TimeLimiter

El TimeLimiter establece un tiempo máximo de ejecución para operaciones asíncronas.

```yaml
resilience4j:
  timelimiter:
    instances:
      inventarioService:
        timeoutDuration: 3s
        cancelRunningFuture: true
      paymentGateway:
        timeoutDuration: 10s
        cancelRunningFuture: true
      reportService:
        timeoutDuration: 30s
        cancelRunningFuture: false
```

```java
@TimeLimiter(name = "reportService", fallbackMethod = "generateReportFallback")
@Bulkhead(name = "reportService", type = Bulkhead.Type.THREADPOOL)
public CompletableFuture<ReportResponse> generateReport(ReportRequest request) {
    return CompletableFuture.supplyAsync(() ->
        restClient.post()
                .uri("/api/v1/reports")
                .body(request)
                .retrieve()
                .body(ReportResponse.class)
    );
}

private CompletableFuture<ReportResponse> generateReportFallback(ReportRequest request, TimeoutException ex) {
    log.warn("Timeout generando reporte: {}", ex.getMessage());
    return CompletableFuture.completedFuture(
        ReportResponse.builder()
                .status(ReportStatus.TIMEOUT)
                .message("El reporte está tomando más tiempo del esperado. Se enviará por email.")
                .build()
    );
}
```

### 8. Combinando Patrones (Decoradores)

Cuando se combinan múltiples anotaciones, Resilience4j las ejecuta en este orden:

```
Retry -> CircuitBreaker -> RateLimiter -> TimeLimiter -> Bulkhead -> Función

1. Retry: ¿Debo reintentar? (envuelve todo)
2. CircuitBreaker: ¿El circuito está cerrado?
3. RateLimiter: ¿Hay permisos disponibles?
4. TimeLimiter: ¿La operación terminó a tiempo?
5. Bulkhead: ¿Hay capacidad disponible?
6. Se ejecuta la función real
```

#### Stacking de Anotaciones

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class InventarioClient {

    private final RestClient restClient;

    /**
     * Combinación completa de patrones de resiliencia.
     * 1. @Retry: Reintenta hasta 3 veces con backoff exponencial
     * 2. @CircuitBreaker: Si >60% de fallos, abre el circuito
     * 3. @RateLimiter: Máximo 30 llamadas/segundo
     * 4. @Bulkhead: Máximo 25 llamadas concurrentes
     */
    @CircuitBreaker(name = "inventarioService", fallbackMethod = "checkStockFallback")
    @Retry(name = "inventarioService")
    @RateLimiter(name = "inventarioService")
    @Bulkhead(name = "inventarioService")
    public StockResponse checkStock(Long productId) {
        return restClient.get()
                .uri("/api/v1/inventario/products/{id}/stock", productId)
                .retrieve()
                .body(StockResponse.class);
    }

    private StockResponse checkStockFallback(Long productId, Exception ex) {
        log.warn("Fallback para checkStock({}): {} - {}",
                productId, ex.getClass().getSimpleName(), ex.getMessage());
        return StockResponse.degraded(productId, "FALLBACK");
    }
}
```

#### Personalizar el Orden de Ejecución

```yaml
resilience4j:
  circuitbreaker:
    circuit-breaker-aspect-order: 1
  retry:
    retry-aspect-order: 2
  ratelimiter:
    rate-limiter-aspect-order: 3
  bulkhead:
    bulkhead-aspect-order: 4
  timelimiter:
    time-limiter-aspect-order: 5
# Número menor = se ejecuta primero (más externo)
```

### 9. Ejemplo Completo: Servicio de E-Commerce

#### Configuración Completa application.yml

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        registerHealthIndicator: true
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 100
        minimumNumberOfCalls: 10
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 10s
        failureRateThreshold: 50
        slowCallRateThreshold: 100
        slowCallDurationThreshold: 3s
        recordExceptions:
          - java.io.IOException
          - java.net.ConnectException
          - java.net.SocketTimeoutException
          - org.springframework.web.client.HttpServerErrorException
        ignoreExceptions:
          - com.acme.sales.domain.exception.BusinessException
    instances:
      inventarioService:
        baseConfig: default
        slidingWindowSize: 50
        waitDurationInOpenState: 15s
        failureRateThreshold: 60
      paymentGateway:
        baseConfig: default
        slidingWindowSize: 20
        minimumNumberOfCalls: 5
        waitDurationInOpenState: 30s
        failureRateThreshold: 40
        slowCallDurationThreshold: 5s

  retry:
    configs:
      default:
        maxAttempts: 3
        waitDuration: 500ms
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - java.io.IOException
          - java.net.SocketTimeoutException
        ignoreExceptions:
          - com.acme.sales.domain.exception.BusinessException
    instances:
      inventarioService:
        baseConfig: default
      paymentGateway:
        baseConfig: default
        maxAttempts: 2
        waitDuration: 1000ms

  ratelimiter:
    instances:
      inventarioService:
        limitForPeriod: 30
        limitRefreshPeriod: 1s
        timeoutDuration: 3s
      paymentGateway:
        limitForPeriod: 10
        limitRefreshPeriod: 1s
        timeoutDuration: 5s

  bulkhead:
    instances:
      inventarioService:
        maxConcurrentCalls: 25
        maxWaitDuration: 500ms
      paymentGateway:
        maxConcurrentCalls: 10
        maxWaitDuration: 1000ms

  timelimiter:
    instances:
      inventarioService:
        timeoutDuration: 3s
        cancelRunningFuture: true
      paymentGateway:
        timeoutDuration: 10s
        cancelRunningFuture: true
```

#### Cliente Completo con Todos los Patrones

```java
package com.acme.sales.infrastructure.client;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventarioClient {

    private final RestClient inventarioRestClient;

    @CircuitBreaker(name = "inventarioService", fallbackMethod = "checkStockFallback")
    @Retry(name = "inventarioService")
    @RateLimiter(name = "inventarioService")
    @Bulkhead(name = "inventarioService")
    public StockResponse checkStock(Long productId) {
        log.debug("Consultando stock para producto: {}", productId);
        return inventarioRestClient.get()
                .uri("/api/v1/products/{id}/stock", productId)
                .retrieve()
                .body(StockResponse.class);
    }

    @CircuitBreaker(name = "inventarioService", fallbackMethod = "reserveStockFallback")
    @Retry(name = "inventarioService")
    @Bulkhead(name = "inventarioService")
    public ReservationResponse reserveStock(StockReservationRequest request) {
        log.info("Reservando stock: {} unidades del producto {}",
                request.quantity(), request.productId());
        return inventarioRestClient.post()
                .uri("/api/v1/reservations")
                .body(request)
                .retrieve()
                .body(ReservationResponse.class);
    }

    // Fallback específico por tipo de excepción (se prefiere sobre genérico)
    private StockResponse checkStockFallback(Long productId, CallNotPermittedException ex) {
        log.warn("CircuitBreaker OPEN para inventario. Producto: {}", productId);
        return StockResponse.degraded(productId, "CIRCUIT_OPEN");
    }

    // Fallback genérico (catch-all)
    private StockResponse checkStockFallback(Long productId, Exception ex) {
        log.warn("Fallback genérico para checkStock({}): {}", productId, ex.getMessage());
        return StockResponse.degraded(productId, "SERVICE_ERROR");
    }

    private ReservationResponse reserveStockFallback(StockReservationRequest request, Exception ex) {
        log.error("Fallback para reserveStock (producto {}): {}", request.productId(), ex.getMessage());
        return ReservationResponse.builder()
                .productId(request.productId())
                .status(ReservationStatus.PENDING_VERIFICATION)
                .message("Reserva pendiente - servicio temporalmente no disponible")
                .build();
    }
}
```

### 10. Monitoreo con Actuator

#### Configuración de Health Indicators y Métricas

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,circuitbreakers,retries,ratelimiters
  endpoint:
    health:
      show-details: always
  health:
    circuitbreakers:
      enabled: true
    ratelimiters:
      enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
    distribution:
      percentiles-histogram:
        resilience4j.circuitbreaker.calls: true
```

#### Endpoints de Actuator Disponibles

| Endpoint | Descripción |
|----------|-------------|
| `/actuator/health` | Estado general incluyendo circuit breakers |
| `/actuator/circuitbreakers` | Detalle de todos los circuit breakers |
| `/actuator/circuitbreakerevents` | Eventos recientes de circuit breakers |
| `/actuator/retries` | Estado de los retries configurados |
| `/actuator/retryevents` | Eventos recientes de retries |
| `/actuator/ratelimiters` | Estado de los rate limiters |
| `/actuator/prometheus` | Métricas en formato Prometheus |

#### Ejemplo de Respuesta /actuator/health

```json
{
  "status": "UP",
  "components": {
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "inventarioService": {
          "status": "UP",
          "details": {
            "failureRate": "12.5%",
            "failureRateThreshold": "60.0%",
            "bufferedCalls": 48,
            "failedCalls": 6,
            "notPermittedCalls": 0,
            "state": "CLOSED"
          }
        }
      }
    }
  }
}
```

#### Métricas Prometheus Clave

```
# Circuit Breaker
resilience4j_circuitbreaker_state{name="inventarioService"} 0.0
resilience4j_circuitbreaker_calls_seconds_count{name="inventarioService",kind="successful"} 142
resilience4j_circuitbreaker_calls_seconds_count{name="inventarioService",kind="failed"} 6
resilience4j_circuitbreaker_failure_rate{name="inventarioService"} 12.5

# Retry
resilience4j_retry_calls_total{name="inventarioService",kind="successful_without_retry"} 130
resilience4j_retry_calls_total{name="inventarioService",kind="successful_with_retry"} 12
resilience4j_retry_calls_total{name="inventarioService",kind="failed_with_retry"} 6

# Bulkhead
resilience4j_bulkhead_available_concurrent_calls{name="inventarioService"} 23
resilience4j_bulkhead_max_allowed_concurrent_calls{name="inventarioService"} 25
```

#### Monitoreo Programático

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ResilienceMonitor {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Scheduled(fixedRate = 30000)  // Cada 30 segundos
    public void logCircuitBreakerStatus() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            CircuitBreaker.Metrics metrics = cb.getMetrics();
            log.info("CircuitBreaker [{}]: state={}, failureRate={}%, calls={}, failed={}",
                    cb.getName(),
                    cb.getState(),
                    metrics.getFailureRate(),
                    metrics.getNumberOfBufferedCalls(),
                    metrics.getNumberOfFailedCalls()
            );
        });
    }
}
```

### 11. Testing

#### Test de CircuitBreaker

```java
@SpringBootTest
class InventarioClientCircuitBreakerTest {

    @Autowired
    private InventarioClient inventarioClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockitoBean
    private RestClient inventarioRestClient;

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("inventarioService");
        circuitBreaker.reset();  // Resetear estado entre tests
    }

    @Test
    void shouldOpenCircuitBreakerAfterFailures() {
        // Given: El servicio de inventario falla consistentemente
        when(inventarioRestClient.get()).thenThrow(new IOException("Connection refused"));

        // When: Se superan las llamadas mínimas con alta tasa de error
        for (int i = 0; i < 15; i++) {
            try { inventarioClient.checkStock(1L); }
            catch (Exception ignored) { }
        }

        // Then: El circuit breaker debe estar OPEN
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void shouldReturnFallbackWhenCircuitBreakerIsOpen() {
        // Given: Forzar el circuit breaker a estado OPEN
        circuitBreaker.transitionToOpenState();

        // When
        StockResponse response = inventarioClient.checkStock(42L);

        // Then: Se debe ejecutar el fallback
        assertThat(response).isNotNull();
        assertThat(response.getSource()).isEqualTo("CIRCUIT_OPEN");
        assertThat(response.getProductId()).isEqualTo(42L);
    }

    @Test
    void shouldTransitionToHalfOpenAfterWaitDuration() {
        circuitBreaker.transitionToOpenState();
        circuitBreaker.transitionToHalfOpenState();  // Transición manual en tests

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
    }
}
```

#### Test de Retry

```java
@SpringBootTest
class PaymentGatewayClientRetryTest {

    @Autowired
    private PaymentGatewayClient paymentGatewayClient;

    @MockitoBean
    private RestClient paymentRestClient;

    @Test
    void shouldRetryOnTransientFailure() {
        // Given: Primer intento falla, segundo exitoso
        when(paymentRestClient.post())
                .thenThrow(new IOException("Connection reset"))
                .thenReturn(/* mock response chain */);

        // When
        PaymentRequest request = new PaymentRequest(1L, BigDecimal.valueOf(99.99));
        PaymentResponse response = paymentGatewayClient.processPayment(request);

        // Then
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        verify(paymentRestClient, times(2)).post();
    }

    @Test
    void shouldReturnFallbackAfterAllRetriesExhausted() {
        when(paymentRestClient.post()).thenThrow(new IOException("Service unavailable"));

        PaymentRequest request = new PaymentRequest(1L, BigDecimal.valueOf(99.99));
        PaymentResponse response = paymentGatewayClient.processPayment(request);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(response.getErrorMessage()).contains("no disponible");
    }
}
```

#### Test Unitario sin Spring Context

```java
class CircuitBreakerUnitTest {

    @Test
    void shouldOpenAfterThresholdExceeded() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(4)
                .slidingWindowSize(4)
                .waitDurationInOpenState(Duration.ofSeconds(1))
                .build();

        CircuitBreaker cb = CircuitBreaker.of("testService", config);

        // Simular 2 éxitos + 2 fallos = 50% failure rate
        cb.onSuccess(0, TimeUnit.NANOSECONDS);
        cb.onSuccess(0, TimeUnit.NANOSECONDS);
        cb.onError(0, TimeUnit.NANOSECONDS, new IOException("fail"));
        cb.onError(0, TimeUnit.NANOSECONDS, new IOException("fail"));

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void shouldIgnoreBusinessExceptions() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(4)
                .slidingWindowSize(4)
                .ignoreExceptions(BusinessException.class)
                .build();

        CircuitBreaker cb = CircuitBreaker.of("testService", config);

        for (int i = 0; i < 10; i++) {
            cb.onError(0, TimeUnit.NANOSECONDS, new BusinessException("invalid"));
        }

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
}
```

---

## ✅ Hacer

1. **Configurar `recordExceptions` explícitamente** - Solo excepciones de infraestructura (IOException, ConnectException)
2. **Usar `ignoreExceptions` para errores de negocio** - `BusinessException`, `ValidationException` no deben abrir el circuito
3. **Implementar fallbacks significativos** - Respuestas degradadas, caché local, colas de reintentos
4. **Separar configuraciones por servicio** - Cada servicio externo tiene SLA diferente
5. **Usar configuraciones base (`baseConfig`)** - Evitar duplicación en application.yml
6. **Habilitar health indicators** - `registerHealthIndicator: true` para visibilidad en Actuator
7. **Monitorear métricas en Prometheus/Grafana** - Dashboards para tasa de fallos y estado de circuitos
8. **Resetear circuit breakers en tests** - `circuitBreaker.reset()` en `@BeforeEach`
9. **Configurar timeouts en RestClient** - Complementar TimeLimiter con connect/read timeouts
10. **Documentar el comportamiento de fallback** - El equipo debe saber qué ocurre cuando un servicio cae

## ❌ Evitar

1. **Retry para operaciones no idempotentes** - No reintentar POST de pagos sin idempotency key
2. **Circuit Breaker en operaciones locales** - Solo para llamadas a servicios externos
3. **Fallback que lanza excepciones** - El fallback debe retornar una respuesta degradada, no fallar
4. **`slidingWindowSize` muy pequeño** - Valores menores a 10 producen falsos positivos
5. **`waitDurationInOpenState` muy corto** - Dar tiempo real al servicio para recuperarse
6. **Ignorar las métricas** - Sin monitoreo, no sabrás cuándo los circuitos se abren
7. **Retry con backoff exponencial sin límite** - Siempre configurar `maxAttempts`
8. **Rate limiter sin fallback** - `RequestNotPermitted` debe manejarse con gracia
9. **Misma configuración para todos los servicios** - Un servicio de pagos necesita parámetros diferentes que uno de notificaciones
10. **Tests que dependen de tiempos reales** - Usar `circuitBreaker.transitionToOpenState()` en vez de esperar timeouts

---

## Checklist de Implementación

### Dependencias
- [ ] `resilience4j-spring-boot3` en pom.xml
- [ ] `spring-boot-starter-aop` para soporte de anotaciones
- [ ] `spring-boot-starter-actuator` para métricas y health indicators

### CircuitBreaker
- [ ] Configuración `default` con valores base en application.yml
- [ ] Instancias específicas por servicio externo (inventario, pagos, etc.)
- [ ] `recordExceptions` con excepciones de infraestructura
- [ ] `ignoreExceptions` con excepciones de negocio
- [ ] `@CircuitBreaker` con `fallbackMethod` en cada método de cliente
- [ ] Fallback methods con firma correcta (mismos parámetros + Exception)

### Retry
- [ ] `maxAttempts` configurado (2-5 según el servicio)
- [ ] `enableExponentialBackoff: true` con multiplicador
- [ ] `retryExceptions` solo para errores transitorios
- [ ] `ignoreExceptions` para errores de negocio (no reintentar 404, 422)

### RateLimiter
- [ ] `limitForPeriod` ajustado al SLA del servicio externo
- [ ] `timeoutDuration` para espera máxima antes de rechazar
- [ ] Fallback que encola o degrada la respuesta

### Bulkhead
- [ ] `maxConcurrentCalls` por servicio externo
- [ ] Semaphore para llamadas síncronas
- [ ] ThreadPool para operaciones async (reportes, batch)

### Monitoreo
- [ ] Health indicators habilitados para circuit breakers
- [ ] Endpoints de Actuator expuestos (`circuitbreakers`, `retries`)
- [ ] Métricas Prometheus configuradas
- [ ] Event listeners para logging de transiciones de estado

### Testing
- [ ] Tests de integración con `CircuitBreakerRegistry`
- [ ] Tests de fallback (circuito abierto retorna respuesta degradada)
- [ ] Tests de retry (verifica número de intentos)
- [ ] `circuitBreaker.reset()` en `@BeforeEach`
- [ ] Tests unitarios sin Spring context para lógica de configuración

---

*Documento generado con Context7 - Resilience4j 2.x + Spring Boot 3.4.x*
