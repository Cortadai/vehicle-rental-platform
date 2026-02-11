# Area 16: Virtual Threads - Java 21+ y Spring Boot 3.4

> **Audiencia**: Desarrolladores junior/mid (guia detallada) + Seniors (referencia rapida)
> **Stack**: Spring Boot 3.4.x, Java 21+, Virtual Threads (Project Loom)

---

## 1. Que son los Virtual Threads

### Referencia Rapida (Seniors)

Virtual Threads (Project Loom) son threads ligeros gestionados por la JVM, no por el sistema operativo. En Spring Boot 3.4, se habilitan con una sola propiedad y transforman automaticamente Tomcat, `@Async`, `@Scheduled` y otros componentes.

### Comparativa

| Aspecto | Platform Threads | Virtual Threads |
|---------|-----------------|-----------------|
| Creacion | ~1MB stack, costoso | ~pocos KB, muy barato |
| Cantidad | ~miles (limitado por OS) | ~millones |
| Scheduling | OS scheduler | JVM scheduler |
| Blocking | Bloquea platform thread | Desmonta del carrier thread |
| Uso ideal | CPU-bound | I/O-bound |
| Pool tuning | Necesario y critico | No necesario |
| Daemon | Configurable | Siempre daemon |

### Guia Detallada (Junior/Mid)

En una aplicacion e-commerce tipica, un request para obtener el dashboard de un cliente necesita:
1. Consultar datos del cliente en PostgreSQL (~50ms)
2. Llamar al servicio de pedidos via HTTP (~100ms)
3. Consultar el credito disponible (~30ms)
4. Leer preferencias de Redis (~5ms)

Con platform threads, cada request ocupa un thread del pool durante ~185ms. Con un pool de 200 threads, solo puedes manejar ~1,080 requests/segundo.

Con virtual threads, cuando el thread hace I/O, se **desmonta** del carrier thread. Ese carrier queda libre para ejecutar otro virtual thread. Resultado: decenas de miles de requests concurrentes con el mismo hardware.

---

## 2. Habilitacion en Spring Boot 3.4

### Referencia Rapida (Seniors)

```yaml
spring:
  threads:
    virtual:
      enabled: true
  main:
    keep-alive: true  # Importante: VT son daemon threads
```

### Guia Detallada (Junior/Mid)

```yaml
# application.yml
spring:
  threads:
    virtual:
      enabled: true     # Habilita virtual threads en toda la aplicacion
  main:
    keep-alive: true    # CRITICO: VT son daemon threads, sin esto la JVM puede terminar
```

Verificar que funciona con un log en cualquier bean:

```java
@PostConstruct
public void verify() {
    log.info("Thread: {} | Virtual: {}",
            Thread.currentThread().getName(),
            Thread.currentThread().isVirtual());
}
```

### Que cambia automaticamente

| Componente | Sin VT | Con VT |
|------------|--------|--------|
| Tomcat request handling | `ThreadPoolExecutor` (200 threads) | `VirtualThreadExecutor` |
| `@Async` tasks | `ThreadPoolTaskExecutor` | `SimpleAsyncTaskExecutor` con VT |
| `@Scheduled` tasks | `ThreadPoolTaskScheduler` | `SimpleAsyncTaskScheduler` con VT |
| Spring MVC async | Thread pool | Virtual threads |
| Spring AMQP listeners | Thread pool | Virtual threads |

**Importante**: Con VT habilitados, las propiedades `spring.task.execution.pool.*` **no tienen efecto**. Los virtual threads se programan en un pool de platform threads a nivel de JVM, no en pools dedicados.

---

## 3. Como Funcionan (Guia Detallada)

### Modelo de Ejecucion

```
Platform Thread (Carrier 1) ──┬── Virtual Thread 1 (ejecutando codigo)
                               ├── Virtual Thread 2 (bloqueado en DB) --> desmontado
                               └── Virtual Thread 3 (bloqueado en HTTP) --> desmontado

Platform Thread (Carrier 2) ──┬── Virtual Thread 4 (ejecutando codigo)
                               └── Virtual Thread 5 (ejecutando codigo)

                               Virtual Thread 2 (I/O completo) --> remontado en Carrier 1 o 2
```

### Ciclo de vida de un request e-commerce

```java
@PostMapping("/api/v1/pedidos")
public ResponseEntity<PedidoResponse> crearPedido(@Valid @RequestBody CrearPedidoRequest request) {
    // Thread.currentThread().isVirtual() == true

    // 1. Validar stock -> Query a DB (VT se desmonta, carrier libre)
    // 2. Reservar stock -> Update en DB (VT se desmonta, carrier libre)
    // 3. Calcular precio -> Llamada HTTP a servicio precios (VT se desmonta)
    // 4. Crear pedido -> Insert en DB (VT se desmonta, carrier libre)
    // 5. Publicar evento -> Envio a RabbitMQ (VT se desmonta, carrier libre)

    PedidoDto pedido = pedidoService.crearPedido(request.toCommand());
    return ResponseEntity.status(HttpStatus.CREATED).body(PedidoResponse.from(pedido));
}
```

- Cuando el VT hace I/O (DB, HTTP, etc.), se desmonta del carrier thread
- El carrier ejecuta otro VT que este listo
- Cuando el I/O completa, el VT se remonta en cualquier carrier disponible
- Un solo carrier puede atender cientos de VTs esperando I/O

---

## 4. Cuando Usar Virtual Threads

### Ideal para

```java
// ✅ Aplicacion e-commerce tipica: MUCHAS operaciones I/O
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ClienteRepository clienteRepository;           // I/O: DB
    private final PedidoClient pedidoClient;                     // I/O: HTTP
    private final InventarioClient inventarioClient;             // I/O: HTTP
    private final RedisTemplate<String, String> redisTemplate;   // I/O: Redis

    public DashboardDto obtenerDashboard(Long clienteId) {
        var cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ClienteNotFoundException(clienteId));
        var pedidos = pedidoClient.obtenerUltimos(clienteId, 10);
        var stockFavoritos = inventarioClient.checkStock(cliente.getProductosFavoritos());
        var preferencias = redisTemplate.opsForHash().entries("prefs:" + clienteId);

        return DashboardDto.builder()
                .cliente(ClienteDto.from(cliente))
                .ultimosPedidos(pedidos)
                .stockFavoritos(stockFavoritos)
                .preferencias(preferencias)
                .build();
    }
}
```

| Escenario | Usar VT? | Razon |
|-----------|----------|-------|
| API REST con DB + HTTP calls | Si | I/O-bound, alta concurrencia |
| Microservicio que llama a otros servicios | Si | Multiples HTTP calls |
| Procesamiento de pagos (espera gateway) | Si | Espera I/O de terceros |
| Worker que lee de cola y escribe en DB | Si | I/O-bound |
| Calculo de recomendaciones ML | No | CPU-bound |
| Procesamiento de imagenes | No | CPU-bound |
| Generacion masiva de reportes PDF | No | CPU-bound |

### NO ideal para

```java
// ❌ CPU-intensive: VT no aportan beneficio aqui
@Service
public class ReportePdfService {
    public byte[] generarReporteComplejo(ReporteRequest request) {
        var datos = calcularEstadisticas(request);   // CPU-bound
        var graficos = renderizarGraficos(datos);      // CPU-bound
        return generarPdf(datos, graficos);            // CPU-bound
    }
}
```

---

## 5. Pinning: El Principal Problema

### Referencia Rapida (Seniors)

Pinning ocurre cuando un VT se bloquea **dentro de `synchronized`** y no puede desmontarse del carrier. Solucion: `ReentrantLock`. Deteccion: `-Djdk.tracePinnedThreads=full`.

### Guia Detallada (Junior/Mid)

```java
// ❌ EVITAR: synchronized causa "pinning" del carrier thread
public class CacheService {

    private final Map<String, String> cache = new HashMap<>();

    public synchronized String obtenerDato(String key) {
        if (!cache.containsKey(key)) {
            // I/O dentro de synchronized = PINNING!
            String valor = httpClient.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create("http://servicio-externo/datos/" + key))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            ).body();
            cache.put(key, valor);
        }
        return cache.get(key);
    }
}
```

```java
// ✅ MEJOR: Usar ReentrantLock en lugar de synchronized
public class CacheService {

    private final Map<String, String> cache = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    public String obtenerDato(String key) {
        lock.lock();
        try {
            if (!cache.containsKey(key)) {
                String valor = httpClient.send(
                        HttpRequest.newBuilder()
                                .uri(URI.create("http://servicio-externo/datos/" + key))
                                .build(),
                        HttpResponse.BodyHandlers.ofString()
                ).body();
                cache.put(key, valor);
            }
            return cache.get(key);
        } catch (Exception e) {
            throw new RuntimeException("Error obteniendo dato: " + key, e);
        } finally {
            lock.unlock();
        }
    }
}
```

### Ejemplo e-commerce: Reserva de inventario sin pinning

```java
@Service
@RequiredArgsConstructor
public class InventarioService {

    private final InventarioRepository inventarioRepository;
    private final ReentrantLock reservaLock = new ReentrantLock();

    public ReservaResult reservarStock(Long productoId, int cantidad) {
        reservaLock.lock();
        try {
            var inventario = inventarioRepository.findByProductoId(productoId)
                    .orElseThrow(() -> new ProductoNotFoundException(productoId));
            if (inventario.getStockDisponible() < cantidad) {
                return ReservaResult.sinStock(productoId, inventario.getStockDisponible());
            }
            inventario.reducirStock(cantidad);
            inventarioRepository.save(inventario);  // I/O: VT se desmonta normalmente
            return ReservaResult.exitosa(productoId, cantidad);
        } finally { reservaLock.unlock(); }
    }
}
```

### Detectar Pinning

```bash
# JDK Flight Recorder
java -XX:StartFlightRecording:filename=recording.jfr,settings=profile -jar app.jar

# Flag de diagnostico JVM (solo desarrollo)
java -Djdk.tracePinnedThreads=full -jar ecommerce-service.jar

# Thread dump con jcmd
jcmd <PID> Thread.dump_to_file -format=json threads.json
```

---

## 6. Configuracion Avanzada

### Referencia Rapida (Seniors)

```java
@Configuration
public class VirtualThreadConfig {
    @Bean
    public AsyncTaskExecutor applicationTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setVirtualThreads(true);
        executor.setTaskDecorator(new MdcTaskDecorator());
        return executor;
    }
}
```

### Guia Detallada (Junior/Mid)

#### MDC Task Decorator + Executor completo

Los virtual threads no heredan el MDC automaticamente. Se necesita un `TaskDecorator`:

```java
// MdcTaskDecorator.java - Propaga traceId/requestId a virtual threads
public class MdcTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            try {
                if (contextMap != null) MDC.setContextMap(contextMap);
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}

// VirtualThreadConfig.java
@Configuration
@EnableAsync
public class VirtualThreadConfig {
    @Bean
    public AsyncTaskExecutor applicationTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setVirtualThreads(true);
        executor.setThreadNamePrefix("ecommerce-vt-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        return executor;
    }
}
```

#### Uso con @Async en servicio e-commerce

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoNotificacionService {

    private final EmailClient emailClient;
    private final SmsClient smsClient;
    private final PushNotificationClient pushClient;

    @Async
    public void notificarPedidoCreado(PedidoDto pedido) {
        log.info("Notificando pedido {} en VT: {}",
                pedido.getId(), Thread.currentThread().isVirtual());

        emailClient.enviarConfirmacion(pedido.getClienteEmail(), pedido);
        smsClient.enviarConfirmacion(pedido.getClienteTelefono(), pedido.getId());
        pushClient.enviarNotificacion(pedido.getClienteId(), "Pedido confirmado");
    }
}
```

---

## 7. Structured Concurrency (Preview - Java 21+)

### Referencia Rapida (Seniors)

```java
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    Supplier<A> a = scope.fork(() -> serviceA.call());
    Supplier<B> b = scope.fork(() -> serviceB.call());
    scope.join();
    scope.throwIfFailed();
    return combine(a.get(), b.get());
}
```

### Guia Detallada (Junior/Mid)

```java
// ❌ LENTO: Secuencial - 50+100+30+80 = 260ms total
var customer = customerClient.getById(customerId);       // 50ms
var orders = orderClient.getByCustomer(customerId);      // 100ms
var credit = creditClient.check(customerId);             // 30ms
var recommendations = recoClient.getFor(customerId);     // 80ms

// ✅ PARALELO: max(50,100,30,80) = ~100ms total. Requiere --enable-preview
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    Supplier<ClienteDto> customer = scope.fork(
            () -> customerClient.getById(customerId));
    Supplier<List<PedidoDto>> orders = scope.fork(
            () -> orderClient.getByCustomer(customerId));
    Supplier<CreditoDto> credit = scope.fork(
            () -> creditClient.check(customerId));
    Supplier<List<ProductoDto>> recommendations = scope.fork(
            () -> recoClient.getFor(customerId));

    scope.join();            // Espera a que TODAS completen
    scope.throwIfFailed();   // Si alguna fallo, propaga la excepcion

    return new CustomerDashboardDto(
            customer.get(), orders.get(),
            credit.get(), recommendations.get());
}
```

Variante `ShutdownOnSuccess`: El primero que responda gana. Util para buscar en multiples proveedores.

```java
try (var scope = new StructuredTaskScope.ShutdownOnSuccess<ProductoDto>()) {
    scope.fork(() -> proveedorAClient.buscar(sku));
    scope.fork(() -> proveedorBClient.buscar(sku));
    scope.fork(() -> proveedorCClient.buscar(sku));
    scope.join();
    return scope.result();  // Primera respuesta exitosa
}
```

Nota: Structured Concurrency requiere `--enable-preview` en `maven-compiler-plugin` y JVM args.

---

## 8. Performance Considerations

### Referencia Rapida (Seniors)

| Metrica | Platform Threads (200 pool) | Virtual Threads |
|---------|---------------------------|-----------------|
| Requests concurrentes max | ~200 | ~10,000+ |
| Latencia por request | Similar | Similar o mejor |
| Throughput (alta concurrencia) | Limitado por pool | Significativamente mejor |
| Memoria por thread | ~1MB | ~pocos KB |
| Context switch | OS-level (costoso) | JVM-level (barato) |

### Guia Detallada (Junior/Mid)

```
=== Platform Threads (pool de 200) ===
Request #1   [===DB===][===HTTP===][===DB===]   Thread-1 OCUPADO 185ms
Request #200 [===DB===][===HTTP===][===DB===]   Thread-200 OCUPADO 185ms
Request #201 [.....ESPERANDO EN COLA.....]      Sin threads!

=== Virtual Threads ===
VT #1     [DB]     [HTTP]        [DB]    <-- Carrier libre entre I/O
VT #5000      [DB]     [HTTP]    [DB]    <-- Todos se ejecutan!
~8 carrier threads manejan miles de VTs alternandose.
```

### Monitoring

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    enable:
      jvm.threads: true     # Metricas: jvm.threads.live, jvm.threads.daemon, jvm.threads.peak
```

---

## 9. Migracion desde Thread Pools

### Referencia Rapida (Seniors)

| Antes | Despues |
|-------|---------|
| `@Async` + `ThreadPoolTaskExecutor` (pool size, queue) | `spring.threads.virtual.enabled=true` |
| `@Scheduled` + `ThreadPoolTaskScheduler` | `spring.threads.virtual.enabled=true` |
| `Executors.newFixedThreadPool(N)` | `Executors.newVirtualThreadPerTaskExecutor()` |
| Tuning: core/max pool, queue, rejection policy | Sin tuning de pool necesario |

### Guia Detallada (Junior/Mid)

```java
// ❌ ANTES: Thread pool con tuning complejo
@Bean("pedidoExecutor")
public AsyncTaskExecutor pedidoExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);
    executor.setMaxPoolSize(50);
    executor.setQueueCapacity(100);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
}
```

```yaml
# ✅ DESPUES: Toda la configuracion de pools reemplazada por esto
spring:
  threads:
    virtual:
      enabled: true
  main:
    keep-alive: true
# Las propiedades spring.task.execution.pool.* ya NO tienen efecto
```

```java
// ❌ ANTES: Executors.newFixedThreadPool(20)
// ✅ DESPUES: Executors.newVirtualThreadPerTaskExecutor()
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Future<ProductoDto>> futures = productoIds.stream()
            .map(id -> executor.submit(() -> precioClient.actualizar(id)))
            .toList();
}
```

---

## 10. Compatibilidad con Libraries

| Library | Compatible con VT | Notas |
|---------|-------------------|-------|
| Spring MVC | Totalmente | Requests en VT automaticamente |
| Spring Data JPA / Hibernate | Con precaucion | Evitar `synchronized` en custom code |
| HikariCP | Si | Pool limitado, VTs esperan sin bloquear carrier |
| Lettuce (Redis) | Si | Non-blocking by default |
| Spring AMQP (RabbitMQ) | Si | Listeners en VT |
| Resilience4j | Parcial | Bulkhead semaphore ok, ThreadPool bulkhead no aplica |
| Spring Security | Si | SecurityContext se propaga correctamente |
| Micrometer | Si | Metricas de threads disponibles |
| Jackson | Si | Sin I/O, sin problemas |

### HikariCP con Virtual Threads

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20       # DB tiene limite de conexiones
      connection-timeout: 30000   # VTs esperan aqui sin bloquear carrier
```

**Importante**: Aunque puedes tener miles de VTs, tu base de datos tiene un limite de conexiones. HikariCP sigue siendo necesario. La diferencia: cuando un VT espera una conexion del pool, **no bloquea el carrier thread**.

### Resilience4j: Usar Bulkhead SEMAPHORE

```java
@CircuitBreaker(name = "precioService", fallbackMethod = "precioFallback")
@Retry(name = "precioService")
@Bulkhead(name = "precioService", type = Bulkhead.Type.SEMAPHORE)  // NO THREADPOOL
public PrecioDto obtenerPrecio(Long productoId) {
    return restClient.get()
            .uri("/api/v1/precios/{id}", productoId)
            .retrieve()
            .body(PrecioDto.class);
}
```

```yaml
resilience4j:
  bulkhead:
    instances:
      precioService:
        maxConcurrentCalls: 50
        maxWaitDuration: 500ms
```

### ThreadLocal vs ScopedValue

```java
// ⚠️ ThreadLocal funciona pero consume memoria con millones de VTs
private static final ThreadLocal<String> tenantId = new ThreadLocal<>();

// ✅ MEJOR: ScopedValue (Preview Java 21+) - mas eficiente, auto-limpieza
public static final ScopedValue<String> CURRENT_TENANT = ScopedValue.newInstance();

ScopedValue.where(CURRENT_TENANT, "acme-corp")
           .run(() -> procesarPedido(pedidoId));
```

---

## Hacer / Evitar

### Hacer

1. **Habilitar VT con una propiedad** - `spring.threads.virtual.enabled=true` es suficiente para Spring Boot 3.4
2. **Configurar keep-alive** - `spring.main.keep-alive=true` para evitar que la JVM termine prematuramente
3. **Usar ReentrantLock** - En lugar de `synchronized` cuando hay I/O dentro del bloque critico
4. **Propagar MDC** - Usar `TaskDecorator` para mantener trazabilidad en logs
5. **Limitar conexiones a DB** - HikariCP sigue siendo necesario; los VTs esperan sin bloquear carriers
6. **Detectar pinning** - Usar `-Djdk.tracePinnedThreads=full` en desarrollo
7. **Usar Bulkhead SEMAPHORE** - Con Resilience4j, no usar ThreadPool bulkhead
8. **Monitorear metricas de threads** - `management.metrics.enable.jvm.threads=true`
9. **Migrar gradualmente** - Habilitar VT y verificar comportamiento antes de eliminar pool config
10. **Limpiar ThreadLocal** - Siempre usar `try/finally` con `ThreadLocal.remove()`

### Evitar

1. **No usar VT para CPU-bound** - Calculos intensivos, procesamiento de imagenes, ML
2. **No usar synchronized con I/O** - Causa pinning, usar `ReentrantLock`
3. **No configurar thread pools con VT** - Las propiedades `spring.task.execution.pool.*` se ignoran
4. **No crear millones de ThreadLocals** - Con VTs, cada uno tiene su copia; usar `ScopedValue`
5. **No esperar que VT mejoren latencia** - Mejoran throughput bajo alta concurrencia, no latencia individual
6. **No usar ThreadPool Bulkhead** - Con Resilience4j, usar Semaphore bulkhead
7. **No olvidar keep-alive** - Sin `spring.main.keep-alive=true`, la JVM puede cerrarse
8. **No asumir compatibilidad total** - Verificar libraries de terceros por uso de `synchronized`
9. **No mezclar reactive con VT** - Si ya usas WebFlux/reactive, VT no aportan beneficio adicional
10. **No ignorar el pool de conexiones DB** - VTs no eliminan la necesidad de limitar conexiones

---

## Checklist de Implementacion

### Prerequisitos
- [ ] Java 21+ instalado y configurado en `pom.xml`
- [ ] Spring Boot 3.4.x como parent
- [ ] Entender si la aplicacion es I/O-bound (VT util) o CPU-bound (VT no util)

### Configuracion Base
- [ ] `spring.threads.virtual.enabled=true` en `application.yml`
- [ ] `spring.main.keep-alive=true` en `application.yml`
- [ ] Verificar con log que threads son virtuales (`Thread.currentThread().isVirtual()`)

### Pinning
- [ ] Buscar bloques `synchronized` en el codigo propio
- [ ] Reemplazar `synchronized` por `ReentrantLock` donde haya I/O
- [ ] Ejecutar con `-Djdk.tracePinnedThreads=full` en desarrollo
- [ ] Verificar libraries de terceros por pinning

### Contexto y Trazabilidad
- [ ] Configurar `MdcTaskDecorator` para propagar MDC a tareas asincronas
- [ ] Verificar que `traceId` / `requestId` se propagan en logs
- [ ] Limpiar `ThreadLocal` con `try/finally` en todos los usos

### Migracion
- [ ] Eliminar configuracion de thread pools innecesaria
- [ ] Simplificar beans de `ThreadPoolTaskExecutor` a `SimpleAsyncTaskExecutor`
- [ ] Reemplazar `Executors.newFixedThreadPool()` por `Executors.newVirtualThreadPerTaskExecutor()`
- [ ] Actualizar `@Async` executors si se usaban multiples pools

### Performance y Monitoring
- [ ] Habilitar metricas de threads JVM (`jvm.threads.*`)
- [ ] Mantener HikariCP pool size adecuado (VTs no eliminan la necesidad)
- [ ] Ejecutar pruebas de carga comparativas (antes/despues de VT)

### Resilience y Testing
- [ ] Configurar Resilience4j Bulkhead como SEMAPHORE (no THREADPOOL)
- [ ] Tests de integracion con `spring.threads.virtual.enabled=true`
- [ ] Tests de concurrencia alta (1000+ requests simultaneos)
- [ ] Verificar propagacion de MDC en tests

---

*Documento generado con Context7 - Java 21+ Virtual Threads + Spring Boot 3.4.x*
