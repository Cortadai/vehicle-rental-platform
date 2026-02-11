# Área 4: Testing - Estrategia Completa

> **Audiencia**: Desarrolladores junior/mid (guía detallada) + Seniors (referencia rápida)
> **Stack**: Spring Boot 3.4.x, Java 17+, JUnit 5, Mockito, Testcontainers, PostgreSQL

---

## 1. Pirámide de Tests y Estrategia

### Referencia Rápida (Seniors)

```
         /\
        /  \      E2E Tests (5%)
       /────\     - Selenium, Playwright
      /      \
     /        \   Integration Tests (20%)
    /──────────\  - @SpringBootTest, Testcontainers
   /            \
  /              \ Unit Tests (75%)
 /────────────────\ - JUnit 5, Mockito, sin Spring
```

### Guía Detallada (Junior/Mid)

| Tipo | Velocidad | Qué testea | Herramientas |
|------|-----------|------------|--------------|
| **Unit** | ~ms | Una clase/método aislado | JUnit 5, Mockito, AssertJ |
| **Integration** | ~segundos | Componentes integrados | @SpringBootTest, Testcontainers |
| **Slice** | ~cientos ms | Una capa específica | @WebMvcTest, @DataJpaTest |
| **E2E** | ~minutos | Flujo completo | Selenium, RestAssured |

**Convención de nombres**:
- `*Test.java` → Unit tests
- `*IT.java` → Integration tests
- `*E2ETest.java` → End-to-end tests

---

## 2. Unit Testing con JUnit 5

### Referencia Rápida

```java
@Test
@DisplayName("debe calcular el total correctamente")
void debeCalcularTotalCorrectamente() {
    // given - when - then
}
```

### Guía Detallada

#### Estructura básica de un test

```java
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class PedidoServiceTest {

    private PedidoService pedidoService;
    private ClienteRepository clienteRepository;
    private PedidoRepository pedidoRepository;

    @BeforeEach
    void setUp() {
        clienteRepository = mock(ClienteRepository.class);
        pedidoRepository = mock(PedidoRepository.class);
        pedidoService = new PedidoService(pedidoRepository, clienteRepository);
    }

    @Test
    @DisplayName("debe crear pedido cuando cliente existe y tiene crédito")
    void debeCrearPedidoCuandoClienteExisteYTieneCredito() {
        // given (Arrange)
        Long clienteId = 1L;
        Cliente cliente = new Cliente(clienteId, "Juan", BigDecimal.valueOf(1000));
        CrearPedidoCommand command = new CrearPedidoCommand(clienteId, List.of(
                new LineaItem("PROD-1", 2, BigDecimal.valueOf(100))
        ));

        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            return new Pedido(1L, p.getCliente(), p.getLineas(), p.getTotal());
        });

        // when (Act)
        Pedido resultado = pedidoService.crear(command);

        // then (Assert)
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getTotal()).isEqualTo(BigDecimal.valueOf(200));

        verify(pedidoRepository).save(any(Pedido.class));
        verify(clienteRepository).findById(clienteId);
    }

    @Test
    @DisplayName("debe lanzar excepción cuando cliente no existe")
    void debeLanzarExcepcionCuandoClienteNoExiste() {
        // given
        Long clienteId = 999L;
        CrearPedidoCommand command = new CrearPedidoCommand(clienteId, List.of());

        when(clienteRepository.findById(clienteId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> pedidoService.crear(command))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Cliente")
                .hasMessageContaining("999");

        verify(pedidoRepository, never()).save(any());
    }
}
```

#### Tests parametrizados

```java
@ParameterizedTest(name = "descuento {0}% sobre {1} = {2}")
@CsvSource({
        "10, 100.00, 90.00",
        "20, 100.00, 80.00",
        "0, 100.00, 100.00",
        "50, 200.00, 100.00"
})
@DisplayName("debe calcular descuento correctamente")
void debeCalcularDescuentoCorrectamente(int porcentaje, BigDecimal precio, BigDecimal esperado) {
    // when
    BigDecimal resultado = calculadoraDescuento.aplicar(porcentaje, precio);

    // then
    assertThat(resultado).isEqualByComparingTo(esperado);
}

@ParameterizedTest
@EnumSource(EstadoPedido.class)
@DisplayName("debe validar todos los estados de pedido")
void debeValidarTodosLosEstadosDePedido(EstadoPedido estado) {
    assertThat(estado.name()).isNotBlank();
}

@ParameterizedTest
@MethodSource("proveerCasosDeValidacion")
@DisplayName("debe validar email correctamente")
void debeValidarEmailCorrectamente(String email, boolean esperadoValido) {
    assertThat(validador.esEmailValido(email)).isEqualTo(esperadoValido);
}

static Stream<Arguments> proveerCasosDeValidacion() {
    return Stream.of(
            Arguments.of("usuario@ejemplo.com", true),
            Arguments.of("usuario@", false),
            Arguments.of("@ejemplo.com", false),
            Arguments.of("", false),
            Arguments.of(null, false)
    );
}
```

#### Tests anidados para organización

```java
@DisplayName("PedidoService")
class PedidoServiceTest {

    @Nested
    @DisplayName("cuando se crea un pedido")
    class CuandoSeCreaPedido {

        @Test
        @DisplayName("debe asignar estado PENDIENTE")
        void debeAsignarEstadoPendiente() { /* ... */ }

        @Test
        @DisplayName("debe calcular total correctamente")
        void debeCalcularTotalCorrectamente() { /* ... */ }

        @Nested
        @DisplayName("y el cliente no tiene crédito")
        class YClienteNoTieneCredito {

            @Test
            @DisplayName("debe lanzar CreditoInsuficienteException")
            void debeLanzarCreditoInsuficienteException() { /* ... */ }
        }
    }

    @Nested
    @DisplayName("cuando se cancela un pedido")
    class CuandoSeCancelaPedido {

        @Test
        @DisplayName("debe cambiar estado a CANCELADO")
        void debeCambiarEstadoACancelado() { /* ... */ }
    }
}
```

---

## 3. Mockito: Qué Mockear y Qué No

### Referencia Rápida

| Mockear | NO Mockear |
|---------|------------|
| Repositorios | Entidades/DTOs |
| Clientes HTTP | Lógica de negocio pura |
| Servicios externos | Utilerías simples |
| Colas de mensajes | Value Objects |

### Guía Detallada

✅ **Mockear**: Dependencias externas e infraestructura

```java
// Repositorios
@Mock
private ClienteRepository clienteRepository;

// Clientes HTTP externos
@Mock
private InventarioClient inventarioClient;

// Publishers de eventos
@Mock
private ApplicationEventPublisher eventPublisher;

// Servicios de terceros
@Mock
private EmailService emailService;
```

❌ **NO Mockear**: Lógica de dominio y objetos de valor

```java
// MALO: Mockear la entidad
Pedido pedido = mock(Pedido.class);
when(pedido.getTotal()).thenReturn(BigDecimal.valueOf(100));

// BIEN: Usar instancia real
Pedido pedido = new Pedido(cliente, lineas);
assertThat(pedido.getTotal()).isEqualTo(BigDecimal.valueOf(100));
```

#### Mockito BDD Style (Recomendado)

```java
import static org.mockito.BDDMockito.*;

@Test
void debeNotificarCuandoPedidoCreado() {
    // given
    given(pedidoRepository.save(any())).willReturn(pedidoGuardado);

    // when
    pedidoService.crear(command);

    // then
    then(emailService).should().enviarConfirmacion(any());
    then(eventPublisher).should().publishEvent(any(PedidoCreadoEvent.class));
}
```

#### ArgumentCaptor para verificaciones complejas

```java
@Test
void debePublicarEventoConDatosCorrectos() {
    // given
    ArgumentCaptor<PedidoCreadoEvent> captor = ArgumentCaptor.forClass(PedidoCreadoEvent.class);
    given(pedidoRepository.save(any())).willReturn(pedidoGuardado);

    // when
    pedidoService.crear(command);

    // then
    then(eventPublisher).should().publishEvent(captor.capture());

    PedidoCreadoEvent evento = captor.getValue();
    assertThat(evento.pedidoId()).isEqualTo(pedidoGuardado.getId());
    assertThat(evento.clienteId()).isEqualTo(cliente.getId());
    assertThat(evento.total()).isEqualTo(pedidoGuardado.getTotal());
}
```

---

## 4. Testing de Capas

### Testing de @Service

```java
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void debeCrearClienteCuandoEmailNoExiste() {
        // given
        CrearClienteCommand command = new CrearClienteCommand("Juan", "juan@ejemplo.com");
        given(clienteRepository.existsByEmail(command.email())).willReturn(false);
        given(clienteRepository.save(any())).willAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            return new Cliente(1L, c.getNombre(), c.getEmail());
        });

        // when
        Cliente resultado = clienteService.crear(command);

        // then
        assertThat(resultado.getId()).isNotNull();
        assertThat(resultado.getNombre()).isEqualTo("Juan");
        then(clienteRepository).should().save(any(Cliente.class));
    }

    @Test
    void debeLanzarConflictExceptionCuandoEmailYaExiste() {
        // given
        CrearClienteCommand command = new CrearClienteCommand("Juan", "existente@ejemplo.com");
        given(clienteRepository.existsByEmail(command.email())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> clienteService.crear(command))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("email");

        then(clienteRepository).should(never()).save(any());
    }
}
```

### Testing de @Controller con @WebMvcTest

```java
@WebMvcTest(ClienteController.class)
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean  // @MockitoBean reemplaza a @MockBean (deprecated desde Spring Boot 3.4)
    // import: org.springframework.test.context.bean.override.mockito.MockitoBean
    private ClienteService clienteService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void debeRetornar201CuandoClienteCreado() throws Exception {
        // given
        CrearClienteRequest request = new CrearClienteRequest("Juan", "juan@ejemplo.com");
        Cliente clienteCreado = new Cliente(1L, "Juan", "juan@ejemplo.com");

        given(clienteService.crear(any())).willReturn(clienteCreado);

        // when & then
        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Juan"))
                .andExpect(jsonPath("$.email").value("juan@ejemplo.com"));
    }

    @Test
    void debeRetornar400CuandoEmailInvalido() throws Exception {
        // given
        CrearClienteRequest request = new CrearClienteRequest("Juan", "email-invalido");

        // when & then
        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
    }

    @Test
    void debeRetornar404CuandoClienteNoExiste() throws Exception {
        // given
        given(clienteService.obtenerPorId(999L))
                .willThrow(new NotFoundException("Cliente", 999L));

        // when & then
        mockMvc.perform(get("/api/v1/clientes/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }
}
```

### Testing de @Repository con @DataJpaTest

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ClienteRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void debeBuscarClientePorEmail() {
        // given
        Cliente cliente = new Cliente(null, "Juan", "juan@ejemplo.com");
        entityManager.persistAndFlush(cliente);

        // when
        Optional<Cliente> resultado = clienteRepository.findByEmail("juan@ejemplo.com");

        // then
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNombre()).isEqualTo("Juan");
    }

    @Test
    void debeRetornarVacioCuandoEmailNoExiste() {
        // when
        Optional<Cliente> resultado = clienteRepository.findByEmail("noexiste@ejemplo.com");

        // then
        assertThat(resultado).isEmpty();
    }

    @Test
    void debeVerificarExistenciaPorEmail() {
        // given
        Cliente cliente = new Cliente(null, "Juan", "juan@ejemplo.com");
        entityManager.persistAndFlush(cliente);

        // when & then
        assertThat(clienteRepository.existsByEmail("juan@ejemplo.com")).isTrue();
        assertThat(clienteRepository.existsByEmail("otro@ejemplo.com")).isFalse();
    }
}
```

---

## 5. Integration Tests con Testcontainers

### Referencia Rápida

```java
@SpringBootTest
@Testcontainers
class MiAppIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");
}
```

### Configuración Maven

```xml
<dependencies>
    <!-- Test dependencies -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-testcontainers</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>rabbitmq</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Guía Detallada

#### Base class para Integration Tests

```java
// test/java/com/acme/ecommerce/BaseIntegrationTest.java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Container
    @ServiceConnection
    protected static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @Container
    @ServiceConnection
    protected static RabbitMQContainer rabbitmq =
            new RabbitMQContainer("rabbitmq:3.12-management-alpine");

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    @LocalServerPort
    protected int port;

    protected String baseUrl() {
        return "http://localhost:" + port;
    }
}
```

#### Integration Test completo

```java
class PedidoIT extends BaseIntegrationTest {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @BeforeEach
    void setUp() {
        pedidoRepository.deleteAll();
        clienteRepository.deleteAll();
    }

    @Test
    void debeCrearPedidoCompleto() {
        // given - crear cliente primero
        Cliente cliente = clienteRepository.save(
                new Cliente(null, "Juan", "juan@ejemplo.com", BigDecimal.valueOf(1000))
        );

        CrearPedidoRequest request = new CrearPedidoRequest(
                cliente.getId(),
                List.of(new LineaItemRequest("PROD-1", 2, BigDecimal.valueOf(50)))
        );

        // when
        ResponseEntity<PedidoResponse> response = restTemplate.postForEntity(
                baseUrl() + "/api/v1/pedidos",
                request,
                PedidoResponse.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().total()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(response.getBody().estado()).isEqualTo("PENDIENTE");

        // verificar en base de datos
        Optional<Pedido> pedidoGuardado = pedidoRepository.findById(response.getBody().id());
        assertThat(pedidoGuardado).isPresent();
    }

    @Test
    void debeRetornar404CuandoClienteNoExiste() {
        // given
        CrearPedidoRequest request = new CrearPedidoRequest(
                999L,
                List.of(new LineaItemRequest("PROD-1", 1, BigDecimal.valueOf(50)))
        );

        // when
        ResponseEntity<ProblemDetail> response = restTemplate.postForEntity(
                baseUrl() + "/api/v1/pedidos",
                request,
                ProblemDetail.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getDetail()).contains("Cliente");
    }

    @Test
    void debeListarPedidosDeCliente() {
        // given
        Cliente cliente = clienteRepository.save(
                new Cliente(null, "Juan", "juan@ejemplo.com", BigDecimal.valueOf(1000))
        );
        pedidoRepository.save(new Pedido(null, cliente, List.of(), BigDecimal.ZERO, EstadoPedido.PENDIENTE));
        pedidoRepository.save(new Pedido(null, cliente, List.of(), BigDecimal.ZERO, EstadoPedido.COMPLETADO));

        // when
        ResponseEntity<PageResponse<PedidoResponse>> response = restTemplate.exchange(
                baseUrl() + "/api/v1/clientes/{id}/pedidos",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {},
                cliente.getId()
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().content()).hasSize(2);
    }
}
```

---

## 6. @SpringBootTest vs Slice Tests

### Referencia Rápida

| Anotación | Contexto | Velocidad | Usar para |
|-----------|----------|-----------|-----------|
| `@SpringBootTest` | Completo | Lento | Integration tests E2E |
| `@WebMvcTest` | Web MVC | Rápido | Controllers |
| `@DataJpaTest` | JPA | Rápido | Repositories |
| `@JsonTest` | JSON | Muy rápido | Serialización |

### Cuándo usar cada uno

```java
// @SpringBootTest - Contexto completo
// Usar cuando: necesitas todo el contexto, tests E2E
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ApplicationIT { }

// @WebMvcTest - Solo capa web
// Usar cuando: testear controllers sin BD ni servicios reales
@WebMvcTest(ClienteController.class)
class ClienteControllerTest { }

// @DataJpaTest - Solo capa de datos
// Usar cuando: testear queries y repositorios
@DataJpaTest
class ClienteRepositoryTest { }

// @JsonTest - Solo serialización
// Usar cuando: testear JSON serialization/deserialization
@JsonTest
class ClienteDtoJsonTest { }
```

### Combinar slices con @SpringBootTest

```java
@SpringBootTest
@AutoConfigureMockMvc  // Agrega MockMvc a @SpringBootTest
class ClienteControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testConContextoCompletoYMockMvc() throws Exception {
        mockMvc.perform(get("/api/v1/clientes"))
                .andExpect(status().isOk());
    }
}
```

---

## 7. Testing de RabbitMQ

```java
@SpringBootTest
@Testcontainers
class PedidoEventIT {

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitmq =
            new RabbitMQContainer("rabbitmq:3.12-management-alpine");

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private PedidoRepository pedidoRepository;

    @MockitoSpyBean  // @MockitoSpyBean reemplaza a @SpyBean (deprecated desde Spring Boot 3.4)
    // import: org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    private NotificacionService notificacionService;

    @Test
    void debePublicarEventoCuandoPedidoCreado() {
        // given
        PedidoCreadoEvent evento = new PedidoCreadoEvent(1L, 1L, BigDecimal.valueOf(100));

        // when
        rabbitTemplate.convertAndSend("pedidos.creados", evento);

        // then - esperar procesamiento asíncrono
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                verify(notificacionService).enviarConfirmacion(any())
        );
    }
}
```

---

## 8. Testing de Legacy Code

### Characterization Tests

✅ **Hacer**: Tests que documentan el comportamiento actual (antes de refactorizar)

```java
/**
 * Characterization test: documenta el comportamiento actual
 * del código legacy antes de refactorizar.
 * NO modifiques estos tests hasta entender completamente el sistema.
 */
@DisplayName("Characterization: CalculadoraPreciosLegacy")
class CalculadoraPreciosLegacyCharacterizationTest {

    private CalculadoraPreciosLegacy calculadora;

    @BeforeEach
    void setUp() {
        calculadora = new CalculadoraPreciosLegacy();
    }

    @Test
    @DisplayName("comportamiento actual: descuento se aplica después de IVA")
    void descuentoSeAplicaDespuesDeIva() {
        // Documentando comportamiento actual (posiblemente incorrecto)
        BigDecimal resultado = calculadora.calcularPrecioFinal(
                BigDecimal.valueOf(100),  // precio base
                21,                        // IVA 21%
                10                         // descuento 10%
        );

        // El código legacy aplica: (100 + 21) * 0.9 = 108.9
        // En vez de: (100 * 0.9) + (90 * 0.21) = 108.9
        assertThat(resultado).isEqualByComparingTo(BigDecimal.valueOf(108.9));
    }

    @Test
    @DisplayName("comportamiento actual: null devuelve cero")
    void nullDevuelveCero() {
        BigDecimal resultado = calculadora.calcularPrecioFinal(null, 21, 10);

        // Documentar: el código no lanza excepción, devuelve 0
        assertThat(resultado).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @ParameterizedTest
    @CsvSource({
            "100, 21, 0, 121.00",
            "100, 21, 10, 108.90",
            "100, 21, 100, 0.00",
            "0, 21, 10, 0.00"
    })
    @DisplayName("comportamiento actual: tabla de casos conocidos")
    void tablaDeCasosConocidos(BigDecimal precio, int iva, int descuento, BigDecimal esperado) {
        BigDecimal resultado = calculadora.calcularPrecioFinal(precio, iva, descuento);
        assertThat(resultado).isEqualByComparingTo(esperado);
    }
}
```

### Añadir tests a código sin ellos

```java
/**
 * Estrategia para añadir tests a código legacy:
 * 1. Escribir characterization tests (documentar comportamiento actual)
 * 2. Identificar "seams" (puntos donde inyectar dependencias)
 * 3. Extraer dependencias y mockearlas
 * 4. Refactorizar gradualmente con tests como red de seguridad
 */
class LegacyServiceRefactoringTest {

    // Paso 1: El código legacy tiene dependencia estática
    // ProcesadorLegacy.procesar() llama a DatabaseHelper.getInstance()

    // Paso 2: Crear wrapper para poder mockear
    interface DatabaseAccess {
        List<Registro> obtenerRegistros();
    }

    // Paso 3: Adaptar código legacy para usar interface
    static class DatabaseAccessAdapter implements DatabaseAccess {
        @Override
        public List<Registro> obtenerRegistros() {
            return DatabaseHelper.getInstance().getRegistros();
        }
    }

    // Paso 4: Ahora podemos testear con mock
    @Test
    void debeProcesarRegistrosCorrectamente() {
        // given
        DatabaseAccess mockDatabase = mock(DatabaseAccess.class);
        when(mockDatabase.obtenerRegistros()).thenReturn(List.of(
                new Registro(1, "dato1"),
                new Registro(2, "dato2")
        ));

        ProcesadorRefactorizado procesador = new ProcesadorRefactorizado(mockDatabase);

        // when
        Resultado resultado = procesador.procesar();

        // then
        assertThat(resultado.getCantidadProcesados()).isEqualTo(2);
    }
}
```

---

## 9. AssertJ Best Practices

```java
// Assertions fluidas y legibles
assertThat(cliente.getNombre()).isEqualTo("Juan");
assertThat(cliente.getEmail()).isNotBlank().contains("@");

// Colecciones
assertThat(pedidos)
        .hasSize(3)
        .extracting(Pedido::getEstado)
        .containsExactlyInAnyOrder(PENDIENTE, ENVIADO, COMPLETADO);

// Excepciones
assertThatThrownBy(() -> service.procesarPedidoInvalido())
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("inválido")
        .hasFieldOrPropertyWithValue("errorCode", "VALIDATION_ERROR");

// Soft assertions (reportar todos los fallos, no solo el primero)
SoftAssertions.assertSoftly(softly -> {
    softly.assertThat(cliente.getNombre()).isEqualTo("Juan");
    softly.assertThat(cliente.getEmail()).isEqualTo("juan@ejemplo.com");
    softly.assertThat(cliente.getCredito()).isPositive();
});

// Comparación de BigDecimal
assertThat(pedido.getTotal())
        .isEqualByComparingTo(BigDecimal.valueOf(100.00));

// Fechas
assertThat(pedido.getFechaCreacion())
        .isAfter(LocalDateTime.now().minusMinutes(1))
        .isBefore(LocalDateTime.now().plusMinutes(1));
```

---

## 10. Configuración de Tests

### application-test.yml

```yaml
spring:
  datasource:
    # Testcontainers configura automáticamente con @ServiceConnection
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  rabbitmq:
    # Testcontainers configura automáticamente con @ServiceConnection

logging:
  level:
    org.springframework.test: DEBUG
    org.testcontainers: INFO
    com.acme: DEBUG
```

### Configuración Maven Surefire/Failsafe

```xml
<build>
    <plugins>
        <!-- Unit tests -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
                <includes>
                    <include>**/*Test.java</include>
                </includes>
                <excludes>
                    <exclude>**/*IT.java</exclude>
                </excludes>
            </configuration>
        </plugin>

        <!-- Integration tests -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-failsafe-plugin</artifactId>
            <configuration>
                <includes>
                    <include>**/*IT.java</include>
                </includes>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>integration-test</goal>
                        <goal>verify</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

---

## Checklist de Testing

| Aspecto | ✅ Correcto | ❌ Incorrecto |
|---------|------------|---------------|
| Naming | `*Test.java`, `*IT.java` | `Test*.java`, nombres genéricos |
| Estructura | given-when-then | Código sin estructura |
| Mocks | Repos, clientes HTTP | Entidades, lógica de negocio |
| Assertions | AssertJ fluent | JUnit básico, muchos asserts sueltos |
| BD en tests | Testcontainers | H2 en memoria |
| Controllers | @WebMvcTest | @SpringBootTest para cada test |
| Repositories | @DataJpaTest | @SpringBootTest |
| Contexto | Reusar entre tests | Nuevo contexto por test |
| Legacy | Characterization tests primero | Refactorizar sin tests |

---

## Estructura de Tests

```
src/test/java/com/acme/ecommerce/
├── BaseIntegrationTest.java           ← Base para ITs
├── cliente/
│   ├── ClienteControllerTest.java     ← Unit test controller
│   ├── ClienteServiceTest.java        ← Unit test service
│   ├── ClienteRepositoryIT.java       ← Integration test repo
│   └── ClienteIT.java                 ← Integration test completo
├── pedido/
│   ├── PedidoServiceTest.java
│   ├── PedidoControllerTest.java
│   └── PedidoIT.java
└── shared/
    └── TestFixtures.java              ← Builders/factories para tests
```

---

## Próximos Pasos

Este documento cubre el **Área 4: Testing Estrategia Completa**.

Cuando estés listo, continuamos con el **Área 5: Convenciones de Código Java/Spring Boot** que incluirá:
- Naming conventions
- @Component vs @Service vs @Repository
- Constructor injection vs field injection
- Records vs POJOs para DTOs
- Optional best practices
- Lombok: qué usar, qué evitar

---

*Documento generado con Context7 - JUnit 5, Spring Boot 3.4.x, Testcontainers*
