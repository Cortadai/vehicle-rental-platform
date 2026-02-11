# Área 15: Documentación de APIs - SpringDoc OpenAPI

> **Audiencia**: Desarrolladores junior/mid (guía detallada) + Seniors (referencia rápida)

> **Stack**: Spring Boot 3.4.x, Java 17+, SpringDoc OpenAPI 2.x, Swagger UI

## Referencia Rápida (Senior Developers)

### Dependencia Maven

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

### Configuración Mínima

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
    enabled: true
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    tags-sorter: alpha
    operations-sorter: method
  show-actuator: false
  packages-to-scan: com.acme.sales.adapter.in.web
  default-produces-media-type: application/json
```

### Configuración Global

```java
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "ACME Sales API",
        version = "1.0.0",
        description = "API REST para gestión de ventas ACME",
        contact = @Contact(name = "Equipo Backend", email = "backend@acme.com"),
        license = @License(name = "Proprietary")
    ),
    servers = {
        @Server(url = "https://api.acme.com", description = "Producción"),
        @Server(url = "https://staging-api.acme.com", description = "Staging"),
        @Server(url = "http://localhost:8080", description = "Local")
    },
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Token JWT de autenticación"
)
public class OpenApiConfig {}
```

### Documentar Controller

```java
@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Clientes", description = "Gestión de clientes")
public class CustomerController {

    @Operation(summary = "Obtener cliente por ID",
        description = "Retorna los detalles completos de un cliente")
    @ApiResponse(responseCode = "200", description = "Cliente encontrado",
        content = @Content(schema = @Schema(implementation = CustomerDetailDto.class)))
    @ApiResponse(responseCode = "404", description = "Cliente no encontrado",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDetailDto> findById(
        @Parameter(description = "ID del cliente", required = true, example = "123")
        @PathVariable Long id
    ) { }
}
```

### Documentar DTO

```java
@Schema(description = "Datos para crear un cliente")
public record CreateCustomerRequest(
    @Schema(description = "Nombre completo", example = "Juan García", minLength = 2, maxLength = 100)
    @NotBlank String name,

    @Schema(description = "Email corporativo", example = "juan@acme.com", format = "email")
    @NotNull @Email String email,

    @Schema(description = "Teléfono con código de país", example = "+34612345678",
            pattern = "^\\+?[1-9]\\d{1,14}$")
    String phone
) {}
```

### Groups y Producción

```yaml
springdoc:
  group-configs:
    - group: public-api
      paths-to-match: /api/v1/**
      packages-to-scan: com.acme.sales.adapter.in.web
    - group: internal-api
      paths-to-match: /api/internal/**
    - group: admin-api
      paths-to-match: /api/v1/admin/**
```

```yaml
# application-prod.yml
springdoc:
  swagger-ui:
    enabled: false
  api-docs:
    enabled: false
```

---

## Guía Detallada (Junior/Mid Developers)

### 1. Dependencias y Configuración Básica

#### ¿Qué es SpringDoc OpenAPI?

SpringDoc OpenAPI genera automáticamente documentación OpenAPI 3.0 a partir de tu código Spring Boot. Lee las anotaciones de Spring (`@RestController`, `@GetMapping`, `@RequestBody`, etc.) y produce una especificación OpenAPI completa, que luego se visualiza en Swagger UI.

| Característica | Descripción |
|----------------|-------------|
| Auto-descubrimiento | Lee anotaciones de Spring y genera la spec automáticamente |
| Swagger UI integrado | Interfaz visual para probar los endpoints |
| Soporte Spring Boot 3.x | Compatible con Jakarta namespace y WebMVC/WebFlux |
| Personalizable | Anotaciones y beans para controlar cada detalle |

```xml
<!-- Para Spring MVC (la mayoría de proyectos) -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

> **Nota**: Para proyectos WebFlux usar `springdoc-openapi-starter-webflux-ui`.

#### Configuración en application.yml

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
    enabled: true
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    tags-sorter: alpha
    operations-sorter: method
    display-request-duration: true
    filter: true
    try-it-out-enabled: true
  show-actuator: false
  packages-to-scan: com.acme.sales.adapter.in.web
  default-produces-media-type: application/json
```

#### Acceso a la Documentación

| URL | Descripción |
|-----|-------------|
| `http://localhost:8080/swagger-ui.html` | Interfaz visual de Swagger UI |
| `http://localhost:8080/v3/api-docs` | Spec OpenAPI en formato JSON |
| `http://localhost:8080/v3/api-docs.yaml` | Spec OpenAPI en formato YAML |

Si usas Spring Security, permite el acceso a los endpoints de documentación:

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
        .anyRequest().authenticated()
    );
    return http.build();
}
```

### 2. Configuración Global con @OpenAPIDefinition

```java
package com.acme.sales.infrastructure.config;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "ACME Sales API",
        version = "1.0.0",
        description = """
            API REST para la gestión de ventas de ACME.
            ## Autenticación
            Todas las peticiones requieren un token JWT válido en el header Authorization.
            ## Rate Limiting
            - 100 requests/minuto para endpoints públicos
            - 1000 requests/minuto para endpoints autenticados
            """,
        contact = @Contact(name = "Equipo Backend", email = "backend@acme.com",
            url = "https://wiki.acme.com/backend"),
        license = @License(name = "Proprietary", url = "https://acme.com/license")
    ),
    servers = {
        @Server(url = "https://api.acme.com", description = "Producción"),
        @Server(url = "https://staging-api.acme.com", description = "Staging"),
        @Server(url = "http://localhost:8080", description = "Local")
    },
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Token JWT de autenticación. Obtener en POST /api/v1/auth/login"
)
public class OpenApiConfig {}
```

| Anotación | Propósito |
|-----------|-----------|
| `@OpenAPIDefinition` | Define metadatos globales de la API |
| `@Info` | Título, versión, descripción, contacto y licencia |
| `@Server` | URLs de los entornos donde se despliega la API |
| `@SecurityRequirement` | Esquema de seguridad requerido globalmente |
| `@SecurityScheme` | Define cómo funciona la autenticación (JWT, API Key, OAuth2) |

### 3. Documentando Controllers

```java
@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Clientes", description = "Operaciones CRUD para gestión de clientes")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerUseCase customerUseCase;

    @Operation(summary = "Obtener cliente por ID",
        description = "Retorna los detalles completos de un cliente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente encontrado",
            content = @Content(schema = @Schema(implementation = CustomerDetailDto.class))),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDetailDto> findById(
        @Parameter(description = "ID del cliente", required = true, example = "123")
        @PathVariable Long id
    ) {
        return ResponseEntity.ok(customerUseCase.findById(id));
    }

    @Operation(summary = "Crear cliente", description = "Registra un nuevo cliente en el sistema")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Cliente creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "409", description = "Email ya registrado")
    })
    @PostMapping
    public ResponseEntity<CustomerDetailDto> create(@Valid @RequestBody CreateCustomerRequest request) {
        CustomerDetailDto created = customerUseCase.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Eliminar cliente", description = "Soft delete de un cliente")
    @ApiResponse(responseCode = "204", description = "Cliente eliminado")
    @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        customerUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

| Anotación | Nivel | Propósito |
|-----------|-------|-----------|
| `@Tag` | Clase | Agrupa endpoints bajo un nombre en Swagger UI |
| `@Operation` | Método | Describe un endpoint (summary, description) |
| `@ApiResponse` | Método | Documenta cada código de respuesta posible |
| `@Parameter` | Parámetro | Describe path/query/header params |
| `@Content` / `@Schema` | Dentro de ApiResponse | Define el schema de la respuesta |

Para ocultar endpoints: `@Operation(hidden = true)`.

### 4. Documentando DTOs con @Schema

```java
@Schema(description = "Datos para crear un cliente")
public record CreateCustomerRequest(
    @Schema(description = "Nombre completo", example = "Juan García López",
        minLength = 2, maxLength = 100)
    @NotBlank @Size(min = 2, max = 100) String name,

    @Schema(description = "Email corporativo", example = "juan.garcia@acme.com", format = "email")
    @NotNull @Email String email,

    @Schema(description = "Teléfono con código de país", example = "+34612345678",
        pattern = "^\\+?[1-9]\\d{1,14}$", nullable = true)
    String phone,

    @Schema(description = "Tipo de cliente", example = "BUSINESS", defaultValue = "INDIVIDUAL",
        allowableValues = {"INDIVIDUAL", "BUSINESS", "ENTERPRISE"})
    CustomerType type
) {}

@Schema(description = "Detalle completo de un cliente")
public record CustomerDetailDto(
    @Schema(description = "ID único", example = "123") Long id,
    @Schema(description = "Nombre completo", example = "Juan García López") String name,
    @Schema(description = "Email", example = "juan.garcia@acme.com") String email,
    @Schema(description = "Estado del cliente", example = "ACTIVE") CustomerStatus status,
    @Schema(description = "Fecha de registro", example = "2024-01-15T10:30:00Z") Instant createdAt,
    @Schema(description = "Importe total gastado en EUR", example = "15430.50") BigDecimal totalSpent
) {}
```

#### Enums y Campos Especiales

```java
@Schema(description = "Estado del cliente en el sistema", enumAsRef = true)
public enum CustomerStatus {
    @Schema(description = "Cliente activo") ACTIVE,
    @Schema(description = "Cliente inactivo") INACTIVE,
    @Schema(description = "Cuenta suspendida") SUSPENDED
}

// Campos de solo lectura y ocultos
@Schema(description = "ID del pedido", accessMode = Schema.AccessMode.READ_ONLY) Long id;
@Schema(description = "Contraseña interna", hidden = true) String internalSecret;
```

### 5. Groups (Agrupación de APIs)

Los groups permiten dividir la documentación cuando tu aplicación tiene múltiples conjuntos de APIs. Cada grupo genera su propia spec y aparece como selector desplegable en Swagger UI.

```yaml
springdoc:
  group-configs:
    - group: public-api
      display-name: "API Pública v1"
      paths-to-match: /api/v1/**
      paths-to-exclude: /api/v1/admin/**
      packages-to-scan: com.acme.sales.adapter.in.web
    - group: internal-api
      display-name: "API Interna"
      paths-to-match: /api/internal/**
    - group: admin-api
      display-name: "API de Administración"
      paths-to-match: /api/v1/admin/**
```

Configuración programática equivalente con `GroupedOpenApi.builder()`:

```java
@Bean
public GroupedOpenApi publicApi() {
    return GroupedOpenApi.builder()
        .group("public-api")
        .displayName("API Pública v1")
        .pathsToMatch("/api/v1/**")
        .pathsToExclude("/api/v1/admin/**")
        .packagesToScan("com.acme.sales.adapter.in.web")
        .build();
}
```

### 6. Customización Programática

```java
@Bean
public OpenApiCustomizer requestIdHeaderCustomizer() {
    return openApi -> {
        openApi.getPaths().values().forEach(pathItem ->
            pathItem.readOperations().forEach(operation -> {
                operation.addParametersItem(new Parameter()
                    .in("header")
                    .name("X-Request-ID")
                    .description("ID único de la petición para trazabilidad")
                    .schema(new StringSchema()
                        .example("550e8400-e29b-41d4-a716-446655440000"))
                    .required(false));
            })
        );
    };
}
```

`OperationCustomizer` permite personalizar operaciones individuales basándose en el `HandlerMethod`:

```java
@Bean
public OperationCustomizer operationCustomizer() {
    return (operation, handlerMethod) -> {
        PreAuthorize preAuth = handlerMethod.getMethodAnnotation(PreAuthorize.class);
        if (preAuth != null) {
            String desc = operation.getDescription() != null ? operation.getDescription() : "";
            operation.setDescription(desc +
                "\n\n**Autorización requerida**: `" + preAuth.value() + "`");
        }
        return operation;
    };
}
```

### 7. Seguridad en Swagger UI

Con la configuración de `@SecurityScheme`, Swagger UI muestra un botón "Authorize" para ingresar el token JWT.

```java
// Marcar un endpoint como público (sin autenticación requerida)
@Operation(summary = "Listar catálogo", security = {})
@GetMapping("/api/v1/catalog/products")
public ResponseEntity<List<ProductSummaryDto>> listCatalog() { }
```

Para OAuth2 completo, usar `@SecurityScheme(type = SecuritySchemeType.OAUTH2)` con `@OAuthFlows` y `@OAuthFlow` especificando `authorizationUrl`, `tokenUrl` y `scopes`.

#### Deshabilitar en Producción

```yaml
# application-prod.yml
springdoc:
  swagger-ui:
    enabled: false
  api-docs:
    enabled: false
```

```java
// Alternativa: solo activo cuando NO es producción
@Configuration
@Profile("!prod")
@OpenAPIDefinition(/* ... */)
public class OpenApiConfig {}
```

### 8. Generación de Clientes

```bash
# Exportar spec
curl http://localhost:8080/v3/api-docs > openapi.json

# Generar cliente TypeScript
openapi-generator-cli generate \
    -i openapi.json \
    -g typescript-axios \
    -o generated-client/ \
    --additional-properties=supportsES6=true,npmName=@acme/sales-api-client

# Generar cliente Java para otro microservicio
openapi-generator-cli generate \
    -i openapi.json \
    -g java \
    -o generated-java-client/ \
    --additional-properties=library=restclient,useJakartaEe=true
```

### 9. Validación de API Spec

#### Linting con Spectral en CI

```bash
npm install -g @stoplight/spectral-cli
spectral lint openapi.json --ruleset .spectral.yml
```

#### Contract-First vs Code-First

| Aspecto | Code-First (SpringDoc) | Contract-First |
|---------|------------------------|----------------|
| **Flujo** | Código -> spec generada | Spec -> código generado |
| **Ventaja** | Más rápido para equipos Java | Spec es el contrato oficial |
| **Desventaja** | La spec depende del código | Requiere mantener spec manualmente |
| **Ideal para** | APIs internas, microservicios | APIs públicas, equipos multi-lenguaje |
| **Herramienta** | SpringDoc + anotaciones | openapi-generator + spec YAML |

**Recomendación**: Para la mayoría de equipos, **Code-First con SpringDoc** es la mejor opción. Si necesitas compartir la spec como contrato oficial, expórtala y versionala en el repositorio.

Para exportar la spec automáticamente en build time, usar `springdoc-openapi-maven-plugin` (artifact `org.springdoc:springdoc-openapi-maven-plugin:1.4`) en la fase `integration-test`.

### 10. Ejemplo E-Commerce: Controller de Pedidos

```java
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Pedidos", description = "Gestión del ciclo de vida de pedidos")
@RequiredArgsConstructor
public class OrderController {

    private final OrderUseCase orderUseCase;

    @Operation(summary = "Crear pedido", description = """
        Crea un nuevo pedido a partir del carrito del cliente.
        El pedido se crea en estado PENDING y requiere confirmación de pago.""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Pedido creado"),
        @ApiResponse(responseCode = "400", description = "Carrito vacío o datos inválidos"),
        @ApiResponse(responseCode = "422", description = "Stock insuficiente")
    })
    @PostMapping
    public ResponseEntity<OrderDetailDto> create(@Valid @RequestBody CreateOrderRequest request) {
        OrderDetailDto order = orderUseCase.create(request);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + order.id())).body(order);
    }

    @Operation(summary = "Confirmar pedido",
        description = "Confirma el pago y pasa el pedido a estado CONFIRMED")
    @ApiResponse(responseCode = "200", description = "Pedido confirmado")
    @ApiResponse(responseCode = "409", description = "Pedido no está en estado PENDING")
    @PostMapping("/{id}/confirm")
    public ResponseEntity<OrderDetailDto> confirm(
        @Parameter(description = "ID del pedido") @PathVariable Long id) {
        return ResponseEntity.ok(orderUseCase.confirm(id));
    }
}

@Schema(description = "Solicitud para crear un pedido")
public record CreateOrderRequest(
    @Schema(description = "ID del cliente", example = "456") @NotNull Long customerId,
    @Schema(description = "Líneas del pedido") @NotEmpty @Valid List<OrderLineRequest> lines,
    @Schema(description = "Código de cupón", example = "VERANO2024", nullable = true) String couponCode
) {}

@Schema(description = "Línea individual de un pedido")
public record OrderLineRequest(
    @Schema(description = "ID del producto", example = "789") @NotNull Long productId,
    @Schema(description = "Cantidad", example = "2", minimum = "1", maximum = "999")
    @NotNull @Min(1) @Max(999) Integer quantity
) {}

@Schema(description = "Detalle completo de un pedido")
public record OrderDetailDto(
    @Schema(description = "ID del pedido", example = "1001") Long id,
    @Schema(description = "Estado actual", example = "PENDING") OrderStatus status,
    @Schema(description = "Total del pedido", example = "225.00") BigDecimal total,
    @Schema(description = "Fecha de creación", example = "2024-06-15T14:30:00Z") Instant createdAt
) {}
```

---

## ✅ Hacer

1. **Agregar SpringDoc desde el inicio** - Documentar la API desde las primeras iteraciones del proyecto
2. **Usar `@Tag` en cada controller** - Agrupa endpoints lógicamente en Swagger UI
3. **Documentar todos los response codes** - Incluir 400, 404, 409, 422 y no solo el 200
4. **Usar `@Schema` en DTOs** - Con `description`, `example` y restricciones (`minLength`, `maxLength`, `pattern`)
5. **Configurar Groups** - Separar API pública, interna y admin en diferentes specs
6. **Deshabilitar en producción** - `springdoc.swagger-ui.enabled=false` y `springdoc.api-docs.enabled=false`
7. **Exportar la spec como artefacto** - Versionar el `openapi.json` en el repositorio o como artefacto de CI
8. **Usar `@SecurityScheme`** - Documentar JWT/OAuth2 para que consumidores puedan autenticarse desde Swagger UI
9. **Customizers para headers globales** - `OpenApiCustomizer` para agregar `X-Request-ID` y respuestas de error comunes
10. **Generar clientes SDK** - Usar openapi-generator para crear clientes TypeScript, Java o Kotlin

## ❌ Evitar

1. **Dejar Swagger UI accesible en producción** - Expone la estructura completa de tu API
2. **Documentar con texto libre sin `@Schema`** - Pierde tipado, ejemplos y validación en la spec
3. **Ignorar enums en la documentación** - Usar `enumAsRef = true` para que aparezcan correctamente
4. **Duplicar información entre `@Operation` y código** - Si Spring ya infiere el tipo de retorno, no repetirlo
5. **Poner descripciones en inglés en API documentada en español** - Mantener consistencia de idioma
6. **Abusar de `@Hidden`** - Si un endpoint existe, generalmente debería estar documentado
7. **No documentar errores** - Un endpoint sin `@ApiResponse(responseCode = "404")` confunde a consumidores
8. **Hardcodear URLs de servidor** - Usar variables de entorno o perfiles para `@Server`
9. **Generar la spec sin validarla** - Integrar linting con Spectral en CI
10. **Mezclar endpoints de distintas versiones en un mismo grupo** - Separar v1, v2 en groups diferentes

---

## Checklist de Implementación

### Dependencias y Configuración
- [ ] `springdoc-openapi-starter-webmvc-ui` agregado al `pom.xml`
- [ ] `springdoc` configurado en `application.yml` (`path`, `packages-to-scan`)
- [ ] Swagger UI accesible en `http://localhost:8080/swagger-ui.html`
- [ ] Endpoints de documentación permitidos en Spring Security

### Configuración Global
- [ ] `@OpenAPIDefinition` con `@Info` (title, version, description, contact)
- [ ] `@Server` configurado para cada ambiente (local, staging, producción)
- [ ] `@SecurityScheme` definido para JWT o OAuth2
- [ ] `@SecurityRequirement` global aplicado

### Controllers
- [ ] `@Tag` en cada controller con nombre y descripción
- [ ] `@Operation` con `summary` y `description` en cada endpoint
- [ ] `@ApiResponse` para cada código de respuesta posible (200, 201, 400, 404, 409)
- [ ] `@Parameter` con `description`, `required` y `example` en path/query params
- [ ] Endpoints internos marcados con `@Hidden` si corresponde

### DTOs
- [ ] `@Schema(description)` en cada record/clase DTO
- [ ] `@Schema(example)` en cada campo con valor de ejemplo realista
- [ ] Restricciones documentadas (`minLength`, `maxLength`, `pattern`, `minimum`, `maximum`)
- [ ] Enums con `enumAsRef = true` y `@Schema(description)` en cada valor
- [ ] Campos sensibles marcados con `@Schema(hidden = true)`

### Groups y Customización
- [ ] Groups configurados si hay múltiples conjuntos de APIs
- [ ] `OpenApiCustomizer` para headers globales (`X-Request-ID`)
- [ ] `OperationCustomizer` si se necesita lógica condicional por endpoint

### Seguridad y Producción
- [ ] Swagger UI deshabilitado en perfil `prod`
- [ ] API docs deshabilitado en perfil `prod`
- [ ] Spec exportada y versionada como artefacto

### Generación y Validación
- [ ] Spec exportable vía `curl` o `springdoc-openapi-maven-plugin`
- [ ] Linting con Spectral integrado en CI (opcional pero recomendado)
- [ ] Generación de clientes SDK configurada si hay consumidores externos

*Documento generado con Context7 - SpringDoc OpenAPI 2.x + Spring Boot 3.4.x*
