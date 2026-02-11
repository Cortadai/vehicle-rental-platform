# Área 19: Docker Compose para Entornos de Desarrollo Multi-Servicio

> **Audiencia**: Desarrolladores junior/mid (guía detallada) + Seniors (referencia rápida)
> **Stack**: Spring Boot 3.4.x, Java 21, PostgreSQL 16+, RabbitMQ 3.13+, Docker Compose
> **Alcance**: Solo desarrollo local. Para Kubernetes ver Área 09.

---

## 1. Estructura del Proyecto Docker

### Referencia Rápida (Seniors)

```
proyecto/
├── docker-compose.yml              ← Compose principal
├── docker/
│   ├── postgres/
│   │   └── init-schemas.sql        ← Init script multi-schema
│   └── rabbitmq/
│       ├── definitions.json        ← Topología pre-cargada
│       └── rabbitmq.conf           ← Config custom (opcional)
├── reservation-service/
│   ├── Dockerfile
│   ├── .dockerignore
│   └── src/...
├── customer-service/
│   ├── Dockerfile
│   ├── .dockerignore
│   └── src/...
├── payment-service/
│   └── ...
├── fleet-service/
│   └── ...
└── Makefile                        ← Scripts helper
```

### Guía Detallada (Junior/Mid)

✅ **Hacer**: Centralizar `docker-compose.yml` en la raíz del proyecto. Separar configuración de infraestructura en `docker/`.

**Por qué**: El compose en la raíz permite ejecutar `docker compose up` sin navegar a subdirectorios. La carpeta `docker/` agrupa scripts de inicialización y configuraciones que solo existen para el entorno de contenedores.

#### .dockerignore para proyectos Maven/Spring Boot

✅ **Hacer**: Crear un `.dockerignore` en cada servicio para evitar copiar archivos innecesarios al contexto de build

```
# .dockerignore (en cada servicio)
.git
.gitignore
.idea
*.iml
.vscode
.mvn/wrapper/maven-wrapper.jar
target/
!target/*.jar
docker-compose.yml
Dockerfile
.dockerignore
README.md
*.md
logs/
```

**Por qué**: Sin `.dockerignore`, Docker copia TODO el directorio al daemon antes de ejecutar el build. En un proyecto Maven con `target/`, eso puede significar cientos de MB innecesarios.

---

## 2. Docker Compose para Infraestructura Base

### Referencia Rápida (Seniors)

```bash
# Levantar solo infraestructura (para desarrollo en IDE)
docker compose --profile infra up -d

# Levantar todo (infra + servicios)
docker compose --profile full up -d

# Ver logs
docker compose logs -f postgres rabbitmq
```

### Guía Detallada (Junior/Mid)

Este compose levanta PostgreSQL y RabbitMQ listos para que los servicios Spring Boot se conecten desde tu IDE.

```yaml
# docker-compose.yml
services:

  # ============================================================
  # INFRAESTRUCTURA
  # ============================================================

  postgres:
    image: postgres:16-alpine
    container_name: acme-postgres
    profiles: ["infra", "full", "debug"]
    environment:
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: admin123
      POSTGRES_DB: acme_platform
      # Optimización para desarrollo (no usar en producción)
      POSTGRES_INITDB_ARGS: "--encoding=UTF8 --locale=en_US.UTF-8"
    ports:
      - "5432:5432"
    volumes:
      # Script de inicialización (se ejecuta solo la primera vez)
      - ./docker/postgres/init-schemas.sql:/docker-entrypoint-initdb.d/01-init-schemas.sql
      # Volumen persistente para datos
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U admin -d acme_platform"]
      interval: 5s
      timeout: 5s
      retries: 5
      start_period: 10s
    # Configuración de memoria y conexiones para desarrollo
    command: >
      postgres
      -c shared_buffers=256MB
      -c max_connections=100
      -c work_mem=16MB
      -c log_statement=all
      -c log_min_duration_statement=100
    networks:
      - acme-network

  rabbitmq:
    image: rabbitmq:3.13-management-alpine
    container_name: acme-rabbitmq
    profiles: ["infra", "full", "debug"]
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    ports:
      - "5672:5672"     # AMQP
      - "15672:15672"   # Management UI
    volumes:
      # Definiciones pre-cargadas (exchanges, queues, bindings)
      - ./docker/rabbitmq/definitions.json:/etc/rabbitmq/definitions.json
      - ./docker/rabbitmq/rabbitmq.conf:/etc/rabbitmq/rabbitmq.conf
      # Volumen persistente para datos
      - rabbitmq_data:/var/lib/rabbitmq
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
      interval: 10s
      timeout: 10s
      retries: 5
      start_period: 30s
    networks:
      - acme-network

  # ============================================================
  # MICROSERVICIOS (solo con profile "full" o "debug")
  # ============================================================

  reservation-service:
    build:
      context: ./reservation-service
      dockerfile: Dockerfile
    container_name: acme-reservation
    profiles: ["full"]
    ports:
      - "8081:8080"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/acme_platform?currentSchema=reservation
      SPRING_DATASOURCE_USERNAME: reservation_user
      SPRING_DATASOURCE_PASSWORD: reservation_pass
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_PORT: 5672
      SPRING_RABBITMQ_USERNAME: guest
      SPRING_RABBITMQ_PASSWORD: guest
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    networks:
      - acme-network

  customer-service:
    build:
      context: ./customer-service
      dockerfile: Dockerfile
    container_name: acme-customer
    profiles: ["full"]
    ports:
      - "8082:8080"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/acme_platform?currentSchema=customer
      SPRING_DATASOURCE_USERNAME: customer_user
      SPRING_DATASOURCE_PASSWORD: customer_pass
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_PORT: 5672
      SPRING_RABBITMQ_USERNAME: guest
      SPRING_RABBITMQ_PASSWORD: guest
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    networks:
      - acme-network

  payment-service:
    build:
      context: ./payment-service
      dockerfile: Dockerfile
    container_name: acme-payment
    profiles: ["full"]
    ports:
      - "8083:8080"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/acme_platform?currentSchema=payment
      SPRING_DATASOURCE_USERNAME: payment_user
      SPRING_DATASOURCE_PASSWORD: payment_pass
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_PORT: 5672
      SPRING_RABBITMQ_USERNAME: guest
      SPRING_RABBITMQ_PASSWORD: guest
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    networks:
      - acme-network

  fleet-service:
    build:
      context: ./fleet-service
      dockerfile: Dockerfile
    container_name: acme-fleet
    profiles: ["full"]
    ports:
      - "8084:8080"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/acme_platform?currentSchema=fleet
      SPRING_DATASOURCE_USERNAME: fleet_user
      SPRING_DATASOURCE_PASSWORD: fleet_pass
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_PORT: 5672
      SPRING_RABBITMQ_USERNAME: guest
      SPRING_RABBITMQ_PASSWORD: guest
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    networks:
      - acme-network

  # ============================================================
  # SERVICIOS CON DEBUG REMOTO
  # ============================================================

  reservation-service-debug:
    build:
      context: ./reservation-service
      dockerfile: Dockerfile
    container_name: acme-reservation-debug
    profiles: ["debug"]
    ports:
      - "8081:8080"
      - "5005:5005"   # Debug port
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/acme_platform?currentSchema=reservation
      SPRING_DATASOURCE_USERNAME: reservation_user
      SPRING_DATASOURCE_PASSWORD: reservation_pass
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_PORT: 5672
      SPRING_RABBITMQ_USERNAME: guest
      SPRING_RABBITMQ_PASSWORD: guest
      JAVA_TOOL_OPTIONS: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    networks:
      - acme-network

# ============================================================
# VOLUMES Y NETWORKS
# ============================================================

volumes:
  postgres_data:
    driver: local
  rabbitmq_data:
    driver: local

networks:
  acme-network:
    driver: bridge
```

---

## 3. Init Scripts de PostgreSQL

### Referencia Rápida (Seniors)

```sql
-- docker/postgres/init-schemas.sql
-- Crea 4 schemas + usuarios con permisos aislados
-- Idempotente: usa IF NOT EXISTS en todo
```

### Guía Detallada (Junior/Mid)

✅ **Hacer**: Crear schemas separados por microservicio con usuarios dedicados que solo tienen acceso a su schema

**Por qué**: Cada servicio opera sobre su propio schema. Si el servicio de pagos tiene un bug de SQL injection, no puede afectar las tablas de flota. Además, cada servicio puede migrar su schema independientemente.

```sql
-- docker/postgres/init-schemas.sql
-- ============================================================
-- Init script para PostgreSQL multi-schema
-- Se ejecuta SOLO la primera vez que se crea el volumen.
-- Si necesitas re-ejecutar: eliminar el volumen primero.
-- ============================================================

-- Crear schemas
CREATE SCHEMA IF NOT EXISTS reservation;
CREATE SCHEMA IF NOT EXISTS customer;
CREATE SCHEMA IF NOT EXISTS payment;
CREATE SCHEMA IF NOT EXISTS fleet;

-- ============================================================
-- Usuarios por servicio con permisos restringidos
-- ============================================================

-- Reservation Service
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'reservation_user') THEN
        CREATE USER reservation_user WITH PASSWORD 'reservation_pass';
    END IF;
END
$$;
GRANT USAGE ON SCHEMA reservation TO reservation_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA reservation TO reservation_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA reservation TO reservation_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA reservation
    GRANT ALL PRIVILEGES ON TABLES TO reservation_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA reservation
    GRANT ALL PRIVILEGES ON SEQUENCES TO reservation_user;

-- Customer Service
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'customer_user') THEN
        CREATE USER customer_user WITH PASSWORD 'customer_pass';
    END IF;
END
$$;
GRANT USAGE ON SCHEMA customer TO customer_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA customer TO customer_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA customer TO customer_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA customer
    GRANT ALL PRIVILEGES ON TABLES TO customer_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA customer
    GRANT ALL PRIVILEGES ON SEQUENCES TO customer_user;

-- Payment Service
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'payment_user') THEN
        CREATE USER payment_user WITH PASSWORD 'payment_pass';
    END IF;
END
$$;
GRANT USAGE ON SCHEMA payment TO payment_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA payment TO payment_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA payment TO payment_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA payment
    GRANT ALL PRIVILEGES ON TABLES TO payment_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA payment
    GRANT ALL PRIVILEGES ON SEQUENCES TO payment_user;

-- Fleet Service
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'fleet_user') THEN
        CREATE USER fleet_user WITH PASSWORD 'fleet_pass';
    END IF;
END
$$;
GRANT USAGE ON SCHEMA fleet TO fleet_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA fleet TO fleet_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA fleet TO fleet_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA fleet
    GRANT ALL PRIVILEGES ON TABLES TO fleet_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA fleet
    GRANT ALL PRIVILEGES ON SEQUENCES TO fleet_user;

-- ============================================================
-- Verificación
-- ============================================================
DO $$
BEGIN
    RAISE NOTICE '✅ Schemas creados: reservation, customer, payment, fleet';
    RAISE NOTICE '✅ Usuarios creados: reservation_user, customer_user, payment_user, fleet_user';
END
$$;
```

> **Nota**: PostgreSQL ejecuta los scripts de `/docker-entrypoint-initdb.d/` en orden alfanumérico y SOLO si el volumen está vacío (primera ejecución). Por eso el prefijo `01-` en el nombre del fichero montado.

---

## 4. Definiciones Pre-cargadas de RabbitMQ

### Referencia Rápida (Seniors)

```
docker/rabbitmq/rabbitmq.conf → habilita carga de definitions.json
docker/rabbitmq/definitions.json → exchanges, queues, bindings pre-creados
```

### Guía Detallada (Junior/Mid)

✅ **Hacer**: Pre-cargar toda la topología de RabbitMQ vía `definitions.json` para que al levantar el compose, exchanges, queues y bindings ya existan

**Por qué**: Sin esto, cada servicio necesita código para crear su topología al arrancar. Con definitions.json, la topología está definida declarativamente y es reproducible.

#### rabbitmq.conf

```ini
# docker/rabbitmq/rabbitmq.conf

# Cargar definiciones al arrancar
management.load_definitions = /etc/rabbitmq/definitions.json

# Configuración de desarrollo
vm_memory_high_watermark.relative = 0.7
disk_free_limit.relative = 1.5
```

#### definitions.json

```json
{
  "rabbit_version": "3.13.0",
  "users": [
    {
      "name": "guest",
      "password_hash": "guest",
      "hashing_algorithm": "rabbit_password_hashing_sha256",
      "tags": ["administrator"]
    }
  ],
  "vhosts": [
    {"name": "/"}
  ],
  "permissions": [
    {
      "user": "guest",
      "vhost": "/",
      "configure": ".*",
      "write": ".*",
      "read": ".*"
    }
  ],
  "exchanges": [
    {
      "name": "order.exchange",
      "vhost": "/",
      "type": "topic",
      "durable": true,
      "auto_delete": false
    },
    {
      "name": "payment.exchange",
      "vhost": "/",
      "type": "topic",
      "durable": true,
      "auto_delete": false
    },
    {
      "name": "saga.exchange",
      "vhost": "/",
      "type": "topic",
      "durable": true,
      "auto_delete": false
    },
    {
      "name": "dlx.exchange",
      "vhost": "/",
      "type": "direct",
      "durable": true,
      "auto_delete": false
    }
  ],
  "queues": [
    {
      "name": "order.created.queue",
      "vhost": "/",
      "durable": true,
      "auto_delete": false,
      "arguments": {
        "x-dead-letter-exchange": "dlx.exchange",
        "x-dead-letter-routing-key": "order.created.dlq"
      }
    },
    {
      "name": "order.created.notification.queue",
      "vhost": "/",
      "durable": true,
      "auto_delete": false,
      "arguments": {}
    },
    {
      "name": "payment.processed.queue",
      "vhost": "/",
      "durable": true,
      "auto_delete": false,
      "arguments": {
        "x-dead-letter-exchange": "dlx.exchange",
        "x-dead-letter-routing-key": "payment.processed.dlq"
      }
    },
    {
      "name": "saga.customer.validate.queue",
      "vhost": "/",
      "durable": true,
      "auto_delete": false,
      "arguments": {}
    },
    {
      "name": "saga.payment.process.queue",
      "vhost": "/",
      "durable": true,
      "auto_delete": false,
      "arguments": {}
    },
    {
      "name": "saga.fleet.confirm.queue",
      "vhost": "/",
      "durable": true,
      "auto_delete": false,
      "arguments": {}
    },
    {
      "name": "saga.response.queue",
      "vhost": "/",
      "durable": true,
      "auto_delete": false,
      "arguments": {}
    },
    {
      "name": "order.created.dlq",
      "vhost": "/",
      "durable": true,
      "auto_delete": false,
      "arguments": {}
    },
    {
      "name": "payment.processed.dlq",
      "vhost": "/",
      "durable": true,
      "auto_delete": false,
      "arguments": {}
    }
  ],
  "bindings": [
    {
      "source": "order.exchange",
      "vhost": "/",
      "destination": "order.created.queue",
      "destination_type": "queue",
      "routing_key": "order.created",
      "arguments": {}
    },
    {
      "source": "order.exchange",
      "vhost": "/",
      "destination": "order.created.notification.queue",
      "destination_type": "queue",
      "routing_key": "order.created.#",
      "arguments": {}
    },
    {
      "source": "payment.exchange",
      "vhost": "/",
      "destination": "payment.processed.queue",
      "destination_type": "queue",
      "routing_key": "payment.processed",
      "arguments": {}
    },
    {
      "source": "saga.exchange",
      "vhost": "/",
      "destination": "saga.customer.validate.queue",
      "destination_type": "queue",
      "routing_key": "saga.customer.validate",
      "arguments": {}
    },
    {
      "source": "saga.exchange",
      "vhost": "/",
      "destination": "saga.payment.process.queue",
      "destination_type": "queue",
      "routing_key": "saga.payment.process",
      "arguments": {}
    },
    {
      "source": "saga.exchange",
      "vhost": "/",
      "destination": "saga.fleet.confirm.queue",
      "destination_type": "queue",
      "routing_key": "saga.fleet.confirm",
      "arguments": {}
    },
    {
      "source": "dlx.exchange",
      "vhost": "/",
      "destination": "order.created.dlq",
      "destination_type": "queue",
      "routing_key": "order.created.dlq",
      "arguments": {}
    },
    {
      "source": "dlx.exchange",
      "vhost": "/",
      "destination": "payment.processed.dlq",
      "destination_type": "queue",
      "routing_key": "payment.processed.dlq",
      "arguments": {}
    }
  ]
}
```

**Para verificar**: Abrir `http://localhost:15672` (user: guest / pass: guest) → pestaña Exchanges y Queues. Toda la topología debería estar creada.

---

## 5. Dockerización de Microservicios Spring Boot

### Referencia Rápida (Seniors)

```
Opción A: Dockerfile multi-stage (control total, recomendado)
Opción B: spring-boot:build-image (Buildpacks, zero config)
```

### Guía Detallada (Junior/Mid)

### 5.1. Dockerfile Multi-Stage (Recomendado)

✅ **Hacer**: Usar un build multi-stage que aprovecha layered jars de Spring Boot para máximo cache de Docker

```dockerfile
# Dockerfile (en cada servicio: reservation-service/Dockerfile, etc.)

# ============================================================
# Stage 1: BUILD — Compilar con Maven
# ============================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copiar archivos de Maven primero (se cachea si no cambian)
COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw ./
RUN chmod +x mvnw

# Descargar dependencias (se cachea mientras no cambie pom.xml)
RUN ./mvnw dependency:resolve -B

# Copiar código fuente y compilar
COPY src ./src
RUN ./mvnw package -DskipTests -B

# Extraer capas del JAR para cache óptimo
RUN java -Djarmode=layertools -jar target/*.jar extract --destination /app/extracted

# ============================================================
# Stage 2: RUNTIME — Imagen final mínima
# ============================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Crear usuario no-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copiar capas extraídas (orden de menor a mayor cambio)
COPY --from=builder /app/extracted/dependencies/ ./
COPY --from=builder /app/extracted/spring-boot-loader/ ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/application/ ./

# Usar usuario no-root
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "application.jar"]
```

**¿Por qué capas separadas?** Docker cachea cada instrucción `COPY`. Las dependencias Maven cambian raramente, así que esa capa se reutiliza. El código de tu aplicación cambia con cada commit, así que esa capa se reconstruye. Resultado: rebuilds de segundos en vez de minutos.

```
Capa 1: dependencies/           ← Cambia cuando agregas/actualizas dependencias (raro)
Capa 2: spring-boot-loader/     ← Cambia cuando actualizas Spring Boot (raro)
Capa 3: snapshot-dependencies/  ← Cambia cuando actualizas módulos internos
Capa 4: application/            ← Cambia con cada commit de código (frecuente)
```

### 5.2. Alternativa: Buildpacks (Zero Config)

Spring Boot 3.4 soporta Cloud Native Buildpacks out-of-the-box con el Maven Plugin:

```bash
# Construir imagen sin Dockerfile
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=acme/reservation-service:latest
```

| Aspecto | Dockerfile Multi-Stage | Buildpacks |
|---------|----------------------|------------|
| Control | Total | Limitado |
| Config | Manual (Dockerfile) | Zero config |
| Cache | Excelente (con layered jars) | Bueno |
| Base image | Tú eliges (alpine, distroless) | Predefinida (Paketo) |
| Tamaño imagen | ~200MB (JRE alpine) | ~300MB (Paketo) |
| Debugging | Fácil (entrar al container) | Más difícil |
| CI/CD | Requiere Docker daemon | Requiere Docker daemon |
| **Recomendado** | **Cuando necesitas control** | **Prototipos rápidos** |

### 5.3. Profile de Spring Boot para Docker

✅ **Hacer**: Crear un profile `docker` que use los nombres de servicio de Docker Compose como hosts

```yaml
# application-docker.yml (en cada servicio)
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  rabbitmq:
    host: ${SPRING_RABBITMQ_HOST:rabbitmq}
    port: ${SPRING_RABBITMQ_PORT:5672}
    username: ${SPRING_RABBITMQ_USERNAME:guest}
    password: ${SPRING_RABBITMQ_PASSWORD:guest}
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        default_schema: ${SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA:public}

server:
  port: 8080

logging:
  level:
    com.acme: DEBUG
    org.springframework.amqp: INFO
```

**¿Por qué funciona `rabbitmq` como host?** Dentro de la red Docker (`acme-network`), los contenedores se resuelven por el nombre del servicio definido en `docker-compose.yml`. El servicio se llama `rabbitmq`, así que `rabbitmq:5672` funciona como hostname.

---

## 6. Profiles de Docker Compose

### Referencia Rápida (Seniors)

```bash
docker compose --profile infra up -d     # Solo PostgreSQL + RabbitMQ
docker compose --profile full up -d      # Infra + 4 servicios
docker compose --profile debug up -d     # Infra + servicios con debug
docker compose --profile infra down      # Parar solo infra
```

### Guía Detallada (Junior/Mid)

✅ **Hacer**: Usar profiles para separar lo que levantas según tu modo de trabajo

| Profile | Qué incluye | Cuándo usarlo |
|---------|-------------|---------------|
| `infra` | PostgreSQL + RabbitMQ | Desarrollo en IDE (lo más común) |
| `full` | Infra + 4 servicios | Probar integración completa |
| `debug` | Infra + servicios con debug ports | Debuggear un servicio dockerizado |

**Flujo de desarrollo más común**:
1. `docker compose --profile infra up -d` — levanta PostgreSQL y RabbitMQ
2. Abrir IntelliJ/VS Code → ejecutar el servicio en el IDE con profile `default` o `local`
3. En `application-local.yml`, apuntar a `localhost:5432` y `localhost:5672`
4. Cuando necesites probar todo junto: `docker compose --profile full up -d`

---

## 7. Volúmenes y Persistencia

### Referencia Rápida (Seniors)

```bash
# Ver volúmenes
docker volume ls | grep acme

# Limpiar todo (datos + volúmenes)
docker compose --profile infra down -v

# Limpiar solo datos de PostgreSQL
docker volume rm proyecto_postgres_data
```

### Guía Detallada (Junior/Mid)

#### Named Volumes vs Bind Mounts

| Tipo | Ejemplo | Cuándo usarlo |
|------|---------|---------------|
| Named volume | `postgres_data:/var/lib/postgresql/data` | Datos persistentes gestionados por Docker |
| Bind mount | `./docker/postgres/init.sql:/docker-entrypoint-initdb.d/init.sql` | Ficheros de configuración que editas |

Los named volumes persisten entre reinicios del contenedor. Los bind mounts mapean archivos locales directamente.

#### Cómo Limpiar Todo para Empezar de Cero

```bash
# Parar todo y eliminar volúmenes
docker compose --profile infra down -v

# Verificar que no quedan volúmenes
docker volume ls | grep acme

# Volver a levantar (ejecutará init scripts de nuevo)
docker compose --profile infra up -d
```

> **Importante**: El init script de PostgreSQL (`init-schemas.sql`) solo se ejecuta cuando el volumen es nuevo. Si quieres re-ejecutar el script, debes eliminar el volumen primero con `-v`.

---

## 8. Desarrollo Local: Hot Reload y Debug

### Referencia Rápida (Seniors)

```
Modo 1 (recomendado): IDE + Docker infra
  → docker compose --profile infra up -d
  → Ejecutar servicio en IDE con profile=local
  → Hot reload via Spring DevTools automático

Modo 2: Todo en Docker + debug remoto
  → docker compose --profile debug up -d
  → IntelliJ: Run → Attach to Process → Remote JVM → localhost:5005
```

### Guía Detallada (Junior/Mid)

### 8.1. Modo Recomendado: IDE + Infraestructura en Docker

Este es el flujo más productivo para desarrollo:

```
┌────────────────┐     ┌─────────────────────────────────┐
│   Tu IDE       │     │     Docker Compose               │
│                │     │                                  │
│ reservation-   │────▶│ postgres   (localhost:5432)      │
│   service      │     │ rabbitmq   (localhost:5672)      │
│ (run local)    │     │            (localhost:15672 UI)  │
└────────────────┘     └─────────────────────────────────┘
```

**application-local.yml** (para correr en IDE):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/acme_platform?currentSchema=reservation
    username: reservation_user
    password: reservation_pass
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
  jpa:
    hibernate:
      ddl-auto: update
  devtools:
    restart:
      enabled: true
    livereload:
      enabled: true
```

### 8.2. Remote Debugging de Servicios Dockerizados

Cuando el servicio corre en Docker y necesitas debuggear:

1. Levantar con profile debug:
```bash
docker compose --profile debug up -d
```

2. En IntelliJ: `Run → Edit Configurations → + → Remote JVM Debug`
   - Host: `localhost`
   - Port: `5005` (reservation), `5006` (customer), etc.
   - Command line arguments: ya configurados en el compose vía `JAVA_TOOL_OPTIONS`

3. Poner breakpoints y attach al proceso

### 8.3. Ver Logs

```bash
# Logs de un servicio específico (follow mode)
docker compose logs -f reservation-service

# Logs de infraestructura
docker compose logs -f postgres rabbitmq

# Últimas 50 líneas de todos
docker compose logs --tail 50

# Logs con timestamps
docker compose logs -f -t reservation-service
```

---

## 9. Utilidades y Scripts Helper

### Referencia Rápida (Seniors)

```bash
make infra-up       # Levantar infra
make infra-down     # Parar infra
make full-up        # Levantar todo
make reset-db       # Reset base de datos
make status         # Estado de servicios
make logs           # Ver logs
```

### Guía Detallada (Junior/Mid)

#### Makefile

```makefile
# Makefile

.PHONY: infra-up infra-down full-up full-down debug-up reset-db status logs clean help

# ============================================================
# INFRAESTRUCTURA
# ============================================================

infra-up: ## Levantar PostgreSQL + RabbitMQ
	docker compose --profile infra up -d
	@echo "✅ Infra levantada"
	@echo "   PostgreSQL: localhost:5432"
	@echo "   RabbitMQ:   localhost:5672"
	@echo "   RabbitMQ UI: http://localhost:15672 (guest/guest)"

infra-down: ## Parar infraestructura (mantiene datos)
	docker compose --profile infra down
	@echo "✅ Infra detenida (datos preservados)"

# ============================================================
# FULL STACK
# ============================================================

full-up: ## Levantar todo (infra + 4 servicios)
	docker compose --profile full up -d --build
	@echo "✅ Stack completo levantado"

full-down: ## Parar todo
	docker compose --profile full down

# ============================================================
# DEBUG
# ============================================================

debug-up: ## Levantar con puertos de debug
	docker compose --profile debug up -d --build
	@echo "✅ Stack debug levantado"
	@echo "   Debug ports: 5005 (reservation)"

# ============================================================
# BASE DE DATOS
# ============================================================

reset-db: ## Reset completo de la base de datos
	docker compose --profile infra down -v
	docker compose --profile infra up -d
	@echo "⏳ Esperando a que PostgreSQL inicie..."
	@sleep 5
	@echo "✅ Base de datos reseteada con schemas limpios"

# ============================================================
# UTILIDADES
# ============================================================

status: ## Ver estado de todos los contenedores
	@echo "=== Contenedores ==="
	@docker compose ps -a
	@echo ""
	@echo "=== Volúmenes ==="
	@docker volume ls | grep -E "postgres|rabbitmq" || echo "  (ninguno)"
	@echo ""
	@echo "=== Red ==="
	@docker network ls | grep acme || echo "  (ninguna)"

logs: ## Ver logs de todos los servicios (follow)
	docker compose logs -f

logs-infra: ## Ver logs solo de infra
	docker compose logs -f postgres rabbitmq

clean: ## Eliminar TODO (contenedores, volúmenes, redes, imágenes)
	docker compose --profile full down -v --rmi local
	@echo "✅ Todo limpio"

help: ## Mostrar esta ayuda
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

# Default target
.DEFAULT_GOAL := help
```

---

## 10. Troubleshooting Común

### 10.1. Puerto ya en uso

```
Error: Bind for 0.0.0.0:5432 failed: port is already allocated
```

**Solución**:
```bash
# Ver qué proceso usa el puerto
# Linux/Mac:
lsof -i :5432
# Windows:
netstat -ano | findstr :5432

# Opción 1: Matar el proceso
kill <PID>

# Opción 2: Cambiar el puerto en docker-compose.yml
ports:
  - "5433:5432"   # Mapear al puerto 5433 del host
```

### 10.2. Contenedor no arranca

```bash
# Ver logs del contenedor que falla
docker compose logs postgres

# Ver eventos del contenedor
docker inspect acme-postgres --format='{{.State.Status}}'

# Entrar al contenedor para investigar
docker exec -it acme-postgres bash
```

### 10.3. Init script de PostgreSQL no se ejecutó

**Causa**: El volumen ya existe de una ejecución anterior. PostgreSQL solo ejecuta los init scripts cuando el directorio de datos está vacío.

```bash
# Eliminar volumen y recrear
docker compose --profile infra down -v
docker compose --profile infra up -d
```

### 10.4. RabbitMQ no tiene las queues esperadas

**Causa 1**: El `definitions.json` tiene un error de formato JSON.
```bash
# Validar JSON
python -m json.tool docker/rabbitmq/definitions.json
```

**Causa 2**: El fichero no se montó correctamente.
```bash
# Verificar que el fichero existe dentro del contenedor
docker exec acme-rabbitmq cat /etc/rabbitmq/definitions.json
```

**Causa 3**: El `rabbitmq.conf` no tiene la línea de carga.
```bash
# Verificar configuración
docker exec acme-rabbitmq cat /etc/rabbitmq/rabbitmq.conf
```

### 10.5. Servicio no conecta a PostgreSQL/RabbitMQ

**Causa**: El servicio arrancó antes de que la infra estuviera ready.

✅ **Solución**: Usar `depends_on` con `condition: service_healthy` (ya incluido en el compose de arriba).

```yaml
depends_on:
  postgres:
    condition: service_healthy  # Espera al health check
  rabbitmq:
    condition: service_healthy
```

**Causa alternativa**: El servicio usa `localhost` en vez del nombre del servicio Docker.

```yaml
# MAL (dentro de Docker)
SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/...

# BIEN (dentro de Docker)
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/...
```

Dentro de la red Docker, los contenedores se comunican por nombre de servicio, no por `localhost`.

---

## 11. Convención de Puertos

| Servicio | Puerto App | Puerto Debug | URL |
|----------|-----------|-------------|-----|
| reservation-service | 8081 | 5005 | http://localhost:8081 |
| customer-service | 8082 | 5006 | http://localhost:8082 |
| payment-service | 8083 | 5007 | http://localhost:8083 |
| fleet-service | 8084 | 5008 | http://localhost:8084 |
| PostgreSQL | 5432 | — | jdbc:postgresql://localhost:5432 |
| RabbitMQ (AMQP) | 5672 | — | amqp://localhost:5672 |
| RabbitMQ (UI) | 15672 | — | http://localhost:15672 |

---

## 12. Checklist de Configuración

### Estructura

- [ ] `docker-compose.yml` en la raíz del proyecto
- [ ] `docker/postgres/init-schemas.sql` con los 4 schemas y usuarios
- [ ] `docker/rabbitmq/definitions.json` con toda la topología
- [ ] `docker/rabbitmq/rabbitmq.conf` con `management.load_definitions`
- [ ] `.dockerignore` en cada servicio
- [ ] `Makefile` con targets helper

### Docker Compose

- [ ] PostgreSQL con health check (`pg_isready`)
- [ ] RabbitMQ con health check (`rabbitmq-diagnostics ping`)
- [ ] `depends_on` con `condition: service_healthy` en cada servicio
- [ ] Named volumes para datos persistentes (`postgres_data`, `rabbitmq_data`)
- [ ] Red interna (`acme-network`) para comunicación entre servicios
- [ ] Profiles configurados: `infra`, `full`, `debug`
- [ ] Variables de entorno para cada servicio (datasource, rabbitmq)

### PostgreSQL

- [ ] Init script idempotente (IF NOT EXISTS)
- [ ] Un schema por microservicio (reservation, customer, payment, fleet)
- [ ] Un usuario por servicio con permisos restringidos a su schema
- [ ] `ALTER DEFAULT PRIVILEGES` para tablas creadas por Flyway/Hibernate

### RabbitMQ

- [ ] `definitions.json` válido (validar con `python -m json.tool`)
- [ ] Exchanges, queues y bindings pre-definidos
- [ ] DLQ configuradas para las queues principales
- [ ] Management UI accesible en `http://localhost:15672`

### Microservicios

- [ ] Dockerfile multi-stage con cache de dependencias Maven
- [ ] Layered jars extraídos para cache óptimo de Docker
- [ ] `application-docker.yml` con variables de entorno
- [ ] `application-local.yml` apuntando a `localhost`
- [ ] Puerto 8080 interno, mapeado a 8081-8084 externamente
- [ ] Usuario no-root en el container (`appuser`)

### Verificación Final

- [ ] `docker compose --profile infra up -d` levanta PostgreSQL y RabbitMQ sin errores
- [ ] `pg_isready` reporta healthy en menos de 15 segundos
- [ ] RabbitMQ Management UI muestra exchanges y queues pre-definidas
- [ ] Servicio Spring Boot desde IDE conecta a `localhost:5432` y `localhost:5672`
- [ ] `docker compose --profile full up -d` levanta los 4 servicios sin errores
- [ ] `make status` muestra todos los contenedores healthy
- [ ] `make reset-db` elimina datos y recrea schemas correctamente

---

*Documento generado con Context7 - Docker Compose, PostgreSQL 16, RabbitMQ 3.13, Spring Boot 3.4.x*
