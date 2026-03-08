## Context

La plataforma tiene 4 servicios hexagonales con 4 capas cada uno (domain, application, infrastructure, container). Las boundaries se respetan hoy por disciplina manual, pero no hay enforcement automático. JaCoCo (#22) ya estableció el precedente de quality gates permanentes en el build. ArchUnit completa el cuadro: JaCoCo valida cobertura, ArchUnit valida estructura.

Dependencia `archunit-junit5` 1.2.1 ya declarada en `dependencyManagement` del parent POM.

## Goals / Non-Goals

**Goals:**
- Validar automáticamente que domain no importa Spring, JPA, ni capas superiores
- Validar que application solo depende de un allowlist explícito (domain, common, java, lombok, spring-tx)
- Validar que las dependencias fluyen inward-only (infrastructure → application → domain)
- Tests siempre activos en `mvn verify`, sin profile
- Cobertura de los 4 servicios + common desde un único módulo

**Non-Goals:**
- Validar container layer (wiring de Spring Boot, poco valor vs fragilidad)
- Validar convenciones de nombrado (e.g., sufijos *UseCase, *Adapter)
- Validar structure interna de paquetes (e.g., que port/input existe)
- Aggregate report o métricas de ArchUnit — solo pass/fail

## Decisions

### D1: Módulo dedicado vs tests en cada container

**Decisión:** Módulo dedicado `architecture-tests` en la raíz del proyecto.

**Alternativa descartada:** Tests en cada `*-container/src/test/`. Duplicaría las mismas reglas 4 veces y requeriría mantenerlas sincronizadas.

**Rationale:** Un solo lugar para todas las reglas. El módulo depende de los 4 `*-infrastructure` (que traen application + domain transitivamente) + `common-messaging`, así que tiene todas las clases en classpath para análisis.

### D2: Organización por categoría de regla (no por servicio)

**Decisión:** 3 clases de test organizadas por tipo de regla:
- `DomainPurityTest` — domain no importa Spring/JPA/capas superiores
- `ApplicationIsolationTest` — application allowlist
- `DependencyFlowTest` — dependencias inward-only entre capas

**Alternativa descartada:** 4 clases por servicio (CustomerArchitectureTest, etc.). ArchUnit ya reporta la clase y paquete exacto en el error — la separación por servicio no añade valor diagnóstico y cuadruplica el código.

### D3: Allowlist para application (no blocklist)

**Decisión:** La regla de application usa `should onlyDependOnClassesThat` (allowlist) en vez de `should notDependOnClassesThat` (blocklist).

**Rationale:** Application tiene una excepción legítima: `org.springframework.transaction` (para `@Transactional`). Un blocklist requeriría listar cada paquete Spring prohibido. Un allowlist es más robusto — si alguien añade una dependencia no esperada, falla automáticamente.

**Paquetes permitidos:** `com.vehiclerental..domain..`, `com.vehiclerental.common..`, `java..`, `lombok..`, `org.slf4j..`, `com.fasterxml.jackson..`, `org.springframework.transaction..`

**Nota:** No se incluye `com.vehiclerental..application..` — ArchUnit ya permite dependencias internas (clases del propio paquete application se ven entre sí). Incluir `..application..` sería innecesariamente permisivo: permitiría importar cualquier clase bajo un subpaquete `application` en cualquier parte del codebase.

### D4: Scope de escaneo — com.vehiclerental completo

**Decisión:** `@AnalyzeClasses(packages = "com.vehiclerental")` — escanea todo el base package.

**Rationale:** Incluir common y common-messaging en el scan es una ventaja. Las reglas de domain aplican naturalmente (domain no importa common.messaging). No hay razón para excluir partes del codebase.

### D5: JaCoCo skip

**Decisión:** `jacoco.skip=true` en el POM de `architecture-tests`, igual que los containers.

**Rationale:** El módulo no tiene código de producción (solo `src/test/java`). Medir cobertura de tests de arquitectura no tiene sentido.

### D6: No incluir en dependencyManagement

**Decisión:** El módulo se declara en `<modules>` pero NO en `<dependencyManagement>`.

**Rationale:** Nadie depende de `architecture-tests`. Añadirlo a dependencyManagement sería ruido sin propósito.

## Risks / Trade-offs

**[Classpath gordo]** → El módulo trae los 16 módulos de servicio + Spring + JPA + RabbitMQ en su classpath. Mitigation: no hay código de producción, solo tests. El classpath pesado solo afecta `mvn test` de este módulo (~segundos de análisis estático).

**[Falsos positivos en allowlist]** → Si application necesita una nueva dependencia legítima (e.g., un nuevo framework de validación), el test fallará. Mitigation: esto es una feature, no un bug — fuerza una decisión consciente de añadir la dependencia al allowlist.

**[Reglas domain vs common.domain]** → `common.domain` contiene `AggregateRoot`, `BaseEntity`, `DomainEvent` — clases que domain importa legítimamente. Las reglas deben permitir `com.vehiclerental.common.domain..` pero prohibir `com.vehiclerental.common.messaging..`. Mitigation: regla explícita para common-messaging en DomainPurityTest.
