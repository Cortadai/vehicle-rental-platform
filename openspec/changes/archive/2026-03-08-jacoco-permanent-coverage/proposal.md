## Why

JaCoCo está configurado como profile opcional (`-Pcoverage`), lo que significa que la cobertura solo se mide cuando alguien recuerda activarlo. Con 492 tests y el patrón SAGA completo, necesitamos una red de seguridad permanente que rompa el build si la cobertura cae por debajo de umbrales mínimos. Esto protege la inversión en testing y previene regresiones silenciosas.

## What Changes

- Mover JaCoCo de profile `coverage` a plugin activo en todos los módulos (siempre ejecuta `prepare-agent` + `report`)
- Añadir goal `check` con umbrales diferenciados por capa:
  - `*-domain` y `common`/`common-messaging`: 80% instrucciones
  - `*-application`: 75% instrucciones
  - `*-infrastructure`: 60% instrucciones
- Excluir módulos `*-container` del plugin JaCoCo (solo contienen `@SpringBootApplication` + `BeanConfiguration`)
- Excluir clases `*JpaEntity` y `OutboxEvent` del check (data holders JPA sin lógica)
- Eliminar el profile `coverage` (ya no necesario)
- Mappers manuales INCLUIDOS en cobertura (contienen lógica real de conversión)

## Capabilities

### New Capabilities

- `jacoco-coverage-enforcement`: Configuración de JaCoCo permanente con goal `check`, umbrales diferenciados por capa, y exclusiones de clases JPA

### Modified Capabilities

- `build-enforcement`: Se añade JaCoCo como enforcement permanente del build (antes era solo Enforcer plugin)
- `multi-module-build`: Cambia la sección de profiles (se elimina `coverage`) y la configuración de plugins activos

## Impact

- **POM afectado**: Solo `pom.xml` raíz (toda la configuración JaCoCo está centralizada en pluginManagement + plugins activos)
- **Build time**: Incremento mínimo (~2-5s) por la instrumentación JaCoCo en cada `mvn test`/`verify`
- **Build failures**: Si algún módulo cae por debajo del umbral, `mvn verify` falla — esto es intencional
- **CI/CD**: No requiere cambios; `mvn verify` ya es el comando estándar
- **Módulos container**: Necesitan exclusión explícita del plugin para evitar que JaCoCo falle por 0% cobertura en clases de configuración
