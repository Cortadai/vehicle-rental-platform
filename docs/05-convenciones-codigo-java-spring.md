# Área 5: Convenciones de Código Java/Spring Boot

> **Audiencia**: Desarrolladores junior/mid (guía detallada) + Seniors (referencia rápida)
> **Stack**: Spring Boot 3.4.x, Java 17+, Lombok

---

## 1. Naming Conventions

### Referencia Rápida (Seniors)

| Elemento | Convención | Ejemplo |
|----------|------------|---------|
| Clases | PascalCase, sustantivo | `ClienteService`, `PedidoController` |
| Interfaces | PascalCase, adjetivo/sustantivo | `Serializable`, `ClienteRepository` |
| Métodos | camelCase, verbo | `crearPedido()`, `obtenerPorId()` |
| Variables | camelCase | `clienteActual`, `totalPedido` |
| Constantes | SCREAMING_SNAKE | `MAX_REINTENTOS`, `DEFAULT_TIMEOUT` |
| Paquetes | minúsculas | `com.acme.pedido.service` |

### Guía Detallada (Junior/Mid)

#### Clases

```java
// ✅ BIEN: Sustantivos descriptivos
public class Cliente { }
public class PedidoService { }
public class ClienteController { }
public class PedidoNotFoundException { }

// ❌ MAL
public class Gestionar { }        // Verbo
public class ClienteData { }      // Sufijo Data innecesario
public class ClienteImpl { }      // Impl sin interfaz clara
public class ClienteClass { }     // Sufijo Class redundante
```

#### Interfaces

```java
// ✅ BIEN
public interface ClienteRepository { }
public interface Pageable { }
public interface EventPublisher { }

// ❌ MAL
public interface IClienteRepository { }  // Prefijo I (estilo C#)
public interface ClienteRepositoryInterface { }  // Sufijo redundante
```

#### Métodos

```java
// ✅ BIEN: Verbos que describen la acción
public Cliente crearCliente(CrearClienteCommand cmd) { }
public Optional<Cliente> buscarPorEmail(String email) { }
public List<Pedido> listarPedidosPendientes() { }
public void cancelarPedido(Long pedidoId) { }
public boolean existeEmail(String email) { }
public int contarPedidosActivos() { }

// ❌ MAL
public Cliente cliente(CrearClienteCommand cmd) { }  // Sin verbo
public Optional<Cliente> email(String email) { }     // Sin verbo
public List<Pedido> pedidos() { }                    // Ambiguo
```

**Prefijos estándar para métodos**:

| Prefijo | Uso | Ejemplo |
|---------|-----|---------|
| `crear`, `registrar` | Crear nuevo recurso | `crearCliente()` |
| `obtener`, `buscar` | Recuperar recurso(s) | `obtenerPorId()`, `buscarPorEmail()` |
| `listar` | Recuperar colección | `listarTodos()` |
| `actualizar`, `modificar` | Modificar recurso | `actualizarDatos()` |
| `eliminar`, `borrar` | Eliminar recurso | `eliminarCliente()` |
| `existe`, `tiene` | Verificar existencia | `existeEmail()` |
| `contar` | Contar elementos | `contarActivos()` |
| `validar` | Validar datos | `validarEmail()` |
| `es`, `puede` | Booleanos | `esActivo()`, `puedeModificar()` |

#### Variables

```java
// ✅ BIEN
Cliente clienteActual;
List<Pedido> pedidosPendientes;
BigDecimal totalConIva;
int cantidadItems;
boolean estaActivo;

// ❌ MAL
Cliente c;                // Muy corto
List<Pedido> list;        // Genérico
BigDecimal x;             // Sin significado
int i;                    // Solo OK en loops
boolean flag;             // Ambiguo
```

#### Constantes

```java
// ✅ BIEN
public static final int MAX_REINTENTOS = 3;
public static final Duration TIMEOUT_DEFAULT = Duration.ofSeconds(30);
public static final String HEADER_CORRELATION_ID = "X-Correlation-ID";

// ❌ MAL
public static final int maxReintentos = 3;     // No es SCREAMING_SNAKE
public static final int MAX = 3;                // Muy ambiguo
private static final int MAGIC_NUMBER = 42;     // Sin contexto
```

---

## 2. @Component vs @Service vs @Repository vs @Controller

### Referencia Rápida

| Anotación | Capa | Uso | Beneficio extra |
|-----------|------|-----|-----------------|
| `@Component` | Genérica | Componentes que no encajan en otra categoría | Ninguno |
| `@Service` | Negocio | Lógica de negocio | Semántico |
| `@Repository` | Datos | Acceso a datos | Traducción de excepciones |
| `@Controller` | Web | Endpoints HTTP | MVC handling |
| `@RestController` | Web | REST APIs | `@Controller` + `@ResponseBody` |

### Guía Detallada

#### @Service - Lógica de Negocio

```java
@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteService clienteService;
    private final InventarioClient inventarioClient;

    @Transactional
    public Pedido crear(CrearPedidoCommand command) {
        // Lógica de negocio aquí
        Cliente cliente = clienteService.obtenerPorId(command.clienteId());
        validarCredito(cliente, command.total());

        Pedido pedido = new Pedido(cliente, command.lineas());
        return pedidoRepository.save(pedido);
    }
}
```

#### @Repository - Acceso a Datos

```java
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT c FROM Cliente c WHERE c.activo = true AND c.credito > :minimo")
    List<Cliente> findClientesActivosConCredito(@Param("minimo") BigDecimal minimo);
}
```

**Beneficio de @Repository**: Traduce automáticamente excepciones de persistencia (SQLException, etc.) a la jerarquía `DataAccessException` de Spring.

#### @Component - Componentes Genéricos

```java
// Generadores, utilidades con estado, adaptadores
@Component
public class PdfGenerator {
    public byte[] generarFactura(Pedido pedido) { /* ... */ }
}

@Component
public class EmailTemplateRenderer {
    public String renderizar(String template, Map<String, Object> variables) { /* ... */ }
}

@Component
public class MetricsCollector {
    public void registrarOperacion(String nombre, Duration duracion) { /* ... */ }
}
```

❌ **NO usar @Component para**:
```java
// MAL: Debería ser @Service
@Component
public class ClienteService { }

// MAL: Debería ser @Repository
@Component
public class ClienteDao { }

// MAL: Debería ser @Configuration
@Component
public class DatabaseConfig { }
```

---

## 3. Constructor Injection vs Field Injection

### Referencia Rápida

| Tipo | Recomendado | Testeable | Inmutable |
|------|-------------|-----------|-----------|
| Constructor | ✅ Sí | ✅ Fácil | ✅ Sí |
| Field (@Autowired) | ❌ No | ❌ Difícil | ❌ No |
| Setter | ⚠️ Casos específicos | ✅ Fácil | ❌ No |

### Guía Detallada

✅ **Hacer**: Constructor Injection (siempre)

```java
@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteService clienteService;
    private final ApplicationEventPublisher eventPublisher;

    // Constructor único = @Autowired implícito en Spring
    public PedidoService(
            PedidoRepository pedidoRepository,
            ClienteService clienteService,
            ApplicationEventPublisher eventPublisher) {
        this.pedidoRepository = pedidoRepository;
        this.clienteService = clienteService;
        this.eventPublisher = eventPublisher;
    }
}
```

✅ **Mejor con Lombok**:

```java
@Service
@RequiredArgsConstructor  // Genera constructor para campos final
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteService clienteService;
    private final ApplicationEventPublisher eventPublisher;

    // Lombok genera el constructor automáticamente
}
```

❌ **Evitar**: Field Injection

```java
// MAL: Field injection
@Service
public class PedidoService {

    @Autowired  // ❌ No usar
    private PedidoRepository pedidoRepository;

    @Autowired  // ❌ No usar
    private ClienteService clienteService;
}
```

**Por qué evitar Field Injection**:
1. **No testeable sin Spring**: Necesitas reflexión o Spring para inyectar mocks
2. **Oculta dependencias**: No ves las dependencias en la firma de la clase
3. **Permite estado mutable**: Los campos no pueden ser `final`
4. **Viola SRP**: Fácil añadir muchas dependencias sin darte cuenta

⚠️ **Setter Injection**: Solo para dependencias opcionales

```java
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final EmailSender emailSender;  // Requerido

    private SmsSender smsSender;  // Opcional

    @Autowired(required = false)
    public void setSmsSender(SmsSender smsSender) {
        this.smsSender = smsSender;
    }
}
```

---

## 4. Records vs POJOs para DTOs

### Referencia Rápida

| Característica | Record | POJO + Lombok |
|----------------|--------|---------------|
| Inmutable | ✅ Siempre | ⚠️ Con @Value |
| Boilerplate | ✅ Cero | ⚠️ Requiere anotaciones |
| Herencia | ❌ No soporta | ✅ Soporta |
| JPA Entity | ❌ No usar | ✅ Usar |
| Jackson | ✅ Soportado | ✅ Soportado |
| Validación | ✅ En parámetros | ✅ En campos |

### Guía Detallada

✅ **Usar Records para**: DTOs de entrada/salida de APIs

```java
// Request DTO
public record CrearClienteRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no es válido")
        String email,

        @Size(min = 9, max = 15, message = "Teléfono debe tener entre 9 y 15 dígitos")
        String telefono
) {
    // Método de conversión a comando
    public CrearClienteCommand toCommand() {
        return new CrearClienteCommand(nombre, email, telefono);
    }
}

// Response DTO
public record ClienteResponse(
        Long id,
        String nombre,
        String email,
        String telefono,
        LocalDateTime fechaRegistro
) {
    // Factory method desde entidad
    public static ClienteResponse from(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getEmail(),
                cliente.getTelefono(),
                cliente.getFechaRegistro()
        );
    }
}

// DTO con nested records
public record PedidoResponse(
        Long id,
        ClienteResumen cliente,
        List<LineaResponse> lineas,
        BigDecimal total,
        String estado,
        LocalDateTime fechaCreacion
) {
    public record ClienteResumen(Long id, String nombre) {}
    public record LineaResponse(String producto, int cantidad, BigDecimal precio) {}

    public static PedidoResponse from(Pedido pedido) {
        return new PedidoResponse(
                pedido.getId(),
                new ClienteResumen(pedido.getCliente().getId(), pedido.getCliente().getNombre()),
                pedido.getLineas().stream()
                        .map(l -> new LineaResponse(l.getProducto(), l.getCantidad(), l.getPrecio()))
                        .toList(),
                pedido.getTotal(),
                pedido.getEstado().name(),
                pedido.getFechaCreacion()
        );
    }
}
```

✅ **Usar Records para**: Commands y Value Objects

```java
// Command (acción a realizar)
public record CrearPedidoCommand(
        Long clienteId,
        List<LineaItem> items
) {
    public record LineaItem(String productoId, int cantidad) {}
}

// Value Object
public record Direccion(
        String calle,
        String numero,
        String ciudad,
        String codigoPostal,
        String pais
) {
    // Validación en constructor compacto
    public Direccion {
        if (codigoPostal != null && !codigoPostal.matches("\\d{5}")) {
            throw new IllegalArgumentException("Código postal inválido");
        }
    }
}

// Money value object
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount no puede ser negativo");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency es requerido");
        }
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currencies deben coincidir");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }
}
```

❌ **NO usar Records para**: Entidades JPA

```java
// ❌ MAL: Record como Entity
@Entity
public record Cliente(Long id, String nombre) { }  // NO FUNCIONA

// ✅ BIEN: Clase normal para Entity
@Entity
@Table(name = "clientes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String email;

    // Constructor para crear nuevos
    public Cliente(String nombre, String email) {
        this.nombre = nombre;
        this.email = email;
    }
}
```

---

## 5. Optional Best Practices

### Referencia Rápida

| Uso | Recomendado | Ejemplo |
|-----|-------------|---------|
| Retorno de método | ✅ Sí | `Optional<Cliente> buscarPorId(Long id)` |
| Parámetro de método | ❌ No | Usar overloading o `@Nullable` |
| Campo de clase | ❌ No | Usar null o valor por defecto |
| Colecciones | ❌ No | Retornar colección vacía |

### Guía Detallada

✅ **Hacer**: Retornar Optional cuando el valor puede no existir

```java
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByEmail(String email);
}

@Service
public class ClienteService {

    public Optional<Cliente> buscarPorEmail(String email) {
        return clienteRepository.findByEmail(email);
    }

    public Cliente obtenerPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cliente", id));
    }
}
```

✅ **Hacer**: Usar métodos funcionales de Optional

```java
// orElseThrow - cuando la ausencia es error
Cliente cliente = clienteRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Cliente", id));

// orElse - valor por defecto (evaluación eager)
String nombre = optional.orElse("Desconocido");

// orElseGet - valor por defecto (evaluación lazy)
Cliente cliente = optional.orElseGet(() -> crearClienteDefault());

// map - transformar si presente
String nombreUpper = optionalCliente
        .map(Cliente::getNombre)
        .map(String::toUpperCase)
        .orElse("N/A");

// filter - filtrar si presente
Optional<Cliente> clienteActivo = optionalCliente
        .filter(Cliente::isActivo);

// ifPresent - ejecutar si presente
optionalCliente.ifPresent(c -> log.info("Cliente encontrado: {}", c.getNombre()));

// ifPresentOrElse (Java 9+) - ejecutar si presente o ausente
optionalCliente.ifPresentOrElse(
        c -> log.info("Encontrado: {}", c.getNombre()),
        () -> log.warn("Cliente no encontrado")
);
```

❌ **Evitar**: Anti-patterns con Optional

```java
// ❌ MAL: isPresent() + get()
if (optional.isPresent()) {
    Cliente cliente = optional.get();
    // usar cliente
}

// ✅ BIEN: Usar métodos funcionales
optional.ifPresent(cliente -> {
    // usar cliente
});

// ❌ MAL: Optional como parámetro
public void procesar(Optional<String> filtro) { }

// ✅ BIEN: Overloading o nullable
public void procesar() { procesar(null); }
public void procesar(@Nullable String filtro) { }

// ❌ MAL: Optional para colecciones
public Optional<List<Cliente>> listarClientes() { }

// ✅ BIEN: Retornar lista vacía
public List<Cliente> listarClientes() {
    return clienteRepository.findAll();  // Nunca null, puede ser vacía
}

// ❌ MAL: Optional como campo
public class Pedido {
    private Optional<String> comentario;  // NO
}

// ✅ BIEN: Nullable o String vacío
public class Pedido {
    @Nullable
    private String comentario;  // OK

    public Optional<String> getComentario() {  // OK en getter
        return Optional.ofNullable(comentario);
    }
}

// ❌ MAL: Optional.of() con valor posiblemente null
Optional<Cliente> opt = Optional.of(clientePosiblementeNull);  // NullPointerException

// ✅ BIEN: Optional.ofNullable()
Optional<Cliente> opt = Optional.ofNullable(clientePosiblementeNull);
```

---

## 6. Stream API: Cuándo Usar, Cuándo Evitar

### Referencia Rápida

| Usar Stream | Usar Loop tradicional |
|-------------|----------------------|
| Transformaciones (map, filter) | Lógica compleja con estado |
| Colecciones pequeñas/medianas | Rendimiento crítico |
| Código declarativo y legible | Break/continue necesario |
| Operaciones paralelas | Modificar colección original |

### Guía Detallada

✅ **Usar Streams para**: Transformaciones simples

```java
// Transformar lista
List<ClienteResponse> responses = clientes.stream()
        .map(ClienteResponse::from)
        .toList();

// Filtrar y transformar
List<String> emailsActivos = clientes.stream()
        .filter(Cliente::isActivo)
        .map(Cliente::getEmail)
        .toList();

// Agrupar
Map<EstadoPedido, List<Pedido>> pedidosPorEstado = pedidos.stream()
        .collect(Collectors.groupingBy(Pedido::getEstado));

// Sumar
BigDecimal totalVentas = pedidos.stream()
        .map(Pedido::getTotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

// Buscar primero que cumpla condición
Optional<Cliente> clienteVip = clientes.stream()
        .filter(c -> c.getComprasTotal().compareTo(BigDecimal.valueOf(10000)) > 0)
        .findFirst();

// Verificar condiciones
boolean todosActivos = clientes.stream().allMatch(Cliente::isActivo);
boolean algunoMoroso = clientes.stream().anyMatch(Cliente::esMoroso);
boolean ningunoSuspendido = clientes.stream().noneMatch(Cliente::estaSuspendido);
```

❌ **Evitar Streams para**: Lógica compleja o con efectos secundarios

```java
// ❌ MAL: Stream con efectos secundarios
List<String> errores = new ArrayList<>();
clientes.stream()
        .filter(c -> {
            if (!validar(c)) {
                errores.add("Error en: " + c.getId());  // Efecto secundario
                return false;
            }
            return true;
        })
        .forEach(this::procesar);

// ✅ BIEN: Loop tradicional para lógica compleja
List<String> errores = new ArrayList<>();
List<Cliente> clientesValidos = new ArrayList<>();

for (Cliente cliente : clientes) {
    if (!validar(cliente)) {
        errores.add("Error en: " + cliente.getId());
        continue;
    }
    clientesValidos.add(cliente);
}
clientesValidos.forEach(this::procesar);

// ❌ MAL: Stream muy anidado/complejo
pedidos.stream()
        .filter(p -> p.getEstado() == PENDIENTE)
        .flatMap(p -> p.getLineas().stream())
        .filter(l -> l.getProducto().getCategoria().equals("ELECTRO"))
        .map(l -> new Object[] {l.getProducto(), l.getCantidad()})
        .collect(Collectors.groupingBy(
                arr -> ((Producto)arr[0]).getProveedor(),
                Collectors.summingInt(arr -> (Integer)arr[1])
        ));

// ✅ BIEN: Extraer a métodos con nombres descriptivos
Map<Proveedor, Integer> cantidadPorProveedor = calcularCantidadElectroPorProveedor(pedidosPendientes);

private Map<Proveedor, Integer> calcularCantidadElectroPorProveedor(List<Pedido> pedidos) {
    return pedidos.stream()
            .flatMap(p -> p.getLineas().stream())
            .filter(this::esLineaDeElectro)
            .collect(Collectors.groupingBy(
                    l -> l.getProducto().getProveedor(),
                    Collectors.summingInt(LineaPedido::getCantidad)
            ));
}

private boolean esLineaDeElectro(LineaPedido linea) {
    return "ELECTRO".equals(linea.getProducto().getCategoria());
}
```

---

## 7. Lombok: Qué Usar, Qué Evitar

### Referencia Rápida

| Anotación | Recomendado | Notas |
|-----------|-------------|-------|
| `@RequiredArgsConstructor` | ✅ Sí | Para DI |
| `@Getter` | ✅ Sí | En entities y DTOs |
| `@Slf4j` | ✅ Sí | Logging |
| `@Builder` | ✅ Sí | Para objetos complejos |
| `@Data` | ⚠️ Cuidado | Evitar en entities |
| `@Setter` | ⚠️ Cuidado | Rompe inmutabilidad |
| `@AllArgsConstructor` | ⚠️ Cuidado | Orden de parámetros |
| `@EqualsAndHashCode` | ⚠️ Cuidado | Problemas con JPA |
| `@ToString` | ⚠️ Cuidado | Circular references |

### Guía Detallada

✅ **Usar**: Anotaciones seguras

```java
// @RequiredArgsConstructor para inyección de dependencias
@Service
@RequiredArgsConstructor
@Slf4j
public class ClienteService {
    private final ClienteRepository clienteRepository;
    private final EmailService emailService;

    public void procesar(Long id) {
        log.info("Procesando cliente: {}", id);
    }
}

// @Getter para entities (sin @Setter para inmutabilidad)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String email;

    public Cliente(String nombre, String email) {
        this.nombre = nombre;
        this.email = email;
    }

    // Métodos de negocio para modificar estado
    public void actualizarEmail(String nuevoEmail) {
        this.email = nuevoEmail;
    }
}

// @Builder para objetos complejos
@Builder
@Getter
public class ReporteVentas {
    private final LocalDate fechaInicio;
    private final LocalDate fechaFin;
    private final BigDecimal totalVentas;
    private final int cantidadPedidos;
    @Builder.Default
    private final List<LineaReporte> lineas = new ArrayList<>();
}

// Uso del builder
ReporteVentas reporte = ReporteVentas.builder()
        .fechaInicio(LocalDate.now().minusMonths(1))
        .fechaFin(LocalDate.now())
        .totalVentas(BigDecimal.valueOf(50000))
        .cantidadPedidos(150)
        .build();
```

⚠️ **Usar con cuidado**: Anotaciones potencialmente problemáticas

```java
// @Data en entities = PROBLEMAS
@Data  // ❌ Genera @Setter, @EqualsAndHashCode (problemas con lazy loading)
@Entity
public class Pedido { }

// @EqualsAndHashCode en entities = PROBLEMAS
@Entity
@EqualsAndHashCode  // ❌ Puede causar LazyInitializationException
public class Pedido {
    @ManyToOne(fetch = FetchType.LAZY)
    private Cliente cliente;  // Se accede en equals/hashCode
}

// ✅ BIEN: Equals/HashCode solo por ID en entities
@Entity
@Getter
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pedido pedido)) return false;
        return id != null && id.equals(pedido.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

// @ToString con relaciones = StackOverflow
@ToString  // ❌ Si Cliente tiene List<Pedido> y Pedido tiene Cliente
@Entity
public class Pedido {
    @ManyToOne
    private Cliente cliente;  // cliente.toString() -> pedidos.toString() -> cliente.toString()
}

// ✅ BIEN: Excluir relaciones
@ToString(exclude = {"cliente", "lineas"})
@Entity
public class Pedido { }

// O mejor: implementar manualmente
@Override
public String toString() {
    return "Pedido{id=" + id + ", estado=" + estado + "}";
}
```

---

## 8. Java 17+ Features a Adoptar

### Referencia Rápida

| Feature | Desde | Usar para |
|---------|-------|-----------|
| Records | Java 14 | DTOs, Value Objects |
| Pattern Matching instanceof | Java 16 | Type checks |
| Sealed Classes | Java 17 | Jerarquías cerradas |
| Text Blocks | Java 15 | SQL, JSON, HTML |
| Switch Expressions | Java 14 | Múltiples casos |

### Guía Detallada

#### Records (ya cubierto arriba)

#### Pattern Matching para instanceof

```java
// ❌ Antes de Java 16
if (obj instanceof String) {
    String s = (String) obj;
    System.out.println(s.toUpperCase());
}

// ✅ Java 16+
if (obj instanceof String s) {
    System.out.println(s.toUpperCase());
}

// Combinado con lógica
if (obj instanceof String s && s.length() > 5) {
    System.out.println(s.toUpperCase());
}

// En switch (Java 21+)
String resultado = switch (obj) {
    case Integer i -> "Entero: " + i;
    case String s -> "String: " + s.toUpperCase();
    case null -> "Nulo";
    default -> "Otro: " + obj;
};
```

#### Sealed Classes

```java
// Jerarquía cerrada de excepciones
public sealed class PedidoException extends RuntimeException
        permits PedidoNoEncontradoException,
                PedidoNoModificableException,
                StockInsuficienteException {

    protected PedidoException(String message) {
        super(message);
    }
}

public final class PedidoNoEncontradoException extends PedidoException {
    public PedidoNoEncontradoException(Long id) {
        super("Pedido no encontrado: " + id);
    }
}

public final class PedidoNoModificableException extends PedidoException {
    public PedidoNoModificableException(Long id, EstadoPedido estado) {
        super("Pedido " + id + " no modificable en estado " + estado);
    }
}

// Estados de pedido como sealed
public sealed interface EstadoPedido permits
        EstadoPendiente, EstadoConfirmado, EstadoEnviado, EstadoEntregado, EstadoCancelado {

    boolean puedeTransicionarA(EstadoPedido nuevoEstado);
}

public record EstadoPendiente() implements EstadoPedido {
    @Override
    public boolean puedeTransicionarA(EstadoPedido nuevoEstado) {
        return nuevoEstado instanceof EstadoConfirmado || nuevoEstado instanceof EstadoCancelado;
    }
}
```

#### Text Blocks

```java
// ❌ Antes
String query = "SELECT c.id, c.nombre, c.email " +
               "FROM clientes c " +
               "WHERE c.activo = true " +
               "  AND c.credito > :minimo " +
               "ORDER BY c.nombre";

// ✅ Text blocks (Java 15+)
String query = """
        SELECT c.id, c.nombre, c.email
        FROM clientes c
        WHERE c.activo = true
          AND c.credito > :minimo
        ORDER BY c.nombre
        """;

// JSON para tests
String json = """
        {
            "nombre": "Juan",
            "email": "juan@ejemplo.com",
            "activo": true
        }
        """;

// HTML para emails
String emailBody = """
        <html>
        <body>
            <h1>Confirmación de Pedido</h1>
            <p>Estimado %s,</p>
            <p>Su pedido #%d ha sido confirmado.</p>
        </body>
        </html>
        """.formatted(cliente.getNombre(), pedido.getId());
```

#### Switch Expressions

```java
// ❌ Antes
String mensaje;
switch (estado) {
    case PENDIENTE:
        mensaje = "Esperando confirmación";
        break;
    case CONFIRMADO:
        mensaje = "Pedido confirmado";
        break;
    case ENVIADO:
        mensaje = "En camino";
        break;
    default:
        mensaje = "Estado desconocido";
}

// ✅ Switch expression (Java 14+)
String mensaje = switch (estado) {
    case PENDIENTE -> "Esperando confirmación";
    case CONFIRMADO -> "Pedido confirmado";
    case ENVIADO -> "En camino";
    case ENTREGADO -> "Entregado";
    case CANCELADO -> "Cancelado";
};

// Con bloques
HttpStatus status = switch (resultado) {
    case Exito e -> HttpStatus.OK;
    case ErrorValidacion e -> HttpStatus.BAD_REQUEST;
    case NoEncontrado e -> HttpStatus.NOT_FOUND;
    case ErrorInterno e -> {
        log.error("Error interno: {}", e.getMessage());
        yield HttpStatus.INTERNAL_SERVER_ERROR;
    }
};
```

---

## Checklist de Convenciones

| Aspecto | ✅ Correcto | ❌ Incorrecto |
|---------|------------|---------------|
| Clases | PascalCase, sustantivo | camelCase, verbo |
| Métodos | `crearCliente()` | `cliente()` |
| Constantes | `MAX_REINTENTOS` | `maxReintentos` |
| Injection | Constructor (final) | Field (@Autowired) |
| DTOs | Records | POJOs mutables |
| Entities | Clase + @Getter | @Data |
| Optional | Retorno de método | Parámetro o campo |
| Streams | Transformaciones simples | Lógica con estado |
| Lombok | @RequiredArgsConstructor | @Data en entities |
| Java 17+ | Pattern matching, records | instanceof + cast |

---

## Próximos Pasos

Este documento cubre el **Área 5: Convenciones de Código Java/Spring Boot**.

Cuando estés listo, continuamos con el **Área 6: Configuración Application Properties/YAML** que incluirá:
- application.yml vs application.properties
- Perfiles (dev, test, prod)
- @ConfigurationProperties vs @Value
- Configuración para Kubernetes

---

*Documento generado con Context7 - Spring Boot 3.4.x, Lombok, Java 17+*
