## Context

JaCoCo está configurado en `pluginManagement` del parent POM con 4 executions (prepare-agent, report, prepare-agent-integration, report-integration) pero solo se activa mediante el profile `coverage` (`mvn verify -Pcoverage`). No hay goal `check` — la cobertura se puede consultar pero nunca rompe el build.

El proyecto tiene 18 módulos organizados en 4 capas por servicio (domain, application, infrastructure, container) más 2 módulos compartidos (common, common-messaging). Cada capa tiene una naturaleza diferente en cuanto a testabilidad y coste de cobertura.

## Goals / Non-Goals

**Goals:**
- JaCoCo siempre activo en el build (sin profile)
- Goal `check` con umbrales diferenciados que rompe el build si no se cumplen
- Exclusión de clases JPA data holders del cálculo de cobertura
- Exclusión completa de módulos container (solo config de Spring Boot)

**Non-Goals:**
- Aggregate report consolidado del proyecto (decisión: no aporta valor como quality gate; los reports per-module son suficientes)
- Cobertura de branch (solo instruction coverage como métrica de check)
- Cobertura mínima por clase individual (solo ratio global del módulo)

## Decisions

### D1: Contador INSTRUCTION explícito en el goal check

**Decisión**: Usar `INSTRUCTION` como counter en todas las reglas del goal `check`.

**Alternativas**:
- `LINE`: Menos granular, una línea con múltiples instrucciones cuenta como 1
- `BRANCH`: Demasiado estricto para código con pocos branches (value objects, DTOs)
- Default implícito: JaCoCo usa `INSTRUCTION` por defecto, pero dejarlo implícito es frágil ante cambios de versión

**Rationale**: INSTRUCTION es el contador más granular y habitual. Especificarlo explícitamente en el XML hace la configuración auto-documentada y resistente a cambios de default.

### D2: Umbrales diferenciados por capa

**Decisión**: Tres niveles de umbral según la naturaleza del código:

| Capa | Umbral | Módulos | Rationale |
|------|--------|---------|-----------|
| 80% | domain | `*-domain`, `common`, `common-messaging` | Lógica pura sin dependencias externas. Tests unitarios baratos. |
| 75% | application | `*-application` | Orquestación con mocks. Algo más de ceremony en tests. |
| 60% | infrastructure | `*-infrastructure` | Adapters + Testcontainers. Tests costosos, adapters simples de forwarding. |
| — | container | `*-container` | Excluidos completamente (solo `@SpringBootApplication` + `BeanConfiguration`). |

**Alternativa considerada**: 80% uniforme para todos. Descartado porque forzaría tests de bajo valor en infrastructure (adapters triviales) o tests excesivamente detallados en application (mocking de orquestación).

**Implementación**: La diferenciación se logra configurando el `check` goal con reglas distintas en la sección `pluginManagement`. Cada módulo hijo hereda la configuración del parent. El umbral correcto se aplica según el módulo, usando tres bloques de configuración condicional en el parent POM:

```
pluginManagement → jacoco-maven-plugin → check (con exclusiones globales)
  ↓
Módulos domain/common: heredan check → 80%
Módulos application:   override en su POM → 75%
Módulos infrastructure: override en su POM → 60%
Módulos container:     jacoco.skip=true → sin check
```

**Nota**: JaCoCo no soporta umbrales condicionales por artifactId en `pluginManagement`. El parent define el umbral default (80%). Los módulos application e infrastructure sobreescriben el umbral en sus propios POMs con una configuración mínima del plugin.

### D3: Exclusión de containers vía property `jacoco.skip`

**Decisión**: Usar la property `jacoco.skip` para excluir módulos container.

**Implementación**:
- Parent POM: `<jacoco.skip>false</jacoco.skip>` en `<properties>`
- Cada container POM: `<jacoco.skip>true</jacoco.skip>` en `<properties>`

**Alternativas**:
- Declarar `<skip>true</skip>` en bloque `<plugin>` de cada container: Más verbose, requiere bloque XML completo
- No activar JaCoCo en parent y activar solo en módulos deseados: 14 activaciones vs 4 exclusiones

**Rationale**: Una sola línea en `<properties>` de cada container POM. Sin bloques de plugin adicionales. La property `jacoco.skip` es nativa del plugin JaCoCo.

### D4: Exclusiones de clases JPA

**Decisión**: Excluir del check las clases que matchean `*JpaEntity` y `OutboxEvent`.

**Implementación**: En la configuración `check` dentro de `pluginManagement`, usar `<excludes>`:

```xml
<excludes>
    <exclude>**/entity/*JpaEntity.class</exclude>
    <exclude>**/outbox/OutboxEvent.class</exclude>
</excludes>
```

Las mismas exclusiones se aplican al goal `report` para consistencia (que el report no cuente clases que el check ignora).

**No excluidos**: Mappers manuales (`*PersistenceMapper`, `*ApplicationMapper`) — contienen lógica real de conversión (typed IDs, enums, listas).

### D5: Activación permanente (eliminar profile)

**Decisión**: Mover JaCoCo de profile `coverage` a `<plugins>` activos del parent.

**Implementación**:
1. Eliminar el profile `<id>coverage</id>` del parent POM
2. Añadir `<plugin>` JaCoCo en la sección `<plugins>` activos (junto a enforcer)
3. Añadir executions `check` y `check-integration` al `pluginManagement`

**Resultado**: `mvn test` ejecuta prepare-agent + report + check (unit). `mvn verify` añade prepare-agent-integration + report-integration + check-integration.

### D6: Estructura de executions en pluginManagement

**Decisión**: 6 executions totales en `pluginManagement`:

| Execution ID | Goal | Phase | Descripción |
|---|---|---|---|
| `prepare-agent` | prepare-agent | (default) | Instrumenta para tests unitarios |
| `report` | report | test | Genera report unitario |
| `check` | check | test | Valida umbral con tests unitarios |
| `prepare-agent-integration` | prepare-agent-integration | (default) | Instrumenta para ITs |
| `report-integration` | report | post-integration-test | Genera report IT |
| `check-integration` | check | post-integration-test | Valida umbral con tests IT (mismo umbral) |

**Nota sobre check-integration**: Se usa el mismo umbral que el check unitario pero con `dataFile` apuntando a `jacoco-it.exec`. Esto permite que módulos con pocos unit tests pero buenos ITs (como infrastructure) cumplan el umbral combinando ambas fases.

**Revisión**: Tras reflexionar, el `check-integration` en `post-integration-test` sería redundante si los ITs contribuyen al mismo `.exec` que los unit tests. Pero como usamos `prepare-agent` y `prepare-agent-integration` separados, los datos están en archivos distintos (`jacoco.exec` y `jacoco-it.exec`). El check en fase `test` solo ve cobertura unitaria.

Para infrastructure (60%), el umbral debe ser alcanzable solo con ITs (que es donde vive la mayor parte de su cobertura). Por tanto, el `check-integration` es necesario y el `check` en fase `test` podría ser problemático para infrastructure modules que tienen pocos unit tests.

**Solución**: Para módulos infrastructure, el check unitario se puede configurar con un umbral más bajo o desactivar, y confiar en el check-integration. Sin embargo, esto añade complejidad.

**Decisión simplificada**: Un solo `check` en fase `verify` que combine ambos data files usando `merge` goal, o bien usar solo el check sobre `jacoco.exec` que incluye datos de Surefire. Dado que `prepare-agent` instrumenta para Surefire y `prepare-agent-integration` para Failsafe, y los módulos infrastructure ejecutan sus tests mayoritariamente como ITs via Failsafe, la opción más limpia es:

- **Módulos domain/application/common**: `check` en fase `test` (sus tests son unitarios vía Surefire)
- **Módulos infrastructure**: `check` en fase `verify` sobre `jacoco-it.exec` (sus tests son ITs vía Failsafe)

Esto se resuelve con: el parent define check en fase `verify` sobre ambos data files. Se usa el goal `merge` para combinar `jacoco.exec` + `jacoco-it.exec` en `jacoco-merged.exec`, y el check corre sobre el merged.

**Decisión final**: Añadir execution `merge` antes del `check` final:

| Execution ID | Goal | Phase | Descripción |
|---|---|---|---|
| `prepare-agent` | prepare-agent | (default) | Instrumenta para Surefire |
| `report` | report | test | Report unitario |
| `prepare-agent-integration` | prepare-agent-integration | (default) | Instrumenta para Failsafe |
| `report-integration` | report | post-integration-test | Report IT |
| `merge-results` | merge | verify | Merge jacoco.exec + jacoco-it.exec → jacoco-merged.exec |
| `check` | check | verify | Valida umbral sobre jacoco-merged.exec |

De esta forma, un solo check en `verify` valida la cobertura combinada (unit + IT). Los módulos domain cumplen con unit tests, infrastructure cumple con ITs, application cumple con mix.

## Risks / Trade-offs

**[Falsos positivos en infrastructure]** → Si un módulo infrastructure tiene muchos adapters triviales y pocos tests, el 60% podría no cumplirse inicialmente.
→ Mitigación: Verificar cobertura actual antes de fijar umbrales. Ajustar si es necesario.

**[Build más lento]** → JaCoCo instrumentación añade ~2-5 segundos al build total.
→ Trade-off aceptable: la seguridad de cobertura vale más que 5 segundos.

**[Umbral override en módulos hijos]** → Los módulos application e infrastructure necesitan sobreescribir el umbral del parent. Esto añade configuración a 8 POMs (4 application + 4 infrastructure).
→ Mitigación: Configuración mínima (solo la regla del check con el nuevo minimum).

**[Merge de .exec files]** → El goal `merge` requiere que ambos archivos existan. Si un módulo no tiene ITs (como domain), `jacoco-it.exec` no existirá.
→ Mitigación: JaCoCo `merge` goal ignora archivos inexistentes por configuración. Verificar con prueba real.
