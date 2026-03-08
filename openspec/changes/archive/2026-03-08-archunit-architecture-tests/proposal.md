## Why

Las reglas de arquitectura hexagonal (domain sin Spring, application sin infrastructure, dependencias solo hacia adentro) se cumplen hoy — pero solo por disciplina manual. No hay nada que impida que un developer añada un `@Entity` en domain o importe un adapter en application. ArchUnit convierte estas convenciones en tests ejecutables que fallan en el build, haciendo las boundaries autoenforzadas.

## What Changes

- Nuevo módulo `architecture-tests` con tests ArchUnit que validan boundaries hexagonales
- 3 clases de test organizadas por categoría de regla:
  - **Domain purity**: domain no importa Spring, JPA, application, infrastructure, ni common-messaging
  - **Application isolation**: application solo depende de domain, common, java, lombok, y `org.springframework.transaction`
  - **Dependency flow**: las dependencias entre capas solo fluyen hacia adentro (infrastructure → application → domain)
- Escaneo completo de `com.vehiclerental` (cubre los 4 servicios + common)
- Siempre activo en `mvn verify` (sin profile), coherente con JaCoCo
- `jacoco.skip=true` en el módulo (tests de arquitectura, no código de producción)

## Capabilities

### New Capabilities

- `archunit-hexagonal-rules`: Reglas ArchUnit que validan boundaries de arquitectura hexagonal — domain purity, application isolation, y dependency flow inward-only

### Modified Capabilities

- `multi-module-build`: El root POM necesita declarar el nuevo módulo `architecture-tests` en `<modules>`

## Impact

- **Maven build**: nuevo módulo `architecture-tests` al final de `<modules>` en root POM
- **Dependencias**: el módulo depende de los 4 `*-infrastructure` (transitivamente trae application + domain) + `common-messaging` + `archunit-junit5`
- **Tiempo de build**: impacto mínimo — ArchUnit es análisis estático en memoria, no levanta Spring context ni containers
- **No hay cambios en código de producción** — solo se añaden tests
