# Área 8: APIs REST Best Practices

## Referencia Rápida (Senior Developers)

### HTTP Status Codes por Operación

| Operación | Éxito | Error Cliente | Error Servidor |
|-----------|-------|---------------|----------------|
| GET (encontrado) | 200 OK | 400 Bad Request | 500 Internal Server Error |
| GET (no encontrado) | 404 Not Found | - | - |
| POST (creado) | 201 Created | 400/422 Validation | 500 Internal Server Error |
| PUT (actualizado) | 200 OK | 400/404 | 500 Internal Server Error |
| PUT (creado) | 201 Created | - | - |
| PATCH (actualizado) | 200 OK | 400/404 | 500 Internal Server Error |
| DELETE (eliminado) | 204 No Content | 404 Not Found | 500 Internal Server Error |
| DELETE (con body) | 200 OK | - | - |

### Códigos de Error Comunes

| Código | Significado | Cuándo Usar |
|--------|-------------|-------------|
| 400 | Bad Request | Sintaxis inválida, JSON malformado |
| 401 | Unauthorized | Sin autenticación |
| 403 | Forbidden | Autenticado pero sin permisos |
| 404 | Not Found | Recurso no existe |
| 409 | Conflict | Conflicto de estado (duplicado, concurrencia) |
| 422 | Unprocessable Entity | Validación de negocio fallida |
| 429 | Too Many Requests | Rate limiting |

### Versionado de API

```java
// Opción 1: Path versioning (RECOMENDADO para APIs públicas)
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerV1Controller { }

@RestController
@RequestMapping("/api/v2/customers")
public class CustomerV2Controller { }

// Opción 2: Header versioning (APIs internas)
@GetMapping(value = "/customers", headers = "X-API-Version=1")
public ResponseEntity<CustomerV1Dto> getCustomerV1() { }

// Opción 3: Accept header (Content Negotiation)
@GetMapping(value = "/customers", produces = "application/vnd.acme.v1+json")
public ResponseEntity<CustomerV1Dto> getCustomerV1() { }
```

### Estructura de Respuesta Estándar

```java
// Para recursos individuales
public record ApiResponse<T>(
    T data,
    ApiMetadata meta
) {}

public record ApiMetadata(
    Instant timestamp,
    String requestId
) {}

// Para colecciones paginadas
public record PagedResponse<T>(
    List<T> data,
    PageInfo pagination,
    ApiMetadata meta
) {}

public record PageInfo(
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext,
    boolean hasPrevious
) {}
```

### Validación Quick Reference

```java
// Request DTO con validaciones
public record CreateCustomerRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100)
    String name,

    @NotNull @Email
    String email,

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone format")
    String phone,

    @Valid  // Validación en cascada
    AddressRequest address
) {}

// Controller con validación
@PostMapping
public ResponseEntity<CustomerResponse> create(
    @Valid @RequestBody CreateCustomerRequest request
) { }
```

---

## Guía Detallada (Junior/Mid Developers)

### 1. Diseño de URLs RESTful

#### Principios Fundamentales

```
# Usar sustantivos plurales para recursos
GET    /api/v1/customers           # Lista de clientes
GET    /api/v1/customers/{id}      # Cliente específico
POST   /api/v1/customers           # Crear cliente
PUT    /api/v1/customers/{id}      # Actualizar cliente completo
PATCH  /api/v1/customers/{id}      # Actualizar parcialmente
DELETE /api/v1/customers/{id}      # Eliminar cliente

# Recursos anidados (relaciones)
GET    /api/v1/customers/{id}/orders         # Pedidos de un cliente
POST   /api/v1/customers/{id}/orders         # Crear pedido para cliente
GET    /api/v1/customers/{id}/orders/{orderId}

# Acciones que no son CRUD (usar verbos como excepción)
POST   /api/v1/orders/{id}/cancel            # Cancelar pedido
POST   /api/v1/customers/{id}/verify-email   # Verificar email
```

#### Controller Completo con Best Practices

```java
package com.acme.sales.adapter.in.web;

import com.acme.sales.adapter.in.web.dto.*;
import com.acme.sales.application.port.in.CustomerUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerUseCase customerUseCase;

    /**
     * Lista paginada de clientes con filtros opcionales.
     *
     * GET /api/v1/customers?page=0&size=20&sort=name,asc&status=ACTIVE
     */
    @GetMapping
    public ResponseEntity<PagedResponse<CustomerSummaryDto>> findAll(
            @RequestParam(required = false) CustomerStatus status,
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        Page<CustomerSummaryDto> page = customerUseCase.findAll(status, pageable);
        return ResponseEntity.ok(PagedResponse.from(page));
    }

    /**
     * Obtiene un cliente por ID.
     *
     * GET /api/v1/customers/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDetailDto>> findById(
            @PathVariable Long id
    ) {
        CustomerDetailDto customer = customerUseCase.findById(id);
        return ResponseEntity.ok(ApiResponse.of(customer));
    }

    /**
     * Crea un nuevo cliente.
     *
     * POST /api/v1/customers
     * Returns: 201 Created + Location header
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerDetailDto>> create(
            @Valid @RequestBody CreateCustomerRequest request
    ) {
        CustomerDetailDto created = customerUseCase.create(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(ApiResponse.of(created));
    }

    /**
     * Actualiza un cliente existente (reemplazo completo).
     *
     * PUT /api/v1/customers/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDetailDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request
    ) {
        CustomerDetailDto updated = customerUseCase.update(id, request);
        return ResponseEntity.ok(ApiResponse.of(updated));
    }

    /**
     * Actualización parcial de un cliente.
     *
     * PATCH /api/v1/customers/{id}
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDetailDto>> partialUpdate(
            @PathVariable Long id,
            @Valid @RequestBody PatchCustomerRequest request
    ) {
        CustomerDetailDto updated = customerUseCase.partialUpdate(id, request);
        return ResponseEntity.ok(ApiResponse.of(updated));
    }

    /**
     * Elimina un cliente.
     *
     * DELETE /api/v1/customers/{id}
     * Returns: 204 No Content
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        customerUseCase.delete(id);
    }

    /**
     * Acción específica: verificar email del cliente.
     *
     * POST /api/v1/customers/{id}/verify-email
     */
    @PostMapping("/{id}/verify-email")
    public ResponseEntity<ApiResponse<VerificationResultDto>> verifyEmail(
            @PathVariable Long id,
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        VerificationResultDto result = customerUseCase.verifyEmail(id, request);
        return ResponseEntity.ok(ApiResponse.of(result));
    }
}
```

### 2. Versionado de APIs

#### Estrategia Recomendada: Path Versioning

```java
// Estructura de paquetes por versión
com.acme.sales.adapter.in.web.v1/
    CustomerV1Controller.java
    dto/
        CustomerV1Dto.java
com.acme.sales.adapter.in.web.v2/
    CustomerV2Controller.java
    dto/
        CustomerV2Dto.java

// V1 Controller - versión original
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerV1Controller {

    @GetMapping("/{id}")
    public ResponseEntity<CustomerV1Dto> findById(@PathVariable Long id) {
        // Retorna estructura V1
        return ResponseEntity.ok(customerMapper.toV1Dto(customer));
    }
}

// V2 Controller - nueva versión con campos adicionales
@RestController
@RequestMapping("/api/v2/customers")
public class CustomerV2Controller {

    @GetMapping("/{id}")
    public ResponseEntity<CustomerV2Dto> findById(@PathVariable Long id) {
        // Retorna estructura V2 con campos adicionales
        return ResponseEntity.ok(customerMapper.toV2Dto(customer));
    }
}
```

#### Deprecación Gradual de Versiones

```java
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerV1Controller {

    /**
     * @deprecated Use /api/v2/customers instead. Will be removed in 2025-06.
     */
    @Deprecated(since = "2024-01", forRemoval = true)
    @GetMapping("/{id}")
    public ResponseEntity<CustomerV1Dto> findById(@PathVariable Long id) {
        // Añadir header de deprecación
        return ResponseEntity.ok()
                .header("Deprecation", "true")
                .header("Sunset", "Sat, 01 Jun 2025 00:00:00 GMT")
                .header("Link", "</api/v2/customers>; rel=\"successor-version\"")
                .body(customerMapper.toV1Dto(customer));
    }
}
```

### 3. DTOs vs Entities

#### Separación Obligatoria

```java
// Entity (JPA) - NUNCA exponer directamente
@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String passwordHash;  // Sensible - nunca exponer
    private String taxId;         // PII - controlar exposición

    @OneToMany(mappedBy = "customer")
    private List<Order> orders;   // Puede causar N+1 queries

    @Version
    private Long version;         // Interno

    private Instant createdAt;
    private Instant updatedAt;
}

// Request DTO - para entrada
public record CreateCustomerRequest(
    @NotBlank @Size(max = 100)
    String name,

    @NotNull @Email
    String email,

    @NotBlank @Size(min = 8)
    String password,  // Texto plano, se hashea en el service

    @Pattern(regexp = "^[A-Z0-9]{8,12}$")
    String taxId
) {}

// Response DTO - para salida (sin datos sensibles)
public record CustomerDetailDto(
    Long id,
    String name,
    String email,
    String maskedTaxId,  // "XXXX1234" - parcialmente oculto
    CustomerStatus status,
    Instant createdAt
) {}

// Summary DTO - para listados (datos mínimos)
public record CustomerSummaryDto(
    Long id,
    String name,
    String email,
    CustomerStatus status
) {}
```

#### Mapper con MapStruct

```java
@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    Customer toEntity(CreateCustomerRequest request);

    @Mapping(target = "maskedTaxId", expression = "java(maskTaxId(customer.getTaxId()))")
    CustomerDetailDto toDetailDto(Customer customer);

    CustomerSummaryDto toSummaryDto(Customer customer);

    List<CustomerSummaryDto> toSummaryDtoList(List<Customer> customers);

    default String maskTaxId(String taxId) {
        if (taxId == null || taxId.length() < 4) return "****";
        return "****" + taxId.substring(taxId.length() - 4);
    }
}
```

### 4. Validación Completa

#### Anotaciones de Validación Estándar

```java
public record CreateOrderRequest(
    // Validaciones básicas
    @NotNull(message = "Customer ID is required")
    Long customerId,

    @NotEmpty(message = "Order must have at least one item")
    @Size(max = 100, message = "Maximum 100 items per order")
    List<@Valid OrderItemRequest> items,

    // Validaciones de formato
    @Pattern(regexp = "^[A-Z]{2}$", message = "Invalid country code")
    String countryCode,

    // Validaciones numéricas
    @Positive(message = "Discount must be positive")
    @Max(value = 50, message = "Maximum discount is 50%")
    Integer discountPercent,

    // Validaciones de fecha
    @Future(message = "Delivery date must be in the future")
    LocalDate requestedDeliveryDate,

    // Validación custom
    @ValidCouponCode  // Anotación personalizada
    String couponCode
) {}

public record OrderItemRequest(
    @NotNull
    Long productId,

    @Positive
    @Max(999)
    Integer quantity,

    @PositiveOrZero
    @Digits(integer = 10, fraction = 2)
    BigDecimal unitPrice
) {}
```

#### Custom Validator

```java
// Anotación personalizada
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CouponCodeValidator.class)
@Documented
public @interface ValidCouponCode {
    String message() default "Invalid or expired coupon code";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// Implementación del validador
@Component
@RequiredArgsConstructor
public class CouponCodeValidator implements ConstraintValidator<ValidCouponCode, String> {

    private final CouponRepository couponRepository;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;  // @NotBlank se encarga si es requerido
        }

        return couponRepository.findByCodeAndExpiresAtAfter(value, Instant.now())
                .isPresent();
    }
}
```

#### Validación a Nivel de Clase (Cross-Field)

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
public @interface ValidDateRange {
    String message() default "End date must be after start date";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String startField();
    String endField();
}

@ValidDateRange(startField = "startDate", endField = "endDate")
public record ReportRequest(
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    ReportType type
) {}

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, Object> {

    private String startField;
    private String endField;

    @Override
    public void initialize(ValidDateRange annotation) {
        this.startField = annotation.startField();
        this.endField = annotation.endField();
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        try {
            BeanWrapper wrapper = new BeanWrapperImpl(obj);
            LocalDate start = (LocalDate) wrapper.getPropertyValue(startField);
            LocalDate end = (LocalDate) wrapper.getPropertyValue(endField);

            if (start == null || end == null) {
                return true;  // @NotNull se encarga
            }

            return !end.isBefore(start);
        } catch (Exception e) {
            return false;
        }
    }
}
```

#### Grupos de Validación

```java
// Definición de grupos
public interface OnCreate {}
public interface OnUpdate {}

public record CustomerRequest(
    @Null(groups = OnCreate.class, message = "ID must not be provided for creation")
    @NotNull(groups = OnUpdate.class, message = "ID is required for update")
    Long id,

    @NotBlank(groups = {OnCreate.class, OnUpdate.class})
    String name,

    @NotNull(groups = OnCreate.class)
    @Email
    String email  // Requerido solo en creación
) {}

// Uso en controller
@PostMapping
public ResponseEntity<?> create(
    @Validated(OnCreate.class) @RequestBody CustomerRequest request
) { }

@PutMapping("/{id}")
public ResponseEntity<?> update(
    @PathVariable Long id,
    @Validated(OnUpdate.class) @RequestBody CustomerRequest request
) { }
```

### 5. Paginación y Sorting

#### Configuración de Paginación

```yaml
# application.yml
spring:
  data:
    web:
      pageable:
        default-page-size: 20
        max-page-size: 100
        one-indexed-parameters: false  # page empieza en 0
        page-parameter: page
        size-parameter: size
      sort:
        sort-parameter: sort
```

#### Controller con Paginación

```java
@GetMapping
public ResponseEntity<PagedResponse<CustomerSummaryDto>> findAll(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) CustomerStatus status,
        @PageableDefault(
            size = 20,
            sort = "createdAt",
            direction = Sort.Direction.DESC
        ) Pageable pageable
) {
    // Limitar el tamaño máximo de página
    Pageable safePageable = PageRequest.of(
        pageable.getPageNumber(),
        Math.min(pageable.getPageSize(), 100),
        pageable.getSort()
    );

    Page<CustomerSummaryDto> page = customerUseCase.search(search, status, safePageable);
    return ResponseEntity.ok(PagedResponse.from(page));
}
```

#### Wrapper de Respuesta Paginada

```java
public record PagedResponse<T>(
    List<T> data,
    PageInfo pagination,
    ApiMetadata meta
) {
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
            page.getContent(),
            new PageInfo(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
            ),
            new ApiMetadata(Instant.now(), MDC.get("requestId"))
        );
    }
}

public record PageInfo(
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext,
    boolean hasPrevious
) {}
```

#### Ejemplo de Respuesta Paginada

```json
{
  "data": [
    {"id": 1, "name": "Acme Corp", "email": "contact@acme.com", "status": "ACTIVE"},
    {"id": 2, "name": "Beta Inc", "email": "info@beta.com", "status": "ACTIVE"}
  ],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 156,
    "totalPages": 8,
    "hasNext": true,
    "hasPrevious": false
  },
  "meta": {
    "timestamp": "2024-01-15T10:30:00Z",
    "requestId": "abc-123-def"
  }
}
```

### 6. Respuestas de Error Estandarizadas (RFC 7807)

> ⚠️ **Nota**: Spring Boot 3.x incluye `org.springframework.http.ProblemDetail` de forma nativa. La implementación custom mostrada abajo es útil si necesitas campos adicionales, pero considera usar la clase nativa del framework activando `spring.mvc.problemdetails.enabled=true` en tu `application.yml`.

```java
// Estructura de error según RFC 7807 Problem Details
public record ProblemDetail(
    URI type,           // URI que identifica el tipo de error
    String title,       // Título legible del error
    int status,         // HTTP status code
    String detail,      // Descripción específica de este error
    URI instance,       // URI de la instancia del error (request)
    Instant timestamp,
    String requestId,
    List<FieldError> errors  // Errores de validación
) {
    public record FieldError(
        String field,
        String message,
        Object rejectedValue
    ) {}
}

// Global Exception Handler
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        List<ProblemDetail.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ProblemDetail.FieldError(
                    error.getField(),
                    error.getDefaultMessage(),
                    error.getRejectedValue()
                ))
                .toList();

        ProblemDetail problem = new ProblemDetail(
            URI.create("https://api.acme.com/errors/validation-failed"),
            "Validation Failed",
            HttpStatus.BAD_REQUEST.value(),
            "One or more fields have validation errors",
            URI.create(((ServletWebRequest) request).getRequest().getRequestURI()),
            Instant.now(),
            MDC.get("requestId"),
            fieldErrors
        );

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            ResourceNotFoundException ex,
            WebRequest request
    ) {
        ProblemDetail problem = new ProblemDetail(
            URI.create("https://api.acme.com/errors/resource-not-found"),
            "Resource Not Found",
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            URI.create(((ServletWebRequest) request).getRequest().getRequestURI()),
            Instant.now(),
            MDC.get("requestId"),
            null
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }
}
```

#### Ejemplo de Respuesta de Error

```json
{
  "type": "https://api.acme.com/errors/validation-failed",
  "title": "Validation Failed",
  "status": 400,
  "detail": "One or more fields have validation errors",
  "instance": "/api/v1/customers",
  "timestamp": "2024-01-15T10:30:00Z",
  "requestId": "abc-123-def",
  "errors": [
    {
      "field": "email",
      "message": "must be a valid email address",
      "rejectedValue": "invalid-email"
    },
    {
      "field": "name",
      "message": "must not be blank",
      "rejectedValue": ""
    }
  ]
}
```

### 7. HATEOAS (Hypermedia)

#### Cuándo Usar HATEOAS

| Escenario | HATEOAS | Sin HATEOAS |
|-----------|---------|-------------|
| API pública con clientes desconocidos | Considerar | - |
| API interna entre microservicios | - | Preferido |
| API con flujos de estado complejos | Recomendado | - |
| API simple CRUD | - | Preferido |

#### Implementación Básica con Spring HATEOAS

```java
// Dependencia
// spring-boot-starter-hateoas

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderAssembler orderAssembler;

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<OrderDto>> findById(@PathVariable Long id) {
        OrderDto order = orderService.findById(id);
        EntityModel<OrderDto> model = orderAssembler.toModel(order);
        return ResponseEntity.ok(model);
    }
}

@Component
public class OrderAssembler implements RepresentationModelAssembler<OrderDto, EntityModel<OrderDto>> {

    @Override
    public EntityModel<OrderDto> toModel(OrderDto order) {
        EntityModel<OrderDto> model = EntityModel.of(order);

        // Link a sí mismo
        model.add(linkTo(methodOn(OrderController.class).findById(order.id()))
                .withSelfRel());

        // Link al cliente
        model.add(linkTo(methodOn(CustomerController.class).findById(order.customerId()))
                .withRel("customer"));

        // Links condicionales según estado
        if (order.status() == OrderStatus.PENDING) {
            model.add(linkTo(methodOn(OrderController.class).confirm(order.id()))
                    .withRel("confirm"));
            model.add(linkTo(methodOn(OrderController.class).cancel(order.id()))
                    .withRel("cancel"));
        }

        if (order.status() == OrderStatus.CONFIRMED) {
            model.add(linkTo(methodOn(OrderController.class).ship(order.id()))
                    .withRel("ship"));
        }

        return model;
    }
}
```

#### Respuesta con HATEOAS

```json
{
  "id": 123,
  "customerId": 456,
  "status": "PENDING",
  "totalAmount": 150.00,
  "_links": {
    "self": {
      "href": "https://api.acme.com/api/v1/orders/123"
    },
    "customer": {
      "href": "https://api.acme.com/api/v1/customers/456"
    },
    "confirm": {
      "href": "https://api.acme.com/api/v1/orders/123/confirm"
    },
    "cancel": {
      "href": "https://api.acme.com/api/v1/orders/123/cancel"
    }
  }
}
```

### 8. Configuración de Jackson

```java
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder
            // Formato de fechas ISO 8601
            .serializers(new JavaTimeModule())
            .featuresToDisable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
            )
            .featuresToEnable(
                DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES
            )
            // Excluir campos nulos
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            // Naming strategy
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }
}
```

```yaml
# application.yml - alternativa via properties
spring:
  jackson:
    serialization:
      write-dates-as-timestamps: false
      indent-output: false
    deserialization:
      fail-on-unknown-properties: false
      fail-on-null-for-primitives: true
    default-property-inclusion: non_null
    property-naming-strategy: SNAKE_CASE
    date-format: "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
```

### 9. Request/Response Interceptors

#### Logging de Requests

```java
@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Generar Request ID
        String requestId = Optional.ofNullable(request.getHeader("X-Request-ID"))
                .orElse(UUID.randomUUID().toString());
        MDC.put("requestId", requestId);

        long startTime = System.currentTimeMillis();

        try {
            // Agregar Request ID a la respuesta
            response.setHeader("X-Request-ID", requestId);

            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            log.info("HTTP {} {} - {} - {}ms",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration
            );

            MDC.clear();
        }
    }
}
```

#### Rate Limiting Header

```java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter rateLimiter;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {

        String clientId = extractClientId(request);
        RateLimitInfo info = rateLimiter.checkLimit(clientId);

        // Headers estándar de rate limiting
        response.setHeader("X-RateLimit-Limit", String.valueOf(info.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(info.remaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(info.resetAt()));

        if (!info.allowed()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(info.retryAfterSeconds()));
            return false;
        }

        return true;
    }
}
```

---

## ✅ Hacer

1. **Usar HTTP status codes correctos** - 201 para creación, 204 para delete sin body
2. **Separar DTOs de Entities** - Nunca exponer entidades JPA directamente
3. **Validar TODOS los inputs** - Usar `@Valid` + anotaciones de Bean Validation
4. **Versionar APIs desde el inicio** - Path versioning para APIs públicas
5. **Paginar colecciones** - Nunca retornar listas completas sin límite
6. **Documentar con OpenAPI** - Swagger/SpringDoc para documentación automática
7. **Usar records para DTOs** - Inmutabilidad y código más limpio
8. **Incluir Request ID** - Para trazabilidad de errores
9. **Respuestas de error consistentes** - Seguir RFC 7807 Problem Details
10. **Nombres de recursos en plural** - `/customers`, no `/customer`

## ❌ Evitar

1. **Exponer entidades JPA** - Riesgo de exposición de datos sensibles y N+1
2. **Verbos en URLs** - `/getCustomer` → `/customers/{id}`
3. **Ignorar validación** - Confiar en validación del frontend
4. **200 para todo** - Usar status codes apropiados
5. **Paginación sin límite máximo** - Permitir `size=999999`
6. **Retornar stack traces** - En producción nunca mostrar errores internos
7. **PUT para updates parciales** - Usar PATCH para actualizaciones parciales
8. **Respuestas inconsistentes** - Diferentes estructuras en diferentes endpoints
9. **URLs con acciones excesivas** - `/customers/create`, `/orders/deleteOrder`
10. **Versiones sin deprecation plan** - Mantener versiones indefinidamente

---

## Checklist de Implementación

### Estructura del Controller
- [ ] Ruta base con versión: `/api/v1/{recurso}`
- [ ] Métodos HTTP correctos (GET, POST, PUT, PATCH, DELETE)
- [ ] `@Valid` en todos los `@RequestBody`
- [ ] `@PathVariable` con validación de tipo
- [ ] `@PageableDefault` para endpoints de listado
- [ ] `ResponseEntity` con status apropiados

### DTOs
- [ ] Records para request/response DTOs
- [ ] Validaciones con Jakarta Bean Validation
- [ ] Separación de Create/Update/Detail/Summary DTOs
- [ ] MapStruct para conversión Entity ↔ DTO
- [ ] Sin campos sensibles en responses

### Respuestas
- [ ] Wrapper estándar (`ApiResponse<T>`)
- [ ] Paginación con metadatos (`PagedResponse<T>`)
- [ ] Errores con Problem Details (RFC 7807)
- [ ] Headers: `X-Request-ID`, Content-Type
- [ ] Location header en 201 Created

### Validación
- [ ] `@NotNull`, `@NotBlank`, `@Size` en campos requeridos
- [ ] `@Email`, `@Pattern` para formatos
- [ ] Custom validators para reglas de negocio
- [ ] Grupos de validación si es necesario
- [ ] Mensajes de error descriptivos

### Seguridad
- [ ] Rate limiting configurado
- [ ] CORS configurado correctamente
- [ ] Sin exposición de datos sensibles
- [ ] Validación de ownership en recursos protegidos
