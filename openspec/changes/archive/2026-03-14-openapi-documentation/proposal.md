## Why

La plataforma tiene 4 servicios con 15 endpoints REST pero ninguna documentacion navegable. Los E2E de Bruno validan los flujos, pero para explorar y entender las APIs hace falta consultar el codigo fuente. Springdoc-openapi (ya en dependencyManagement) genera Swagger UI automaticamente con anotaciones minimas en los controllers.

## What Changes

- Añadir dependencia `springdoc-openapi-starter-webmvc-ui` en los 4 container POMs
- Anotar los 4 controllers con `@Tag` (agrupacion) y `@Operation` + `@ApiResponse` (documentacion por metodo)
- Swagger UI disponible en `http://localhost:{port}/swagger-ui.html` para cada servicio
- Sin `@Schema` en DTOs — springdoc genera specs razonables por introspeccion de records Java
- Sin cambios en application.yml — springdoc 2.x se autoconfigura

## Capabilities

### New Capabilities
- `openapi-documentation`: Documentacion OpenAPI generada automaticamente con Swagger UI navegable en los 4 servicios

### Modified Capabilities
(ninguna — este change no modifica comportamiento existente, solo añade documentacion)

## Impact

- **Ficheros modificados**: 4 POMs (dependencia) + 4 controllers (anotaciones)
- **Dependencia nueva**: `springdoc-openapi-starter-webmvc-ui` 2.3.0 (ya en dependencyManagement)
- **Sin cambios en DTOs**: introspeccion automatica de records + validaciones Jakarta
- **Sin cambios en application.yml**: autoconfiguracion de springdoc
- **Sin impacto en tests**: anotaciones no afectan comportamiento funcional
- **Colision de nombres**: `ApiResponse` de swagger vs el del proyecto — resuelto con fully qualified name en anotaciones
