## Context

La plataforma tiene 4 microservicios hexagonales y 2 containers de infraestructura (PostgreSQL + RabbitMQ) definidos en `docker-compose.yml` bajo el profile `infra`. Los servicios se ejecutan manualmente con `mvn spring-boot:run`. Para habilitar testing E2E con Bruno (próximo change), necesitamos que todo levante con un solo `docker compose up -d`.

Cada servicio ya tiene variables de entorno configurables en `application.yml` (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `RABBITMQ_HOST`, etc.). El `init-schemas.sql` ya crea schemas y usuarios dedicados por servicio.

`spring-boot-maven-plugin` ya está configurado en los 4 container modules — soporta `mvn spring-boot:build-image` out-of-the-box con Paketo Buildpacks.

## Goals / Non-Goals

**Goals:**
- `docker compose up -d` levanta toda la plataforma (infra + 4 servicios)
- Imágenes OCI generadas con Paketo (sin Dockerfiles)
- Cada servicio usa su usuario/schema dedicado en PostgreSQL
- Healthchecks vía Spring Boot Actuator `/actuator/health`
- Servicios esperan a que postgres y rabbitmq estén healthy antes de arrancar

**Non-Goals:**
- Observabilidad avanzada (prometheus, liveness/readiness probes) — change futuro
- Service discovery o load balancing entre servicios
- Hot-reload o dev mode en Docker
- CI/CD pipeline para build de imágenes

## Decisions

### D1: Eliminar profiles, no añadir nuevos

**Decisión:** Eliminar `profiles: [infra]` de postgres y rabbitmq. Los servicios se definen sin profile.

**Alternativa descartada:** Mantener `profiles: [infra]` y añadir `profiles: [services]` a los 4 servicios. Requiere `--profile infra --profile services` para todo — verbose y fácil de olvidar.

**Rationale:** `docker compose up -d` levanta todo. Para solo infra: `docker compose up postgres rabbitmq -d`. Simple y sin flags.

### D2: Paketo via spring-boot:build-image (sin Dockerfiles)

**Decisión:** Generar imágenes OCI con `mvn spring-boot:build-image -DskipTests` desde los container modules.

**Alternativa descartada:** Dockerfiles multi-stage (`FROM maven AS build` + `FROM eclipse-temurin:21-jre`). Más explícito pero requiere mantener Dockerfiles.

**Rationale:** El plugin ya está configurado. Paketo genera imágenes optimizadas con layered JARs. Coherente con el workflow del usuario en su trabajo.

**Naming convention:** `vehicle-rental/<service-name>:latest` (e.g., `vehicle-rental/customer-service:latest`). Configurable vía `<image><name>` en el plugin, pero el default de spring-boot-maven-plugin usa `docker.io/library/<artifactId>:version`. Sobreescribiremos con el naming deseado.

### D3: BD unificada con schemas dedicados

**Decisión:** Todos los servicios se conectan a `vehicle_rental_db` con usuarios dedicados (`customer_user`/`customer_pass`, etc.) vía variables de entorno en el compose.

**Alternativa descartada:** 4 bases de datos separadas. Requeriría modificar `init-schemas.sql` o crear un init script que cree múltiples DBs.

**Rationale:** `init-schemas.sql` ya prepara schemas + usuarios + search_path. Solo hay que sobreescribir `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` en el compose. Los `application.yml` no se tocan para la conexión a BD.

### D4: Actuator mínimo — solo health

**Decisión:** Añadir `spring-boot-starter-actuator` y exponer solo el endpoint `health`.

**Alternativa descartada:** Config completa con liveness/readiness probes, prometheus, info. Innecesaria sin Kubernetes ni stack de observabilidad.

**Rationale:** El único consumidor del healthcheck es Docker Compose (`depends_on` + `condition: service_healthy`). `/actuator/health` es suficiente.

### D5: Actuator como dependencia en cada container POM

**Decisión:** Añadir `spring-boot-starter-actuator` en cada `*-container/pom.xml`, no en el parent `dependencyManagement`.

**Alternativa descartada:** Declarar en `dependencyManagement` del parent y heredar. No todos los módulos necesitan actuator — solo los containers.

**Rationale:** Consistente con el patrón existente: cada container declara sus dependencias Spring Boot explícitamente. Actuator es una dependencia del container, no del dominio ni la aplicación.

### D6: Sin Docker healthcheck en servicios (Paketo limitation)

**Decisión:** No incluir `healthcheck` en los service blocks del compose. Solo infra (postgres, rabbitmq) tiene healthcheck.

**Alternativa descartada:** `curl -f http://localhost:<port>/actuator/health`. Las imágenes Paketo (Bellsoft Liberica tiny stack) no incluyen curl, wget, bash ni ningún shell utility. No hay forma práctica de ejecutar un healthcheck desde dentro del container.

**Rationale:** Los `depends_on` con `condition: service_healthy` en infra garantizan el orden de arranque. Para verificar que los servicios están UP, se usa `curl` desde el host contra `localhost:8181-8184/actuator/health`. Actuator sigue siendo útil para verificación externa (Bruno E2E tests, scripts).

### D7: RabbitMQ definitions.json — password field (no password_hash)

**Decisión:** Usar `"password": "guest"` en lugar de `"password_hash": "guest"` + `"hashing_algorithm"` en definitions.json.

**Rationale:** RabbitMQ 3.13 requiere que `password_hash` sea un hash base64 válido. Usar `"password"` hace que RabbitMQ hashee internamente durante la importación de definiciones. Más simple y portable.

### D8: Spring Boot 3.4.1 → 3.4.13 upgrade

**Decisión:** Actualizar la versión de Spring Boot en el parent POM de 3.4.1 a 3.4.13.

**Rationale:** Spring Boot 3.4.1 hardcodea Docker API v1.24 en el plugin `build-image`, pero Docker 29.x requiere mínimo v1.44. Issue spring-boot#48050, resuelto en 3.4.12. Efecto secundario: Spring 6.2 cambia el tracking de runtime bean types, lo que requirió simplificar los BeanConfiguration (ver D9).

### D9: BeanConfiguration simplificados (Spring 6.2 bean type resolution)

**Decisión:** Eliminar los `@Bean` wrapper de use cases de todos los BeanConfiguration. Solo registrar el `*ApplicationService` principal.

**Rationale:** Spring 6.2 trackea runtime types de beans. Cuando `CustomerApplicationService` implementa `CreateCustomerUseCase` y `GetCustomerUseCase`, Spring resuelve automáticamente los interface types desde el bean principal. Los wrappers redundantes causaban "expected single matching bean but found 2".

## Risks / Trade-offs

**[Build previo requerido]** → El usuario debe ejecutar `mvn spring-boot:build-image -DskipTests` antes de `docker compose up`. Mitigation: documentar en CLAUDE.md y considerar un target en Makefile.

**[Imágenes grandes]** → Paketo genera imágenes de ~300MB por servicio. Mitigation: aceptable para desarrollo local; no es un concern de producción en este POC.

**[Sin Docker healthcheck en servicios]** → Docker no reporta el estado health de los servicios. Solo infra reporta "healthy". Mitigation: verificación via host curl; para CI/CD futuro se puede usar un script wait-for-it o health loop externo.
