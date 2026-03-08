## Why

Hoy los 4 microservicios solo se ejecutan manualmente con `mvn spring-boot:run`. Para hacer testing E2E (próximo change con Bruno) necesitamos los 4 servicios levantando en Docker Compose con un solo `docker compose up -d`, incluyendo la infra de la que dependen.

## What Changes

- Eliminar `profiles: [infra]` de postgres y rabbitmq — `docker compose up -d` levanta todo
- Añadir 4 service blocks al compose: customer-service, fleet-service, reservation-service, payment-service
- Cada servicio usa imágenes Paketo generadas con `mvn spring-boot:build-image` (sin Dockerfiles)
- Cada servicio se conecta a `vehicle_rental_db` con su usuario/schema dedicado (ya preparado por `init-schemas.sql`)
- Añadir `spring-boot-starter-actuator` para healthchecks vía `/actuator/health`
- Config mínima de actuator en los `application.yml` (solo health, sin probes k8s ni prometheus)

## Capabilities

### New Capabilities

- `docker-compose-services`: Definición de los 4 microservicios en Docker Compose — imágenes Paketo, variables de entorno, healthchecks con actuator, depends_on con condition: service_healthy
- `actuator-health`: Spring Boot Actuator con configuración mínima (solo endpoint health) para healthchecks de Docker Compose

### Modified Capabilities

- `docker-compose-infra`: Eliminar profiles de los containers de infraestructura — todo levanta con `docker compose up -d`

## Impact

- **docker-compose.yml**: 4 nuevos service blocks + eliminación de profiles de infra
- **Dependencias Maven**: `spring-boot-starter-actuator` como nueva dependencia en containers
- **application.yml**: Config mínima de actuator en los 4 containers
- **Build workflow**: Requiere `mvn spring-boot:build-image` previo al `docker compose up`
