# openapi-documentation Specification

## Purpose
Documentacion OpenAPI generada automaticamente con Swagger UI navegable en los 4 servicios REST.

## ADDED Requirements

### Requirement: Swagger UI available on each service
Cada servicio SHALL exponer Swagger UI en su puerto.

#### Scenario: Swagger UI accessible
- **WHEN** el servicio esta corriendo
- **THEN** SHALL responder en `/swagger-ui.html` con la interfaz Swagger UI
- **AND** SHALL responder en `/v3/api-docs` con la spec OpenAPI en JSON

### Requirement: Controllers annotated with OpenAPI metadata
Los 4 controllers SHALL tener anotaciones OpenAPI para organizacion y documentacion.

#### Scenario: Controller tag grouping
- **WHEN** se inspecciona la spec OpenAPI de un servicio
- **THEN** los endpoints SHALL estar agrupados bajo un @Tag descriptivo (e.g., "Customer Service", "Fleet Service")

#### Scenario: Operation documentation
- **WHEN** se inspecciona un endpoint en la spec OpenAPI
- **THEN** SHALL tener un `summary` descriptivo via @Operation
- **AND** SHALL documentar los response codes esperados via @ApiResponse (201, 200, 204 segun endpoint)

### Requirement: Request DTOs documented via introspection
Los DTOs de request SHALL estar documentados automaticamente sin @Schema.

#### Scenario: Record introspection
- **WHEN** springdoc genera la spec para un request DTO (Java record)
- **THEN** los campos SHALL aparecer con sus tipos correctos
- **AND** los campos con @NotBlank o @NotNull SHALL marcarse como `required`

### Requirement: No configuration required
La integracion de springdoc SHALL funcionar sin configuracion adicional en application.yml.

#### Scenario: Zero-config autoconfiguration
- **WHEN** se añade la dependencia `springdoc-openapi-starter-webmvc-ui` al container POM
- **THEN** Swagger UI y API docs SHALL estar disponibles sin tocar application.yml
