## Context

4 microservicios con 15 endpoints REST, sin documentacion OpenAPI. Springdoc-openapi 2.3.0 ya esta en dependencyManagement del parent POM. Los DTOs son Java records con validaciones Jakarta (@NotBlank, @NotNull, @Valid) que springdoc traduce automaticamente a `required` en la spec.

## Goals / Non-Goals

**Goals:**
- Swagger UI navegable en cada servicio
- Controllers anotados con @Tag, @Operation, @ApiResponse
- Zero configuracion YAML — autoconfiguracion de springdoc

**Non-Goals:**
- @Schema en DTOs — introspeccion de records es suficiente
- @Schema en ApiResponse/ApiMetadata de common — evitar swagger en capa common
- Autenticacion/seguridad — endpoints son publicos
- Customizacion de tema o branding de Swagger UI
- OpenAPI spec exportada a fichero — se genera en runtime

## Decisions

### D1: Sin @Schema en DTOs — solo controller annotations

**Decision:** No añadir `@Schema` en los response DTOs (application layer) ni en ApiResponse/ApiMetadata (common). Solo anotar controllers (infrastructure layer).

**Alternativa descartada:** Añadir `swagger-annotations` como dependencia en application y common para usar @Schema. Romperia la pureza de capas — application solo tiene spring-tx, common tiene zero Spring.

**Rationale:** Springdoc 2.x con Java records genera specs razonables por introspeccion: nombres de campos, tipos, @NotBlank → required. @Schema solo aportaria `description` y `example` extras — nice-to-have, no esencial para un blueprint de aprendizaje.

### D2: Fully qualified name para swagger @ApiResponse

**Decision:** Usar import corto para `com.vehiclerental.common.api.ApiResponse` (el del proyecto, usado en return types) y fully qualified `@io.swagger.v3.oas.annotations.responses.ApiResponse` para las anotaciones swagger.

**Alternativa descartada:** Renombrar el ApiResponse del proyecto a ApiResponseWrapper. Refactor innecesario de todo el proyecto por un conflicto cosmetico.

**Rationale:** El fully qualified solo aparece en anotaciones sobre metodos — no rompe la legibilidad del codigo funcional. El ApiResponse del proyecto se usa en signatures y bodies, mucho mas frecuente.

### D3: Sin configuracion en application.yml

**Decision:** No añadir configuracion springdoc en application.yml. Springdoc 2.x con el starter webmvc-ui se autoconfigura: Swagger UI en `/swagger-ui.html`, API docs en `/v3/api-docs`.

**Rationale:** La exposicion del actuator no necesita cambios — springdoc registra endpoints fuera de `/actuator`. Menos configuracion = menos mantenimiento.

### D4: Dependencia solo en container POMs

**Decision:** Añadir `springdoc-openapi-starter-webmvc-ui` solo en los 4 container POMs, no en infrastructure ni application.

**Rationale:** Es un concern de deployment/presentation, no de logica de negocio. El container module es donde viven las dependencias de runtime.

## Risks / Trade-offs

**[Specs sin descriptions]** → Los campos de los DTOs no tendran descriptions en la spec generada. Mitigacion: los nombres de campos son descriptivos (firstName, licensePlate, trackingId). Para un POC es suficiente.

**[ArchUnit]** → Las anotaciones swagger en controllers (infrastructure) no deberian disparar reglas ArchUnit. Verificar con `mvn test` tras implementar.
