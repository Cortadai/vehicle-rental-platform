# Área 2: Organización de Paquetes Enterprise

> **Audiencia**: Desarrolladores junior/mid (guía detallada) + Seniors (referencia rápida)
> **Stack**: Spring Boot 3.4.x, Java 17+, Maven 3.9+, PostgreSQL, RabbitMQ, Kubernetes

---

## 1. Naming Conventions para Packages

### Referencia Rápida (Seniors)

```
com.<empresa>.<producto>.<modulo>.<capa>
com.acme.ecommerce.pedido.service
```

### Guía Detallada (Junior/Mid)

✅ **Hacer**: Seguir convención de dominio inverso + producto + módulo

**Estructura base**:
```
com.<empresa>.<producto>.<modulo>
│    │         │          │
│    │         │          └── Funcionalidad específica (pedido, cliente, inventario)
│    │         └── Nombre del sistema/producto (ecommerce, crm, erp)
│    └── Nombre de empresa (acme, miempresa)
└── Dominio inverso
```

**Ejemplos correctos**:
```
com.acme.ecommerce.pedido
com.acme.ecommerce.cliente
com.acme.ecommerce.shared.config
com.acme.crm.contacto
```

❌ **Evitar**:

| Anti-pattern | Problema |
|--------------|----------|
| `com.acme.controllers` | Muy genérico, mezcla todo |
| `pedido.service` | Sin dominio de empresa |
| `com.acme.ecommerce.PedidoService` | Clase en nombre de paquete |
| `com.acme.e_commerce` | Guiones bajos |
| `com.acme.ECommerce` | CamelCase en paquetes |

**Reglas**:
- Todo en **minúsculas**
- Sin guiones ni guiones bajos
- Sustantivos en **singular** (`pedido`, no `pedidos`)
- Máximo 3-4 niveles de profundidad

---

## 2. Capas y Stereotypes de Spring

### Referencia Rápida

| Capa | Stereotype | Responsabilidad |
|------|------------|-----------------|
| Presentación | `@Controller` / `@RestController` | HTTP, validación entrada |
| Servicio | `@Service` | Lógica de negocio |
| Persistencia | `@Repository` | Acceso a datos |
| Configuración | `@Configuration` | Beans y setup |
| Genérico | `@Component` | Otros componentes |

### Guía Detallada

#### @Controller / @RestController

✅ **Usar para**: Endpoints HTTP, manejo de requests/responses

```java
@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping
    public ResponseEntity<PedidoResponse> crear(@Valid @RequestBody CrearPedidoRequest request) {
        // Solo coordinación: validar → delegar → responder
        Pedido pedido = pedidoService.crear(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PedidoResponse.from(pedido));
    }
}
```

❌ **Evitar en Controller**: Lógica de negocio, acceso directo a Repository, transacciones

---

#### @Service

✅ **Usar para**: Lógica de negocio, orquestación, transacciones

```java
@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteService clienteService;
    private final InventarioService inventarioService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Pedido crear(CrearPedidoCommand command) {
        // Lógica de negocio
        Cliente cliente = clienteService.obtenerPorId(command.clienteId());
        inventarioService.reservar(command.items());

        Pedido pedido = new Pedido(cliente, command.items());
        pedido = pedidoRepository.save(pedido);

        eventPublisher.publishEvent(new PedidoCreadoEvent(pedido));
        return pedido;
    }
}
```

**Características**:
- Contiene reglas de negocio
- Coordina múltiples repositorios/servicios
- Maneja transacciones (`@Transactional`)
- Publica eventos de dominio

---

#### @Repository

✅ **Usar para**: Acceso a datos, queries personalizadas

```java
@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByClienteIdAndEstado(Long clienteId, EstadoPedido estado);

    @Query("SELECT p FROM Pedido p WHERE p.fechaCreacion >= :desde AND p.estado = :estado")
    List<Pedido> findRecientesPorEstado(
            @Param("desde") LocalDateTime desde,
            @Param("estado") EstadoPedido estado);
}
```

**Beneficio de @Repository**: Traducción automática de excepciones de persistencia a `DataAccessException`.

---

#### @Component

✅ **Usar para**: Componentes que no encajan en Service/Repository/Controller

```java
@Component
public class NotificacionEmailSender {
    // Infraestructura de envío de emails
}

@Component
public class PedidoPdfGenerator {
    // Generador de PDFs
}

@Component
public class MetricsCollector {
    // Recolector de métricas custom
}
```

❌ **Evitar**: Usar `@Component` para servicios de negocio (usar `@Service`) o DAOs (usar `@Repository`)

---

#### @Configuration

✅ **Usar para**: Definir beans, configuraciones de librerías externas

```java
@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue pedidosQueue() {
        return QueueBuilder.durable("pedidos.creados")
                .withArgument("x-dead-letter-exchange", "pedidos.dlx")
                .build();
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
```

---

## 3. Package-by-Layer vs Package-by-Feature

### Comparación Directa

| Aspecto | Package-by-Layer | Package-by-Feature |
|---------|------------------|-------------------|
| Cohesión | Baja (clases relacionadas dispersas) | Alta (todo junto) |
| Acoplamiento | Alto entre paquetes | Bajo entre paquetes |
| Navegación | Difícil (saltar entre paquetes) | Fácil (todo en un lugar) |
| Extracción a microservicio | Compleja | Simple (copiar paquete) |
| Equipos paralelos | Conflictos frecuentes | Aislamiento natural |
| Proyectos pequeños | Aceptable | Overkill |
| Proyectos medianos/grandes | No recomendado | Recomendado |

### Package-by-Layer (Cuándo usarlo)

✅ **Usar cuando**:
- Proyecto pequeño (< 10 entidades)
- Equipo de 1-2 personas
- Prototipo o MVP
- CRUD simple sin lógica de negocio compleja

```
com.acme.miapp/
├── controller/
│   ├── ClienteController.java
│   └── ProductoController.java
├── service/
│   ├── ClienteService.java
│   └── ProductoService.java
├── repository/
│   ├── ClienteRepository.java
│   └── ProductoRepository.java
├── entity/
│   ├── Cliente.java
│   └── Producto.java
└── dto/
    ├── ClienteDto.java
    └── ProductoDto.java
```

❌ **Evitar cuando**: Proyecto enterprise, múltiples equipos, potencial de crecer

---

### Package-by-Feature (Recomendado Enterprise)

✅ **Usar cuando**:
- Proyecto mediano/grande (> 10 entidades)
- Múltiples desarrolladores/equipos
- Potencial de extraer microservicios
- Dominios de negocio claros

```
com.acme.ecommerce/
├── EcommerceApplication.java
│
├── pedido/                          ← FEATURE COMPLETO
│   ├── Pedido.java                  ← Entity
│   ├── EstadoPedido.java            ← Enum
│   ├── LineaPedido.java             ← Entity relacionada
│   ├── PedidoRepository.java        ← Repository
│   ├── PedidoService.java           ← Service
│   ├── PedidoController.java        ← Controller
│   ├── CrearPedidoRequest.java      ← DTO entrada
│   ├── PedidoResponse.java          ← DTO salida
│   ├── PedidoMapper.java            ← Mapper
│   ├── PedidoNotFoundException.java ← Excepción específica
│   └── PedidoEventListener.java     ← Listener eventos
│
├── cliente/                         ← FEATURE COMPLETO
│   ├── Cliente.java
│   ├── ClienteRepository.java
│   ├── ClienteService.java
│   ├── ClienteController.java
│   └── ...
│
├── producto/                        ← FEATURE COMPLETO
│   └── ...
│
└── shared/                          ← CÓDIGO COMPARTIDO
    ├── config/
    ├── exception/
    ├── security/
    └── util/
```

**Beneficios**:
1. **Cohesión**: Todo lo de "Pedido" está junto
2. **Encapsulación**: Puedes hacer clases package-private
3. **Escalabilidad**: Extraer `pedido/` a microservicio = copiar paquete
4. **Equipos**: Equipo A trabaja en `pedido/`, Equipo B en `cliente/`

---

## 4. Hexagonal / Clean Architecture en Spring Boot

### Referencia Rápida

```
feature/
├── adapter/           ← Infraestructura (entrada/salida)
│   ├── in/           ← Adaptadores de entrada
│   │   └── web/      ← Controllers REST
│   └── out/          ← Adaptadores de salida
│       ├── persistence/  ← Repositories JPA
│       └── messaging/    ← RabbitMQ publishers
├── application/       ← Casos de uso (orquestación)
│   ├── port/         ← Interfaces (contratos)
│   │   ├── in/       ← Puertos de entrada
│   │   └── out/      ← Puertos de salida
│   └── service/      ← Implementación casos de uso
└── domain/           ← Modelo de dominio (puro)
    ├── model/        ← Entities, Value Objects
    └── event/        ← Eventos de dominio
```

### Cuándo Aplicarlo

✅ **Aplicar Hexagonal/Clean cuando**:
- Lógica de negocio compleja
- Necesitas testear dominio sin infraestructura
- Múltiples adaptadores (REST + GraphQL + gRPC)
- Equipo grande que necesita contratos claros
- Proyecto de larga duración (> 2 años)

❌ **NO aplicar cuando**:
- CRUD simple
- Proyecto pequeño o prototipo
- Equipo no familiarizado (curva de aprendizaje alta)
- Time-to-market crítico

### Ejemplo Práctico

```
com.acme.ecommerce.pedido/
│
├── adapter/
│   ├── in/
│   │   └── web/
│   │       ├── PedidoController.java
│   │       ├── CrearPedidoRequest.java
│   │       └── PedidoResponse.java
│   └── out/
│       ├── persistence/
│       │   ├── PedidoJpaRepository.java      ← Interface JPA
│       │   ├── PedidoRepositoryAdapter.java  ← Implementa puerto
│       │   └── PedidoJpaEntity.java          ← Entity JPA
│       └── messaging/
│           └── PedidoRabbitPublisher.java
│
├── application/
│   ├── port/
│   │   ├── in/
│   │   │   └── CrearPedidoUseCase.java       ← Interface
│   │   └── out/
│   │       ├── PedidoRepositoryPort.java     ← Interface
│   │       └── PedidoEventPublisherPort.java ← Interface
│   └── service/
│       └── PedidoApplicationService.java     ← Implementa UseCase
│
└── domain/
    ├── model/
    │   ├── Pedido.java                       ← Domain Entity (POJO puro)
    │   ├── LineaPedido.java
    │   └── EstadoPedido.java
    └── event/
        └── PedidoCreadoEvent.java
```

**Puerto de entrada** (caso de uso):
```java
// application/port/in/CrearPedidoUseCase.java
public interface CrearPedidoUseCase {
    Pedido ejecutar(CrearPedidoCommand command);
}
```

**Puerto de salida** (repositorio):
```java
// application/port/out/PedidoRepositoryPort.java
public interface PedidoRepositoryPort {
    Pedido guardar(Pedido pedido);
    Optional<Pedido> buscarPorId(PedidoId id);
}
```

**Implementación del caso de uso**:
```java
// application/service/PedidoApplicationService.java
@Service
@RequiredArgsConstructor
public class PedidoApplicationService implements CrearPedidoUseCase {

    private final PedidoRepositoryPort pedidoRepository;
    private final PedidoEventPublisherPort eventPublisher;

    @Override
    @Transactional
    public Pedido ejecutar(CrearPedidoCommand command) {
        // Lógica de aplicación
        Pedido pedido = Pedido.crear(command.clienteId(), command.items());
        pedido = pedidoRepository.guardar(pedido);
        eventPublisher.publicar(new PedidoCreadoEvent(pedido));
        return pedido;
    }
}
```

**Adaptador de persistencia**:
```java
// adapter/out/persistence/PedidoRepositoryAdapter.java
@Repository
@RequiredArgsConstructor
public class PedidoRepositoryAdapter implements PedidoRepositoryPort {

    private final PedidoJpaRepository jpaRepository;
    private final PedidoEntityMapper mapper;

    @Override
    public Pedido guardar(Pedido pedido) {
        PedidoJpaEntity entity = mapper.toJpaEntity(pedido);
        entity = jpaRepository.save(entity);
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<Pedido> buscarPorId(PedidoId id) {
        return jpaRepository.findById(id.valor())
                .map(mapper::toDomain);
    }
}
```

---

## 5. Código Compartido entre Microservicios

### Estrategias

| Estrategia | Cuándo usar | Ejemplo |
|------------|-------------|---------|
| **Módulo Maven común** | Misma empresa, mismo repo | `acme-common` |
| **Librería publicada** | Equipos independientes | `acme-utils:1.2.0` |
| **Copiar (duplicar)** | Mínimo compartido | DTOs de API |
| **No compartir** | Microservicios puros | Cada servicio autónomo |

### Módulo Common - Qué incluir

✅ **Incluir en common**:

```
acme-common/
└── src/main/java/com/acme/common/
    ├── dto/
    │   ├── ApiResponse.java          ← Wrapper respuestas
    │   ├── PageResponse.java         ← Paginación estándar
    │   └── ErrorResponse.java        ← Formato errores
    ├── exception/
    │   ├── BusinessException.java    ← Excepción base
    │   ├── NotFoundException.java    ← 404
    │   └── ValidationException.java  ← 400
    ├── validation/
    │   ├── ValidUUID.java            ← Validador custom
    │   └── UUIDValidator.java
    └── util/
        ├── JsonUtils.java
        └── DateTimeUtils.java
```

```java
// dto/ApiResponse.java
public record ApiResponse<T>(
        T data,
        String message,
        Instant timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, "OK", Instant.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(null, message, Instant.now());
    }
}
```

```java
// dto/PageResponse.java
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
```

❌ **NO incluir en common**:

| No incluir | Razón |
|------------|-------|
| Entities JPA | Acopla esquema de BD |
| Repositories | Cada servicio tiene su BD |
| Services de negocio | Lógica específica |
| Configuraciones Spring | Cada servicio configura distinto |
| Feign clients | Acoplamiento directo |

### DTOs de Comunicación entre Servicios

**Opción 1**: Módulo de API contracts (recomendado)

```
pedido-service-api/           ← Módulo solo con DTOs
└── src/main/java/
    └── com/acme/pedido/api/
        ├── PedidoCreadoEvent.java
        └── PedidoDto.java
```

Otros servicios dependen solo del módulo `-api`:
```xml
<dependency>
    <groupId>com.acme</groupId>
    <artifactId>pedido-service-api</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Opción 2**: Duplicar DTOs (para máxima independencia)

Cada servicio tiene su propia copia del DTO. Más código duplicado, pero cero acoplamiento.

---

## 6. Visibilidad de Paquetes (Package-Private)

✅ **Hacer**: Usar visibilidad package-private para encapsular

```java
// pedido/Pedido.java - Entity pública
public class Pedido { ... }

// pedido/PedidoRepository.java - Repository package-private
interface PedidoRepository extends JpaRepository<Pedido, Long> { ... }

// pedido/PedidoService.java - Service package-private
@Service
class PedidoService { ... }

// pedido/PedidoController.java - Controller público (expone API)
@RestController
public class PedidoController { ... }
```

**Beneficio**: Otros paquetes no pueden acceder directamente al Repository o Service, deben pasar por la API pública del módulo.

---

## Checklist de Organización

| Aspecto | ✅ Correcto | ❌ Incorrecto |
|---------|------------|---------------|
| Package naming | `com.acme.ecommerce.pedido` | `com.acme.Controllers` |
| Organización | Package-by-feature (enterprise) | Package-by-layer (enterprise) |
| @Service | Lógica de negocio | Solo delegar a repository |
| @Repository | Acceso a datos | Lógica de negocio |
| @Controller | Coordinación HTTP | Lógica de negocio |
| Hexagonal | Proyectos complejos | CRUD simple |
| Common module | DTOs, utils, exceptions | Entities, repos, services |
| Visibilidad | Package-private default | Todo public |

---

## Decisión: ¿Qué Arquitectura Elegir?

```
¿Proyecto enterprise con lógica compleja?
│
├─ NO → Package-by-layer simple
│
└─ SÍ → ¿Múltiples adaptadores (REST+GraphQL+Events)?
         │
         ├─ NO → Package-by-feature
         │
         └─ SÍ → Hexagonal Architecture
```

**Recomendación para ACME (consultoría enterprise)**:
- **Proyectos nuevos**: Package-by-feature
- **Proyectos con DDD/microservicios complejos**: Hexagonal
- **Legacy/mantenimiento**: Respetar estructura existente, migrar gradualmente

---

## Próximos Pasos

Este documento cubre el **Área 2: Organización de Paquetes Enterprise**.

Cuando estés listo, continuamos con el **Área 3: Manejo de Excepciones Enterprise-Grade** que incluirá:
- @ControllerAdvice y @ExceptionHandler patterns
- Custom exceptions hierarchy
- RFC 7807 Problem Details
- Exception handling en microservicios

---

*Documento generado con Context7 - Documentación oficial Spring Framework 6.2 y Spring Boot 3.4.x*
