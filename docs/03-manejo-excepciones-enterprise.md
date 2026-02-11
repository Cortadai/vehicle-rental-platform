# Área 3: Manejo de Excepciones Enterprise-Grade

> **Audiencia**: Desarrolladores junior/mid (guía detallada) + Seniors (referencia rápida)
> **Stack**: Spring Boot 3.4.x, Java 17+, Maven 3.9+, PostgreSQL, RabbitMQ, Kubernetes

---

## 1. Jerarquía de Excepciones Custom

### Referencia Rápida (Seniors)

```
RuntimeException
└── BusinessException (base)
    ├── NotFoundException
    ├── ValidationException
    ├── ConflictException
    └── ForbiddenException
```

### Guía Detallada (Junior/Mid)

✅ **Hacer**: Crear una jerarquía clara de excepciones de negocio

```java
// shared/exception/BusinessException.java
public abstract class BusinessException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    protected BusinessException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    protected BusinessException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
```

```java
// shared/exception/NotFoundException.java
public class NotFoundException extends BusinessException {

    public NotFoundException(String resourceType, Object resourceId) {
        super(
            String.format("%s con id '%s' no encontrado", resourceType, resourceId),
            "RESOURCE_NOT_FOUND",
            HttpStatus.NOT_FOUND
        );
    }

    public NotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
```

```java
// shared/exception/ValidationException.java
public class ValidationException extends BusinessException {

    private final List<FieldError> fieldErrors;

    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        this.fieldErrors = List.of();
    }

    public ValidationException(String message, List<FieldError> fieldErrors) {
        super(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        this.fieldErrors = fieldErrors;
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    public record FieldError(String field, String message, Object rejectedValue) {}
}
```

```java
// shared/exception/ConflictException.java
public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super(message, "CONFLICT", HttpStatus.CONFLICT);
    }

    public static ConflictException duplicateResource(String resourceType, String field, Object value) {
        return new ConflictException(
            String.format("%s con %s '%s' ya existe", resourceType, field, value)
        );
    }
}
```

```java
// shared/exception/ForbiddenException.java
public class ForbiddenException extends BusinessException {

    public ForbiddenException(String message) {
        super(message, "FORBIDDEN", HttpStatus.FORBIDDEN);
    }

    public ForbiddenException() {
        super("No tiene permisos para realizar esta acción", "FORBIDDEN", HttpStatus.FORBIDDEN);
    }
}
```

**Uso en servicios**:
```java
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public Cliente obtenerPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cliente", id));
    }

    @Transactional
    public Cliente crear(CrearClienteCommand command) {
        if (clienteRepository.existsByEmail(command.email())) {
            throw ConflictException.duplicateResource("Cliente", "email", command.email());
        }
        return clienteRepository.save(new Cliente(command.nombre(), command.email()));
    }
}
```

❌ **Evitar**: Excepciones genéricas sin contexto

```java
// MAL
throw new RuntimeException("Error");
throw new Exception("No encontrado");

// BIEN
throw new NotFoundException("Cliente", clienteId);
throw new ConflictException("El email ya está registrado");
```

---

## 2. RFC 7807/9457 Problem Details

### Referencia Rápida

```json
{
    "type": "https://api.acme.com/errors/resource-not-found",
    "title": "Resource Not Found",
    "status": 404,
    "detail": "Cliente con id '123' no encontrado",
    "instance": "/api/v1/clientes/123"
}
```

### Guía Detallada

✅ **Hacer**: Habilitar Problem Details en Spring Boot 3.4

**application.yml**:
```yaml
spring:
  mvc:
    problemdetails:
      enabled: true
```

**Modelo de error extendido** (para campos adicionales):
```java
// shared/exception/ApiProblemDetail.java
public class ApiProblemDetail extends ProblemDetail {

    private String errorCode;
    private Instant timestamp;
    private String traceId;
    private List<FieldError> fieldErrors;

    public static ApiProblemDetail forStatus(HttpStatus status) {
        ApiProblemDetail problem = new ApiProblemDetail();
        problem.setStatus(status.value());
        problem.setTimestamp(Instant.now());
        return problem;
    }

    public ApiProblemDetail withErrorCode(String errorCode) {
        this.errorCode = errorCode;
        setProperty("errorCode", errorCode);
        return this;
    }

    public ApiProblemDetail withTraceId(String traceId) {
        this.traceId = traceId;
        setProperty("traceId", traceId);
        return this;
    }

    public ApiProblemDetail withFieldErrors(List<FieldError> fieldErrors) {
        this.fieldErrors = fieldErrors;
        setProperty("fieldErrors", fieldErrors);
        return this;
    }

    // Getters...

    public record FieldError(String field, String message, Object rejectedValue) {}
}
```

**Respuesta JSON resultante**:
```json
{
    "type": "https://api.acme.com/errors/validation-error",
    "title": "Validation Error",
    "status": 400,
    "detail": "Error de validación en los datos de entrada",
    "instance": "/api/v1/clientes",
    "errorCode": "VALIDATION_ERROR",
    "timestamp": "2024-01-15T10:30:00Z",
    "traceId": "abc123def456",
    "fieldErrors": [
        {
            "field": "email",
            "message": "debe ser un email válido",
            "rejectedValue": "invalid-email"
        }
    ]
}
```

---

## 3. Global Exception Handler con @ControllerAdvice

### Referencia Rápida

```java
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    // Maneja todas las excepciones de forma centralizada
}
```

### Guía Detallada

✅ **Hacer**: Handler centralizado que extiende `ResponseEntityExceptionHandler`

```java
// shared/exception/GlobalExceptionHandler.java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String ERROR_TYPE_BASE = "https://api.acme.com/errors/";

    // ==================== EXCEPCIONES DE NEGOCIO ====================

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request) {

        log.warn("Business exception: {} - {}", ex.getErrorCode(), ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(ex.getHttpStatus());
        problem.setType(URI.create(ERROR_TYPE_BASE + ex.getErrorCode().toLowerCase().replace("_", "-")));
        problem.setTitle(formatTitle(ex.getErrorCode()));
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("timestamp", Instant.now());

        addTraceId(problem);

        return ResponseEntity.status(ex.getHttpStatus()).body(problem);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(
            ValidationException ex,
            HttpServletRequest request) {

        log.warn("Validation exception: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create(ERROR_TYPE_BASE + "validation-error"));
        problem.setTitle("Validation Error");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errorCode", "VALIDATION_ERROR");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("fieldErrors", ex.getFieldErrors());

        addTraceId(problem);

        return ResponseEntity.badRequest().body(problem);
    }

    // ==================== EXCEPCIONES DE SPRING ====================

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        log.warn("Validation failed: {}", ex.getMessage());

        List<ValidationException.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ValidationException.FieldError(
                        error.getField(),
                        error.getDefaultMessage(),
                        error.getRejectedValue()))
                .toList();

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create(ERROR_TYPE_BASE + "validation-error"));
        problem.setTitle("Validation Error");
        problem.setDetail("Error de validación en los datos de entrada");
        problem.setProperty("errorCode", "VALIDATION_ERROR");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("fieldErrors", fieldErrors);

        addTraceId(problem);

        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        log.warn("Malformed JSON: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create(ERROR_TYPE_BASE + "malformed-request"));
        problem.setTitle("Malformed Request");
        problem.setDetail("El cuerpo de la petición no es válido");
        problem.setProperty("errorCode", "MALFORMED_REQUEST");
        problem.setProperty("timestamp", Instant.now());

        addTraceId(problem);

        return ResponseEntity.badRequest().body(problem);
    }

    // ==================== EXCEPCIONES DE BASE DE DATOS ====================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        log.error("Data integrity violation: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setType(URI.create(ERROR_TYPE_BASE + "data-integrity-violation"));
        problem.setTitle("Data Integrity Violation");
        problem.setDetail("Violación de integridad de datos. Posible duplicado o referencia inválida.");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errorCode", "DATA_INTEGRITY_VIOLATION");
        problem.setProperty("timestamp", Instant.now());

        addTraceId(problem);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    // ==================== CATCH-ALL ====================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request) {

        // SIEMPRE loggear errores inesperados con stack trace
        log.error("Unexpected error processing request: {} {}",
                request.getMethod(),
                request.getRequestURI(),
                ex);

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setType(URI.create(ERROR_TYPE_BASE + "internal-server-error"));
        problem.setTitle("Internal Server Error");
        problem.setDetail("Ha ocurrido un error inesperado. Por favor, contacte al administrador.");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errorCode", "INTERNAL_SERVER_ERROR");
        problem.setProperty("timestamp", Instant.now());

        addTraceId(problem);

        // NO exponer detalles del error en producción
        return ResponseEntity.internalServerError().body(problem);
    }

    // ==================== HELPERS ====================

    private void addTraceId(ProblemDetail problem) {
        // Si usas Spring Cloud Sleuth / Micrometer Tracing
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            problem.setProperty("traceId", traceId);
        }
    }

    private String formatTitle(String errorCode) {
        return Arrays.stream(errorCode.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
```

### Alternativa: Usar ProblemDetail Nativo de Spring Boot 3.x

Spring Boot 3.x incluye soporte nativo para RFC 7807/9457 mediante `org.springframework.http.ProblemDetail`:

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Error de Negocio");
        problem.setType(URI.create("https://api.acme.com/errors/business"));
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Recurso No Encontrado");
        problem.setType(URI.create("https://api.acme.com/errors/not-found"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
```

> ✅ **Recomendación Spring Boot 3.4.x**: Preferir el `ProblemDetail` nativo de Spring (`org.springframework.http.ProblemDetail`) sobre implementaciones custom, ya que se integra automáticamente con el framework y soporta content negotiation.

Para habilitarlo globalmente:
```yaml
# application.yml
spring:
  mvc:
    problemdetails:
      enabled: true
```

---

## 4. Logging de Excepciones

### Referencia Rápida

| Tipo de Error | Nivel Log | ¿Stack Trace? | ¿Alertar? |
|---------------|-----------|---------------|-----------|
| Validación (400) | WARN | No | No |
| Not Found (404) | WARN | No | No |
| Conflict (409) | WARN | No | No |
| Forbidden (403) | WARN | No | Monitorear |
| Internal Error (500) | ERROR | Sí | Sí |
| External Service Error | ERROR | Sí | Sí |

### Guía Detallada

✅ **Hacer**: Loggear según severidad y contexto

```java
// Errores de validación/negocio: WARN sin stack trace
log.warn("Validation failed for cliente creation: email={}", request.email());
log.warn("Cliente not found: id={}", clienteId);

// Errores inesperados: ERROR con stack trace completo
log.error("Unexpected error processing pedido: pedidoId={}", pedidoId, exception);

// Errores de servicios externos: ERROR con contexto
log.error("Failed to call inventory service: url={}, status={}, body={}",
        url, response.getStatusCode(), response.getBody(), exception);
```

✅ **Hacer**: Structured logging para Kubernetes/ELK

```java
// Usar MDC para contexto adicional
MDC.put("clienteId", clienteId.toString());
MDC.put("operation", "crearPedido");
try {
    // operación
} finally {
    MDC.clear();
}
```

**logback-spring.xml** para JSON structured logging:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <!-- Perfil para desarrollo (formato legible) -->
    <springProfile name="dev">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss.SSS} %highlight(%-5level) [%thread] %cyan(%logger{36}) - %msg%n</pattern>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

    <!-- Perfil para producción (JSON para ELK/agregadores) -->
    <springProfile name="prod">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LogstashEncoder">
                <includeMdcKeyName>traceId</includeMdcKeyName>
                <includeMdcKeyName>spanId</includeMdcKeyName>
                <includeMdcKeyName>clienteId</includeMdcKeyName>
                <includeMdcKeyName>operation</includeMdcKeyName>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>
</configuration>
```

❌ **Evitar**:

```java
// MAL: Loggear información sensible
log.info("User login: email={}, password={}", email, password);

// MAL: Loggear objetos completos sin control
log.info("Created pedido: {}", pedido); // Puede exponer datos sensibles

// MAL: Usar System.out/printStackTrace
System.out.println("Error: " + e.getMessage());
e.printStackTrace();

// MAL: Swallow exceptions silenciosamente
try {
    // operación
} catch (Exception e) {
    // No hacer nada
}
```

---

## 5. Exception Handling en Microservicios

### Referencia Rápida

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Gateway   │────▶│  Service A  │────▶│  Service B  │
└─────────────┘     └─────────────┘     └─────────────┘
       │                   │                   │
       ▼                   ▼                   ▼
  Propaga error      Traduce error      Genera error
  con traceId        externo            original
```

### Guía Detallada

#### Cliente HTTP con manejo de errores (RestClient - Spring Boot 3.4+)

```java
// shared/client/BaseRestClient.java
@Slf4j
public abstract class BaseRestClient {

    protected final RestClient restClient;
    protected final String serviceName;

    protected BaseRestClient(RestClient.Builder builder, String baseUrl, String serviceName) {
        this.serviceName = serviceName;
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultStatusHandler(HttpStatusCode::isError, this::handleError)
                .build();
    }

    private void handleError(HttpRequest request, ClientHttpResponse response) throws IOException {
        HttpStatusCode status = response.getStatusCode();
        String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);

        log.error("Error calling {}: {} {} returned {} - {}",
                serviceName,
                request.getMethod(),
                request.getURI(),
                status.value(),
                body);

        if (status.value() == 404) {
            throw new ExternalServiceException(
                    serviceName,
                    "Recurso no encontrado en servicio externo",
                    status.value());
        }

        if (status.is4xxClientError()) {
            throw new ExternalServiceException(
                    serviceName,
                    "Error de cliente en servicio externo: " + body,
                    status.value());
        }

        if (status.is5xxServerError()) {
            throw new ExternalServiceException(
                    serviceName,
                    "Servicio externo no disponible",
                    status.value());
        }
    }
}
```

```java
// shared/exception/ExternalServiceException.java
public class ExternalServiceException extends RuntimeException {

    private final String serviceName;
    private final int statusCode;

    public ExternalServiceException(String serviceName, String message, int statusCode) {
        super(message);
        this.serviceName = serviceName;
        this.statusCode = statusCode;
    }

    public ExternalServiceException(String serviceName, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
        this.statusCode = 503;
    }

    public String getServiceName() {
        return serviceName;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
```

#### Cliente específico de servicio

```java
// inventario/client/InventarioClient.java
@Component
@Slf4j
public class InventarioClient extends BaseRestClient {

    public InventarioClient(
            RestClient.Builder builder,
            @Value("${services.inventario.url}") String baseUrl) {
        super(builder, baseUrl, "inventario-service");
    }

    public Optional<StockDto> consultarStock(String productoId) {
        try {
            StockDto stock = restClient.get()
                    .uri("/api/v1/productos/{id}/stock", productoId)
                    .retrieve()
                    .body(StockDto.class);
            return Optional.ofNullable(stock);
        } catch (ExternalServiceException e) {
            if (e.getStatusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public void reservarStock(ReservarStockRequest request) {
        restClient.post()
                .uri("/api/v1/reservas")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
```

#### Handler para errores de servicios externos

```java
// En GlobalExceptionHandler.java, agregar:

@ExceptionHandler(ExternalServiceException.class)
public ResponseEntity<ProblemDetail> handleExternalServiceException(
        ExternalServiceException ex,
        HttpServletRequest request) {

    log.error("External service error: service={}, status={}, message={}",
            ex.getServiceName(),
            ex.getStatusCode(),
            ex.getMessage());

    // Traducir el error externo a un error apropiado para el cliente
    HttpStatus status = ex.getStatusCode() >= 500
            ? HttpStatus.SERVICE_UNAVAILABLE
            : HttpStatus.BAD_GATEWAY;

    ProblemDetail problem = ProblemDetail.forStatus(status);
    problem.setType(URI.create(ERROR_TYPE_BASE + "external-service-error"));
    problem.setTitle("External Service Error");
    problem.setDetail("Error al comunicarse con servicio externo. Intente más tarde.");
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("errorCode", "EXTERNAL_SERVICE_ERROR");
    problem.setProperty("timestamp", Instant.now());
    problem.setProperty("serviceName", ex.getServiceName());

    addTraceId(problem);

    return ResponseEntity.status(status).body(problem);
}

@ExceptionHandler(ResourceAccessException.class)
public ResponseEntity<ProblemDetail> handleResourceAccessException(
        ResourceAccessException ex,
        HttpServletRequest request) {

    log.error("Connection error to external service: {}", ex.getMessage(), ex);

    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
    problem.setType(URI.create(ERROR_TYPE_BASE + "service-unavailable"));
    problem.setTitle("Service Unavailable");
    problem.setDetail("Servicio temporalmente no disponible. Intente más tarde.");
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("errorCode", "SERVICE_UNAVAILABLE");
    problem.setProperty("timestamp", Instant.now());

    addTraceId(problem);

    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
}
```

---

## 6. Manejo de Errores en RabbitMQ

### Referencia Rápida

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        retry:
          enabled: true
          max-attempts: 3
        default-requeue-rejected: false
```

### Guía Detallada

✅ **Hacer**: Configurar Dead Letter Queue (DLQ) para mensajes fallidos

```java
// shared/config/RabbitMQConfig.java
@Configuration
public class RabbitMQConfig {

    public static final String PEDIDOS_QUEUE = "pedidos.creados";
    public static final String PEDIDOS_DLQ = "pedidos.creados.dlq";
    public static final String PEDIDOS_DLX = "pedidos.dlx";

    @Bean
    public Queue pedidosQueue() {
        return QueueBuilder.durable(PEDIDOS_QUEUE)
                .withArgument("x-dead-letter-exchange", PEDIDOS_DLX)
                .withArgument("x-dead-letter-routing-key", PEDIDOS_DLQ)
                .build();
    }

    @Bean
    public Queue pedidosDlq() {
        return QueueBuilder.durable(PEDIDOS_DLQ).build();
    }

    @Bean
    public DirectExchange pedidosDlx() {
        return new DirectExchange(PEDIDOS_DLX);
    }

    @Bean
    public Binding pedidosDlqBinding() {
        return BindingBuilder.bind(pedidosDlq())
                .to(pedidosDlx())
                .with(PEDIDOS_DLQ);
    }
}
```

✅ **Hacer**: Listener con manejo de errores apropiado

```java
// pedido/listener/PedidoEventListener.java
@Component
@Slf4j
@RequiredArgsConstructor
public class PedidoEventListener {

    private final NotificacionService notificacionService;

    @RabbitListener(queues = RabbitMQConfig.PEDIDOS_QUEUE)
    public void handlePedidoCreado(PedidoCreadoEvent event) {
        log.info("Processing PedidoCreadoEvent: pedidoId={}", event.pedidoId());

        try {
            notificacionService.enviarConfirmacion(event);
            log.info("Successfully processed PedidoCreadoEvent: pedidoId={}", event.pedidoId());

        } catch (TransientException e) {
            // Error temporal (red, servicio no disponible) - reintentable
            log.warn("Transient error processing event, will retry: pedidoId={}, error={}",
                    event.pedidoId(), e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Transient error, sending to DLQ", e);

        } catch (BusinessException e) {
            // Error de negocio - no reintentable, va a DLQ
            log.error("Business error processing event, sending to DLQ: pedidoId={}, error={}",
                    event.pedidoId(), e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Business error", e);

        } catch (Exception e) {
            // Error inesperado - loggear y enviar a DLQ
            log.error("Unexpected error processing event: pedidoId={}", event.pedidoId(), e);
            throw new AmqpRejectAndDontRequeueException("Unexpected error", e);
        }
    }
}
```

**application.yml** con retry:
```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    listener:
      simple:
        acknowledge-mode: auto
        retry:
          enabled: true
          initial-interval: 1000
          max-attempts: 3
          multiplier: 2.0
          max-interval: 10000
        default-requeue-rejected: false  # Enviar a DLQ si falla
```

---

## 7. Excepciones en Feature Específico

✅ **Hacer**: Excepciones específicas del dominio cuando añaden valor

```java
// pedido/exception/PedidoException.java
public abstract class PedidoException extends BusinessException {
    protected PedidoException(String message, String errorCode, HttpStatus httpStatus) {
        super(message, errorCode, httpStatus);
    }
}

// pedido/exception/PedidoNoModificableException.java
public class PedidoNoModificableException extends PedidoException {

    public PedidoNoModificableException(Long pedidoId, EstadoPedido estadoActual) {
        super(
            String.format("Pedido %d no puede modificarse en estado %s", pedidoId, estadoActual),
            "PEDIDO_NO_MODIFICABLE",
            HttpStatus.CONFLICT
        );
    }
}

// pedido/exception/StockInsuficienteException.java
public class StockInsuficienteException extends PedidoException {

    private final String productoId;
    private final int cantidadSolicitada;
    private final int stockDisponible;

    public StockInsuficienteException(String productoId, int solicitada, int disponible) {
        super(
            String.format("Stock insuficiente para producto %s: solicitado=%d, disponible=%d",
                    productoId, solicitada, disponible),
            "STOCK_INSUFICIENTE",
            HttpStatus.CONFLICT
        );
        this.productoId = productoId;
        this.cantidadSolicitada = solicitada;
        this.stockDisponible = disponible;
    }

    // Getters para incluir en la respuesta si es necesario
}
```

---

## 8. Checklist de Implementación

| Aspecto | ✅ Correcto | ❌ Incorrecto |
|---------|------------|---------------|
| Base exception | `BusinessException` con código y status | `RuntimeException` genérico |
| 404 errors | `NotFoundException("Cliente", id)` | `throw new Exception("not found")` |
| Handler global | Extiende `ResponseEntityExceptionHandler` | Handler sin base class |
| Formato respuesta | RFC 7807 ProblemDetail | JSON custom inconsistente |
| Log 4xx | WARN sin stack trace | ERROR con stack trace |
| Log 5xx | ERROR con stack trace completo | WARN o sin loggear |
| Servicios externos | Traducir error + loggear original | Propagar error crudo |
| RabbitMQ errors | DLQ + retry configurado | Perder mensajes |
| Info sensible | No loggear passwords/tokens | Loggear todo |

---

## Estructura de Archivos

```
shared/
├── exception/
│   ├── BusinessException.java           ← Base abstracta
│   ├── NotFoundException.java           ← 404
│   ├── ValidationException.java         ← 400
│   ├── ConflictException.java           ← 409
│   ├── ForbiddenException.java          ← 403
│   ├── ExternalServiceException.java    ← Servicios externos
│   └── GlobalExceptionHandler.java      ← @ControllerAdvice
├── client/
│   └── BaseRestClient.java              ← Cliente HTTP base
└── config/
    └── RabbitMQConfig.java              ← Config con DLQ

pedido/
└── exception/
    ├── PedidoException.java             ← Base del feature
    ├── PedidoNoModificableException.java
    └── StockInsuficienteException.java
```

---

## Próximos Pasos

Este documento cubre el **Área 3: Manejo de Excepciones Enterprise-Grade**.

Cuando estés listo, continuamos con el **Área 4: Testing Estrategia Completa** que incluirá:
- JUnit 5 best practices
- Mockito patterns
- @SpringBootTest vs slices
- Testcontainers con PostgreSQL
- Testing de APIs REST
- Characterization tests para legacy

---

*Documento generado con Context7 - Documentación oficial Spring Framework 6.2 y Spring Boot 3.4.x*
