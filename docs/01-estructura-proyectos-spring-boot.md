# Área 1: Estructura de Proyectos Spring Boot 3.x con Maven

> **Audiencia**: Desarrolladores junior/mid (guía detallada) + Seniors (referencia rápida)
> **Stack**: Spring Boot 3.4.x, Java 17+, Maven 3.9+, PostgreSQL, RabbitMQ, Kubernetes

---

## 1. Estructura de Directorios Base

### Referencia Rápida (Seniors)

```
proyecto-service/
├── src/main/java/com/empresa/proyecto/
├── src/main/resources/
├── src/test/java/
└── pom.xml
```

### Guía Detallada (Junior/Mid)

✅ **Hacer**: Ubicar la clase principal `@SpringBootApplication` en el paquete raíz

**Por qué**: Spring Boot escanea componentes desde el paquete de la clase principal hacia abajo. Si está en un subpaquete, necesitarás configurar `@ComponentScan` manualmente.

**Ejemplo**:
```
src/main/java/
└── com/
    └── acme/
        └── inventario/
            ├── InventarioApplication.java    ← Clase principal AQUÍ
            ├── cliente/
            │   ├── Cliente.java
            │   ├── ClienteController.java
            │   ├── ClienteService.java
            │   └── ClienteRepository.java
            └── pedido/
                ├── Pedido.java
                ├── PedidoController.java
                ├── PedidoService.java
                └── PedidoRepository.java
```

```java
package com.acme.inventario;

@SpringBootApplication
public class InventarioApplication {
    public static void main(String[] args) {
        SpringApplication.run(InventarioApplication.class, args);
    }
}
```

❌ **Evitar**: Poner la clase principal en un paquete profundo como `com.acme.inventario.config`

**Por qué**: Componentes en `com.acme.inventario.cliente` no serán escaneados automáticamente.

---

## 2. Estructura de Resources

✅ **Hacer**: Organizar resources por tipo y propósito

**Ejemplo**:
```
src/main/resources/
├── application.yml                    ← Config principal
├── application-dev.yml                ← Perfil desarrollo
├── application-prod.yml               ← Perfil producción
├── application-test.yml               ← Perfil testing
├── db/
│   └── migration/                     ← Flyway/Liquibase
│       ├── V1__create_tables.sql
│       └── V2__add_indexes.sql
├── static/                            ← Archivos estáticos (si aplica)
├── templates/                         ← Thymeleaf (si aplica)
└── messages/                          ← i18n
    ├── messages.properties
    └── messages_es.properties
```

❌ **Evitar**: Mezclar scripts SQL, configs y archivos estáticos en el raíz de resources

---

## 3. Organización Package-by-Feature (Recomendado para Enterprise)

### Referencia Rápida

```
com.acme.inventario/
├── cliente/          ← Feature completa
├── pedido/           ← Feature completa
├── producto/         ← Feature completa
└── shared/           ← Código compartido
```

### Guía Detallada

✅ **Hacer**: Agrupar por feature/dominio, no por capa técnica

**Por qué**:
- Mejor cohesión: todo lo relacionado está junto
- Facilita microservicios: extraer un feature es copiar un paquete
- Reduce conflictos en equipos: cada dev trabaja en su feature

**Ejemplo Package-by-Feature**:
```
com.acme.inventario/
├── InventarioApplication.java
│
├── cliente/                           ← FEATURE: Cliente
│   ├── Cliente.java                   ← Entity
│   ├── ClienteDto.java                ← DTO
│   ├── ClienteMapper.java             ← Mapper
│   ├── ClienteController.java         ← REST API
│   ├── ClienteService.java            ← Lógica de negocio
│   ├── ClienteRepository.java         ← Acceso a datos
│   └── ClienteNotFoundException.java  ← Excepción específica
│
├── pedido/                            ← FEATURE: Pedido
│   ├── Pedido.java
│   ├── PedidoDto.java
│   ├── PedidoMapper.java
│   ├── PedidoController.java
│   ├── PedidoService.java
│   ├── PedidoRepository.java
│   └── EstadoPedido.java              ← Enum específico
│
└── shared/                            ← COMPARTIDO
    ├── config/
    │   ├── SecurityConfig.java
    │   ├── RabbitMQConfig.java
    │   └── OpenApiConfig.java
    ├── exception/
    │   ├── GlobalExceptionHandler.java
    │   └── ApiError.java
    └── util/
        └── DateUtils.java
```

❌ **Evitar**: Package-by-layer para proyectos medianos/grandes

**Anti-pattern**:
```
com.acme.inventario/
├── controller/        ← MALO: Todos los controllers juntos
│   ├── ClienteController.java
│   └── PedidoController.java
├── service/           ← MALO: Todos los services juntos
├── repository/
├── dto/
└── entity/
```

**Por qué evitarlo**:
- Alta dispersión: un cambio en "Cliente" toca 5+ paquetes
- Difícil extraer a microservicio
- Conflictos constantes en git

---

## 4. Ubicación de Componentes Específicos

### DTOs

✅ **Hacer**: DTOs dentro del feature que los usa

```
cliente/
├── ClienteDto.java              ← DTO request/response
├── ClienteCreateRequest.java    ← DTO específico creación
└── ClienteUpdateRequest.java    ← DTO específico actualización
```

❌ **Evitar**: Paquete global `dto/` con todos los DTOs

---

### Mappers

✅ **Hacer**: Mappers junto a las entities/DTOs que mapean

```
cliente/
├── Cliente.java
├── ClienteDto.java
└── ClienteMapper.java
```

```java
@Mapper(componentModel = "spring")
public interface ClienteMapper {
    ClienteDto toDto(Cliente entity);
    Cliente toEntity(ClienteDto dto);
}
```

---

### Configs

✅ **Hacer**: Configs globales en `shared/config/`, configs específicas en su feature

```
shared/config/
├── SecurityConfig.java          ← Global
├── JpaConfig.java               ← Global
└── RabbitMQConfig.java          ← Global

notificaciones/
└── NotificacionesRabbitConfig.java  ← Específica del feature
```

---

### Utils y Constants

✅ **Hacer**: Utils específicos en el feature, utils globales en `shared/util/`

```
shared/
├── util/
│   ├── DateUtils.java           ← Usado en múltiples features
│   └── StringUtils.java
└── constant/
    └── ApiConstants.java        ← Constantes globales API

cliente/
└── ClienteConstants.java        ← Constantes solo de cliente
```

---

## 5. Estructura Multi-Módulo Maven para Microservicios

### Referencia Rápida

```
acme-platform/
├── pom.xml                      ← Parent POM
├── acme-common/                ← Código compartido
├── acme-cliente-service/       ← Microservicio
├── acme-pedido-service/        ← Microservicio
└── acme-gateway/               ← API Gateway
```

### Guía Detallada

✅ **Hacer**: Parent POM que centraliza versiones y plugins

**Parent POM** (`acme-platform/pom.xml`):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.acme</groupId>
    <artifactId>acme-platform</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.1</version>
        <relativePath/>
    </parent>

    <modules>
        <module>acme-common</module>
        <module>acme-cliente-service</module>
        <module>acme-pedido-service</module>
        <module>acme-gateway</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
        <acme.version>1.0.0-SNAPSHOT</acme.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Módulos internos -->
            <dependency>
                <groupId>com.acme</groupId>
                <artifactId>acme-common</artifactId>
                <version>${acme.version}</version>
            </dependency>

            <!-- Librerías externas con versión centralizada -->
            <dependency>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct</artifactId>
                <version>${mapstruct.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

**Módulo Common** (`acme-common/pom.xml`):
```xml
<project>
    <parent>
        <groupId>com.acme</groupId>
        <artifactId>acme-platform</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>acme-common</artifactId>
    <packaging>jar</packaging>

    <!-- NO incluir spring-boot-maven-plugin aquí -->
    <!-- Este módulo es una librería, no un ejecutable -->
</project>
```

**Microservicio** (`acme-cliente-service/pom.xml`):
```xml
<project>
    <parent>
        <groupId>com.acme</groupId>
        <artifactId>acme-platform</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>acme-cliente-service</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.acme</groupId>
            <artifactId>acme-common</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 6. Estructura del Módulo Common

✅ **Hacer**: Solo código verdaderamente compartido, sin dependencias pesadas

```
acme-common/
└── src/main/java/com/acme/common/
    ├── dto/
    │   ├── ApiResponse.java         ← Response wrapper estándar
    │   └── PageResponse.java        ← Paginación estándar
    ├── exception/
    │   ├── BusinessException.java   ← Excepción base negocio
    │   └── ErrorCode.java           ← Enum códigos error
    ├── util/
    │   └── JsonUtils.java
    └── validation/
        └── ValidUUID.java           ← Validador custom
```

❌ **Evitar**: Poner entities, repositories o lógica de negocio en common

**Por qué**: Crea acoplamiento entre microservicios y complica deploys independientes.

---

## 7. Separación API vs Domain

✅ **Hacer**: Separar claramente capa de presentación (API) y dominio (negocio)

```
cliente/
├── api/                              ← CAPA API (presentación)
│   ├── ClienteController.java        ← REST endpoints
│   ├── ClienteRequest.java           ← DTOs entrada
│   └── ClienteResponse.java          ← DTOs salida
│
├── domain/                           ← CAPA DOMINIO (negocio)
│   ├── Cliente.java                  ← Entity
│   ├── ClienteService.java           ← Lógica negocio
│   └── ClienteRepository.java        ← Acceso datos
│
└── infrastructure/                   ← CAPA INFRAESTRUCTURA (opcional)
    ├── ClienteRabbitPublisher.java   ← Integración RabbitMQ
    └── ClienteExternalClient.java    ← Llamadas a otros servicios
```

**Regla de dependencias**: API → Domain ← Infrastructure

```java
// ClienteController.java (API)
@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;  // Depende de Domain

    @PostMapping
    public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteRequest request) {
        Cliente cliente = clienteService.crear(request.nombre(), request.email());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ClienteResponse.from(cliente));
    }
}
```

```java
// ClienteService.java (Domain)
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Cliente crear(String nombre, String email) {
        // Lógica de negocio pura, sin conocer HTTP
        Cliente cliente = new Cliente(nombre, email);
        cliente = clienteRepository.save(cliente);
        eventPublisher.publishEvent(new ClienteCreadoEvent(cliente));
        return cliente;
    }
}
```

---

## 8. Estructura para Kubernetes

✅ **Hacer**: Configurar health probes y graceful shutdown

**application.yml**:
```yaml
spring:
  application:
    name: cliente-service
  lifecycle:
    timeout-per-shutdown-phase: 30s  # Graceful shutdown

server:
  port: 8080
  shutdown: graceful                  # Habilitar graceful shutdown

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true                 # Habilita /actuator/health/liveness y /readiness
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true
```

**Kubernetes Deployment**:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cliente-service
spec:
  template:
    spec:
      containers:
        - name: cliente-service
          image: acme/cliente-service:1.0.0
          ports:
            - containerPort: 8080
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
            failureThreshold: 3
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "1000m"
```

---

## 9. Estructura de Tests

✅ **Hacer**: Espejar la estructura de main en test

```
src/
├── main/java/com/acme/inventario/
│   └── cliente/
│       ├── ClienteController.java
│       ├── ClienteService.java
│       └── ClienteRepository.java
│
└── test/java/com/acme/inventario/
    └── cliente/
        ├── ClienteControllerTest.java      ← Unit test
        ├── ClienteControllerIT.java        ← Integration test
        ├── ClienteServiceTest.java
        └── ClienteRepositoryIT.java
```

**Convención de nombres**:
- `*Test.java` → Unit tests (rápidos, sin Spring context)
- `*IT.java` → Integration tests (con Spring context, DB, etc.)

---

## Checklist Rápido

| Aspecto | ✅ Correcto | ❌ Incorrecto |
|---------|------------|---------------|
| Clase @SpringBootApplication | Paquete raíz | Subpaquete |
| Organización | Package-by-feature | Package-by-layer |
| DTOs | Junto al feature | Paquete global dto/ |
| Configs globales | shared/config/ | Dispersas |
| Multi-módulo | Parent POM con BOM | Versiones duplicadas |
| Common module | Solo utils/DTOs base | Entities/Repos |
| K8s probes | /actuator/health/* | Endpoint custom |
| Tests | Espejo de main + sufijo IT | Todo mezclado |

---

## Próximos Pasos

Este documento cubre el **Área 1: Estructura de Proyectos**.

Cuando estés listo, continuamos con el **Área 2: Organización de Paquetes Enterprise** que profundizará en:
- Package by layer vs package by feature (cuándo usar cada uno)
- Hexagonal/Clean Architecture en Spring Boot
- Código compartido entre microservicios

---

*Documento generado con Context7 - Documentación oficial Spring Boot 3.4.x*
