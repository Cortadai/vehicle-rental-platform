# Área 12: Estrategias de Caché - Spring Cache + Redis

> **Audiencia**: Desarrolladores junior/mid (guía detallada) + Seniors (referencia rápida)
> **Stack**: Spring Boot 3.4.x, Java 17+, Redis 7.x, Caffeine

---

## 1. Spring Cache Abstraction

### Referencia Rápida (Seniors)

```xml
<!-- Dependencias -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

```yaml
# application.yml
spring:
  cache:
    type: redis
    cache-names: "customers,products,orders"
    redis:
      time-to-live: "10m"
      cache-null-values: true
      key-prefix: "app:"
      use-key-prefix: true
```

```java
@SpringBootApplication
@EnableCaching
public class EcommerceApplication { }
```

### Guía Detallada (Junior/Mid)

La abstracción de caché de Spring permite agregar caching a cualquier método con anotaciones, sin acoplarse a un proveedor específico (Redis, Caffeine, EhCache, etc.).

**Paso 1**: Habilitar caching con `@EnableCaching`

```java
package com.acme.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching  // Activa la infraestructura de caché
public class EcommerceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EcommerceApplication.class, args);
    }
}
```

**Por qué**: Sin `@EnableCaching`, las anotaciones `@Cacheable`, `@CacheEvict`, etc. son ignoradas silenciosamente. Spring Boot auto-configura el `CacheManager` según el proveedor detectado en el classpath.

**Paso 2**: Anotaciones principales

| Anotación | Propósito | Cuándo se ejecuta |
|-----------|-----------|-------------------|
| `@Cacheable` | Almacena el resultado en caché | Antes del método (si hay cache hit, no ejecuta) |
| `@CacheEvict` | Elimina entradas del caché | Después del método (por defecto) |
| `@CachePut` | Actualiza el caché sin interferir con la ejecución | Siempre ejecuta el método |
| `@Caching` | Combina múltiples operaciones de caché | Según las anotaciones contenidas |

**Paso 3**: Expresiones SpEL para claves

```java
// Clave simple: usa el parámetro directamente
@Cacheable(value = "productos", key = "#id")
public ProductoDto buscarPorId(Long id) { }

// Clave compuesta: combina múltiples parámetros
@Cacheable(value = "productos", key = "#categoria + '-' + #pagina")
public List<ProductoDto> buscarPorCategoria(String categoria, int pagina) { }

// Acceso a propiedades del objeto
@Cacheable(value = "productos", key = "#filtro.categoria")
public List<ProductoDto> buscar(ProductoFiltro filtro) { }

// Clave con método estático
@Cacheable(value = "productos", key = "T(java.util.Objects).hash(#nombre, #categoria)")
public List<ProductoDto> buscarPorNombreYCategoria(String nombre, String categoria) { }
```

**Paso 4**: Caché condicional

```java
// Solo cachear si el resultado no es null
@Cacheable(value = "productos", key = "#id", unless = "#result == null")
public ProductoDto buscarPorId(Long id) {
    return productoRepository.findById(id)
        .map(productoMapper::toDto)
        .orElse(null);
}

// Solo cachear si el precio es mayor a 0
@Cacheable(value = "productos", key = "#id", unless = "#result.precio <= 0")
public ProductoDto buscarProductoActivo(Long id) { }

// Condición para decidir si consultar el caché
@Cacheable(value = "productos", key = "#id", condition = "#id > 0")
public ProductoDto buscarPorIdPositivo(Long id) { }
```

> **Nota**: `condition` decide si se consulta/escribe el caché. `unless` decide si se escribe el resultado al caché después de ejecutar el método.

---

## 2. Caffeine (Caché Local)

### Referencia Rápida (Seniors)

```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=500,expireAfterWrite=5m
```

### Guía Detallada (Junior/Mid)

Caffeine es un caché en memoria de alto rendimiento para Java. Es ideal para:
- **Baja latencia**: Lecturas en nanosegundos (sin red)
- **Datasets pequeños**: Catálogos, configuraciones, datos de referencia
- **Caché por instancia**: Cada réplica del servicio tiene su propia copia
- **Desarrollo local**: No requiere infraestructura externa

```yaml
# application.yml
spring:
  cache:
    type: caffeine
    cache-names: "categorias,configuraciones,tipos-envio"
    caffeine:
      spec: maximumSize=500,expireAfterWrite=5m
```

**Opciones del spec string**:

| Opción | Descripción | Ejemplo |
|--------|-------------|---------|
| `maximumSize` | Máximo de entradas en caché | `maximumSize=1000` |
| `expireAfterWrite` | TTL desde la escritura | `expireAfterWrite=10m` |
| `expireAfterAccess` | TTL desde último acceso | `expireAfterAccess=5m` |
| `recordStats` | Habilitar estadísticas | `recordStats` |

**Ejemplo de uso con configuración programática**:

```java
package com.acme.ecommerce.shared.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@Profile("local")  // Solo en desarrollo local
public class CaffeineCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "categorias", "configuraciones", "tipos-envio"
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats());
        return cacheManager;
    }
}
```

> **Cuándo usar Caffeine vs Redis**: Usa Caffeine cuando tienes una sola instancia o cuando la consistencia entre réplicas no es crítica. Usa Redis cuando necesitas caché compartido entre múltiples instancias del servicio.

---

## 3. Redis (Caché Distribuido)

### Referencia Rápida (Seniors)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 4
```

### Guía Detallada (Junior/Mid)

Redis es un almacén de datos en memoria que permite caché distribuido: todas las instancias de tu servicio comparten el mismo caché.

**Paso 1**: Agregar la dependencia

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

Spring Boot incluye Lettuce como cliente Redis por defecto. Lettuce es no bloqueante y thread-safe.

**Paso 2**: Configurar la conexión Redis

```yaml
# application.yml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 16    # Conexiones activas máximas
          max-idle: 8       # Conexiones idle máximas
          min-idle: 4       # Conexiones idle mínimas (pre-calentadas)
          max-wait: 2000ms  # Tiempo máximo de espera por conexión
        shutdown-timeout: 200ms
```

**Por qué configurar el pool**: Sin pool, cada operación crea una nueva conexión TCP. Con pool, las conexiones se reutilizan, reduciendo latencia y carga en Redis.

**Paso 3**: Configurar el caché con Redis

```yaml
# application.yml
spring:
  cache:
    type: redis
    cache-names: "productos,clientes,pedidos"
    redis:
      time-to-live: "10m"          # TTL por defecto: 10 minutos
      cache-null-values: true       # Cachear nulls para evitar cache stampede
      key-prefix: "ecommerce:"     # Prefijo para todas las claves
      use-key-prefix: true          # Habilitar prefijo
```

**Paso 4**: Serialización JSON (recomendado sobre JDK)

```java
package com.acme.ecommerce.shared.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer jsonSerializer =
            new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(jsonSerializer))
            .prefixCacheNameWith("ecommerce:");

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .build();
    }
}
```

**Por qué JSON sobre JDK**: La serialización JDK por defecto produce datos binarios ilegibles en Redis CLI, es frágil ante cambios de clase (`serialVersionUID`), y no es interoperable con otros lenguajes. JSON es legible, debuggeable y más tolerante a cambios de esquema.

---

## 4. Patrón Cache-Aside (Implementación)

### Referencia Rápida (Seniors)

```java
@Cacheable(value = "productos", key = "#id", unless = "#result == null")
public ProductoDto buscarPorId(Long id) { }

@CacheEvict(value = "productos", key = "#id")
public ProductoDto actualizar(Long id, ActualizarProductoRequest request) { }

@Caching(evict = {
    @CacheEvict(value = "productos", key = "#id"),
    @CacheEvict(value = "producto-listas", allEntries = true)
})
public void eliminar(Long id) { }
```

### Guía Detallada (Junior/Mid)

El patrón Cache-Aside (o Lazy Loading) funciona así:
1. **Lectura**: Primero consulta el caché. Si hay hit, retorna. Si no, consulta la DB, almacena en caché y retorna.
2. **Escritura**: Escribe en la DB y luego invalida (evict) el caché.

```java
package com.acme.ecommerce.producto.domain;

import com.acme.ecommerce.producto.api.ActualizarProductoRequest;
import com.acme.ecommerce.producto.api.CrearProductoRequest;
import com.acme.ecommerce.producto.api.ProductoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;

    /**
     * Buscar producto por ID con caché.
     * - Si está en caché, retorna sin consultar DB
     * - Si no está, consulta DB, guarda en caché y retorna
     * - No cachea resultados null (evita caché de valores inexistentes)
     */
    @Cacheable(value = "productos", key = "#id", unless = "#result == null")
    @Transactional(readOnly = true)
    public ProductoDto buscarPorId(Long id) {
        log.debug("Cache MISS para producto id={}, consultando DB", id);
        return productoRepository.findById(id)
            .map(productoMapper::toDto)
            .orElse(null);
    }

    /**
     * Buscar productos por categoría con caché.
     * La clave combina categoría y página para resultados paginados.
     */
    @Cacheable(value = "producto-listas", key = "#categoria + '-' + #pagina")
    @Transactional(readOnly = true)
    public List<ProductoDto> buscarPorCategoria(String categoria, int pagina) {
        log.debug("Cache MISS para categoria={}, pagina={}", categoria, pagina);
        return productoRepository.findByCategoria(categoria, PageRequest.of(pagina, 20))
            .map(productoMapper::toDto)
            .getContent();
    }

    /**
     * Actualizar producto: escribe en DB e invalida caché.
     * Usa @CacheEvict para que la próxima lectura obtenga datos frescos.
     */
    @CacheEvict(value = "productos", key = "#id")
    @Transactional
    public ProductoDto actualizar(Long id, ActualizarProductoRequest request) {
        log.info("Actualizando producto id={}, invalidando caché", id);
        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new ProductoNoEncontradoException(id));

        producto.setNombre(request.nombre());
        producto.setPrecio(request.precio());
        producto.setDescripcion(request.descripcion());

        return productoMapper.toDto(productoRepository.save(producto));
    }

    /**
     * Crear producto: usa @CachePut para agregar al caché sin interferir.
     * A diferencia de @Cacheable, SIEMPRE ejecuta el método.
     */
    @CachePut(value = "productos", key = "#result.id")
    @Transactional
    public ProductoDto crear(CrearProductoRequest request) {
        log.info("Creando nuevo producto: {}", request.nombre());
        Producto producto = productoMapper.toEntity(request);
        producto = productoRepository.save(producto);
        return productoMapper.toDto(producto);
    }

    /**
     * Eliminar producto: invalida múltiples cachés.
     * - Invalida el caché individual del producto
     * - Invalida TODAS las listas de productos (pueden contener el producto eliminado)
     */
    @Caching(evict = {
        @CacheEvict(value = "productos", key = "#id"),
        @CacheEvict(value = "producto-listas", allEntries = true)
    })
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando producto id={}, invalidando cachés", id);
        if (!productoRepository.existsById(id)) {
            throw new ProductoNoEncontradoException(id);
        }
        productoRepository.deleteById(id);
    }

    /**
     * Forzar refresco del caché completo (operación administrativa).
     */
    @CacheEvict(value = {"productos", "producto-listas"}, allEntries = true)
    public void refrescarCache() {
        log.info("Caché de productos limpiado completamente");
    }
}
```

**Ejemplo para ClienteService**:

```java
package com.acme.ecommerce.cliente.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;

    @Cacheable(value = "clientes", key = "#id", unless = "#result == null")
    @Transactional(readOnly = true)
    public ClienteDto buscarPorId(Long id) {
        return clienteRepository.findById(id)
            .map(clienteMapper::toDto)
            .orElse(null);
    }

    @Cacheable(value = "clientes", key = "'email-' + #email", unless = "#result == null")
    @Transactional(readOnly = true)
    public ClienteDto buscarPorEmail(String email) {
        return clienteRepository.findByEmail(email)
            .map(clienteMapper::toDto)
            .orElse(null);
    }

    @CacheEvict(value = "clientes", key = "#id")
    @Transactional
    public ClienteDto actualizar(Long id, ActualizarClienteRequest request) {
        Cliente cliente = clienteRepository.findById(id)
            .orElseThrow(() -> new ClienteNoEncontradoException(id));
        cliente.setNombre(request.nombre());
        cliente.setTelefono(request.telefono());
        return clienteMapper.toDto(clienteRepository.save(cliente));
    }
}
```

---

## 5. RedisCacheManager Custom con TTL por Caché

### Referencia Rápida (Seniors)

```java
@Bean
public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
    RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(10))
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer()));

    Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
        "productos", defaultConfig.entryTtl(Duration.ofHours(1)),
        "clientes", defaultConfig.entryTtl(Duration.ofMinutes(30)),
        "pedidos", defaultConfig.entryTtl(Duration.ofMinutes(5))
    );

    return RedisCacheManager.builder(factory)
        .cacheDefaults(defaultConfig)
        .withInitialCacheConfigurations(cacheConfigs)
        .build();
}
```

### Guía Detallada (Junior/Mid)

Diferentes tipos de datos tienen diferentes necesidades de frescura. Un catálogo de productos puede cachearse por horas, pero los pedidos necesitan datos más recientes.

```java
package com.acme.ecommerce.shared.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Serializer JSON para valores legibles en Redis
        GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();

        // Configuración por defecto: TTL 10 minutos
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(jsonSerializer))
            .prefixCacheNameWith("ecommerce:");

        // TTL personalizado por caché
        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
            // Productos: cambian poco, TTL largo
            "productos", defaultConfig.entryTtl(Duration.ofHours(1)),
            // Listas de productos: invalidadas frecuentemente
            "producto-listas", defaultConfig.entryTtl(Duration.ofMinutes(15)),
            // Clientes: datos semi-estáticos
            "clientes", defaultConfig.entryTtl(Duration.ofMinutes(30)),
            // Pedidos: datos que cambian frecuentemente
            "pedidos", defaultConfig.entryTtl(Duration.ofMinutes(5)),
            // Categorías: datos muy estáticos
            "categorias", defaultConfig.entryTtl(Duration.ofHours(6)),
            // Configuraciones: casi nunca cambian
            "configuraciones", defaultConfig.entryTtl(Duration.ofHours(24))
        );

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .transactionAware()  // Sincronizar eviction con transacciones DB
            .build();
    }

    private GenericJackson2JsonRedisSerializer createJsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }
}
```

**Por qué `.transactionAware()`**: Sincroniza las operaciones de caché con las transacciones de Spring. Si un `@Transactional` hace rollback, el `@CacheEvict` también se deshace, evitando inconsistencias entre caché y DB.

**Criterios para definir TTL por caché**:

| Caché | TTL | Razón |
|-------|-----|-------|
| `productos` | 1 hora | Catálogo cambia poco, alta tasa de lectura |
| `producto-listas` | 15 min | Listas pueden volverse obsoletas con nuevos productos |
| `clientes` | 30 min | Datos del perfil cambian con poca frecuencia |
| `pedidos` | 5 min | Estado cambia frecuentemente (procesando, enviado, etc.) |
| `categorias` | 6 horas | Datos maestros, casi estáticos |
| `configuraciones` | 24 horas | Cambian solo por deploy o admin manual |

---

## 6. Caché Multi-Nivel (L1 Caffeine + L2 Redis)

### Referencia Rápida (Seniors)

```java
@Bean
public CacheManager cacheManager(RedisConnectionFactory redisFactory) {
    CaffeineCacheManager l1 = new CaffeineCacheManager();
    l1.setCaffeine(Caffeine.newBuilder().maximumSize(200).expireAfterWrite(2, TimeUnit.MINUTES));

    RedisCacheManager l2 = RedisCacheManager.builder(redisFactory)
        .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(30)))
        .build();

    return new CompositeCacheManager(l1, l2);
}
```

### Guía Detallada (Junior/Mid)

El patrón multi-nivel combina las ventajas de ambos cachés:
- **L1 (Caffeine)**: Ultra rápido (nanosegundos), local a cada instancia
- **L2 (Redis)**: Compartido entre instancias, sobrevive a reinicios

**Flujo de lectura**: L1 hit -> retorna | L1 miss -> L2 hit -> llena L1, retorna | L2 miss -> DB -> llena L1 y L2, retorna

```java
package com.acme.ecommerce.shared.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class MultiLevelCacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // L1: Caffeine - caché local rápido
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(200)                        // Max 200 entradas por caché
            .expireAfterWrite(2, TimeUnit.MINUTES)   // TTL corto: 2 minutos
            .recordStats());

        // L2: Redis - caché distribuido
        RedisCacheConfiguration redisConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        RedisCacheManager redisCacheManager = RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(redisConfig)
            .build();

        // Composite: intenta L1 primero, luego L2
        return new CompositeCacheManager(caffeineCacheManager, redisCacheManager);
    }
}
```

**Consideraciones de consistencia**:

1. **L1 puede tener datos obsoletos**: Cuando una instancia invalida el caché en Redis (L2), las otras instancias aún tendrán datos viejos en su Caffeine (L1) hasta que expire el TTL de L1.
2. **TTL de L1 debe ser corto**: Usa 1-2 minutos máximo para L1 y minimizar la ventana de inconsistencia.
3. **Para consistencia estricta**: Implementa invalidación vía Redis Pub/Sub (ver sección 7).

> **Cuándo usar multi-nivel**: APIs con alta tasa de lectura donde el mismo dato se pide múltiples veces por segundo (ej: página de producto en un e-commerce con alto tráfico).

---

## 7. Estrategias de Invalidación de Caché

### Referencia Rápida (Seniors)

| Estrategia | Mecanismo | Caso de uso |
|-----------|-----------|-------------|
| TTL | `entryTtl()` | Datos que pueden ser eventualmente consistentes |
| Eviction explícito | `@CacheEvict` | Escrituras conocidas |
| Pub/Sub | Redis channels | Multi-instancia, L1 invalidation |
| Cache warming | `@PostConstruct` / `@Scheduled` | Datos críticos que no deben tener cold start |

### Guía Detallada (Junior/Mid)

#### 7.1 Invalidación por TTL

La forma más simple: las entradas expiran automáticamente después de un tiempo.

```yaml
spring:
  cache:
    redis:
      time-to-live: "10m"  # Todas las entradas expiran en 10 minutos
```

✅ **Hacer**: Definir un TTL razonable según la volatilidad de los datos.

❌ **Evitar**: TTL infinito o sin TTL. Siempre debe haber un tiempo de expiración como red de seguridad.

**Ventaja**: No requiere lógica adicional. **Desventaja**: Datos pueden estar obsoletos hasta que expire el TTL.

#### 7.2 Invalidación Basada en Eventos (@CacheEvict)

Invalida el caché explícitamente cuando se sabe que los datos cambiaron.

```java
@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final PedidoMapper pedidoMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Cacheable(value = "pedidos", key = "#id", unless = "#result == null")
    @Transactional(readOnly = true)
    public PedidoDto buscarPorId(Long id) {
        return pedidoRepository.findById(id)
            .map(pedidoMapper::toDto)
            .orElse(null);
    }

    @CacheEvict(value = "pedidos", key = "#id")
    @Transactional
    public PedidoDto actualizarEstado(Long id, EstadoPedido nuevoEstado) {
        Pedido pedido = pedidoRepository.findById(id)
            .orElseThrow(() -> new PedidoNoEncontradoException(id));

        pedido.setEstado(nuevoEstado);
        pedido = pedidoRepository.save(pedido);

        // Publicar evento para que otros servicios invaliden sus cachés
        eventPublisher.publishEvent(new PedidoActualizadoEvent(pedido.getId(), nuevoEstado));

        return pedidoMapper.toDto(pedido);
    }
}
```

#### 7.3 Pub/Sub para Invalidación Multi-Instancia

Cuando múltiples instancias tienen caché L1 local, necesitas notificar a todas cuando cambia un dato.

```java
package com.acme.ecommerce.shared.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationPublisher {

    private static final String CACHE_INVALIDATION_CHANNEL = "cache:invalidation";

    private final StringRedisTemplate redisTemplate;

    /**
     * Publica un mensaje de invalidación que todas las instancias recibirán.
     */
    public void publishInvalidation(String cacheName, String key) {
        String message = cacheName + ":" + key;
        redisTemplate.convertAndSend(CACHE_INVALIDATION_CHANNEL, message);
        log.debug("Cache invalidation publicada: {}", message);
    }

    /**
     * Invalida todas las entradas de un caché.
     */
    public void publishFullInvalidation(String cacheName) {
        String message = cacheName + ":*";
        redisTemplate.convertAndSend(CACHE_INVALIDATION_CHANNEL, message);
        log.debug("Full cache invalidation publicada: {}", message);
    }
}

@Component
@RequiredArgsConstructor
@Slf4j
class CacheInvalidationListener implements MessageListener {

    private final CacheManager cacheManager;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody());
        String[] parts = payload.split(":", 2);

        if (parts.length == 2) {
            String cacheName = parts[0];
            String key = parts[1];

            if ("*".equals(key)) {
                // Limpiar todo el caché local
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    log.debug("Caché local '{}' limpiado completamente", cacheName);
                }
            } else {
                // Limpiar entrada específica del caché local
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.evict(key);
                    log.debug("Entrada '{}' evicted del caché local '{}'", key, cacheName);
                }
            }
        }
    }
}
```

**Configuración del listener Redis Pub/Sub**:

```java
package com.acme.ecommerce.shared.config;

import com.acme.ecommerce.shared.cache.CacheInvalidationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisPubSubConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            CacheInvalidationListener cacheInvalidationListener) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
            cacheInvalidationListener,
            new ChannelTopic("cache:invalidation")
        );
        return container;
    }
}
```

#### 7.4 Cache Warming (Pre-carga)

Cargar datos frecuentemente accedidos al iniciar la aplicación para evitar cold starts.

```java
package com.acme.ecommerce.shared.cache;

import com.acme.ecommerce.producto.domain.CategoriaService;
import com.acme.ecommerce.producto.domain.ProductoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheWarmer {

    private final CategoriaService categoriaService;
    private final ProductoService productoService;

    /**
     * Pre-cargar cachés al iniciar la aplicación.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUpOnStartup() {
        log.info("Iniciando cache warming...");

        // Cargar todas las categorías (datos maestros)
        categoriaService.listarTodas();

        // Cargar los 50 productos más vendidos
        productoService.buscarMasVendidos(50);

        log.info("Cache warming completado");
    }

    /**
     * Re-calentar caché de categorías periódicamente.
     */
    @Scheduled(fixedRate = 3600000)  // Cada hora
    public void warmUpCategorias() {
        log.debug("Re-calentando caché de categorías");
        categoriaService.refrescarCache();
        categoriaService.listarTodas();
    }
}
```

---

## 8. Testing de Caché

### Referencia Rápida (Seniors)

```java
@SpringBootTest
@AutoConfigureCache  // Reemplaza CacheManager con NoOpCacheManager
class ProductoServiceTest { }

// Con Testcontainers para integration test real
@Testcontainers
@SpringBootTest
class ProductoServiceCacheIT {
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);
}
```

### Guía Detallada (Junior/Mid)

#### 8.1 Unit Tests sin Caché (caché deshabilitado)

Para unit tests, generalmente quieres probar la lógica de negocio sin la interferencia del caché.

```java
package com.acme.ecommerce.producto.domain;

import org.springframework.boot.test.autoconfigure.cache.AutoConfigureCache;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureCache  // Usa NoOpCacheManager: el caché no interfiere con los tests
class ProductoServiceTest {

    @Autowired
    private ProductoService productoService;

    @MockitoBean
    private ProductoRepository productoRepository;

    @MockitoBean
    private ProductoMapper productoMapper;

    @Test
    void buscarPorId_debeConsultarRepositorio() {
        // Arrange
        Producto producto = new Producto(1L, "Laptop", 999.99);
        ProductoDto dto = new ProductoDto(1L, "Laptop", 999.99);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(productoMapper.toDto(producto)).thenReturn(dto);

        // Act
        ProductoDto resultado = productoService.buscarPorId(1L);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.nombre()).isEqualTo("Laptop");
        verify(productoRepository).findById(1L);
    }
}
```

#### 8.2 Integration Tests con Caché Real (verificar comportamiento del caché)

```java
package com.acme.ecommerce.producto.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
class ProductoServiceCacheTest {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private ProductoRepository productoRepository;

    @MockitoBean
    private ProductoMapper productoMapper;

    @BeforeEach
    void limpiarCache() {
        cacheManager.getCache("productos").clear();
        cacheManager.getCache("producto-listas").clear();
    }

    @Test
    void buscarPorId_debeUsarCacheEnSegundaLlamada() {
        // Arrange
        Producto producto = new Producto(1L, "Laptop", 999.99);
        ProductoDto dto = new ProductoDto(1L, "Laptop", 999.99);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(productoMapper.toDto(producto)).thenReturn(dto);

        // Act - primera llamada: cache miss
        productoService.buscarPorId(1L);
        // Act - segunda llamada: cache hit
        productoService.buscarPorId(1L);

        // Assert - el repositorio solo se llamó una vez (segunda fue cache hit)
        verify(productoRepository, times(1)).findById(1L);
    }

    @Test
    void actualizar_debeInvalidarCache() {
        // Arrange
        Producto producto = new Producto(1L, "Laptop", 999.99);
        ProductoDto dto = new ProductoDto(1L, "Laptop", 999.99);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(productoMapper.toDto(producto)).thenReturn(dto);

        // Llenar caché
        productoService.buscarPorId(1L);

        // Verificar que está en caché
        assertThat(cacheManager.getCache("productos").get(1L)).isNotNull();

        // Act - actualizar invalida el caché
        productoService.actualizar(1L, new ActualizarProductoRequest("Laptop Pro", 1299.99, "Desc"));

        // Assert - caché fue invalidado
        assertThat(cacheManager.getCache("productos").get(1L)).isNull();
    }

    @Test
    void eliminar_debeInvalidarMultiplesCaches() {
        // Arrange
        Producto producto = new Producto(1L, "Laptop", 999.99);
        ProductoDto dto = new ProductoDto(1L, "Laptop", 999.99);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(productoRepository.existsById(1L)).thenReturn(true);
        when(productoMapper.toDto(producto)).thenReturn(dto);

        // Llenar cachés
        productoService.buscarPorId(1L);

        // Act
        productoService.eliminar(1L);

        // Assert
        assertThat(cacheManager.getCache("productos").get(1L)).isNull();
    }
}
```

#### 8.3 Integration Tests con Testcontainers + Redis

```java
package com.acme.ecommerce.producto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class ProductoServiceCacheIT {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private ProductoService productoService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void debeAlmacenarEnRedis() {
        // Act
        productoService.buscarPorId(1L);

        // Assert - verificar que la clave existe en Redis
        var keys = redisTemplate.keys("ecommerce:productos*");
        assertThat(keys).isNotEmpty();
    }

    @Test
    void ttlDebeEstarConfigurado() {
        // Act
        productoService.buscarPorId(1L);

        // Assert - verificar que el TTL está configurado
        var keys = redisTemplate.keys("ecommerce:productos*");
        for (String key : keys) {
            Long ttl = redisTemplate.getExpire(key);
            assertThat(ttl).isGreaterThan(0);  // TTL activo
        }
    }
}
```

---

## 9. Monitoreo y Métricas de Caché

### Referencia Rápida (Seniors)

```yaml
spring:
  cache:
    redis:
      enable-statistics: true

management:
  endpoints:
    web:
      exposure:
        include: health,prometheus,caches
  metrics:
    tags:
      application: ecommerce-service
```

### Guía Detallada (Junior/Mid)

El monitoreo de caché es esencial para detectar problemas como cache miss rate alto, memoria excesiva o TTLs inadecuados.

**Paso 1**: Habilitar estadísticas de caché

```yaml
# application.yml
spring:
  cache:
    redis:
      enable-statistics: true  # Expone métricas vía Micrometer

management:
  endpoints:
    web:
      exposure:
        include: health,prometheus,caches,metrics
  endpoint:
    caches:
      enabled: true  # Endpoint /actuator/caches
  metrics:
    tags:
      application: ${spring.application.name}
```

**Paso 2**: Dependencia de Micrometer

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**Paso 3**: Métricas disponibles

| Métrica | Descripción | Alerta sugerida |
|---------|-------------|-----------------|
| `cache.gets{result=hit}` | Cache hits | - |
| `cache.gets{result=miss}` | Cache misses | Hit rate < 80% |
| `cache.puts` | Escrituras al caché | - |
| `cache.evictions` | Evictions (por TTL o capacidad) | Pico inusual |
| `cache.size` | Número de entradas (Caffeine) | > 90% capacidad |

**Paso 4**: Dashboard de ejemplo (consultas Prometheus)

```promql
# Hit rate por caché (debería ser > 80%)
sum(rate(cache_gets_total{result="hit"}[5m])) by (cache)
/
sum(rate(cache_gets_total[5m])) by (cache) * 100

# Evictions por minuto
rate(cache_evictions_total[5m]) * 60

# Latencia de operaciones Redis
histogram_quantile(0.95, rate(lettuce_command_completion_seconds_bucket[5m]))
```

**Paso 5**: Health check de Redis

```yaml
management:
  health:
    redis:
      enabled: true  # Incluye Redis en /actuator/health
```

```json
// GET /actuator/health
{
  "status": "UP",
  "components": {
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.2.4"
      }
    }
  }
}
```

---

## ✅ Hacer

1. **Usar `@EnableCaching`** - Activar la infraestructura de caché explícitamente
2. **Serializar con JSON** - `GenericJackson2JsonRedisSerializer` sobre serialización JDK
3. **Definir TTLs por caché** - Cada tipo de dato tiene su propia necesidad de frescura
4. **Usar `unless = "#result == null"`** - Evitar cachear nulls innecesariamente
5. **Invalidar en escrituras** - `@CacheEvict` en métodos de update/delete
6. **Configurar connection pool** - Lettuce pool para reutilizar conexiones TCP
7. **Habilitar estadísticas** - `enable-statistics: true` para monitoreo con Micrometer
8. **Usar `key-prefix`** - Evitar colisiones entre microservicios que comparten Redis
9. **Tests de caché** - Verificar hit/miss y eviction con tests de integración
10. **Cache warming** - Pre-cargar datos críticos al inicio para evitar cold starts
11. **`.transactionAware()`** - Sincronizar cache eviction con transacciones de DB
12. **Testcontainers para IT** - Redis real en tests de integración

## ❌ Evitar

1. **Cachear todo** - Solo cachear datos con alta tasa de lectura y baja tasa de escritura
2. **TTL infinito** - Siempre definir un TTL, incluso si es largo (24h)
3. **Serialización JDK** - Es frágil, no legible y no interoperable
4. **Claves sin prefijo** - Múltiples servicios en el mismo Redis causarán colisiones
5. **Ignorar cache stampede** - Cuando expira una clave popular, múltiples threads consultan la DB simultáneamente
6. **`@Cacheable` en métodos void** - No tiene sentido, `@Cacheable` necesita un valor de retorno
7. **Caché como fuente de verdad** - El caché puede perderse; la DB es la fuente de verdad
8. **TTLs iguales para todo** - Un catálogo y un pedido no tienen la misma volatilidad
9. **Olvidar limpiar caché en tests** - Cada test debe empezar con caché limpio (`@BeforeEach`)
10. **Cachear datos sensibles** - Tokens, passwords o PII no deben estar en Redis sin cifrado
11. **Ignorar métricas** - Un hit rate bajo indica que el caché no es efectivo
12. **L1 con TTL largo** - En multi-instancia, L1 largo causa inconsistencia entre réplicas

---

## Checklist de Implementación

### Configuración Base
- [ ] `spring-boot-starter-cache` como dependencia
- [ ] `spring-boot-starter-data-redis` como dependencia
- [ ] `@EnableCaching` en la clase de configuración
- [ ] `spring.cache.type: redis` en application.yml
- [ ] `key-prefix` configurado para evitar colisiones
- [ ] `use-key-prefix: true` habilitado

### Redis
- [ ] Host, port y password configurados con variables de entorno
- [ ] Lettuce connection pool configurado (`max-active`, `max-idle`, `min-idle`)
- [ ] Timeout de conexión definido
- [ ] Health check de Redis habilitado

### Serialización
- [ ] `GenericJackson2JsonRedisSerializer` configurado (no JDK)
- [ ] `JavaTimeModule` registrado en el ObjectMapper
- [ ] `StringRedisSerializer` para claves

### TTL y Eviction
- [ ] TTL por defecto definido (ej: 10 minutos)
- [ ] TTL personalizado por caché según volatilidad
- [ ] `@CacheEvict` en métodos de escritura (update/delete)
- [ ] `@Caching` para invalidar múltiples cachés relacionados
- [ ] `.transactionAware()` en el CacheManager

### Patrón Cache-Aside
- [ ] `@Cacheable` con `unless = "#result == null"` en lecturas
- [ ] `@CacheEvict` en actualizaciones
- [ ] `@CachePut` donde aplique (creación de entidades)
- [ ] Claves de caché bien definidas con SpEL

### Monitoreo
- [ ] `spring.cache.redis.enable-statistics: true`
- [ ] Endpoint `/actuator/caches` habilitado
- [ ] Métricas Prometheus configuradas
- [ ] Alertas para hit rate < 80%
- [ ] Dashboard con hit rate, evictions y latencia

### Testing
- [ ] Tests unitarios con `@AutoConfigureCache` (caché deshabilitado)
- [ ] Tests de integración verificando cache hit/miss
- [ ] Tests de eviction (verificar que `@CacheEvict` limpia el caché)
- [ ] Testcontainers con Redis para tests IT
- [ ] `@BeforeEach` limpiando cachés entre tests

---

*Documento generado con Context7 - Spring Boot 3.4.x + Redis 7.x*
