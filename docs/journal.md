# OpenSpec — Diario de Aprendizaje

## Fecha: 2026-02-11

## Contexto

Estamos aprendiendo el flujo completo de OpenSpec usando un proyecto real: **vehicle-rental-platform**, una plataforma de alquiler de vehiculos basada en microservicios con arquitectura hexagonal, SAGA Orchestration y Outbox Pattern.

---

## Que es OpenSpec?

OpenSpec es una herramienta de **Spec-Driven Development (SDD)** — desarrollo guiado por especificaciones. En lugar de saltar directo al codigo, seguimos un flujo estructurado que nos obliga a pensar antes de implementar.

## El Flujo Completo (Ciclo de un Change)

Un "change" es un contenedor para todo el trabajo asociado a una tarea. Vive en `openspec/changes/<nombre>/`.

### 1. Explore (`/opsx:explore`)
- **Proposito**: Pensar el problema antes de comprometerse con una solucion.
- **Que hicimos**: Revisamos los docs de best practices (`docs/07` para Maven, `docs/01` para estructura), el `project.md` con la arquitectura, y analizamos que habia en el codebase (nada de codigo aun, solo docs).
- **Leccion**: Siempre investigar antes de implementar. Entender el contexto existente.

### 2. New (`/opsx:new <nombre>`)
- **Proposito**: Crear el contenedor del change.
- **Que hicimos**: `openspec new change "parent-pom-multi-module"` — creo la carpeta `openspec/changes/parent-pom-multi-module/` con un `.openspec.yaml`.
- **Leccion**: El nombre debe ser kebab-case y descriptivo. El change agrupa todos los artefactos de planificacion.

### 3. Proposal (proposal.md)
- **Proposito**: Capturar el **POR QUE** del cambio y **QUE** implica a alto nivel.
- **Que hicimos**: Redactamos un proposal con secciones Why, What Changes, Capabilities e Impact.
- **Leccion**: El proposal es el "elevator pitch". Debe ser conciso pero suficiente para que alguien entienda la motivacion sin leer codigo.

### 4. Specs (specs/)
- **Proposito**: Definir **QUE** estamos construyendo en terminos precisos y testeables.
- **Formato**: Usa WHEN/THEN/AND — se leen literalmente como test cases.
- **Que hicimos**: Creamos dos archivos de spec, uno por cada capability del proposal:
  - `specs/multi-module-build/spec.md` — 6 requirements con escenarios para parent POM, coordenadas, modulos, Java 21, dependencias centralizadas, y plugins.
  - `specs/build-enforcement/spec.md` — 4 requirements para enforcer de Maven 3.9+, Java 21+, convergencia de dependencias, y ban de librerias de logging legacy.
- **Leccion**: Cada capability del proposal se convierte en un directorio bajo `specs/`. Los requirements usan SHALL/MUST (lenguaje normativo) y cada uno tiene al menos un escenario WHEN/THEN que funciona como test case. Es clave revisar que las specs cubran todo lo mencionado en el proposal — el reviewer nos pidio agregar el ban de logging legacy que estaba en el proposal pero faltaba en las specs.
- **Estado**: Completado.

### 5. Design (design.md)
- **Proposito**: Capturar **COMO** lo vamos a construir — decisiones tecnicas, tradeoffs.
- **Que hicimos**: Documentamos 4 decisiones clave con rationale y alternativas consideradas:
  1. **Heredar de starter-parent vs BOM import** — starter-parent porque no tenemos parent corporativo y nos da config de plugins gratis.
  2. **Modulos flat con paths vs aggregators intermedios** — paths directos (`reservation-service/reservation-domain`) para evitar POMs intermedios innecesarios.
  3. **Lombok y test como dependencias globales** — para no repetirlas en 13 child POMs. `spring-boot-starter-test` en test scope no rompe la regla de "domain sin Spring" porque no afecta el compile classpath.
  4. **Spring Boot 3.4.1** — coincide con la version de los docs de best practices.
- **Leccion**: Un buen design.md no repite las specs. Se enfoca en decisiones (Decision + Rationale + Alternative considered) y en riesgos con sus mitigaciones. Tambien es importante definir Non-Goals para evitar scope creep.
- **Estado**: Completado.

### 6. Tasks (tasks.md)
- **Proposito**: Desglosar el trabajo en checkboxes concretos que guian la implementacion.
- **Que hicimos**: Creamos 12 tareas agrupadas en 8 secciones logicas: POM Skeleton, Modules, Properties, Dependency Management, Global Dependencies, Plugin Management, Build Enforcement, y Verify.
- **Leccion**: Las tareas deben seguir estrictamente el formato `- [ ] X.Y Descripcion` porque la fase Apply las parsea para trackear progreso. Deben estar ordenadas por dependencia (lo que se necesita primero va primero). Cada tarea debe ser verificable — sabes cuando esta terminada. Referenciamos las specs para saber *que* construir y el design para saber *como*.
- **Estado**: Completado.

### 7. Apply (`/opsx:apply`)
- **Proposito**: Implementar las tareas una por una, marcandolas como completadas.
- **Que hicimos**: Implementamos las 12 tareas secuencialmente, construyendo el `pom.xml` seccion por seccion:
  1. Skeleton (XML declaration, parent, coordenadas)
  2. Modulos (13 modulos con paths directos)
  3. Properties (Java 21, encoding, versiones externas)
  4. Dependency Management (common, MapStruct, SpringDoc, ArchUnit)
  5. Global Dependencies (Lombok optional, test starter)
  6. Plugin Management (compiler, boot, surefire, failsafe)
  7. Enforcer (Maven 3.9+, Java 21+, convergencia, logging baneado)
  8. Validacion XML con PowerShell
- **Leccion**: Cada tarea se anuncia antes de implementarse y se marca `[x]` al completar. Las tareas referencian las specs ("la spec dice 13 modulos") y el design ("Decision 2: flat paths") para justificar decisiones. Agrupar tareas relacionadas (como 1.1-1.3 del skeleton) es aceptable cuando forman un bloque coherente.
- **Estado**: Completado.

### 8. Archive (`/opsx:archive`)
- **Proposito**: Archivar el change completado como registro historico del proyecto.
- **Que hicimos**: `openspec archive "parent-pom-multi-module" --yes` — dos cosas pasaron automaticamente:
  1. El change se movio a `openspec/changes/archive/2026-02-11-parent-pom-multi-module/` con todos sus artefactos intactos.
  2. Las specs delta se sincronizaron al source of truth en `openspec/specs/` — creando `build-enforcement/spec.md` y `multi-module-build/spec.md` con los 10 requirements.
- **Leccion**: El archive no solo guarda el historial — tambien actualiza las specs principales del proyecto. Es por eso que las specs en el change se llaman "delta specs": representan lo que *cambio*, y al archivar se fusionan con el estado actual del sistema. Esto mantiene `openspec/specs/` siempre actualizado como fuente de verdad.
- **Estado**: Completado.

---

## Fecha: 2026-02-12

## Segundo Ciclo: Common Module — Shared Kernel

### Contexto

Con el Parent POM completado en el primer ciclo, el siguiente paso natural era el modulo `common` — el shared kernel del que dependen los 4 microservicios. Sin el, ningun servicio puede definir entidades, aggregate roots, ni value objects. Es el cimiento del dominio.

### Que construimos

**1 POM + 7 clases Java + 4 clases de test = 31 tests pasando**

```
common/src/main/java/com/vehiclerental/common/
├── domain/
│   ├── entity/
│   │   ├── BaseEntity.java        ← abstract, equals/hashCode por ID
│   │   └── AggregateRoot.java     ← extiende BaseEntity, acumula domain events
│   ├── event/
│   │   └── DomainEvent.java       ← interface (no abstract class)
│   ├── vo/
│   │   └── Money.java             ← record con aritmetica currency-safe
│   └── exception/
│       └── DomainException.java   ← abstract, String errorCode (no HttpStatus)
└── api/
    ├── ApiResponse.java           ← record generico con factory of()
    └── ApiMetadata.java           ← record con timestamp + requestId
```

### Flujo OpenSpec — Fast-Forward

A diferencia del primer ciclo donde usamos `/opsx:continue` paso a paso, esta vez usamos `/opsx:ff` (fast-forward) para generar todos los artefactos de una vez: proposal, specs, design y tasks. Esto fue posible porque ya entendiamos el flujo y teniamos claridad sobre que construir.

- **Leccion**: `/opsx:ff` es ideal cuando ya tienes claridad sobre el cambio. Si necesitas explorar o hay ambiguedad, mejor ir paso a paso con `/opsx:continue`.

### Decisiones de Diseno Clave

#### Decision 1: String errorCode en vez de HttpStatus
- El dominio habla lenguaje de negocio: `"CUSTOMER_NOT_FOUND"`, no `404`.
- El mapeo a HTTP queda en infraestructura (GlobalExceptionHandler).
- Esto mantiene el modulo common con **cero dependencias Spring** en produccion.
- **Leccion**: Es tentador poner `HttpStatus` en las excepciones por conveniencia, pero viola la arquitectura hexagonal. El dominio no sabe que existe HTTP.

#### Decision 2: DomainEvent como interface, no abstract class
- Los domain events concretos seran records inmutables (`record ReservationCreatedEvent(...) implements DomainEvent`).
- Los records no pueden extender clases, pero si implementar interfaces.
- Con `interface DomainEvent { UUID eventId(); Instant occurredOn(); }`, los accessors del record satisfacen el contrato automaticamente.
- **Leccion**: Pensar en como se van a *usar* las abstracciones, no solo en como se definen. Si los eventos van a ser records, la base tiene que ser interface.

#### Decision 3: Records para value objects y API responses
- Money, ApiResponse, ApiMetadata son records de Java.
- Inmutables por diseno, equals/hashCode/toString automaticos.
- Jackson 2.12+ los serializa sin annotations, manteniendo cero dependencias.
- **Leccion**: Los records son la solucion nativa de Java para datos inmutables. Elegirlos sobre Lombok `@Value` reduce dependencias y es mas idiomatico.

#### Decision 4: Scope reducido (~8 clases en vez de ~13)
- No implementamos subclases de excepcion (NotFoundException, etc.) ni PagedResponse.
- Se dejan como extensiones naturales para cuando un servicio las necesite.
- **Leccion**: En un POC, menos es mas. Implementar solo lo necesario para desbloquear el siguiente paso (los servicios).

### Test-First en la Practica

Este fue el primer ciclo donde aplicamos test-first de verdad. El flujo fue:

1. Escribir los tests **antes** de las clases (BaseEntityTest, AggregateRootTest, MoneyTest, DomainExceptionTest)
2. Los escenarios WHEN/THEN de las specs se tradujeron directamente a metodos de test
3. Implementar las clases para que los tests pasen
4. Build final: 31 tests, 0 fallos

Ejemplo concreto — la spec decia:
> WHEN two BaseEntity instances have the same non-null ID
> THEN equals() SHALL return true

Y el test se escribio literalmente asi:
```java
@Test
void sameNonNullId_shouldBeEqual() {
    UUID id = UUID.randomUUID();
    var entity1 = new TestEntity(id);
    var entity2 = new TestEntity(id);
    assertThat(entity1).isEqualTo(entity2);
}
```

- **Leccion**: Las specs bien escritas hacen que test-first sea casi mecanico. Cada WHEN/THEN es un test. No hay que inventar — solo traducir.

### Particularidades del Apply

- **El parent POM declara modulos que no existen todavia** (reservation-service, customer-service, etc.), asi que `mvn clean install -pl common` desde la raiz falla. La solucion fue ejecutar `mvn clean install` directamente desde `common/`. Esto funciona porque el POM del common hereda del parent sin necesitar que los otros modulos existan.
- **`spring-boot-starter-test` se hereda del parent** como dependencia global en scope `test`. Esto es aceptable — no afecta el classpath de produccion. La verificacion de "zero Spring imports" se aplica solo a `common/src/main/`.
- **Leccion**: En un multi-module con modulos aun no creados, se puede buildear un modulo individual desde su directorio. Maven resuelve el parent POM sin necesitar parsear todos los sibling modules.

### Archive y Sync de Specs

Al archivar, se sincronizaron 4 nuevas capabilities al source of truth en `openspec/specs/`:
- `domain-base-classes` — BaseEntity, AggregateRoot, DomainEvent (10 requirements)
- `shared-value-objects` — Money con aritmetica (9 requirements)
- `domain-exceptions` — DomainException con errorCode (5 requirements)
- `api-response-wrapper` — ApiResponse + ApiMetadata (6 requirements)

El proyecto ahora tiene 6 capabilities especificadas en total (2 del primer ciclo + 4 de este).

---

## Fecha: 2026-02-14

## Tercer Ciclo: Customer Domain — Primer Modulo de Negocio Real

### Contexto

Con el Parent POM y el Common Shared Kernel completados, el tercer change es el primero que toca logica de negocio real. El modulo `customer-domain` es el dominio del Customer Service — el mas simple de los 4 servicios (sin SAGA, sin dependencias cross-service). Es el candidato ideal para validar que los patrones DDD tacticos del shared kernel funcionan en la practica y establecer el modelo que seguiran los otros 3 servicios.

### Que construimos

**1 POM + 11 clases Java + 7 clases de test = 58 tests pasando**

```
customer-service/customer-domain/src/main/java/com/vehiclerental/customer/domain/
├── model/
│   ├── aggregate/
│   │   └── Customer.java                  ← Aggregate Root (create, reconstruct, suspend, activate, delete)
│   └── vo/
│       ├── CustomerId.java                ← Typed ID (record, UUID)
│       ├── Email.java                     ← Value Object (record, regex validation)
│       ├── PhoneNumber.java               ← Value Object (record, formato y longitud)
│       └── CustomerStatus.java            ← Enum (ACTIVE, SUSPENDED, DELETED)
├── event/
│   ├── CustomerCreatedEvent.java          ← Snapshot completo (customerId, firstName, lastName, email)
│   ├── CustomerSuspendedEvent.java        ← Solo customerId
│   ├── CustomerActivatedEvent.java        ← Solo customerId
│   └── CustomerDeletedEvent.java          ← Solo customerId
├── exception/
│   └── CustomerDomainException.java       ← Extiende DomainException del common
└── port/
    └── output/
        └── CustomerRepository.java        ← Interface (save, findById), solo tipos de dominio
```

### Flujo OpenSpec — Apply Directo

Este ciclo fue diferente: los artefactos (proposal, specs, design, tasks) ya estaban generados con `/opsx:ff` en una sesion previa. Arrancamos directamente con `/opsx:apply`, que leyo los 23 tasks del `tasks.md` y los fue implementando uno a uno.

### Decisiones de Diseno Clave

#### Decision 1: CustomerDomainException como excepcion unica del dominio
- Una sola clase de excepcion para todo el modulo customer-domain.
- Se diferencia por el `errorCode` (e.g., `"CUSTOMER_INVALID_STATE"`, `"CUSTOMER_EMAIL_INVALID"`, `"CUSTOMER_ID_NULL"`).
- No hay subclases como `CustomerNotFoundException` — eso pertenece a la capa de aplicacion.
- **Leccion**: Una excepcion con error codes es mas simple y extensible que una jerarquia de excepciones. El errorCode es lo que importa para el API response, no el tipo de la clase.

#### Decision 2: Factory methods `create()` y `reconstruct()` en vez de constructor publico
- `Customer.create(firstName, lastName, email, phone)` — genera UUID, status ACTIVE, registra `CustomerCreatedEvent`.
- `Customer.reconstruct(id, firstName, lastName, email, phone, status, createdAt)` — reconstruye desde persistencia sin validar ni emitir eventos.
- Constructor privado, sin acceso publico.
- **Leccion**: Este patron separa claramente la creacion de negocio (con invariantes y eventos) de la rehidratacion tecnica (desde JPA/base de datos). Es fundamental en DDD con arquitectura hexagonal porque el adapter de infraestructura necesita reconstruir el agregado sin disparar logica de dominio.

#### Decision 3: Events con snapshot vs solo ID
- `CustomerCreatedEvent` lleva snapshot completo: `customerId`, `firstName`, `lastName`, `email`. Los consumidores (como Reservation Service) pueden necesitar los datos sin hacer query aparte.
- Los eventos de lifecycle (`Suspended`, `Activated`, `Deleted`) solo llevan `customerId` — los consumidores solo necesitan saber *que le paso a quien*.
- **Leccion**: Pensar en los consumidores del evento al decidir que datos incluir. El evento de creacion es el "announcement" del agregado al mundo, los de lifecycle son notificaciones simples.

#### Decision 4: Output port (CustomerRepository) en domain, no en application
- La interface `CustomerRepository` vive en `domain/port/output/` porque no tenemos modulo de aplicacion todavia.
- Es un contrato puro (solo tipos de dominio, cero Spring/JPA).
- **Leccion**: Es pragmatico. La interface no tiene dependencias de framework, asi que moverla al modulo de aplicacion luego es un refactor trivial. No vale la pena crear el modulo de aplicacion solo para alojar una interface.

### Test-First: Patron de Dependencias

El orden de las tasks en el `tasks.md` original seguia la secuencia logica por secciones (1. Module Setup, 2. Value Objects, 3. Exception, 4. Events, 5. Aggregate). Pero al implementar, la excepcion se necesitaba *antes* que los value objects (porque `CustomerId`, `Email` y `PhoneNumber` lanzan `CustomerDomainException` en sus validaciones).

**Lo que hicimos**: Implementamos `CustomerDomainException` (seccion 3) primero, y luego seguimos con value objects (seccion 2), events (seccion 4) y aggregate (seccion 5). Todas las tareas se completaron, pero el orden de ejecucion real fue diferente al orden del archivo.

- **Leccion**: Al escribir tasks para un futuro `/opsx:apply`, ordenarlas por **dependencia de compilacion**, no por agrupacion logica. Las excepciones de dominio se usan en todas partes — van primero. Alternativa: agrupar en "foundational" (exception, enum) y "derived" (VOs, events, aggregate).

### Build Workaround: `mvn install -N`

El root POM declara 13 modulos, pero solo existen `common` y `customer-domain`. Al intentar buildear con `-pl` desde la raiz, Maven falla porque no puede parsear el POM si hay modulos declarados que no existen.

**Solucion**: Tres pasos secuenciales:
1. `mvn install -N` desde la raiz — instala *solo* el parent POM sin descender a modulos (`-N` = non-recursive).
2. `mvn clean install` desde `common/` — compila e instala el shared kernel.
3. `mvn clean install` desde `customer-service/customer-domain/` — compila el dominio con los 58 tests.

- **Leccion**: `-N` (non-recursive) es esencial en multi-modules donde los modulos se construyen incrementalmente. Es el mismo workaround del segundo ciclo, pero ahora con un paso mas (common antes de customer-domain). A medida que agreguemos servicios, la cadena de builds individuales va a crecer. Eventualmente habra que crear los POMs minimos para los modulos faltantes, o usar profiles de Maven para gestionar builds parciales.

### Verificacion Final

- **58 tests, 0 fallos** — CustomerIdTest (3), EmailTest (7), PhoneNumberTest (7), CustomerDomainExceptionTest (4), CustomerDomainEventsTest (16), CustomerTest (10), CustomerLifecycleTest (11).
- **Zero Spring imports** en `customer-service/customer-domain/src/main/` — confirmado con grep.
- **BUILD SUCCESS** en `mvn clean install`.

### Que falta para Customer Service completo

Este change solo cubre el dominio. Faltan dos changes mas:
1. **Application layer**: Use cases (CreateCustomerUseCase, SuspendCustomerUseCase, etc.), commands, DTOs, input ports, `@Transactional`.
2. **Infrastructure + Container**: JPA entities separadas, persistence adapter, REST controller, `BeanConfiguration`, `@SpringBootApplication`.

---

## Comandos Clave

| Comando | Que hace |
|---------|----------|
| `/opsx:explore` | Pensar problemas sin tocar codigo |
| `/opsx:new <nombre>` | Crear un change nuevo, paso a paso |
| `/opsx:ff <nombre>` | Fast-forward: crear todos los artefactos de una vez |
| `/opsx:continue <nombre>` | Continuar un change existente |
| `/opsx:apply <nombre>` | Implementar tareas |
| `/opsx:verify <nombre>` | Verificar que la implementacion coincide con los artefactos |
| `/opsx:archive <nombre>` | Archivar cuando termine |

---

## Decisiones Tomadas

### Tarea elegida: Parent POM
- **Por que**: Es la base de todo el proyecto. Sin el, nada compila. Es pequeno (~150 lineas), autocontenido, y perfecto para aprender el flujo sin perderse en detalles de implementacion.
- **Alternativas descartadas**: Common module (mas archivos), scaffold de reservation-service (mas complejo).

### Hallazgos del Explore
- El proyecto tiene 20 docs de best practices con ejemplos de codigo y checklists.
- `project.md` define la arquitectura completa: 4 servicios, cada uno con 3 modulos Maven (domain, infrastructure, container).
- Java 21 (no 17) para Virtual Threads.
- Spring Boot 3.4.x como parent.
- 13 modulos totales: common + (reservation + customer + payment + fleet) x 3 submodulos.

---

## Reflexiones

### Del primer ciclo (Parent POM)
- **El flujo parece mucho overhead para una tarea tan chica** — y lo es. Pero el punto es aprender el ritmo. Para features grandes, este flujo previene errores costosos y asegura que todos entienden que se esta construyendo y por que.
- **Explore antes de todo**: Leer los docs de best practices antes de generar codigo evita tener que rehacer cosas despues.
- **Proposal como contrato**: Si el proposal no convence, no se sigue adelante. Ahorra implementar algo que nadie pidio.
- **Las specs deben cubrir el proposal completo**: Si el proposal menciona algo (como banear logging legacy), las specs tambien deben incluirlo. Es facil olvidar detalles al pasar de un artefacto a otro — la revision humana es fundamental.
- **Un spec file por capability**: La estructura `specs/<capability>/spec.md` mantiene las cosas organizadas y facilita encontrar que specs aplican a que parte del sistema.
- **GroupId como decision consciente**: Elegimos `com.vehiclerental` para el proyecto. Parece trivial, pero en un proyecto real esta decision afecta namespaces, package names, y es dificil de cambiar despues. Mejor decidirlo explicitamente que dejarlo al azar.

### Del segundo ciclo (Common Module)
- **Test-first funciona cuando las specs son buenas**: Los escenarios WHEN/THEN de las specs se mapean 1:1 a tests. No hay ambiguedad. Escribir los tests primero se siente natural, no forzado.
- **Fast-forward (`/opsx:ff`) vs paso a paso**: Cuando ya sabes que quieres construir, ff ahorra tiempo. Pero para features donde necesitas explorar, el flujo paso a paso con review humano en cada artefacto es mejor.
- **La arquitectura hexagonal se defiende con restricciones concretas**: "Zero Spring in domain" no es un principio vago — es una regla verificable (`grep -r "org.springframework" common/src/main/`). Las restricciones que se pueden automatizar son las que realmente se cumplen.
- **Records de Java son perfectos para DDD value objects**: Inmutabilidad, equals por valor, compact constructors para validacion. Money como record es mas limpio que con Lombok.
- **El shared kernel es minimo por diseno**: Solo lo que todos necesitan. Las excepciones concretas, typed IDs, y otros artefactos especificos pertenecen a cada servicio. Resistir la tentacion de "ya que estamos, agreguemos X" mantiene el kernel limpio.
- **Build parcial en multi-module**: Se puede compilar un modulo individual desde su directorio aunque los sibling modules no existan todavia. Util en las primeras fases del proyecto.

### Del tercer ciclo (Customer Domain)
- **El shared kernel se valida cuando lo usas**: Disenar `AggregateRoot<ID>`, `DomainEvent`, `DomainException` en abstracto es una cosa. Usarlos de verdad con `Customer extends AggregateRoot<CustomerId>` y `CustomerCreatedEvent implements DomainEvent` es donde se confirma que las abstracciones funcionan. Los 58 tests pasaron sin tener que modificar nada en common — buena senal.
- **Factory methods son el patron correcto para Aggregate Roots**: `create()` vs `reconstruct()` es una distincion fundamental que se descubre al pensar en como la infraestructura va a rehidratar el agregado. Sin `reconstruct()`, el adapter de JPA tendria que hackear la creacion para evitar disparar eventos.
- **El orden de tasks importa para la implementacion**: Las tasks agrupadas por concepto (VOs, Events, Aggregate) se leen bien como plan, pero al implementar las dependencias de compilacion mandan. Excepciones y enums primero, VOs despues, events despues, aggregate al final.
- **Records de Java para domain events funcionan perfectamente**: `record CustomerCreatedEvent(UUID eventId, Instant occurredOn, ...) implements DomainEvent` — los accessors del record satisfacen la interface automaticamente. Validacion en compact constructor. Inmutabilidad gratis. Es el patron definitivo para DDD events en Java 21.
- **Un dominio bien disenado es sorprendentemente pequeno**: 11 clases, la mayoria records de <20 lineas. La complejidad esta en las reglas de negocio (transiciones de estado, validaciones), no en la cantidad de codigo. Esto es DDD bien hecho — el modelo refleja el negocio sin infraestructura.

---

## Fecha: 2026-02-14

## Cuarto Ciclo: Customer Application — La Capa de Orquestacion

### Contexto

Con el dominio del Customer Service completo (Aggregate Root, Value Objects, Domain Events, output port), faltaba la pieza que conecta el mundo exterior con el dominio: la capa de aplicacion. Sin ella, los controllers REST no tienen a quien llamar — en arquitectura hexagonal, la infraestructura nunca accede al dominio directamente, siempre pasa por los input ports de la aplicacion.

Este change completa el patron de 4 modulos Maven por servicio que prescribe `docs/17`: domain, **application**, infrastructure, container. El root POM solo declaraba 3 (domain, infrastructure, container); ahora customer-service tiene los 4.

### Que construimos

**1 POM + 13 clases Java + 3 clases de test = 17 tests pasando**

```
customer-service/customer-application/src/main/java/com/vehiclerental/customer/application/
├── port/
│   ├── input/
│   │   ├── CreateCustomerUseCase.java       ← interface: CustomerResponse execute(CreateCustomerCommand)
│   │   ├── GetCustomerUseCase.java          ← interface: CustomerResponse execute(GetCustomerCommand)
│   │   ├── SuspendCustomerUseCase.java      ← interface: void execute(SuspendCustomerCommand)
│   │   ├── ActivateCustomerUseCase.java     ← interface: void execute(ActivateCustomerCommand)
│   │   └── DeleteCustomerUseCase.java       ← interface: void execute(DeleteCustomerCommand)
│   └── output/
│       └── CustomerDomainEventPublisher.java ← interface: void publish(List<DomainEvent>)
├── service/
│   └── CustomerApplicationService.java      ← implementa los 5 input ports, zero business logic
├── dto/
│   ├── command/
│   │   ├── CreateCustomerCommand.java       ← record (firstName, lastName, email, phone)
│   │   ├── GetCustomerCommand.java          ← record (customerId)
│   │   ├── SuspendCustomerCommand.java      ← record (customerId)
│   │   ├── ActivateCustomerCommand.java     ← record (customerId)
│   │   └── DeleteCustomerCommand.java       ← record (customerId)
│   └── response/
│       └── CustomerResponse.java            ← record (customerId, firstName, lastName, email, phone, status, createdAt)
├── mapper/
│   └── CustomerApplicationMapper.java       ← plain Java, toResponse(Customer) → CustomerResponse
└── exception/
    └── CustomerNotFoundException.java       ← extends RuntimeException (NO CustomerDomainException)
```

### Flujo OpenSpec — Apply Directo (otra vez)

Igual que el tercer ciclo, los artefactos (proposal, specs, design, tasks) ya estaban generados previamente. Arrancamos con `/opsx:apply` directo sobre los 26 tasks del `tasks.md`.

### Decisiones de Diseno Clave

#### Decision 1: CustomerRepository se queda en domain
- El output port `CustomerRepository` vive en `customer-domain/port/output/` y la aplicacion lo usa directamente.
- La interface usa solo tipos de dominio (`Customer`, `CustomerId`), no tiene dependencias de framework.
- Moverla a application no aporta nada funcional y solo generaria churn innecesario.
- **Leccion**: Pragmatismo sobre purismo. Si la interface es framework-free y compila bien donde esta, no la muevas solo por dogma. Mover es un refactor trivial si algun dia se necesita.

#### Decision 2: Un Application Service para los 5 use cases
- `CustomerApplicationService` implementa las 5 interfaces de input port.
- Con 5 metodos simples de orquestacion, no justifica 5 clases separadas.
- Si un use case crece (e.g., SAGA), se puede extraer facilmente porque cada uno ya tiene su propia interface.
- **Leccion**: El principio "one class per use case" es un guideline, no un mandato. Con 5 use cases simples, 5 archivos es over-engineering. Las interfaces *si* son una por use case — eso permite extraer despues.

#### Decision 3: Commands con String, no CustomerId
- Los commands llevan `String customerId`, no el typed ID del dominio.
- La conversion `String → CustomerId` es responsabilidad del Application Service — es la "anti-corruption layer".
- **Leccion**: Los DTOs de la capa de aplicacion son la frontera entre el mundo exterior (REST, mensajes) y el dominio. El mundo exterior habla en strings y UUIDs. El dominio habla en typed IDs. La traduccion es la responsabilidad de la capa de aplicacion.

#### Decision 4: CustomerNotFoundException extends RuntimeException (no CustomerDomainException)
- "No encontrado" es un concepto de aplicacion, no de dominio. El dominio no sabe de queries ni de persistencia.
- Separar las excepciones permite al `GlobalExceptionHandler` mapear: `CustomerDomainException → 422`, `CustomerNotFoundException → 404`.
- **Leccion**: No toda excepcion del Customer Service es una "excepcion de dominio". Las excepciones de dominio son violaciones de invariantes (estado invalido, email malformado). "No encontrado" es algo que solo existe cuando intentas cargar algo de la base de datos — eso es aplicacion.

#### Decision 5: Lifecycle use cases retornan void
- Suspend, Activate, Delete retornan `void` — son comandos, no queries (CQS).
- Si el caller necesita el estado actualizado, hace un Get separado.
- **Leccion**: CQS (Command Query Separation) mantiene la API limpia. Un `suspend()` que retorna el customer actualizado mezcla responsabilidades.

#### Decision 6: Mapper manual en vez de MapStruct
- `CustomerApplicationMapper` es una clase plain Java con un metodo `toResponse()` de ~10 lineas.
- MapStruct agrega annotation processing, generacion de codigo y configuracion — todo para mapear 7 campos.
- **Leccion**: MapStruct tiene sentido con 10+ entidades y mappings complejos. Para 7 campos, un metodo manual es mas legible, debuggeable y no agrega dependencias.

### Patron de Orquestacion del Application Service

Todos los metodos de escritura siguen el mismo flujo:

```
1. Convertir command → tipos de dominio
2. Ejecutar operacion de dominio (create/suspend/activate/delete)
3. Persistir via CustomerRepository.save()
4. Publicar eventos via CustomerDomainEventPublisher.publish()
5. Limpiar eventos del agregado: customer.clearDomainEvents()
6. (Solo create) Retornar CustomerResponse via mapper
```

- **Leccion**: La capa de aplicacion es un template method glorificado. No hay logica de negocio — solo coreografia. Si ves un `if` en el Application Service, probablemente deberia estar en el dominio. La unica logica propia es "si no existe, lanzar CustomerNotFoundException".

### Tests del Application Service

Los tests verifican no solo el comportamiento sino tambien las restricciones arquitectonicas:

1. **Flujo de create**: save → publish → clearDomainEvents → return response (con InOrder de Mockito)
2. **Flujo de get**: findById → mapper → response, y el caso not found
3. **Flujo de suspend/activate/delete**: findById → operacion de dominio → save → publish → clearDomainEvents
4. **Sin @Service ni @Component**: verificacion por reflexion
5. **@Transactional en writes, @Transactional(readOnly=true) en gets**: verificacion por reflexion

- **Leccion**: Los tests de reflexion para anotaciones son una herramienta poderosa para validar restricciones arquitectonicas en unit tests. Si alguien agrega `@Service` por error, el test falla. Es mas rapido que ArchUnit para verificaciones puntuales.

### Verificacion Final

- **17 tests, 0 fallos** — CustomerNotFoundExceptionTest (3), CustomerApplicationMapperTest (2), CustomerApplicationServiceTest (12 tests en 5 nested classes).
- **Zero `@Service`/`@Component`** en `customer-application/src/main/` — confirmado.
- **customer-domain sigue compilando** — BUILD SUCCESS, 58 tests previos no se rompieron.

### Root POM: dependencyManagement actualizado

Ademas de agregar el modulo `customer-service/customer-application`, tambien agregamos `customer-domain` y `customer-application` al `<dependencyManagement>` del root POM. Esto centraliza las versiones y permite que los modulos dependientes los referencien sin especificar version.

- **Leccion**: Cada vez que se agrega un modulo interno, deberia ir al `dependencyManagement` del parent. Esto no se habia hecho para `customer-domain` en el ciclo anterior — quedo pendiente y lo resolvimos ahora.

### Que falta para Customer Service completo

Este change cubre domain + application. Falta un change mas:
1. **Infrastructure + Container**: JPA entities separadas con mappers, persistence adapter (`CustomerRepositoryImpl`), REST controller que usa los input ports, `BeanConfiguration` que registra manualmente el `CustomerApplicationService` y el mapper, `@SpringBootApplication`, y los adapters del `CustomerDomainEventPublisher` (outbox pattern).

### Reflexiones del cuarto ciclo

- **La capa de aplicacion es "thin by design"**: 13 clases y la mayoria son interfaces o records de 1-5 lineas. Solo el `CustomerApplicationService` tiene logica real, y son ~60 lineas de pura orquestacion. Esto es correcto — si la capa de aplicacion es gruesa, el dominio esta anemico.
- **Records de Java siguen siendo la eleccion correcta para DTOs**: Commands y responses como records son inmutables, claros y sin boilerplate. El pattern `record CreateCustomerCommand(String firstName, ...)` es exactamente lo que Java 21 ofrece para este caso de uso.
- **El flujo de apply fue rapido porque los artefactos eran claros**: Con las specs y el design bien definidos, implementar los 26 tasks fue mecanico — traducir specs a codigo, verificar con tests. No hubo ambiguedades ni decisiones ad-hoc.
- **El multi-module build sigue requiriendo workarounds**: Los modulos que no existen (reservation-*, payment-*, fleet-*) impiden `mvn -pl` desde la raiz. El workaround de buildear modulo por modulo funciona pero no escala. Eventualmente habra que crear POMs minimos para los modulos faltantes o usar profiles.
- **La cadena de dependencias es clara y unidireccional**: `common ← customer-domain ← customer-application`. No hay dependencias circulares ni shortcuts. Esto es la promesa de la arquitectura hexagonal cumplida en la practica.

---

## Fecha: 2026-02-14

## Quinto Ciclo: Customer Infrastructure + Container — El Microservicio Cobra Vida

### Contexto

Con el dominio (58 tests) y la aplicacion (17 tests) completos, el Customer Service seguia siendo codigo sin vida propia: no podia arrancar, recibir peticiones HTTP ni persistir datos. Este change añade las dos capas externas de la arquitectura hexagonal: **infrastructure** (adaptadores de entrada y salida) y **container** (ensamblaje Spring Boot). Es el change mas grande hasta ahora — 25 tareas, ~20 ficheros de produccion, 11 tests de integracion — y el primero que produce un microservicio ejecutable.

### Que construimos

**2 POMs + ~20 ficheros Java + 1 SQL + 2 YMLs + 3 clases de test = 11 tests de integracion pasando**

```
customer-service/customer-infrastructure/src/main/java/com/vehiclerental/customer/infrastructure/
├── adapter/
│   ├── input/
│   │   └── rest/
│   │       ├── CustomerController.java           ← @RestController, inyecta 5 input ports
│   │       └── dto/
│   │           └── CreateCustomerRequest.java     ← record con @NotBlank, @Email
│   └── output/
│       ├── persistence/
│       │   ├── CustomerJpaRepository.java         ← JpaRepository<CustomerJpaEntity, UUID>
│       │   ├── CustomerRepositoryAdapter.java     ← @Component, implementa CustomerRepository
│       │   ├── entity/
│       │   │   └── CustomerJpaEntity.java         ← @Entity, separada del dominio
│       │   └── mapper/
│       │       └── CustomerPersistenceMapper.java ← Domain ↔ JPA, bidireccional
│       └── event/
│           └── CustomerDomainEventPublisherAdapter.java ← Logger no-op
└── config/
    └── GlobalExceptionHandler.java                ← @RestControllerAdvice

customer-service/customer-container/src/main/java/com/vehiclerental/customer/
├── CustomerServiceApplication.java                ← @SpringBootApplication
└── config/
    └── BeanConfiguration.java                     ← @Configuration, registra beans manuales

customer-service/customer-container/src/main/resources/
├── application.yml                                ← PostgreSQL, Flyway, puerto 8181
├── application-test.yml                           ← Testcontainers JDBC URL
└── db/migration/
    └── V1__create_customer_table.sql              ← Flyway migration
```

### Flujo OpenSpec — La leccion mas valiosa: `/opsx:ff` vs `/opsx:new` + `/opsx:continue`

Este fue el primer change donde usamos `/opsx:new` + `/opsx:continue` paso a paso en lugar de `/opsx:ff` (fast-forward). La diferencia fue **enorme**.

#### El problema con `/opsx:ff` (ciclos 2, 3 y 4)

En los ciclos anteriores usamos `/opsx:ff` para generar todos los artefactos de golpe. El resultado fue:
- Los artefactos salian "contaminados": secciones copiadas del contexto del proyecto que no debian estar ahi, redaccion generica, decisiones sin justificar.
- Hubo que revisar y corregir cada artefacto manualmente despues de generarlo.
- El fast-forward prioriza velocidad sobre calidad — genera todo de una vez pero sin la pausa para reflexionar entre artefactos.

#### El enfoque con `/opsx:new` + `/opsx:continue` (este ciclo)

Aqui el flujo fue diferente:
1. **`/opsx:new customer-infrastructure-and-container`** — creo el contenedor del change.
2. **Proposal escrito por el usuario** — en vez de dejar que la IA lo generase, el usuario redacto la proposal con su propio criterio. Sabia exactamente que queria incluir y que excluir.
3. **`/opsx:continue`** → genero el design.md leyendo la proposal como contexto. 10 decisiones con rationale y alternativas.
4. **`/opsx:continue`** → genero las 5 specs (4 nuevas + 1 modificada) leyendo proposal + design.
5. **`/opsx:continue`** → genero el tasks.md (25 tareas en 12 secciones) leyendo todos los artefactos previos.

**Resultado**: Las 5 specs salieron aprobadas sin cambios. El design.md no necesito retoques. Solo se hizo un ajuste minimo al tasks.md (añadir una tarea 1.4 para los modulos en el root POM).

#### Por que funciono mejor

- **Cada artefacto se genero con contexto completo del anterior**: La IA leyo la proposal antes de escribir el design, leyo proposal + design antes de escribir las specs, etc. No hubo "adivinanza" — cada artefacto tenia su fundamento.
- **Hubo pausa entre artefactos para revisar**: El usuario podia leer y aprobar antes de que se generase el siguiente. Con `/opsx:ff` esa revision es *post-hoc* — ya esta todo generado y corregir es mas engorroso.
- **La proposal escrita por el usuario marco la direccion correcta**: El usuario sabia que incluir (JPA, REST, Flyway, Testcontainers) y que excluir (RabbitMQ, Docker Compose, Security, Swagger). Esa precision se propago al resto de artefactos.
- **Menos tokens desperdiciados**: En `/opsx:ff` se genera todo de una vez y luego se corrige. Con `/opsx:continue` cada artefacto se hace bien a la primera.

#### Cuando usar cada uno

| Escenario | Comando | Razon |
|-----------|---------|-------|
| Change pequeño, scope muy claro, ya lo has hecho antes | `/opsx:ff` | Velocidad, si sale mal se corrige rapido |
| Change grande o complejo, multiples decisiones de diseño | `/opsx:new` + `/opsx:continue` | Calidad, cada artefacto se revisa antes del siguiente |
| Primer change en un area nueva del proyecto | `/opsx:new` + `/opsx:continue` | Necesitas pensar, no ir rapido |
| Change donde el usuario tiene opinion fuerte sobre el proposal | `/opsx:new` + proposal manual | El usuario controla la direccion, la IA rellena los detalles |

**Leccion clave**: `/opsx:ff` es para prisa, `/opsx:continue` es para precision. En un POC de aprendizaje, la precision siempre gana.

### Decisiones de Diseño Clave

#### Decision 1: JPA Entity completamente separada del Domain Entity
- `CustomerJpaEntity` es una clase JPA pura: `@Entity`, `@Table`, setters, constructor publico sin argumentos.
- `Customer` (dominio) tiene factory methods (`create`, `reconstruct`), campos inmutables y logica de negocio.
- Un `CustomerPersistenceMapper` convierte entre las dos (~20 lineas).
- **Leccion**: Es tentador poner `@Entity` en el agregado de dominio "para no duplicar". Pero eso fuerza setters publicos, constructor vacio, y anotaciones de Spring en el dominio — exactamente lo que la arquitectura hexagonal prohibe. La duplicacion de ~7 campos es un precio pequeño por la separacion total.

#### Decision 2: El controller inyecta interfaces (input ports), no el ApplicationService
- `CustomerController` inyecta `CreateCustomerUseCase`, `GetCustomerUseCase`, etc.
- No inyecta `CustomerApplicationService` directamente.
- **Leccion**: Dependency Inversion en la practica. Si mañana un use case necesita SAGA, se extrae a su propia clase y el controller no cambia ni una linea.

#### Decision 3: BeanConfiguration como "pegamento" del hexagono
- `@Configuration` en container registra manualmente `CustomerApplicationMapper`, `CustomerApplicationService`, y los 5 input ports (todos apuntando al mismo service).
- Ni el dominio ni la aplicacion tienen `@Service` ni `@Component`.
- **Leccion**: Este es el precio de la pureza hexagonal — alguien tiene que decirle a Spring que existen esos beans. BeanConfiguration es ese "alguien". Vive en container porque es el unico modulo que conoce todas las capas.

#### Decision 4: Flyway + `ddl-auto: validate`
- El esquema se gestiona con migraciones Flyway (`V1__create_customer_table.sql`).
- Hibernate solo valida que el esquema coincida con las entidades JPA.
- **Leccion**: `ddl-auto: update` es comodo pero peligroso — genera ALTER TABLE silenciosos. `validate` falla ruidosamente si hay desajuste. En produccion siempre Flyway.

#### Decision 5: Testcontainers PostgreSQL, nunca H2
- Los integration tests arrancan un contenedor PostgreSQL real.
- URL JDBC de Testcontainers: `jdbc:tc:postgresql:16-alpine:///customer_db`.
- **Leccion**: H2 simula PostgreSQL pero tiene diferencias reales (tipos de columna, restricciones, case sensitivity). "Funciona en tests, falla en produccion" es el antipatron clasico de usar H2. Testcontainers arranca en ~3 segundos y usa el mismo motor que produccion.

### Errores y Soluciones Durante el Apply

#### Error 1: Constructor `protected` en CustomerJpaEntity
- El mapper esta en `infrastructure.adapter.output.persistence.mapper`, la entidad JPA en `infrastructure.adapter.output.persistence.entity` — paquetes **diferentes**.
- Un constructor `protected` solo es accesible desde el mismo paquete o subclases.
- **Solucion**: Cambiar a constructor `public`. El design decia "protected" siguiendo la convencion JPA, pero la realidad de los paquetes lo impedia.
- **Leccion**: Las convenciones de JPA ("protected no-arg constructor") asumen que el mapper y la entidad estan en el mismo paquete. Cuando no lo estan, hay que ser pragmatico.

#### Error 2: Tipo generico incompatible en GlobalExceptionHandler
- `List<LinkedHashMap<String,String>>` no es asignable a `List<Map<String,String>>` por la invarianza de genericos en Java.
- **Solucion**: Usar `var` para la variable local y tipar explicitamente los elementos como `Map<String, String>`.
- **Leccion**: Los genericos de Java son invariantes: `List<LinkedHashMap>` NO es un `List<Map>` aunque `LinkedHashMap` extienda `Map`. Usar `var` evita el problema.

#### Error 3: Dependencias transitivas sin parent POM instalado
- Al intentar compilar `customer-infrastructure`, Maven no encontraba la version de `customer-domain` porque el parent POM no estaba instalado en el repositorio local.
- **Solucion**: Cadena de instalacion secuencial: `mvn install -N` (parent) → `common` → `customer-domain` → `customer-application` → `customer-infrastructure` → `customer-container`.
- **Leccion**: Mismo problema que en ciclos anteriores, pero ahora con 6 eslabones en la cadena. Es el coste de tener modulos que no existen todavia declarados en el root POM.

### Verificacion Final

- **11 tests de integracion, 0 fallos**:
  - `CustomerControllerIT` — 7 tests (POST 201, GET 200, GET 404, suspend 200, activate 200, DELETE 204, validacion 400)
  - `CustomerRepositoryAdapterIT` — 3 tests (round-trip save/findById, findById vacio, phone nullable)
  - `CustomerServiceApplicationIT` — 1 test (smoke test de arranque de contexto)
- **Domain y application siguen compilando** — BUILD SUCCESS, 75 tests previos intactos.
- **Zero `@Service`/`@Component`** en domain y application `src/main/` — confirmado.
- **Customer Service es ahora un microservicio funcional** — arranca en el puerto 8181, persiste en PostgreSQL, expone REST API con 5 endpoints.

### El Customer Service Completo: 4 Modulos Hexagonales

Con este change, el Customer Service tiene los 4 modulos que prescribe la arquitectura:

```
customer-service/
├── customer-domain/         ← 11 clases, 58 tests unitarios, ZERO Spring
├── customer-application/    ← 13 clases, 17 tests unitarios, solo spring-tx
├── customer-infrastructure/ ← ~12 clases, @Component/@RestController/@Entity
└── customer-container/      ← 2 clases + config, @SpringBootApplication
```

**Cadena de dependencias** (unidireccional):
```
container → infrastructure → application → domain → common
```

Ninguna flecha va hacia atras. El dominio no sabe que existe Spring. La aplicacion no sabe que existe JPA. La infraestructura no sabe como se ensambla el contexto. Cada capa conoce solo lo que necesita.

### Reflexiones del quinto ciclo

- **`/opsx:continue` paso a paso produce artefactos superiores a `/opsx:ff`**: Esta es la leccion estrella de la sesion. 5 specs aprobadas sin cambios vs artefactos contaminados que hay que corregir. La pausa entre artefactos no es un coste — es una inversion.
- **La proposal escrita por el usuario es el artefacto mas valioso**: Cuando el usuario sabe lo que quiere y lo expresa con precision (que incluir, que excluir, que capabilities), el resto de artefactos se generan casi perfectos. La calidad de la proposal determina la calidad de todo el change.
- **La arquitectura hexagonal se ve clara cuando las 4 capas existen**: Con solo domain + application, el hexagono era teoria. Ahora con infrastructure + container, se ve el patron completo: input adapters (REST) → input ports → application service → output ports → output adapters (JPA, Events). Cada pieza tiene su sitio.
- **El "pegamento" del hexagono es mas codigo del esperado**: `BeanConfiguration` con 5 input ports, `CustomerPersistenceMapper`, `CreateCustomerRequest` (DTO REST separado de `CreateCustomerCommand`)... La separacion de capas tiene un coste en ficheros y mappers. Pero cada fichero tiene una responsabilidad clara y es facil de localizar.
- **Los integration tests con Testcontainers son lentos pero fiables**: Arrancar un PostgreSQL tarda unos segundos, pero la confianza de que lo que funciona en tests funciona en produccion no tiene precio. H2 habria sido mas rapido pero con falsa seguridad.
- **El Customer Service es el modelo para los otros 3 servicios**: Reservation, Payment y Fleet seguiran exactamente este patron de 4 modulos. Pero seran mas complejos: SAGA, Outbox, cross-service events. Customer fue el candidato perfecto para establecer el patron base porque es el mas simple.

---

## Fecha: 2026-02-15

## Sexto Ciclo: Fleet Domain — Segundo Bounded Context, Validando la Repetibilidad

### Contexto

Con el Customer Service completo (4 modulos, 86 tests, microservicio funcional), el siguiente paso era construir el segundo bounded context: Fleet. La pregunta clave de este ciclo no era "como se hace" — eso ya lo sabiamos por Customer — sino "es repetible?". Si los patrones DDD y la estructura hexagonal funcionan para un segundo servicio sin modificar el shared kernel, el diseno esta validado.

Fleet gestiona el ciclo de vida de vehiculos: registrar, enviar a mantenimiento, activar, retirar. No gestiona reservas ni estado de alquiler — eso es responsabilidad del Reservation Service, que coordinara con Fleet a traves de SAGA events en un cambio futuro.

### Que construimos

**1 POM + 12 clases Java + 7 clases de test = 50 tests pasando**

```
fleet-service/fleet-domain/src/main/java/com/vehiclerental/fleet/domain/
├── model/
│   ├── aggregate/
│   │   └── Vehicle.java                        ← Aggregate Root (create, reconstruct, sendToMaintenance, activate, retire)
│   └── vo/
│       ├── VehicleId.java                      ← Typed ID (record, UUID)
│       ├── LicensePlate.java                   ← Value Object (record, validacion alphanumeric)
│       ├── VehicleCategory.java                ← Enum (SEDAN, SUV, VAN, MOTORCYCLE)
│       ├── VehicleStatus.java                  ← Enum (ACTIVE, UNDER_MAINTENANCE, RETIRED)
│       └── DailyRate.java                      ← Value Object (record, wraps Money)
├── event/
│   ├── VehicleRegisteredEvent.java             ← Snapshot completo (vehicleId, licensePlate, make, model, year, category, dailyRate, description)
│   ├── VehicleSentToMaintenanceEvent.java      ← Solo vehicleId
│   ├── VehicleActivatedEvent.java              ← Solo vehicleId
│   └── VehicleRetiredEvent.java                ← Solo vehicleId
├── exception/
│   └── FleetDomainException.java               ← Extiende DomainException del common
└── port/
    └── output/
        └── VehicleRepository.java              ← Interface (save, findById), solo tipos de dominio
```

### Flujo OpenSpec — Step-by-Step Confirmado

Este change uso `/opsx:new` + `/opsx:continue` paso a paso, no `/opsx:ff`. Los artefactos (proposal, specs, design, tasks) se generaron en sesiones anteriores con revision entre cada uno, y luego se aplico con `/opsx:apply`. El resultado confirmo la leccion del quinto ciclo: **el flujo step-by-step produce artefactos superiores**. Las 5 specs salieron limpias, el design captura 7 decisiones con rationale, y las 27 tasks se completaron sin sorpresas.

### Decisiones de Diseno Clave

#### Decision 1: Vehicle como Aggregate Root con tres estados, sin RENTED
- Estados: `ACTIVE`, `UNDER_MAINTENANCE`, `RETIRED` (terminal).
- Transiciones validas: ACTIVE → UNDER_MAINTENANCE, UNDER_MAINTENANCE → ACTIVE, ACTIVE|UNDER_MAINTENANCE → RETIRED.
- No hay estado RENTED — el estado de alquiler lo gestiona Reservation Service a traves de queries de disponibilidad por rango de fechas, no como un estado del vehiculo.
- **Leccion**: Es tentador agregar RENTED para "completar" el modelo. Pero eso acopla Fleet al ciclo de vida de Reservation. El principio de bounded contexts dice que cada servicio es dueno de su propio estado. Fleet sabe si un vehiculo esta operativo; Reservation sabe si esta reservado.

#### Decision 2: DailyRate como wrapper de Money con validacion positiva
- `DailyRate` es un record que wrappea `Money` del common y valida que `amount.signum() > 0`.
- `Money` ya valida no-negativo, pero una tarifa diaria de cero no tiene sentido de negocio.
- Este es el primer reuso real de un tipo del shared kernel como building block de un VO de dominio.
- **Leccion**: Los wrappers de tipos del shared kernel son el mecanismo correcto para agregar restricciones de negocio sin modificar el kernel. `Money` es generico ("no negativo"); `DailyRate` es especifico ("estrictamente positivo").

#### Decision 3: LicensePlate con validacion alphanumeric permisiva
- Patron: alphanumerico con guiones y espacios, longitud 2-15.
- Acepta `1234-BCD` (España), `ABC 1234` (otros paises), `M 1234 XY`.
- No valida formato de pais especifico — eso seria over-engineering para un POC.
- **Leccion**: Misma filosofia que `Email` en Customer: validar lo obvio (nulo, blanco, caracteres especiales), no lo exhaustivo. La validacion real de una matricula requiere un registro por pais — fuera de scope.

#### Decision 4: `description` como unico campo nullable
- Todos los campos del Vehicle son obligatorios excepto `description` (maximo 500 caracteres).
- Mismo patron que `PhoneNumber` en Customer — el unico campo opcional.
- **Leccion**: Mantener la cantidad de campos nullables al minimo. Cada nullable es un `if` en cada consumidor. Un dominio con muchos nullables es un dominio mal modelado.

#### Decision 5: `isAvailable()` como convenience query
- `isAvailable()` retorna `true` solo cuando `status == ACTIVE`.
- Semanticamente diferente de `isActive()` de Customer: aqui "available" significa "puede asignarse a una reserva".
- Preparado para la integracion futura con Reservation Service.
- **Leccion**: Nombrar los metodos segun la intencion de negocio, no segun la implementacion. Hoy `isAvailable()` es identico a `status == ACTIVE`, pero el nombre comunica por que se llama.

### El Shared Kernel Validado

El punto mas importante de este ciclo es que el shared kernel (`AggregateRoot`, `DomainEvent`, `DomainException`, `Money`) **funciono sin modificaciones**:

- `Vehicle extends AggregateRoot<VehicleId>` — igual que `Customer extends AggregateRoot<CustomerId>`.
- `VehicleRegisteredEvent implements DomainEvent` — misma interface que `CustomerCreatedEvent`.
- `FleetDomainException extends DomainException` — misma base que `CustomerDomainException`.
- `DailyRate` wrappea `Money` — primer reuso real de un VO del kernel.

50 tests pasaron sin tocar una linea en common. Esto confirma que el diseño del shared kernel en el segundo ciclo fue correcto.

### Verificacion Final

- **50 tests, 0 fallos** — FleetDomainExceptionTest (3), VehicleIdTest (3), LicensePlateTest (7), DailyRateTest (4), FleetDomainEventsTest (7), VehicleTest (15), VehicleLifecycleTest (11).
- **Zero Spring imports** en `fleet-service/fleet-domain/src/main/` — confirmado.
- **BUILD SUCCESS** en `mvn clean install` desde `fleet-service/fleet-domain/`.
- **common y customer-domain siguen compilando** — 58 + 50 = 108 tests de dominio en total.
- El build desde la raiz sigue fallando por los modulos que no existen (reservation-service, payment-service, fleet-infrastructure, fleet-container) — problema pre-existente, no causado por este change.

### Reflexiones del sexto ciclo

- **La repetibilidad es la prueba de fuego de una arquitectura**: Si el segundo servicio requiere hacks, workarounds o cambios en el shared kernel, el diseño fallo. Que Fleet replique el patron de Customer sin fricciones confirma que la estructura es solida. No solo funciona — es reproducible.
- **El segundo bounded context es mas rapido que el primero**: Customer domain tomo mas iteraciones (ajustes en el task ordering, descubrir el patron de factory methods). Fleet se implemento de una sola pasada porque los patrones ya estaban establecidos. Este es el beneficio de invertir en un primer servicio bien hecho.
- **Los tasks ordenados por dependencia de compilacion funcionan**: La leccion del tercer ciclo (excepciones primero, VOs despues, events despues, aggregate al final) se aplico aqui y el flujo fue mecanico. El tasks.md de Fleet ya venia con el orden correcto desde el principio.
- **El workflow step-by-step (`/opsx:new` + `/opsx:continue`) sigue siendo superior**: Consistente con la leccion del quinto ciclo. Los artefactos generados paso a paso son mas limpios, mas precisos y requieren menos correcciones. Para changes de dominio con multiples decisiones de diseño, este es el camino.
- **136 tests de dominio en total (86 Customer + 50 Fleet) sin una sola dependencia de Spring**: Esto valida que los modulos de dominio son verdaderamente puros. La arquitectura hexagonal no es solo un diagrama — es una realidad medible.

### Siguiente paso

**fleet-application** — la capa de orquestacion del Fleet Service. Replicara el patron de customer-application: input ports (use cases), commands, responses, ApplicationService, mapper. Deberia ser mas rapido que customer-application porque el patron ya esta establecido.

---

## Fecha: 2026-02-15

## Septimo Ciclo: Fleet Application — La Orquestacion se Replica

### Contexto

Con el fleet-domain completo (50 tests, Vehicle Aggregate Root, Value Objects, Domain Events, output port), faltaba la capa que conecta el mundo exterior con el dominio: la capa de aplicacion. Este es el segundo modulo de aplicacion en la plataforma, replicando el patron exacto de customer-application. La pregunta de este ciclo no era "como se hace" — eso ya esta documentado en el cuarto ciclo — sino "se replica sin fricciones?".

Este change completa el patron de 4 modulos Maven para el Fleet Service: domain (hecho), **application** (este change), infrastructure y container (futuros). Sigue las mismas decisiones arquitectonicas de customer-application: una interface por use case, commands con tipos primitivos/String, Application Service como pura orquestacion, mapper manual, y `@Transactional` como unica dependencia de Spring.

### Que construimos

**1 POM + 15 clases Java + 3 clases de test = 17 tests pasando**

```
fleet-service/fleet-application/src/main/java/com/vehiclerental/fleet/application/
├── port/
│   ├── input/
│   │   ├── RegisterVehicleUseCase.java       ← interface: VehicleResponse execute(RegisterVehicleCommand)
│   │   ├── GetVehicleUseCase.java            ← interface: VehicleResponse execute(GetVehicleCommand)
│   │   ├── SendToMaintenanceUseCase.java     ← interface: void execute(SendToMaintenanceCommand)
│   │   ├── ActivateVehicleUseCase.java       ← interface: void execute(ActivateVehicleCommand)
│   │   └── RetireVehicleUseCase.java         ← interface: void execute(RetireVehicleCommand)
│   └── output/
│       └── FleetDomainEventPublisher.java    ← interface: void publish(List<DomainEvent>)
├── service/
│   └── FleetApplicationService.java         ← implementa los 5 input ports, zero business logic
├── dto/
│   ├── command/
│   │   ├── RegisterVehicleCommand.java       ← record (licensePlate, make, model, year, category, dailyRateAmount, dailyRateCurrency, description)
│   │   ├── GetVehicleCommand.java            ← record (vehicleId)
│   │   ├── SendToMaintenanceCommand.java     ← record (vehicleId)
│   │   ├── ActivateVehicleCommand.java       ← record (vehicleId)
│   │   └── RetireVehicleCommand.java         ← record (vehicleId)
│   └── response/
│       └── VehicleResponse.java              ← record (vehicleId, licensePlate, make, model, year, category, dailyRateAmount, dailyRateCurrency, description, status, createdAt)
├── mapper/
│   └── FleetApplicationMapper.java           ← plain Java, toResponse(Vehicle) → VehicleResponse
└── exception/
    └── VehicleNotFoundException.java         ← extends RuntimeException (NO FleetDomainException)
```

### Flujo OpenSpec — Artefactos Step-by-Step, Apply Directo

Los artefactos (proposal, specs, design, tasks) se generaron con `/opsx:new` + `/opsx:continue` paso a paso en sesiones previas, y luego se aplico con `/opsx:apply`. Consistente con la leccion de los ultimos ciclos: el flujo step-by-step produce artefactos limpios que no necesitan retoques. Las specs y el design salieron aprobados sin cambios.

Un detalle: durante la revision del tasks.md, el usuario identifico dos omisiones que la IA no habia incluido:
1. Tests separados para `FleetApplicationMapper` (mapeo campo a campo + description null) y `VehicleNotFoundException` (no extiende FleetDomainException, mensaje contiene vehicleId).
2. Un grep explicito de `@Service|@Component` como tarea de verificacion independiente.

Estas omisiones se corrigieron antes del apply, añadiendo tareas 6.11, 6.12 y 7.2.

- **Leccion**: Revisar el tasks.md antes de implementar vale la pena. La IA tiende a asumir que los tests del service cubren todo, pero tests dedicados para mapper y exception verifican cosas que los tests de integracion del servicio no cubren (mapeo campo a campo, jerarquia de excepciones). El ojo humano detecta estos gaps.

### Decisiones de Diseno Clave

Las decisiones replican exactamente las de customer-application. No hubo decisiones nuevas — y eso es lo correcto.

#### Decision 1: VehicleRepository se queda en domain
- Misma decision que Customer. La interface usa solo tipos de dominio y no hay razon para moverla.

#### Decision 2: Un Application Service para los 5 use cases
- Misma decision que Customer. 5 metodos simples de orquestacion en una clase.

#### Decision 3: RegisterVehicleCommand con tipos primitivos
- `String licensePlate`, `String category`, `BigDecimal dailyRateAmount`, `String dailyRateCurrency`.
- La conversion a `LicensePlate`, `VehicleCategory.valueOf()`, `new DailyRate(new Money(...))` es responsabilidad del Application Service.
- El unico matiz vs Customer: `DailyRate` requiere descomponer `Money` en amount + currency, porque `Money` es un tipo compuesto. Esto genera dos campos en el command en vez de uno. Es mas verboso pero mapea naturalmente a JSON.
- **Leccion**: Los commands como "anti-corruption layer" funcionan bien incluso con tipos compuestos del dominio. Si `Money` tuviera 3 campos, el command tendria 3 campos. La verbosidad es el precio de la separacion — y es un precio bajo.

#### Decision 4: VehicleNotFoundException extends RuntimeException
- Misma decision que Customer. "No encontrado" no es una invariante de dominio — es un concepto de la capa de aplicacion.

#### Decision 5: Lifecycle use cases retornan void (CQS)
- SendToMaintenance, Activate, Retire retornan `void`. Mismo patron que Customer.

#### Decision 6: Mapper manual
- `FleetApplicationMapper.toResponse()` mapea ~11 campos (mas que los ~7 de Customer por `dailyRateAmount`, `dailyRateCurrency`, `licensePlate`, `category`).
- Sigue sin justificar MapStruct — son ~15 lineas de codigo.

### Patron de Orquestacion: Identico a Customer

El patron de todos los metodos de escritura es exactamente el mismo:

```
1. Convertir command → tipos de dominio (LicensePlate, VehicleCategory, DailyRate)
2. Ejecutar operacion de dominio (Vehicle.create / sendToMaintenance / activate / retire)
3. Persistir via VehicleRepository.save()
4. Publicar eventos via FleetDomainEventPublisher.publish()
5. Limpiar eventos del agregado: vehicle.clearDomainEvents()
6. (Solo register/get) Retornar VehicleResponse via mapper
```

La similitud con Customer es deliberada. El Application Service es un template con variaciones minimas entre servicios.

### Tests: 17 tests en 3 clases

1. **FleetApplicationServiceTest** (12 tests en 6 nested classes):
   - RegisterVehicle: save → publish → clearDomainEvents → return response (InOrder de Mockito)
   - GetVehicle: found → response, not found → VehicleNotFoundException
   - SendToMaintenance, ActivateVehicle, RetireVehicle: found → operacion + save + publish, not found → exception
   - AnnotationChecks: sin @Service/@Component (reflexion), @Transactional en writes, @Transactional(readOnly=true) en get

2. **FleetApplicationMapperTest** (2 tests):
   - Mapeo de todos los campos incluyendo dailyRateAmount, dailyRateCurrency extraidos de Money
   - Caso description null → null en response

3. **VehicleNotFoundExceptionTest** (3 tests):
   - getMessage() contiene vehicleId
   - Extiende RuntimeException
   - NO extiende FleetDomainException

### Verificacion Final

- **17 tests, 0 fallos** — VehicleNotFoundExceptionTest (3), FleetApplicationMapperTest (2), FleetApplicationServiceTest (12).
- **Zero `@Service`/`@Component`** en `fleet-application/src/main/` — confirmado con grep.
- **BUILD SUCCESS** en `mvn clean install` desde `fleet-service/fleet-application/`.
- **common, customer-domain, customer-application y fleet-domain siguen compilando** — 58 + 17 + 50 = 125 tests previos intactos, 142 tests totales en la plataforma.
- El build desde la raiz sigue fallando por los modulos que no existen — problema pre-existente.

### Reflexiones del septimo ciclo

- **La repetibilidad se confirma otra vez**: Fleet-application replico el patron de customer-application sin fricciones. Cero sorpresas, cero workarounds, cero cambios en modulos previos. El patron de 4 modulos hexagonales es reproducible — no solo para dominios, tambien para capas de aplicacion.
- **El segundo es siempre mas rapido**: Customer-application tomo mas iteraciones (descubrir el patron de CQS, decidir sobre mapper manual vs MapStruct, discutir donde vive el repository). Fleet-application se implemento de una sola pasada porque todas esas decisiones ya estaban tomadas y documentadas. El valor del primer servicio bien hecho no esta solo en el servicio — esta en el precedente que establece.
- **El tasks.md necesita revision humana**: La IA genero 28 tasks razonables, pero omitio tests unitarios para mapper y exception. El usuario detecto el gap comparando con customer-application. Las specs cubrian los requisitos pero no prescribian "un test por clase" — es responsabilidad del reviewer asegurar paridad de tests entre servicios.
- **DailyRate como tipo compuesto es la unica variacion real vs Customer**: La necesidad de descomponer `Money` en `dailyRateAmount` + `dailyRateCurrency` en el command es el unico punto donde fleet-application difiere estructuralmente de customer-application. Todo lo demas (ports, service, exception, mapper) es 1:1 el mismo patron.
- **142 tests, zero Spring en domain, zero @Service en application**: Las metricas hablan. La separacion hexagonal se mantiene consistente a medida que crece el proyecto. Cada modulo nuevo añade tests sin romper los previos.

### Siguiente paso

**fleet-infrastructure-and-container** — los adaptadores de infraestructura y el ensamblaje Spring Boot del Fleet Service. JPA entities, persistence adapter, REST controller con endpoints para Register/Get/SendToMaintenance/Activate/Retire, BeanConfiguration, Flyway migration, Testcontainers. Replicara el patron de customer-infrastructure-and-container.

---

## Fecha: 2026-02-15

## Octavo Ciclo: Fleet Infrastructure + Container — El Segundo Microservicio Cobra Vida

### Contexto

Con el fleet-domain (50 tests) y fleet-application (17 tests) completos, el Fleet Service era codigo sin vida propia — igual que Customer antes de su quinto ciclo. Este change añade las dos capas externas de la arquitectura hexagonal: **infrastructure** (adaptadores REST y persistencia) y **container** (ensamblaje Spring Boot). Es el segundo microservicio funcional de la plataforma, replicando el patron exacto que se establecio con Customer Service en el quinto ciclo.

La pregunta de este ciclo era directa: el patron de infrastructure + container de Customer, con sus 10 decisiones arquitectonicas, se replica para Fleet sin desviaciones? La respuesta: si, sin una sola desviacion.

### Que construimos

**2 POMs + ~12 ficheros Java + 1 SQL + 2 YMLs + 3 clases de test = 12 tests de integracion pasando**

```
fleet-service/fleet-infrastructure/src/main/java/com/vehiclerental/fleet/infrastructure/
├── adapter/
│   ├── input/
│   │   └── rest/
│   │       ├── VehicleController.java              ← @RestController, inyecta 5 input ports
│   │       └── dto/
│   │           └── RegisterVehicleRequest.java      ← record con @NotBlank, @NotNull
│   └── output/
│       ├── persistence/
│       │   ├── VehicleJpaRepository.java            ← JpaRepository<VehicleJpaEntity, UUID>
│       │   ├── VehicleRepositoryAdapter.java        ← @Component, implementa VehicleRepository
│       │   ├── entity/
│       │   │   └── VehicleJpaEntity.java            ← @Entity, separada del dominio, 11 campos
│       │   └── mapper/
│       │       └── VehiclePersistenceMapper.java    ← Domain ↔ JPA, bidireccional
│       └── event/
│           └── FleetDomainEventPublisherAdapter.java ← Logger no-op
└── config/
    └── GlobalExceptionHandler.java                  ← @RestControllerAdvice

fleet-service/fleet-container/src/main/java/com/vehiclerental/fleet/
├── FleetServiceApplication.java                     ← @SpringBootApplication
└── config/
    └── BeanConfiguration.java                       ← @Configuration, registra beans manuales

fleet-service/fleet-container/src/main/resources/
├── application.yml                                   ← PostgreSQL, Flyway, puerto 8182
├── application-test.yml                              ← Testcontainers JDBC URL
└── db/migration/
    └── V1__create_vehicles_table.sql                 ← Flyway migration (NUMERIC, VARCHAR(3))
```

### Flujo OpenSpec — Apply Directo (patron consolidado)

Los artefactos (proposal, specs, design, tasks) ya estaban generados con `/opsx:new` + `/opsx:continue` paso a paso en sesiones previas. Arrancamos con `/opsx:apply` directo sobre los 25 tasks del `tasks.md`. El flujo fue mecanico: leer los artefactos de contexto, implementar tarea por tarea, marcar como completada, verificar.

Ya no hay debate sobre `/opsx:ff` vs `/opsx:continue` — el flujo step-by-step esta consolidado como el enfoque correcto para changes de esta complejidad.

### Replicacion Sin Desviaciones

Este es el aspecto mas destacable del ciclo. Las 10 decisiones arquitectonicas del quinto ciclo (customer-infrastructure-and-container) se aplicaron identicamente:

1. **JPA Entity separada del dominio** — `VehicleJpaEntity` con getters/setters publicos, sin imports de dominio
2. **RepositoryAdapter como persistence adapter** — `@Component`, inyecta JPA repo + mapper
3. **Controller inyecta input ports, no ApplicationService** — 5 use case interfaces
4. **REST DTO separado del Command** — `RegisterVehicleRequest` vs `RegisterVehicleCommand`
5. **Event publisher como logger no-op** — mismo patron de log `"EVENT LOGGED (not published)"`
6. **BeanConfiguration registra beans manuales** — mapper, application service, 5 input ports
7. **Flyway + ddl-auto: validate** — migration SQL + Hibernate valida schema
8. **GlobalExceptionHandler** — NotFound→404, DomainException→422, Validation→400, Generic→500
9. **API path versioning** — `/api/v1/vehicles`
10. **Testcontainers PostgreSQL** — `jdbc:tc:postgresql:16-alpine:///fleet_db`

No hubo ninguna decision nueva. No hubo ninguna desviacion. El patron se replico 1:1.

### Decisiones de Diseno Especificas de Fleet

#### Decision 11: Tipos de columna vehiculares en Flyway
- `daily_rate_amount NUMERIC(10,2) NOT NULL` — primer uso de tipo numerico en la plataforma (Customer solo tenia VARCHARs).
- `daily_rate_currency VARCHAR(3) NOT NULL` — ajustado al largo exacto de ISO 4217 (EUR, USD, etc.).
- `description VARCHAR(500)` — nullable, unico campo opcional. Mismo patron que `phone` en Customer.
- `license_plate VARCHAR(255) NOT NULL UNIQUE` — UNIQUE constraint, mismo rol que `email` en Customer.
- **Leccion**: PostgreSQL `NUMERIC` es el tipo correcto para dinero — precision exacta, sin errores de punto flotante. `VARCHAR(3)` para currency codes es tight pero correcto: ISO 4217 define exactamente 3 caracteres.

#### Decision 12: Puerto 8182
- Customer: 8181, Fleet: 8182. Secuencial y predecible.
- Reservation: 8183, Payment: 8184 cuando se implementen.
- **Leccion**: Asignar puertos secuenciales desde el principio evita conflictos cuando se levantan multiples servicios simultaneamente en desarrollo.

### Incidente: Precision de Instant con TIMESTAMPTZ de PostgreSQL

El unico problema durante la implementacion fue un test que fallaba:

```
expected: 2026-02-15T16:25:23.154364300Z
 but was: 2026-02-15T16:25:23.154364Z
```

**Causa**: Java `Instant` tiene precision de nanosegundos (9 decimales). PostgreSQL `TIMESTAMPTZ` tiene precision de microsegundos (6 decimales). Al persistir y recuperar, los 3 ultimos digitos (los nanosegundos) se pierden.

**Solucion**: Cambiar `assertThat(loaded.getCreatedAt()).isEqualTo(saved.getCreatedAt())` por:
```java
assertThat(loaded.getCreatedAt()).isCloseTo(saved.getCreatedAt(),
        within(1, ChronoUnit.MICROS));
```

**Leccion**: Nunca comparar `Instant` con `isEqualTo` despues de un round-trip por PostgreSQL. La precision de microsegundos de `TIMESTAMPTZ` trunca los nanosegundos de Java. `isCloseTo` con tolerancia de 1 microsegundo es la comparacion correcta. Este mismo problema podria estar latente en los tests de Customer — `CustomerRepositoryAdapterIT.saveAndFindByIdRoundTrip()` pasa porque la Instant generada no tenia nanosegundos extra, pero no esta garantizado. Habria que revisarlo.

### Stubs para Modulos Fantasma del Root POM (Deuda Tecnica)

El root POM declara 13 modulos, pero solo existen 9 (common, customer x4, fleet x4). Los 6 restantes (reservation x3, payment x3) no tienen ni directorios. Esto impide hacer `mvn -pl` desde la raiz.

**Solucion temporal**: Crear POMs minimos con solo `<parent>` y `<artifactId>` para los 6 modulos fantasma. Esto permite al reactor de Maven parsear el POM raiz sin errores.

**Impacto**: Los stubs son ficheros vacios de ~10 lineas. No tienen dependencias, no compilan codigo, no ejecutan tests. Son puramente estructurales para desbloquear el build.

**Deuda tecnica**: Estos stubs son un workaround. Las opciones correctas son:
1. **No declarar modulos que no existen** — pero el root POM fue diseñado en el primer ciclo con todos los modulos previstos.
2. **Usar profiles de Maven** — `<profiles>` para activar/desactivar grupos de modulos segun la fase del proyecto.
3. **Crearlos cuando se implementen** — lo que hariamos naturalmente con los changes de reservation y payment.

- **Leccion**: Declarar modulos futuros en el root POM fue una decision prematura del primer ciclo. Funciona como documentacion de la estructura prevista, pero tiene el coste de requerir stubs hasta que los modulos se implementen. En retrospeccion, habria sido mejor agregar los modulos al POM cuando se crean, no antes. Es una deuda tecnica menor pero molesta.

### Verificacion Final

- **12 tests de integracion, 0 fallos**:
  - `VehicleControllerIT` — 8 tests (POST 201, GET 200, GET 404, maintenance 200, activate 200, retire 200, validacion 400, domain rule 422)
  - `VehicleRepositoryAdapterIT` — 3 tests (round-trip save/findById, findById vacio, description nullable con dailyRate verificado)
  - `FleetServiceApplicationIT` — 1 test (smoke test de arranque de contexto)
- **Domain y application siguen compilando** — BUILD SUCCESS, tests previos intactos.
- **Zero `@Service`/`@Component`** en domain y application `src/main/` — confirmado con grep.
- **Fleet Service es ahora un microservicio funcional** — arranca en el puerto 8182, persiste en PostgreSQL, expone REST API con 5 endpoints.

### Los Dos Microservicios de la Plataforma

Con este change, la plataforma tiene dos servicios funcionales:

```
customer-service/ (puerto 8181)
├── customer-domain/         ← 11 clases, 58 tests, ZERO Spring
├── customer-application/    ← 13 clases, 17 tests, solo spring-tx
├── customer-infrastructure/ ← ~12 clases, Spring full stack
└── customer-container/      ← 2 clases + config, 11 integration tests

fleet-service/ (puerto 8182)
├── fleet-domain/            ← 12 clases, 50 tests, ZERO Spring
├── fleet-application/       ← 15 clases, 17 tests, solo spring-tx
├── fleet-infrastructure/    ← ~12 clases, Spring full stack
└── fleet-container/         ← 2 clases + config, 12 integration tests
```

**Tests totales**: 58 + 17 + 11 + 50 + 17 + 12 = **165 tests**, todos pasando.

**Endpoints REST**:
- Customer: POST/GET/suspend/activate/DELETE en `/api/v1/customers`
- Fleet: POST/GET/maintenance/activate/retire en `/api/v1/vehicles`

### Reflexiones del octavo ciclo

- **El patron se replica sin fricciones**: 10 decisiones arquitectonicas identicas, cero desviaciones. La segunda infrastructure + container fue significativamente mas rapida que la primera porque no hubo decisiones que tomar — solo ejecutar. Esto valida que el quinto ciclo (Customer) establecio un patron solido y reproducible.
- **Los incidentes de precision temporal son sutiles y peligrosos**: El bug de `Instant` vs `TIMESTAMPTZ` no se habria detectado con H2 (que tiene precision diferente a PostgreSQL). Solo aparecio porque usamos Testcontainers con PostgreSQL real. Esto refuerza la decision de nunca usar H2 para tests de integracion — los bugs reales son los que ocurren con la base de datos real.
- **Los stubs de modulos fantasma son deuda tecnica acumulada**: El workaround funciona, pero es un recordatorio de que declarar modulos futuros en el root POM tiene un coste. La proxima vez que se inicie un proyecto multi-modulo, la recomendacion es declarar modulos solo cuando existen.
- **La velocidad de implementacion aumenta exponencialmente con cada servicio**: El primer servicio completo (Customer, ciclos 3-5) tomo 3 changes y multiples iteraciones. El segundo (Fleet, ciclos 6-8) tomo 3 changes pero cada uno fue mas rapido y mecanico. El tercero (Reservation o Payment) deberia ser aun mas rapido. Esta es la curva de aprendizaje de una arquitectura bien diseñada — la inversion inicial se amortiza con cada repeticion.
- **8 changes, 165 tests, cero dependencias circulares**: La plataforma crece de forma predecible y controlada. Cada change agrega funcionalidad sin romper lo existente. Las metricas no mienten.

### Siguiente paso

**Reservation Service** o **Payment Service** — el tercer microservicio. Ambos seran mas complejos que Customer y Fleet porque involucran interacciones cross-service (SAGA, Outbox Pattern, domain events consumidos por otros servicios). Fleet y Customer son los servicios "simples"; Reservation y Payment son donde la complejidad real de los microservicios empieza.

---

## Fecha: 2026-02-21

## Noveno Ciclo: Reservation Domain — El Dominio Mas Complejo de la Plataforma

### Contexto

Con Customer y Fleet completos (4 modulos hexagonales cada uno, 165 tests combinados), el siguiente paso era el tercer bounded context: Reservation. A diferencia de los dos anteriores — servicios CRUD simples con 3 estados cada uno — Reservation es el **coordinador de SAGA** de la plataforma: maquina de estados de 6 transiciones, inner entity con su propio ciclo de vida, IDs cross-context y domain events con snapshots inmutables. Es el primer dominio que pone a prueba las abstracciones del shared kernel con complejidad real.

### Que construimos

**1 POM + 14 clases Java + 12 clases de test = 80 tests pasando**

```
reservation-service/reservation-domain/src/main/java/com/vehiclerental/reservation/domain/
├── model/
│   ├── aggregate/
│   │   └── Reservation.java                    ← Aggregate Root (6 transiciones, create/reconstruct)
│   ├── entity/
│   │   └── ReservationItem.java                ← Inner entity (subtotal = dailyRate x days)
│   └── vo/
│       ├── ReservationId.java                  ← Typed ID (record, UUID)
│       ├── TrackingId.java                     ← ID publico para REST/SAGA (record, UUID)
│       ├── CustomerId.java                     ← Cross-context ID, local al bounded context
│       ├── VehicleId.java                      ← Cross-context ID, local al bounded context
│       ├── DateRange.java                      ← VO con getDays(), invariantes estructurales
│       ├── PickupLocation.java                 ← VO con address + city
│       └── ReservationStatus.java              ← Enum: PENDING, CUSTOMER_VALIDATED, PAID, CONFIRMED, CANCELLING, CANCELLED
├── event/
│   ├── ReservationCreatedEvent.java            ← Record con snapshot completo + items
│   ├── ReservationCancelledEvent.java          ← Record con failureMessages
│   └── ReservationItemSnapshot.java            ← Record inmutable para datos de items en el evento
├── exception/
│   └── ReservationDomainException.java         ← Extiende DomainException del common
└── port/
    └── output/
        └── ReservationRepository.java          ← Interface (save, findById, findByTrackingId)
```

### Decisiones de Diseno Clave

#### Decision 1: Maquina de estados de 6 posiciones, sin shortcuts
- Estados: `PENDING → CUSTOMER_VALIDATED → PAID → CONFIRMED` (happy path) + `CANCELLING → CANCELLED` (compensacion).
- Cada transicion valida su estado origen. `cancel()` acepta tres estados fuente (PENDING, CUSTOMER_VALIDATED, CANCELLING) porque la cancelacion puede entrar desde diferentes puntos de la SAGA.
- **Leccion**: Modelar la maquina de estados completa desde el principio, aunque solo se use `create()` por ahora. Las transiciones intermedias (`validateCustomer`, `pay`, `confirm`, `initCancel`) las invocara el orquestador SAGA — pero las reglas de que transiciones son validas son del dominio.

#### Decision 2: CustomerId y VehicleId son locales al bounded context de Reservation
- No se importan de `customer-domain` ni `fleet-domain`. Cada bounded context es dueño de su representacion de IDs externos.
- Importar crearia acoplamiento Maven entre servicios, violando el principio de bounded contexts.
- **Leccion**: En microservicios, el mismo concepto (un "vehicle ID") puede existir como tipos diferentes en distintos bounded contexts. Lo que se comparte es el valor (UUID), no el tipo.

#### Decision 3: ReservationItem como `BaseEntity<UUID>`, no como typed ID
- `ReservationItem` es una inner entity dentro del aggregate boundary. Nunca se referencia desde fuera.
- Darle un `ReservationItemId` tipado seria ceremonia pura — un `UUID` como identidad interna es suficiente.
- **Leccion**: Los typed IDs son para aggregate roots e IDs que cruzan fronteras. Para entidades internas, `BaseEntity<UUID>` directo es la opcion correcta.

#### Decision 4: Solo dos domain events — los que importan fuera del agregado
- `ReservationCreatedEvent` (arranca la SAGA) y `ReservationCancelledEvent` (auditoria/notificacion).
- Las 4 transiciones intermedias son *reacciones* a eventos de otros servicios, no *fuentes* de eventos nuevos. Publicar eventos para cada transicion duplicaria lo que el orquestador SAGA ya gestiona via Outbox.
- **Leccion**: Un domain event es un hecho que al mundo exterior le interesa. Las transiciones internas de la SAGA no lo son — son mecanismo, no negocio.

#### Decision 5: ReservationCreatedEvent con ReservationItemSnapshot, no con entidades vivas
- El snapshot previene que el evento tenga referencias mutables a entidades del agregado.
- Los eventos son hechos inmutables. Si llevaran `ReservationItem` directamente, un cambio posterior al item se "filtraria" al evento.
- **Leccion**: Siempre copiar datos al evento, nunca referenciar el modelo mutable. El snapshot pattern es el mecanismo correcto.

#### Decision 6: DateRange valida invariantes estructurales, no temporales
- "Devolucion despues de recogida" es un hecho estructural de cualquier rango. "Recogida en el futuro" depende del momento actual y es responsabilidad de la capa de aplicacion.
- Esto tambien simplifica los tests: se pueden usar fechas pasadas sin mockear relojes.
- **Leccion**: Separar invariantes atemporales (domain) de restricciones temporales (application). El dominio no deberia necesitar un `Clock` para validar sus reglas.

### Tests: 80 tests en 12 clases

| Clase de test | Tests | Que cubre |
|--------------|-------|-----------|
| ReservationTest | 12 | Creacion, acceso a campos, reconstruct, rechazo de nulos |
| ReservationLifecycleTest | 16 | Las 6 transiciones, estados fuente validos e invalidos |
| ReservationEventEmissionTest | 4 | Transiciones intermedias no emiten eventos |
| ReservationItemTest | 9 | Calculo de subtotal, validacion, reconstruct |
| ReservationDomainEventsTest | 10 | Ambos records de evento, DomainEvent impl |
| ReservationDomainExceptionTest | 4 | errorCode, message, constructor con cause |
| ReservationIdTest | 3 | Construccion, rechazo de null, igualdad |
| TrackingIdTest | 3 | Construccion, rechazo de null, igualdad |
| CustomerIdTest | 3 | Construccion, rechazo de null, igualdad |
| VehicleIdTest | 3 | Construccion, rechazo de null, igualdad |
| DateRangeTest | 8 | getDays(), dia unico, nulos, fechas iguales/invertidas |
| PickupLocationTest | 5 | Combinaciones de null/blank en address y city |

### Verificacion Final

- **80 tests, 0 fallos** — el dominio mas grande de la plataforma.
- **Zero Spring imports** en `reservation-service/reservation-domain/src/main/` — confirmado.
- **BUILD SUCCESS** en `mvn clean install` desde `reservation-service/reservation-domain/`.

### Diferencias vs Customer/Fleet domain

| Aspecto | Customer / Fleet | Reservation |
|---------|-----------------|-------------|
| Maquina de estados | 3 estados, 4 transiciones | 6 estados, 6 transiciones |
| Inner entities | Ninguna — agregado plano | ReservationItem como BaseEntity<UUID> |
| IDs cross-context | Solo su propio typed ID | CustomerId + VehicleId locales |
| Repository port | Solo findById | findById + findByTrackingId |
| Domain events | 2 eventos simples | 2 eventos + ReservationItemSnapshot |
| VOs nuevos | 2-3 por servicio | DateRange, PickupLocation, TrackingId (3 tipos genuinamente nuevos) |
| Tests | 50-58 | 80 |

### Reflexiones del noveno ciclo

- **El shared kernel se valida con complejidad real**: Customer y Fleet eran dominios simples — agregados planos con 3 estados. Reservation pone a prueba `AggregateRoot<ID>` con inner entities, `DomainEvent` con snapshots compuestos, y `BaseEntity<UUID>` como identidad de entidad interna. Todo funciono sin modificar nada en common. Esto confirma que el diseño del segundo ciclo fue robusto.
- **La maquina de estados de 6 posiciones es sorprendentemente concisa**: 6 transiciones con validacion de estado fuente son ~30 lineas de codigo en el Aggregate Root. La complejidad esta en la precision de las reglas ("cancel acepta 3 estados fuente"), no en la cantidad de codigo.
- **Los tests del aggregate se benefician de la separacion en 3 ficheros**: ReservationTest (creacion), ReservationLifecycleTest (transiciones), ReservationEventEmissionTest (ausencia de eventos en transiciones intermedias). Con 32 tests combinados, un solo fichero seria inmanejable.
- **80 tests de dominio puro — el primer dominio complejo del proyecto**: Customer (58) y Fleet (50) eran warmup. Reservation con 80 tests es el "proof of concept" real de que test-first con specs WHEN/THEN funciona para dominios no triviales.

---

## Fecha: 2026-02-21

## Decimo Ciclo: Reservation Application — La Orquestacion con Complejidad Real

### Contexto

Con el reservation-domain completo (80 tests, 6-state machine, inner entity), faltaba la capa que conecta el mundo exterior con el dominio. A diferencia de customer-application y fleet-application — que tenian 5 use cases simples cada uno — reservation-application empieza con solo 2 use cases (create + track) porque los 5 use cases de transiciones SAGA (`validateCustomer`, `pay`, `confirm`, `initCancel`, `cancel`) se añadiran en el change del orquestador SAGA.

### Que construimos

**1 POM + 10 clases Java + 4 clases de test = 18 tests pasando**

```
reservation-service/reservation-application/src/main/java/com/vehiclerental/reservation/application/
├── port/
│   ├── input/
│   │   ├── CreateReservationUseCase.java       ← interface: CreateReservationResponse execute(CreateReservationCommand)
│   │   └── TrackReservationUseCase.java        ← interface: TrackReservationResponse execute(TrackReservationCommand)
│   └── output/
│       └── ReservationDomainEventPublisher.java ← interface: void publish(List<DomainEvent>)
├── service/
│   └── ReservationApplicationService.java      ← implementa los 2 input ports
├── dto/
│   ├── command/
│   │   ├── CreateReservationCommand.java       ← record + inner CreateReservationItemCommand
│   │   └── TrackReservationCommand.java        ← record (trackingId)
│   └── response/
│       ├── CreateReservationResponse.java      ← record (trackingId, status) — lean
│       └── TrackReservationResponse.java       ← record (14 campos + inner TrackReservationItemResponse) — full
├── mapper/
│   └── ReservationApplicationMapper.java       ← plain Java
└── exception/
    └── ReservationNotFoundException.java       ← extends RuntimeException
```

### Decisiones de Diseno Clave

#### Decision 1: Dos respuestas separadas — lean create, full track (CQS)
- `CreateReservationResponse` lleva solo `trackingId` y `status` — el cliente ya tiene los datos que envio.
- `TrackReservationResponse` es un snapshot completo: 14 campos + lista de items con vehicleId, dailyRate, days, subtotal.
- **Leccion**: CQS aplicado a los DTOs de respuesta. Un unico tipo de respuesta hincharia el create o dejaria hambriento al track. Esta es la decision CQS mas marcada de la plataforma.

#### Decision 2: CreateReservationCommand con inner record para items
- `CreateReservationCommand` tiene `List<CreateReservationItemCommand>` con inner record para vehicleId, dailyRate, days.
- Currency esta a nivel de command (no por item) porque todos los items de una reserva comparten moneda — simplificacion deliberada del POC.
- **Leccion**: Los inner records en commands son el mecanismo correcto para datos compuestos. No necesitan ser clases top-level — su scope es exclusivamente el command.

#### Decision 3: Tests separados por use case
- `ReservationApplicationServiceCreateTest` (8 tests) y `ReservationApplicationServiceTrackTest` (4 tests) en ficheros separados.
- En Customer y Fleet, un solo fichero bastaba. Aqui, el flujo de create (con conversion de items, currency compartida, order de save-publish-clear via InOrder de Mockito) ya genera 8 tests.
- **Leccion**: Separar tests por use case cuando la complejidad lo justifica. La regla no es "un fichero por servicio" sino "un fichero por concern testeable".

#### Decision 4: Solo 2 use cases por ahora — SAGA se añadira despues
- Customer y Fleet ya estan a su numero final de use cases (5). Reservation empieza con 2 y crecera a 7 cuando se implemente la SAGA.
- El `ReservationApplicationService` unico fue elegido porque la extraccion a un `ReservationSagaStepService` sera mecanica cuando llegue el momento.
- **Leccion**: Diseñar para el presente, preparar para el futuro. Las interfaces (una por use case) permiten la extraccion. No hace falta el split ahora.

### Verificacion Final

- **18 tests, 0 fallos** — ReservationApplicationServiceCreateTest (8), ReservationApplicationServiceTrackTest (4), ReservationApplicationMapperTest (4), ReservationNotFoundExceptionTest (2).
- **Zero `@Service`/`@Component`** en `reservation-application/src/main/` — confirmado.
- **BUILD SUCCESS**.

### Reflexiones del decimo ciclo

- **La capa de aplicacion refleja la complejidad del dominio**: Customer/Fleet application tenian 13-15 clases con 5 use cases. Reservation application tiene 10 clases con 2 use cases — pero cada use case es mas denso (items compuestos, snapshots, currency compartida). Menos anchura, mas profundidad.
- **El patron se mantiene pero las decisiones evolucionan**: Las decisiones basicas (commands con primitivos, mapper manual, excepcion extends RuntimeException) se replican. Las nuevas (CQS en respuestas, inner records, tests split por use case) son extensiones naturales del patron, no desviaciones.

---

## Fecha: 2026-02-21

## Undecimo Ciclo: Reservation Infrastructure + Container — El Tercer Microservicio Cobra Vida

### Contexto

Con reservation-domain (80 tests) y reservation-application (18 tests) completos, el Reservation Service era codigo sin vida propia — igual que Customer y Fleet antes de sus ciclos de infrastructure. Este change añade las dos capas externas de la arquitectura hexagonal y produce el tercer microservicio funcional de la plataforma.

La complejidad adicional vs Customer/Fleet es significativa: **parent-child JPA** (primer `@OneToMany` de la plataforma), `findByTrackingId` como segunda via de lookup, nested REST DTOs con validacion cascada, y un mapper de persistencia sustancialmente mas complejo (~50 lineas vs ~20).

### Que construimos

**2 POMs + 10 ficheros Java + 2 clases config + 1 SQL + 2 YMLs + 3 clases de test = 13 tests de integracion pasando**

```
reservation-service/reservation-infrastructure/src/main/java/com/vehiclerental/reservation/infrastructure/
├── adapter/
│   ├── input/
│   │   └── rest/
│   │       ├── ReservationController.java              ← @RestController, inyecta 2 input ports
│   │       └── dto/
│   │           └── CreateReservationRequest.java       ← record con inner CreateReservationItemRequest
│   └── output/
│       ├── persistence/
│       │   ├── ReservationJpaRepository.java           ← JpaRepository + findByTrackingId
│       │   ├── ReservationRepositoryAdapter.java       ← @Component
│       │   ├── entity/
│       │   │   ├── ReservationJpaEntity.java           ← @Entity con @OneToMany(cascade=ALL)
│       │   │   └── ReservationItemJpaEntity.java       ← @Entity con @ManyToOne(LAZY)
│       │   └── mapper/
│       │       └── ReservationPersistenceMapper.java   ← ~50 lineas, reconstruye parent-child
│       └── event/
│           └── ReservationDomainEventPublisherAdapter.java ← Logger no-op (reemplazado por outbox en change #12)
└── config/
    └── GlobalExceptionHandler.java

reservation-service/reservation-container/
├── src/main/java/com/vehiclerental/reservation/
│   ├── ReservationServiceApplication.java              ← @SpringBootApplication
│   └── config/
│       └── BeanConfiguration.java                      ← @Configuration, beans manuales
├── src/main/resources/
│   ├── application.yml                                 ← PostgreSQL, Flyway, puerto 8183
│   └── db/migration/
│       └── V1__create_reservation_tables.sql           ← 2 tablas (reservations + reservation_items) en 1 fichero
```

### Decisiones de Diseno Clave

#### Decision 1: Parent-child JPA con CascadeType.ALL — primer @OneToMany de la plataforma
- `ReservationJpaEntity` tiene `@OneToMany(mappedBy = "reservation", cascade = ALL, orphanRemoval = true)`.
- `ReservationItemJpaEntity` tiene `@ManyToOne(fetch = LAZY)` de vuelta.
- Customer y Fleet no tenian child entities — eran agregados planos.
- **Leccion**: El cascade garantiza atomicidad transaccional — insertar reserva e items en un solo `save()`. Pero trae una consecuencia inesperada: los metodos find del adapter necesitan `@Transactional(readOnly = true)` para evitar `LazyInitializationException` al acceder a la coleccion lazy fuera de la sesion de Hibernate.

#### Decision 2: @Transactional(readOnly = true) en find del adapter
- Sin esta anotacion, la sesion de Hibernate se cierra despues de que `JpaRepository.findById()` retorna. Cuando el mapper intenta acceder a la coleccion `@OneToMany` lazy, lanza `LazyInitializationException`.
- Customer y Fleet no necesitaban esto porque no tienen child entities.
- **Leccion**: Cada `@OneToMany` lazy requiere que la transaccion este abierta cuando se accede a la coleccion. Este patron se documentara como requisito para futuros servicios con entidades hijas (e.g., Payment con line items).

#### Decision 3: failureMessages como TEXT con comma-separated
- Un solo campo `TEXT` en la tabla. El mapper une `List<String>` con comas al persistir y separa al cargar.
- No se necesita tabla separada ni columna JSON — los failure messages son strings de diagnostico del SAGA, nunca se consultan individualmente.
- **Leccion**: No todo necesita normalizacion. Los datos de diagnostico que solo se leen como bloque van bien en un campo TEXT.

#### Decision 4: Dos tablas en una sola migracion Flyway
- `V1__create_reservation_tables.sql` crea `reservations` y `reservation_items` con FK inline.
- Customer y Fleet tenian una tabla cada uno. Aqui las dos tablas forman parte del mismo aggregate boundary — se despliegan juntas.
- **Leccion**: La granularidad de migraciones Flyway sigue la granularidad del aggregate, no la del modelo relacional. Un aggregate = una migracion.

#### Decision 5: Puerto 8183
- Customer: 8181, Fleet: 8182, Reservation: 8183. Secuencial y predecible. Payment sera 8184.

### Verificacion Final

- **13 tests de integracion, 0 fallos**:
  - `ReservationControllerIT` — 6 tests (POST 201 con items, GET 200 snapshot, GET 404, POST 400 invalido, POST 400 items vacios)
  - `ReservationRepositoryAdapterIT` — 5 tests (round-trip con items, findByTrackingId, not found por ID, not found por trackingId)
  - `ReservationServiceApplicationIT` — 2 tests (smoke test de arranque)
- **Domain y application siguen compilando** — tests previos intactos.
- **Reservation Service es ahora un microservicio funcional** — puerto 8183, REST API, PostgreSQL.

### Reflexiones del undecimo ciclo

- **El primer @OneToMany introduce lecciones que los agregados planos no enseñan**: LazyInitializationException, cascade, orphanRemoval, @Transactional en finds — todas estas son lecciones que solo emergen cuando hay child entities. Customer y Fleet eran demasiado simples para revelarlas.
- **El mapper de persistencia es donde la complejidad se acumula**: ~50 lineas reconstruyendo 5 value objects (PickupLocation, DateRange, Money x3) mas una lista de child entities. Es el triple del mapper de Customer. MapStruct no ayudaria aqui porque los factory methods (`reconstruct()`) no siguen la convencion getter/setter.
- **Tres microservicios funcionales**: La plataforma ahora tiene Customer (8181), Fleet (8182) y Reservation (8183). Los tres siguen el mismo patron hexagonal de 4 modulos, validando que la arquitectura escala.

---

## Fecha: 2026-02-21

## Duodecimo Ciclo: Reservation Outbox + Messaging — El Cambio Mas Denso del Proyecto

### Contexto

Los 11 changes anteriores construyeron tres microservicios con arquitectura hexagonal completa, pero la publicacion de eventos era siempre un no-op: un `log.info()` que simulaba publicar. Ningun evento salia del proceso. Este change resuelve el **dual-write problem** e introduce mensajeria real con RabbitMQ.

El problema fundamental: publicar a RabbitMQ dentro de un `@Transactional` crea dos operaciones de I/O independientes. Si la base de datos commitea pero el broker falla (o viceversa), el sistema queda permanentemente inconsistente. El **Outbox Pattern** elimina esto escribiendo el evento a una tabla `outbox_events` atomicamente con la entidad de negocio, y luego un scheduler aparte lo relay a RabbitMQ de forma asincrona.

Este fue el change con mas superficie tecnica de todo el proyecto: un modulo Maven nuevo (`common-messaging`), Docker Compose con PostgreSQL + RabbitMQ, topologia AMQP, Flyway migration, Testcontainers con dos brokers simultaneos, y 3 lecciones documentadas en CLAUDE.md.

### Que construimos

#### Nuevo modulo: `common-messaging` (7 clases Java)

```
common-messaging/src/main/java/com/vehiclerental/common/messaging/
├── outbox/
│   ├── OutboxStatus.java              ← enum: PENDING, PUBLISHED, FAILED
│   ├── OutboxEvent.java               ← @Entity con factory method, markPublished(), markFailed()
│   ├── OutboxEventRepository.java     ← Spring Data JPA + queries custom
│   └── OutboxPublisher.java           ← @Scheduled(fixedDelay=500), TransactionTemplate per event
├── cleanup/
│   └── OutboxCleanupScheduler.java    ← @Scheduled(cron daily 3AM), elimina PUBLISHED > 7 dias
└── config/
    ├── MessagingSchedulingConfig.java ← @EnableScheduling (auto-detected por component scan)
    └── MessageConverterConfig.java    ← Jackson2JsonMessageConverter para RabbitTemplate
```

#### Infraestructura nueva en reservation-service

- `OutboxReservationDomainEventPublisher.java` — reemplaza el logger no-op. Implementa `ReservationDomainEventPublisher` pero no tiene **ningun import de RabbitMQ** — solo depende de `OutboxEventRepository` (JPA) y `ObjectMapper` (Jackson). Escribe a la tabla outbox; el relay a RabbitMQ es trabajo del `OutboxPublisher`.
- `RabbitMQConfig.java` — declara la topologia AMQP: `reservation.exchange` (TopicExchange), `reservation.created.queue` con DLQ routing, `dlx.exchange` (DirectExchange), `reservation.dlq`, y los bindings correspondientes.

#### Docker Compose + infraestructura de desarrollo

- `docker-compose.yml` — 2 servicios bajo perfil `infra`: PostgreSQL 16 (multi-schema con usuarios dedicados) y RabbitMQ 3.13 (management + pre-carga de topologia). Health checks, volumenes nombrados.
- `docker/postgres/init-schemas.sql` — crea 4 schemas (reservation, customer, payment, fleet) con usuarios dedicados y `search_path` por usuario. Idempotente con `IF NOT EXISTS`.
- `docker/rabbitmq/definitions.json` — pre-declara la topologia completa de los 4 servicios al arranque del broker. Previene race conditions donde un servicio arranca antes de que su topologia exista.
- `docker/rabbitmq/rabbitmq.conf` — `load_definitions` apuntando al JSON.
- `Makefile` — 5 targets: `infra-up`, `infra-down`, `infra-reset` (wipe volumes), `infra-status`, `infra-logs`.

#### Migration y tests de integracion

- `V2__create_outbox_events_table.sql` — tabla `BIGSERIAL` con indice compuesto `(status, created_at)`.
- `OutboxAtomicityIT` (3 tests) — el test mas importante del change: verifica que reserva y outbox event se escriben en la misma transaccion, y que un fallo de dominio rollbackea ambos.
- `OutboxPublisherIT` (2 tests) — end-to-end: crea un `OutboxEvent` PENDING, espera con Awaitility a que el scheduler lo marque PUBLISHED, y verifica que el mensaje llego a la cola de RabbitMQ.

### Decisiones de Diseno Clave

#### Decision 1: `common-messaging` como modulo Maven separado, no dentro de `common`
- `common` es pure Java: `DomainEvent`, `AggregateRoot`, `Money`. Meter JPA entities y Spring scheduling ahi violaria el principio hexagonal que este POC enseña.
- Duplicar las ~7 clases de outbox en cada servicio no aporta valor educativo.
- **Leccion**: `common` = shared kernel puro (cero Spring). `common-messaging` = infraestructura compartida (Spring JPA + AMQP). Dos modulos, dos responsabilidades.

#### Decision 2: TransactionTemplate per-event, no @Transactional por batch
- El metodo `@Scheduled` del `OutboxPublisher` NO es `@Transactional`. Cada evento individual se wrappea en `transactionTemplate.executeWithoutResult(...)`.
- Si el evento #47 falla, los 46 anteriores ya estan commiteados como PUBLISHED.
- Una transaccion por batch rollbackearia todos si uno falla.
- **Leccion**: Para procesamiento de listas con efectos secundarios (publicar a broker), la granularidad de transaccion debe ser por item, no por batch.

#### Decision 3: Polling (@Scheduled 500ms), no CDC (Debezium)
- Change Data Capture con Debezium requiere Kafka Connect — infraestructura desproporcionada para un POC.
- 500ms de latencia es aceptable para una plataforma de alquiler de vehiculos.
- El polling es mas debuggeable y retry-safe: si la publicacion falla, el evento queda PENDING y se reintenta en el siguiente ciclo.
- **Leccion**: La solucion simple y correcta es mejor que la sofisticada y compleja. Debezium es la opcion correcta a escala; polling es la opcion correcta para aprender.

#### Decision 4: TopicExchange con routing key `{service}.{event-type}`
- `TopicExchange` permite wildcard bindings: un futuro servicio de auditoria puede bindear `*.created` para recibir todos los eventos de creacion.
- `DirectExchange` seria demasiado rigido, `FanoutExchange` demasiado indiscriminado.
- **Leccion**: TopicExchange es el default sensato para event-driven architectures. La flexibilidad de wildcards justifica la minima complejidad adicional.

#### Decision 5: definitions.json pre-declara la topologia completa de los 4 servicios
- Todas las exchanges, queues, DLQs y bindings de los 4 servicios se pre-crean al arrancar RabbitMQ — aunque 3 servicios no usen messaging todavia.
- Colas vacias no cuestan nada. La topologia estable como infraestructura evita race conditions.
- **Leccion**: La topologia del broker es infraestructura, no configuracion de aplicacion. Declararla centralmente en `definitions.json` es mas fiable que depender de que cada servicio cree sus beans `@Bean` antes de que otro servicio intente publicar.

#### Decision 6: OutboxReservationDomainEventPublisher tiene cero imports de RabbitMQ
- El adapter solo depende de `OutboxEventRepository` (JPA) y `ObjectMapper` (Jackson).
- Escribir al outbox es una operacion de base de datos pura. El relay asincrono a RabbitMQ es responsabilidad del `OutboxPublisher` del modulo `common-messaging`.
- **Leccion**: Separar "guardar para publicar" de "publicar" es la esencia del Outbox Pattern. El adapter que guarda no debe conocer el broker.

### Lecciones Aprendidas (documentadas en CLAUDE.md)

Este change genero 3 lecciones concretas que se añadieron a CLAUDE.md para referencia futura:

**1. Cross-module JPA scanning requiere las 3 anotaciones**
Cuando `reservation-service` importo `common-messaging`, Spring Boot NO auto-detecto `OutboxEvent` (@Entity) ni `OutboxEventRepository` porque viven fuera del package base del servicio. `@SpringBootApplication(scanBasePackages)` solo no basta — `@EntityScan` y `@EnableJpaRepositories` tienen scanning independiente y ambos deben apuntar a `com.vehiclerental`.

**2. Una vez messaging esta en el classpath, TODOS los ITs necesitan RabbitMQ**
Agregar `common-messaging` (que trae `spring-boot-starter-amqp`) al classpath hace que `OutboxPublisher` requiera un `RabbitTemplate`, que requiere conexion a RabbitMQ. Los ITs previamente verdes (`ReservationControllerIT`, `ReservationRepositoryAdapterIT`) empezaron a fallar al cargar el contexto. Solucion: `@Container @ServiceConnection RabbitMQContainer` en cada IT.

**3. Los domain events son transitorios — publicar desde el agregado ORIGINAL**
`reservationRepository.save(reservation)` retorna un objeto nuevo reconstruido desde JPA. Los domain events NO sobreviven el round-trip domain→JPA→domain. Hay que llamar `publish(reservation.getDomainEvents())` sobre la variable original, no sobre el objeto retornado por `save()`.

### Verificacion Final

- **5 tests de integracion nuevos** (OutboxAtomicityIT: 3, OutboxPublisherIT: 2), todos pasando.
- **13 ITs previos del change #11 siguen pasando** (ahora con `RabbitMQContainer` añadido).
- **Total ITs en reservation-container: 18** (13 del walking skeleton + 5 del outbox).
- **`common-messaging`** compila y se instala correctamente como modulo Maven.
- **Docker Compose** levanta PostgreSQL y RabbitMQ con health checks.

### Los Tres Microservicios + Messaging

Con este change, la plataforma tiene:

```
customer-service/  (puerto 8181) ← event publisher: logger no-op
fleet-service/     (puerto 8182) ← event publisher: logger no-op
reservation-service/ (puerto 8183) ← event publisher: OUTBOX PATTERN → RabbitMQ

common/            ← shared kernel, zero Spring
common-messaging/  ← outbox pattern, Spring JPA + AMQP
```

**Tests totales en la plataforma**:
- customer-domain: 58, customer-application: 17, customer-container: 11 = **86**
- fleet-domain: 50, fleet-application: 17, fleet-container: 12 = **79**
- reservation-domain: 80, reservation-application: 18, reservation-container: 18 = **116**
- **Total: 281 tests**, todos pasando.

### Reflexiones del duodecimo ciclo

- **El Outbox Pattern es conceptualmente simple pero operativamente denso**: "Escribir a una tabla, leer y publicar" suena trivial. Pero la implementacion correcta involucra: TransactionTemplate per-event (no por batch), retry con contador, estado FAILED despues de N intentos, cleanup scheduler, IndiceSQL compuesto, Jackson serialization, message headers, DLQ routing. Cada detalle importa.
- **Cross-module Spring scanning es la trampa mas sutil de Spring Boot**: `scanBasePackages` solo NO detecta `@Entity` ni repositorios en otros modulos. Hay que recordar las 3 anotaciones (`@SpringBootApplication`, `@EntityScan`, `@EnableJpaRepositories`) cada vez que se importa un modulo con JPA. Este error es invisible hasta runtime.
- **Los ITs que no tocan messaging se rompen cuando messaging entra al classpath**: Spring Boot auto-configura todo lo que encuentra en el classpath. Si `spring-boot-starter-amqp` esta presente, necesita un broker — aunque el IT solo testee persistencia. La solucion es un `@Container RabbitMQContainer` en cada IT, o mejor, una clase `BaseIT` compartida.
- **Docker Compose como infraestructura declarativa cambia el workflow de desarrollo**: Antes habia que configurar PostgreSQL manualmente. Ahora `make infra-up` levanta todo. `definitions.json` pre-crea la topologia de RabbitMQ. Los Testcontainers en ITs usan imagenes identicas. La paridad dev/test/prod es real.
- **12 changes, 281 tests, primer servicio con messaging real**: La plataforma paso de skeleton a sistema funcional. Customer y Fleet son CRUD simples; Reservation ya tiene maquina de estados, inner entities, outbox pattern y RabbitMQ. El siguiente paso natural es Payment (walking skeleton) y luego la SAGA que los conecta a todos.

### Siguiente paso

**payment-domain** — la capa de dominio del cuarto y ultimo microservicio. Comienza con el agregado Payment, value objects tipados, domain events y el port de repositorio.

---

## Fecha: 2026-02-21

## Decimotercer Ciclo: Payment Domain — El Cuarto Bounded Context, el Mas Simple y el Mas Directo

### Contexto

Con tres servicios completos (Customer, Fleet, Reservation) y el Outbox Pattern funcionando, el cuarto bounded context es el ultimo antes de la SAGA Orchestration. Payment es estructuralmente el mas simple de los cuatro dominios: agregado plano (sin inner entities como Reservation), 4 estados con transiciones asimetricas (como Customer/Fleet pero sin bidireccionalidad), y 3 domain events (uno por transicion). Es el change mas mecanico del proyecto porque todos los patrones ya estan consolidados — pero introduce un concepto nuevo: **transiciones asimetricas y finales** donde cada camino es one-way.

### Que construimos

**1 POM + 8 clases Java + 7 clases de test = 51 tests pasando**

```
payment-service/payment-domain/src/main/java/com/vehiclerental/payment/domain/
├── model/
│   ├── aggregate/
│   │   └── Payment.java                     ← Aggregate Root (create/reconstruct, complete/fail/refund)
│   └── vo/
│       ├── PaymentId.java                   ← Typed ID (record, UUID)
│       ├── ReservationId.java               ← Cross-context ID, local al bounded context
│       ├── CustomerId.java                  ← Cross-context ID, local al bounded context
│       └── PaymentStatus.java               ← Enum: PENDING, COMPLETED, FAILED, REFUNDED
├── event/
│   ├── PaymentCompletedEvent.java           ← Record con snapshot completo (paymentId, reservationId, customerId, amount)
│   ├── PaymentFailedEvent.java              ← Record con failureMessages
│   └── PaymentRefundedEvent.java            ← Record con amount refundado
├── exception/
│   └── PaymentDomainException.java          ← Extiende DomainException del common
└── port/
    └── output/
        └── PaymentRepository.java           ← Interface (save, findById, findByReservationId)
```

### Decisiones de Diseno Clave

#### Decision 1: Transiciones asimetricas y finales — ni bidireccionales ni lineales
- Customer/Fleet tienen transiciones bidireccionales (`suspend ↔ activate`). Reservation tiene una maquina lineal con branch (`PENDING → ... → CONFIRMED` + `CANCELLING → CANCELLED`).
- Payment tiene **tres caminos one-way desde dos origenes**: `PENDING → COMPLETED`, `PENDING → FAILED`, `COMPLETED → REFUNDED`. Ningun estado terminal tiene salida.
- **Leccion**: Cada dominio tiene su propia topologia de estados. No hay un patron unico. Lo que se comparte es el mecanismo (validar estado fuente, actualizar, registrar evento), no la forma.

#### Decision 2: No hay evento en create() — PENDING no es un hecho de negocio
- Customer emite `CustomerCreatedEvent` en `create()`. Reservation emite `ReservationCreatedEvent` en `create()`. Payment NO emite nada.
- Un Payment en PENDING es una instruccion del orquestador SAGA ("prepara un cobro"), no un resultado de negocio. Los eventos relevantes son los resultados: completado, fallido, reembolsado.
- **Leccion**: La pregunta correcta para decidir si emitir un evento en create() es: "¿Le importa al mundo exterior que esto exista en estado inicial?" Para Customer y Reservation, si. Para Payment, no — el mundo exterior se entera cuando el pago se resuelve.

#### Decision 3: failureMessages en fail() — mismo patron que Reservation
- `fail(List<String> failureMessages)` replica el patron de `Reservation.initCancel(List<String>)`.
- Los mensajes vienen de la capa de aplicacion (gateway simulado) y se propagan en el `PaymentFailedEvent` para diagnostico.
- Null y lista vacia se rechazan con `PAYMENT_FAILURE_MESSAGES_REQUIRED`.
- **Leccion**: Una vez que un patron funciona (failureMessages como `List<String>` con validacion), reutilizarlo sin variacion. La consistencia entre bounded contexts facilita el mantenimiento.

#### Decision 4: Money reutilizado de common, sin wrapper
- Fleet tiene `DailyRate` wrapping `Money` porque añade una restriccion de negocio ("estrictamente positivo") y participa en calculos (`dailyRate × days = subtotal`).
- Payment usa `Money` directamente. La unica validacion ("positivo") se hace en `create()`, no justifica un tipo separado.
- **Leccion**: Los wrappers de value objects se justifican cuando añaden invariantes o operaciones propias. Si solo validas en un punto, la validacion inline es suficiente.

#### Decision 5: findByReservationId para idempotencia en capa de aplicacion
- `PaymentRepository` expone `findByReservationId(ReservationId)` ademas de `findById(PaymentId)`.
- La idempotencia (no procesar dos pagos por la misma reserva) no es una regla del agregado — es una regla de orquestacion que el Application Service implementara llamando `findByReservationId` antes de `create()`.
- **Leccion**: El dominio provee la interfaz del port; la logica de orquestacion vive en la capa de aplicacion. El agregado no conoce el concepto de "ya existe un pago para esta reserva".

### Tests: 51 tests en 7 clases

| Clase de test | Tests | Que cubre |
|--------------|-------|-----------|
| PaymentTest | 9 | Creacion, validacion de nulos y amount zero, reconstruct, no public constructors |
| PaymentLifecycleTest | 14 | complete/fail/refund desde cada estado, validacion de failureMessages |
| PaymentDomainEventsTest | 15 | 3 eventos: campos accesibles, null eventId/occurredOn, DomainEvent impl, isRecord |
| PaymentDomainExceptionTest | 4 | errorCode, message, constructor con cause, extends DomainException |
| PaymentIdTest | 3 | Construccion, rechazo de null, igualdad |
| ReservationIdTest | 3 | Construccion, rechazo de null, igualdad |
| CustomerIdTest | 3 | Construccion, rechazo de null, igualdad |

### Verificacion Final

- **51 tests, 0 fallos** — cuarto dominio de la plataforma.
- **Zero Spring imports** en `payment-service/payment-domain/src/main/` — confirmado.
- **Zero cross-domain imports** — ni `com.vehiclerental.customer`, ni `fleet`, ni `reservation`.
- **BUILD SUCCESS** en `mvn clean install` desde root — los 16 modulos compilan.

### Diferencias vs los otros dominios

| Aspecto | Customer / Fleet | Reservation | Payment |
|---------|-----------------|-------------|---------|
| Maquina de estados | 3 estados, bidireccional | 6 estados, lineal + branch | 4 estados, asimetrico y final |
| Inner entities | Ninguna | ReservationItem | Ninguna |
| IDs cross-context | Solo su propio typed ID | CustomerId + VehicleId | ReservationId + CustomerId |
| Repository port | findById | findById + findByTrackingId | findById + findByReservationId |
| Domain events | Evento en create() | Evento en create() + cancel() | Solo en transiciones (no en create) |
| failureMessages | No | Si (initCancel) | Si (fail) |
| Tests | 50-58 | 80 | 51 |

### Reflexiones del decimotercer ciclo

- **El cuarto dominio confirma que el patron esta maduro**: Payment se implemento de principio a fin sin sorpresas arquitecturales. El shared kernel (`AggregateRoot`, `DomainEvent`, `DomainException`, `Money`) funciona sin modificaciones para un cuarto consumidor. El patron de typed IDs como records, factory methods `create()`/`reconstruct()`, transiciones que validan estado fuente — todo se replica sin friccion.
- **La ausencia de evento en create() es una decision de diseño, no una omision**: Las tres decisiones de "emitir o no en create" (Customer: si, Reservation: si, Payment: no) muestran que no hay una regla universal. Depende de si el estado inicial es un hecho de negocio relevante para el mundo exterior.
- **51 tests para un dominio simple es el rango correcto**: Customer (58) y Fleet (50) son comparables. Reservation (80) fue mas grande por inner entities y 6 estados. Payment con 51 tests cubre exhaustivamente las 3 transiciones con sus 4 estados fuente cada una, los 3 eventos, y las validaciones.
- **Todos los dominios completos — listo para SAGA**: Con los 4 bounded contexts implementados (Customer, Fleet, Reservation, Payment), el siguiente paso es la capa de aplicacion de Payment y luego la SAGA Orchestration que los conecta.

### Siguiente paso

**payment-application** — la capa de aplicacion del Payment Service. Use cases para procesar pagos, consultar estado, y manejar reembolsos. Replica el patron de Customer/Fleet/Reservation application con commands, DTOs, input/output ports, y mapper manual.

---

## Fecha: 2026-02-21

## Decimocuarto Ciclo: Payment Application — El PaymentGateway y la Copia Defensiva

### Contexto

Con el dominio de Payment completo (51 tests, 4 estados, 3 transiciones), la capa de aplicacion es el segundo paso antes de la infraestructura. Customer-application (17 tests), fleet-application (17 tests) y reservation-application (18 tests) ya establecieron el patron: input ports ISP, commands con primitivos, un Application Service con `@Transactional`, mapper manual, NotFoundException. Payment replica este patron pero introduce una diferencia fundamental: un **PaymentGateway output port** para delegar el procesamiento de cobros a un sistema externo. Customer y Fleet son deterministas — `suspend()` siempre funciona si el estado es valido. Payment depende de un procesador de pagos cuyo resultado es no-deterministico.

### Que construimos

**1 POM + 12 clases Java + 3 clases de test = 18 tests pasando**

```
payment-service/payment-application/src/main/java/com/vehiclerental/payment/application/
├── port/
│   ├── input/
│   │   ├── ProcessPaymentUseCase.java          ← Input port: crear + cobrar
│   │   ├── RefundPaymentUseCase.java           ← Input port: reembolsar (compensacion SAGA)
│   │   └── GetPaymentUseCase.java              ← Input port: consultar estado
│   └── output/
│       ├── PaymentDomainEventPublisher.java     ← Output port: publicar eventos de dominio
│       ├── PaymentGateway.java                  ← Output port: delegar cobro a sistema externo
│       └── PaymentGatewayResult.java            ← Record: resultado del cobro (success + failureMessages)
├── dto/
│   ├── command/
│   │   ├── ProcessPaymentCommand.java          ← (reservationId, customerId, amount, currency)
│   │   ├── RefundPaymentCommand.java           ← (reservationId — no paymentId)
│   │   └── GetPaymentCommand.java              ← (paymentId)
│   └── response/
│       └── PaymentResponse.java                ← Unico response para los 3 use cases (9 campos)
├── service/
│   └── PaymentApplicationService.java          ← Orquestador: idempotencia + gateway + save + publish
├── mapper/
│   └── PaymentApplicationMapper.java           ← Manual, 9 campos, sin Spring
└── exception/
    └── PaymentNotFoundException.java           ← RuntimeException, no DomainException
```

### Decisiones de Diseno Clave

#### Decision 1: PaymentGateway output port — la diferencia clave vs Customer/Fleet/Reservation

- Customer y Fleet no tienen ports de salida mas alla de repositorio y event publisher. Sus operaciones son deterministas.
- Payment necesita un seam para un sistema externo (procesador de pagos). Sin el gateway port, el Application Service siempre llamaria a `complete()` sin poder testear la ruta de `fail()`.
- **Alternativa rechazada**: flag `simulateFailure` en ProcessPaymentCommand. Contamina el contrato de aplicacion con concerns de testing.
- **Leccion**: Cuando la operacion depende de un resultado externo no-deterministico, la solucion hexagonal es un port de salida. El Application Service depende de una abstraccion; la implementacion (simulada, Stripe, stub de test) se inyecta en infraestructura/container.

#### Decision 2: PaymentGatewayResult como record, no boolean

- `PaymentGatewayResult(boolean success, List<String> failureMessages)` — lleva tanto el resultado como las razones de fallo.
- El dominio exige `fail(List<String> failureMessages)`, asi que el gateway debe devolver esos mensajes. Un boolean no alcanza.
- **Leccion**: Cuando el consumidor necesita mas que un si/no, el tipo de retorno del port debe reflejar esa riqueza. Un record con campos explícitos es mas seguro que un boolean + metodo estateful.

#### Decision 3: Idempotencia — retorno status-agnostic con nota sobre FAILED

- `findByReservationId` antes de `create()`. Si existe un pago (en cualquier estado: COMPLETED, FAILED, REFUNDED), se retorna tal cual sin reintentar.
- Un pago FAILED no se reintenta porque la SAGA crea una nueva reservacion (con nuevo reservationId) para reintentos, no reusa la misma.
- **Leccion**: La idempotencia debe documentar explicitamente que pasa con estados terminales no-exitosos. "Retorna el existente" suena simple, pero "retorna un FAILED sin reintentar" tiene implicaciones que dependen del flujo de la SAGA.

#### Decision 4: RefundPaymentCommand con reservationId, no paymentId

- La SAGA sabe que reservacion disparo la compensacion, pero no necesariamente rastrea el paymentId interno.
- Buscar por reservationId alinea el refund con el flujo de compensacion: "reembolsa el pago de la reservacion X".
- **Leccion**: El key de lookup en un command de compensacion debe ser el identificador que el orquestador conoce naturalmente, no el ID interno del aggregate.

#### Decision 5: List.copyOf() en publish — la copia defensiva

- `AggregateRoot.getDomainEvents()` retorna un `Collections.unmodifiableList()` — una **vista** sobre la lista interna, NO una copia.
- El patron `publish(events) → clearDomainEvents()` parece correcto, pero `clearDomainEvents()` limpia la lista subyacente, y cualquier referencia a la vista (incluida la que captura `ArgumentCaptor` en tests) ve una lista vacia.
- Fix: `eventPublisher.publish(List.copyOf(payment.getDomainEvents()))` — copia defensiva antes de clear.
- Customer/Fleet/Reservation pasan la vista directamente y funciona porque sus tests no capturan ni inspeccionan el contenido de la lista con ArgumentCaptor. Payment si lo necesita (para verificar PaymentCompletedEvent vs PaymentFailedEvent segun el resultado del gateway).
- **Leccion**: `Collections.unmodifiableList()` protege contra escritura pero no contra vaciado del backing list. Cuando la secuencia es publish → clear, y alguien necesita inspeccionar lo que se publico, la copia defensiva es obligatoria.

### Tests: 18 tests en 3 clases

| Clase de test | Tests | Que cubre |
|--------------|-------|-----------|
| PaymentNotFoundExceptionTest | 3 | Mensaje contiene identifier, extends RuntimeException, no extends PaymentDomainException |
| PaymentApplicationMapperTest | 3 | Mapeo completo (9 campos), failureMessages para FAILED, lista vacia (no null) para non-FAILED |
| PaymentApplicationServiceTest | 12 | ProcessPayment: charge succeeds → COMPLETED + evento, charge fails → FAILED + evento, idempotente retorna existente, eventos del aggregate original (no del save), orden save→publish; RefundPayment: happy path → REFUNDED + evento, not found; GetPayment: happy path, not found; Annotations: no @Service/@Component, @Transactional en writes, readOnly en get |

### Verificacion Final

- **18 tests, 0 fallos** — cuarta capa de aplicacion de la plataforma.
- **Zero `@Service`/`@Component`/`@Autowired`** en main sources — confirmado via grep.
- **Unica dependencia Spring**: `org.springframework.transaction.annotation.Transactional` (spring-tx).
- **BUILD SUCCESS** en `mvn clean install` desde root — los 17 modulos compilan.

### Diferencias vs las otras application layers

| Aspecto | Customer / Fleet | Reservation | Payment |
|---------|-----------------|-------------|---------|
| Use cases | 5 (CRUD + lifecycle) | 2 (create + track) | 3 (process + refund + get) |
| Output ports extra | Ninguno | Ninguno | PaymentGateway + PaymentGatewayResult |
| Idempotencia | No | Si (trackingId) | Si (reservationId, status-agnostic) |
| Lookup de compensacion | N/A | N/A | Por reservationId (no paymentId) |
| Response types | 1 (CustomerResponse/VehicleResponse) | 2 (Create vs Track) | 1 (PaymentResponse para todo) |
| Tests | 17 | 18 | 18 |
| Notable | Patron base | Items en create | Gateway routing + copia defensiva |

### Reflexiones del decimocuarto ciclo

- **El PaymentGateway es la primera adicion arquitectural genuina a la capa de aplicacion**: Los tres servicios anteriores siguen exactamente el mismo patron (repository + event publisher). Payment rompe esa simetria con un tercer output port que introduce una dependencia nueva para mockear en tests y un bean nuevo para wiring en container. No es boilerplate — es una necesidad real del dominio.
- **La copia defensiva revelo un subtlety en AggregateRoot**: `Collections.unmodifiableList()` no es lo mismo que `List.copyOf()`. El primero es una vista read-only del backing list; el segundo es un snapshot independiente. Para el patron publish → clear, la vista esta bien si nadie inspecciona el contenido despues del clear. Pero tests con ArgumentCaptor si lo hacen, y ahi la vista falla silenciosamente (lista vacia, no excepcion). `List.copyOf()` es la version segura.
- **La idempotencia status-agnostic necesita documentacion explicita**: "Retorna el existente" parece claro, pero cuando "el existente" es FAILED, la pregunta es: ¿deberia reintentar? La respuesta depende del flujo de la SAGA (no reintenta el mismo reservationId), no del Application Service. Documentar esta relacion evita bugs futuros si la SAGA evoluciona.
- **18 tests para 3 use cases es un conteo razonable**: Customer y Fleet tienen 17 tests para 5 use cases (mas simples). Payment tiene 18 tests para 3 use cases porque ProcessPayment tiene mas variantes (charge succeeds, charge fails, idempotente, eventos del original, orden de operaciones). El conteo refleja la complejidad, no el numero de use cases.
- **Cuatro application layers completas — el patron esta consolidado**: El flujo command → convert → domain method → save → publish(List.copyOf) → clear → response es identico en los 4 servicios. La unica variable es cuantos output ports necesita cada uno.

### Siguiente paso

**payment-infrastructure-and-container** — JPA entities, mappers JPA, REST controllers, Flyway migrations, Spring Boot configuration, y el SimulatedPaymentGateway que siempre retorna success. El ultimo change antes de la SAGA Orchestration.

---

## Fecha: 2026-02-22

## Decimoquinto Ciclo: Payment Infrastructure + Container — El Cuarto Microservicio y el Primer Gateway Externo

### Contexto

Con el dominio (51 tests) y la aplicacion (18 tests) completos, Payment necesitaba las dos capas externas para convertirse en microservicio funcional. Customer (ciclo 5, 25 tareas) y Fleet (ciclo 8, 27 tareas) ya establecieron el patron de infrastructure+container. Payment replica ese patron pero introduce dos novedades: (1) un **SimulatedPaymentGateway**, el primer adaptador de sistema externo en toda la plataforma — Customer y Fleet solo tienen persistence + event publisher, (2) un **PaymentPersistenceMapper como @Component** con ObjectMapper inyectado, rompiendo el patron de mapper POJO plano de Customer/Fleet porque `failureMessages` requiere serializacion JSON.

### Que construimos

**2 POMs + ~16 ficheros Java + 1 SQL + 2 YMLs + 3 clases de test = 13 tests de integracion pasando**

```
payment-service/payment-infrastructure/src/main/java/com/vehiclerental/payment/infrastructure/
├── adapter/
│   ├── input/
│   │   └── rest/
│   │       ├── PaymentController.java              ← @RestController, inyecta 3 input ports
│   │       └── dto/
│   │           ├── ProcessPaymentRequest.java       ← record con @NotBlank, @NotNull
│   │           └── RefundPaymentRequest.java        ← record con @NotBlank
│   └── output/
│       ├── persistence/
│       │   ├── PaymentJpaRepository.java            ← JpaRepository + findByReservationId
│       │   ├── PaymentRepositoryAdapter.java        ← @Component, implementa PaymentRepository
│       │   ├── entity/
│       │   │   └── PaymentJpaEntity.java            ← @Entity, separada del dominio
│       │   └── mapper/
│       │       └── PaymentPersistenceMapper.java    ← @Component con ObjectMapper (JSON)
│       ├── event/
│       │   └── PaymentDomainEventPublisherAdapter.java ← Logger no-op
│       └── gateway/
│           └── SimulatedPaymentGateway.java         ← @Component, siempre retorna success
└── config/
    └── GlobalExceptionHandler.java                  ← @RestControllerAdvice, 422 con errorCode

payment-service/payment-container/src/main/java/com/vehiclerental/payment/
├── PaymentServiceApplication.java                   ← @SpringBootApplication
└── config/
    └── BeanConfiguration.java                       ← @Configuration, 4 output ports + 3 input ports

payment-service/payment-container/src/main/resources/
├── application.yml                                  ← PostgreSQL, Flyway, puerto 8184
├── application-test.yml                             ← Testcontainers + flyway.default-schema: public
└── db/migration/
    └── V1__create_payments_table.sql                ← reservation_id UNIQUE
```

### Decisiones de Diseno Clave

#### Decision 1: PaymentPersistenceMapper como @Component — rompe el patron de Customer/Fleet

- Customer y Fleet usan mappers de persistencia como POJOs planos (sin anotaciones Spring). El mapper solo hace `new Entity()` y setters.
- Payment necesita serializar `List<String> failureMessages` a JSON (TEXT column) y deserializar de vuelta. Eso requiere `ObjectMapper`.
- Inyectar `ObjectMapper` via constructor convierte el mapper en un bean Spring (`@Component`), no un POJO instanciable manualmente.
- **Consecuencia en BeanConfiguration**: En Customer/Fleet, `BeanConfiguration` crea el mapper con `new CustomerPersistenceMapper()`. En Payment, el mapper ya es un `@Component` detectado por component scan — no se registra manualmente.
- **Alternativa rechazada**: Crear `ObjectMapper` internamente con `new ObjectMapper()`. Funciona pero ignora la configuracion global de Jackson de Spring Boot (date formats, naming strategy, modules registrados).
- **Leccion**: Cuando un mapper necesita una dependencia de infraestructura (ObjectMapper, Clock, etc.), la conversion a @Component es la solucion idiomatica en Spring. Es un trade-off aceptable: se pierde la simetria con los otros mappers pero se gana configuracion consistente.

#### Decision 2: failureMessages como TEXT + JSON — la serializacion pragmatica

- El dominio modela `failureMessages` como `List<String>`. La base de datos necesita persistirla.
- **Opcion A**: Tabla separada `payment_failure_messages` con FK. Correcto en 3NF pero overkill — solo se lee junto al pago, nunca se consulta independientemente.
- **Opcion B**: Array de PostgreSQL (`TEXT[]`). Elegante pero con soporte limitado en JPA y problemas de portabilidad.
- **Opcion C (elegida)**: Columna `TEXT` nullable con JSON serializado. `["insufficient_funds", "card_declined"]` o `null` si la lista esta vacia.
- `PaymentPersistenceMapper` maneja la serializacion: `List<String>` → JSON string (o null si vacia), JSON string → `List<String>` (o `List.of()` si null).
- **Leccion**: No todo necesita su propia tabla. Si el dato siempre se lee y escribe junto al aggregate, una columna JSON es mas simple y suficiente.

#### Decision 3: SimulatedPaymentGateway — el primer adaptador de sistema externo

- Customer y Fleet solo tienen dos tipos de adaptadores de salida: persistencia y event publisher. Payment añade un tercero: gateway de pagos.
- `SimulatedPaymentGateway` implementa `PaymentGateway` (output port de aplicacion) y siempre retorna `new PaymentGatewayResult(true, List.of())`.
- Loguea el intento de cobro (monto y moneda) con SLF4J a nivel INFO antes de retornar.
- **Leccion**: En un POC, un adaptador simulado que siempre retorna success es correcto. Demuestra el seam arquitectural (la aplicacion no sabe si es Stripe o un stub) sin acoplar a un SDK externo. Cuando se integre Stripe, solo se reemplaza esta clase — el Application Service no cambia.

#### Decision 4: GlobalExceptionHandler con errorCode para PaymentDomainException

- Customer/Fleet mapean `DomainException` → 422 con solo `message`. Payment añade `errorCode` al response body porque `PaymentDomainException` expone un codigo como `INVALID_PAYMENT_STATE`.
- El error code permite al cliente (o la SAGA) distinguir entre tipos de fallo sin parsear mensajes de texto.
- **Leccion**: Cuando el dominio tiene excepciones con codigos estructurados, el exception handler debe exponerlos. Un mensaje legible para humanos no es suficiente para orquestacion automatizada.

#### Decision 5: BeanConfiguration con 4 dependencias — el wiring mas complejo

- Customer/Fleet `BeanConfiguration` inyecta: repository + event publisher + mapper = 3 dependencias para el ApplicationService.
- Payment `BeanConfiguration` inyecta: repository + event publisher + **gateway** + mapper = 4 dependencias.
- Los 3 input ports (ProcessPaymentUseCase, RefundPaymentUseCase, GetPaymentUseCase) apuntan al mismo `PaymentApplicationService`, igual que en Customer/Fleet.
- **Leccion**: Cada output port nuevo es una dependencia mas en BeanConfiguration. Es el coste visible del hexagono — pero tambien su beneficio: cada dependencia es explicita y testeable independientemente.

### Errores y Soluciones Durante el Apply

#### Error 1: Constructor `protected` en PaymentJpaEntity (error repetido del ciclo 5)

- **Exactamente el mismo error que en Customer** (ciclo 5, Error 1): el mapper esta en `adapter.output.persistence.mapper`, la entidad JPA en `adapter.output.persistence.entity` — paquetes diferentes. Constructor `protected` no es accesible.
- **Solucion**: Cambiar a constructor `public`.
- **Reflexion**: Este error se documento en el ciclo 5 y se repitio aqui porque el design lo volvio a especificar como "protected" siguiendo la convencion JPA. La leccion del ciclo 5 no se propago al template de diseño. Deberia ser una regla permanente: "constructor publico cuando mapper y entidad estan en paquetes separados".

#### Error 2: Flyway `default-schema: payment` rompe Testcontainers (error nuevo)

- `application.yml` configura `spring.flyway.default-schema: payment` para que en Docker Compose (donde todos los servicios comparten un PostgreSQL) cada servicio tenga su propio schema.
- En tests con Testcontainers, Flyway crea la tabla `payments` en el schema `payment`, pero Hibernate `ddl-auto: validate` busca en el schema `public` → `Schema-validation: missing table [payments]`.
- Customer y Fleet no tienen `default-schema` en su application.yml, por eso nunca tuvieron este problema.
- **Solucion**: Añadir `spring.flyway.default-schema: public` en `application-test.yml` para que en tests Flyway cree las tablas en `public` (donde Hibernate las espera).
- **Leccion**: Flyway `default-schema` y Hibernate `default_schema` son configuraciones independientes. Si Flyway crea tablas en un schema custom pero Hibernate no sabe de ese schema, la validacion falla. En tests unitarios donde no hay schema custom, el override a `public` es la solucion correcta.

### Verificacion Final

- **13 tests de integracion, 0 fallos**:
  - `PaymentRepositoryAdapterIT` — 6 tests (round-trip save/findById, findById vacio, findByReservationId, findByReservationId vacio, failureMessages JSON serialization, unique constraint en reservationId)
  - `PaymentControllerIT` — 6 tests (POST 201, GET 200, GET 404, POST refund 200, validacion 400, domain exception 422)
  - `PaymentServiceApplicationIT` — 1 test (smoke test de arranque de contexto)
- **Domain y application siguen compilando** — BUILD SUCCESS, 69 tests previos (51 domain + 18 application) intactos.
- **Zero `@Service`/`@Component`** en domain y application `src/main/` — confirmado via grep.
- **Payment Service es ahora un microservicio funcional** — arranca en el puerto 8184, persiste en PostgreSQL, expone REST API con 3 endpoints.

### El Payment Service Completo: 4 Modulos Hexagonales

Con este change, el Payment Service tiene los 4 modulos que prescribe la arquitectura:

```
payment-service/
├── payment-domain/          ← 15 clases, 51 tests unitarios, ZERO Spring
├── payment-application/     ← 12 clases, 18 tests unitarios, solo spring-tx
├── payment-infrastructure/  ← ~11 clases, @Component/@RestController/@Entity
└── payment-container/       ← 2 clases + config, @SpringBootApplication
```

**Cadena de dependencias** (unidireccional):
```
container → infrastructure → application → domain → common
```

### Diferencias vs las otras capas de infraestructura

| Aspecto | Customer (ciclo 5) | Fleet (ciclo 8) | Payment (este ciclo) |
|---------|-------------------|-----------------|---------------------|
| Tareas | 25 | 27 | 27 |
| ITs | 11 | 11 | 13 |
| Input endpoints | 5 (CRUD + lifecycle) | 5 (CRUD + lifecycle) | 3 (process + refund + get) |
| Output adapters | 2 (persistence + event) | 2 (persistence + event) | 3 (persistence + event + **gateway**) |
| Persistence mapper | POJO plano | POJO plano | **@Component con ObjectMapper** |
| Puerto | 8181 | 8183 | 8184 |
| Flyway default-schema | No | No | **Si (payment)** |
| GlobalExceptionHandler | message only | message only | **message + errorCode** |
| Notable | Patron base | Replica del patron | Gateway externo + JSON serialization |

### Reflexiones del decimoquinto ciclo

- **El cuarto microservicio confirma que el patron esta consolidado**: El 90% del codigo de infrastructure+container es boilerplate estructural que se replica de Customer/Fleet. Las unicas partes nuevas son el SimulatedPaymentGateway y el PaymentPersistenceMapper con ObjectMapper. Esto valida que la arquitectura hexagonal de 4 modulos es un patron repetible y predecible.
- **El SimulatedPaymentGateway es trivial pero arquitecturalmente significativo**: 5 lineas de logica (log + return success), pero demuestra el tercer tipo de adaptador de salida. Cuando se integre Stripe o PayPal, solo se reemplaza esta clase. El Application Service, el dominio, los tests de aplicacion — nada cambia. Esa es la promesa del hexagono cumplida.
- **El error del constructor `protected` se repitio — necesita convertirse en regla**: Documentar un error en el journal no es suficiente si el design template lo vuelve a generar. La regla "constructor publico cuando mapper y entidad estan en paquetes separados" deberia estar en los docs de best practices o en el project.md, no solo en el journal.
- **Flyway `default-schema` es una trampa sutil para Testcontainers**: Es el primer error genuinamente nuevo en 4 ciclos de infrastructure+container. Customer y Fleet no lo tuvieron porque no usan schema separation. Payment lo necesita para Docker Compose (base compartida). La solucion (override en application-test.yml) es simple, pero el error es dificil de diagnosticar: Flyway no falla, Hibernate falla con "missing table" sin mencionar que busco en el schema equivocado.
- **failureMessages como JSON en TEXT es una decision que escalara bien**: Una tabla separada habria sido mas "relacional" pero sin beneficio practico — nunca se consultaran los failure messages independientemente del pago. El JSON es legible en la base de datos, simple de serializar, y no requiere JOINs.
- **13 ITs (vs 11 de Customer/Fleet) reflejan la mayor complejidad**: 6 tests de repositorio (vs 3 de Customer) porque hay que testear findByReservationId, JSON serialization de failureMessages, y el unique constraint en reservation_id. La complejidad de los tests refleja la complejidad del dominio, no el numero de endpoints.
- **Cuatro microservicios funcionales — la plataforma esta lista para la SAGA**: Customer (8181), Reservation (8182), Fleet (8183), Payment (8184). Los 4 arrancan, persisten en PostgreSQL, exponen REST API, y publican eventos (por ahora via logger no-op). El siguiente paso es la orquestacion: la SAGA que coordina los 4 servicios en un flujo de reserva end-to-end.

### Siguiente paso

**Payment Outbox + Messaging** — conectar Payment al bus de mensajeria existente usando el Outbox Pattern que ya funciona en Reservation. Los 3 eventos de dominio (completed, failed, refunded) necesitan llegar a RabbitMQ para que la futura SAGA sepa el resultado de cada pago.

---

## Fecha: 2026-02-22

## Decimosexto Ciclo: Payment Outbox + Messaging — Enchufar el Cuarto Servicio al Bus

### Contexto

Payment Service tiene 4 modulos hexagonales funcionando, 13 ITs pasando, y 3 eventos de dominio (`PaymentCompletedEvent`, `PaymentFailedEvent`, `PaymentRefundedEvent`) que se "publican" via un logger no-op (`PaymentDomainEventPublisherAdapter`). Los eventos se loguean y se pierden — no llegan a RabbitMQ, no pueden ser consumidos por la SAGA.

Toda la infraestructura pesada ya existe desde el ciclo #12 (`reservation-outbox-and-messaging`): el modulo `common-messaging` con OutboxEvent/OutboxPublisher/OutboxCleanupScheduler, Docker Compose con PostgreSQL + RabbitMQ, `definitions.json` con topologia pre-declarada para los 4 servicios, e `init-schemas.sql` con schema `payment` + `payment_user`.

Este change es deliberadamente mecanico: aplica el patron consolidado del ciclo #12 a un nuevo servicio. La unica novedad es que Payment tiene 3 eventos (vs 2 de Reservation), lo que implica 3 queues, 3 bindings y 3 DLQ bindings en lugar de 1.

### Que construimos

#### Nuevo adaptador: `OutboxPaymentDomainEventPublisher`

```
payment-service/payment-infrastructure/src/main/java/.../adapter/output/event/
├── OutboxPaymentDomainEventPublisher.java  ← NUEVO: implementa PaymentDomainEventPublisher
└── PaymentDomainEventPublisherAdapter.java ← ELIMINADO: logger no-op
```

- Implementa `PaymentDomainEventPublisher` (el output port de application).
- Tiene **cero imports de RabbitMQ** — solo depende de `OutboxEventRepository` (JPA) y `ObjectMapper` (Jackson).
- Usa `AGGREGATE_TYPE = "PAYMENT"` y `EXCHANGE = "payment.exchange"`.
- Deriva routing keys por convencion: `PaymentCompletedEvent` → `payment.completed`, `PaymentFailedEvent` → `payment.failed`, `PaymentRefundedEvent` → `payment.refunded`.
- Pattern matching sobre 3 tipos de evento para extraer `paymentId().value().toString()` como aggregateId.
- `@Component` — Spring auto-detecta el reemplazo del viejo adapter sin cambios en BeanConfiguration.

#### Nueva configuracion: `RabbitMQConfig`

```
payment-service/payment-infrastructure/src/main/java/.../config/
├── GlobalExceptionHandler.java  ← existente, sin cambios
└── RabbitMQConfig.java          ← NUEVO: topologia AMQP completa
```

- Declara `payment.exchange` (TopicExchange) y `dlx.exchange` (DirectExchange, idempotente con Reservation).
- 3 queues principales: `payment.completed.queue`, `payment.failed.queue`, `payment.refunded.queue` — cada una con DLQ routing.
- 1 DLQ compartida: `payment.dlq`.
- 6 bindings: 3 de queues a exchange + 3 de DLQ a dlx.exchange.

#### Cambios en container

- `PaymentServiceApplication` — añadidas las 3 anotaciones de scanning cross-module: `@SpringBootApplication(scanBasePackages)`, `@EntityScan`, `@EnableJpaRepositories`.
- `application.yml` — añadidas propiedades `spring.rabbitmq.host/port/username/password` con defaults via env vars.
- `V2__create_outbox_events_table.sql` — migración Flyway identica a la de Reservation (tabla generica por diseno).
- `payment-infrastructure/pom.xml` — añadida dependencia `common-messaging`.
- `payment-container/pom.xml` — añadidas dependencias test `testcontainers-rabbitmq` + `awaitility`.

#### Actualización de `definitions.json`

- Añadida `payment.refunded.queue` (faltaba — solo existian completed y failed).
- Añadido binding de `payment.exchange` → `payment.refunded.queue` con routing key `payment.refunded`.
- Añadido DLQ binding de `dlx.exchange` → `payment.dlq` con routing key `payment.refunded.dlq`.

#### ITs existentes actualizados + 2 ITs nuevos

- `PaymentRepositoryAdapterIT`, `PaymentControllerIT`, `PaymentServiceApplicationIT` — añadido `@Container @ServiceConnection RabbitMQContainer` a los 3 (leccion del ciclo #12: una vez messaging esta en classpath, todos los ITs necesitan broker).
- `OutboxAtomicityIT` — verifica que payment + outbox event persisten en la misma transaccion, y que un fallo de dominio (amount zero) rollbackea ambos.
- `OutboxPublisherIT` — inserta un OutboxEvent PENDING, espera con Awaitility a que el scheduler lo marque PUBLISHED, y verifica que el mensaje llego a `payment.completed.queue`.

### Notable Findings During Implementation

#### Finding 1: La implementacion fue completamente mecanica — zero errores, zero sorpresas

A diferencia del ciclo #12 (que genero 3 lecciones criticas documentadas en CLAUDE.md), este change se implemento sin ningun error ni iteracion. Las 14 tareas se completaron en secuencia directa y `mvn clean verify` paso al primer intento con los 16 tests (13 existentes + 2 nuevos + 1 smoke).

**Por que**: Las 3 lecciones del ciclo #12 (cross-module scanning, RabbitMQ en todos los ITs, domain events transitorios) ya estaban documentadas en CLAUDE.md y se aplicaron proactivamente. El design.md del change referencio explicitamente cada leccion. Esto valida que **documentar lecciones aprendidas en un archivo persistente (CLAUDE.md) tiene retorno real**: los mismos errores no se repiten.

**Reflexion**: En el ciclo #15 el error del constructor `protected` se repitio porque la leccion estaba en el journal pero no en los docs de best practices. En este ciclo, las lecciones estaban en CLAUDE.md (que el agente lee antes de cada sesion) y no se repitieron. La diferencia no es donde se documenta, sino **si el contexto de implementacion lo lee automaticamente**.

#### Finding 2: 3 eventos vs 2 — la unica dimension nueva

Reservation tiene 2 eventos (created, cancelled) → 1 queue + 1 DLQ binding. Payment tiene 3 eventos (completed, failed, refunded) → 3 queues + 3 DLQ bindings. Esta diferencia cuantitativa no añadio complejidad conceptual, pero si superficie:

- `RabbitMQConfig` tiene 12 beans (vs 6 de Reservation).
- `definitions.json` necesitaba un queue + 2 bindings adicionales que no existian (payment.refunded.queue).
- `extractAggregateId()` tiene 3 ramas de pattern matching (vs 2 de Reservation).

**Leccion**: El patron escala linealmente con el numero de eventos. Cada evento nuevo añade: 1 queue, 1 binding, 1 DLQ binding, 1 rama en extractAggregateId, y 1 linea en deriveRoutingKey. No hay complejidad emergente.

#### Finding 3: `definitions.json` estaba incompleto — payment.refunded.queue no existia

El ciclo #12 pre-declaro la topologia de los 4 servicios en `definitions.json`, pero solo incluyo `payment.completed.queue` y `payment.failed.queue`. La queue para `payment.refunded` no existia porque en ese momento Payment era solo un skeleton sin domain events definidos — el tercer evento (refund) se especifico en el ciclo #13.

Esto confirma la decision del ciclo #12 de que `RabbitMQConfig` como beans Spring es una "validacion idempotente" de lo pre-declarado en `definitions.json`: si el JSON esta incompleto, los beans de Spring crean la topologia faltante al arrancar. Pero la buena practica es mantener `definitions.json` como fuente de verdad y actualizarlo cuando se añaden eventos.

#### Finding 4: BeanConfiguration no necesito cambios — @Component es plug-and-play

El viejo `PaymentDomainEventPublisherAdapter` era `@Component`. El nuevo `OutboxPaymentDomainEventPublisher` tambien es `@Component` implementando la misma interfaz. Spring auto-detecta el reemplazo sin tocar `BeanConfiguration`. Esto funciona porque `BeanConfiguration` inyecta `PaymentDomainEventPublisher` (la interfaz), no la implementacion concreta.

**Leccion**: Cuando el output port se resuelve por component scan (no por `@Bean` manual), reemplazar un adapter es tan simple como borrar el viejo y crear el nuevo con la misma interfaz. El hexagono hace su trabajo.

### Verificacion Final

- **16 tests de integracion, 0 fallos**:
  - `PaymentRepositoryAdapterIT` — 6 tests (sin cambios en logica, solo añadido RabbitMQ container)
  - `PaymentControllerIT` — 6 tests (sin cambios en logica, solo añadido RabbitMQ container)
  - `PaymentServiceApplicationIT` — 1 test (smoke test, ahora con RabbitMQ container)
  - `OutboxAtomicityIT` — 2 tests (atomicidad + rollback)
  - `OutboxPublisherIT` — 1 test (scheduler → RabbitMQ)
- **BUILD SUCCESS** al primer intento.
- **`PaymentDomainEventPublisherAdapter.java`** confirmado eliminado del source tree.

### Comparativa: Reservation Outbox (ciclo #12) vs Payment Outbox (este ciclo)

| Aspecto | Reservation (ciclo #12) | Payment (este ciclo) |
|---------|------------------------|---------------------|
| Tareas | 20+ (incluye Docker, common-messaging) | 14 (solo "enchufar") |
| Modulos nuevos | 1 (common-messaging) | 0 |
| Clases Java nuevas | ~10 (common-messaging + infra) | 2 (OutboxPublisher + RabbitMQConfig) |
| Docker changes | docker-compose.yml, Makefile, init-schemas.sql, rabbitmq.conf, definitions.json | Solo definitions.json (1 queue + 2 bindings) |
| Eventos | 2 (created, cancelled) | 3 (completed, failed, refunded) |
| Queues | 1 + 1 DLQ | 3 + 1 DLQ |
| Beans en RabbitMQConfig | 6 | 12 |
| ITs nuevos | 5 (3 atomicidad + 2 publisher) | 3 (2 atomicidad + 1 publisher) |
| Lecciones nuevas en CLAUDE.md | 3 (scanning, RabbitMQ en ITs, events transitorios) | 0 (todas aplicadas del ciclo #12) |
| Errores durante implementacion | 3+ (scanning, ITs rotos, events perdidos) | 0 |
| Tiempo conceptual | Alto (patron nuevo) | Bajo (patron replicado) |

### Reflexiones del decimosexto ciclo

- **Zero errores valida que las lecciones del ciclo #12 estan bien documentadas**: Las 3 lecciones en CLAUDE.md (cross-module scanning, RabbitMQ en todos los ITs, domain events transitorios) previnieron exactamente los 3 errores que habrian ocurrido. Documentar lecciones en un archivo que se lee automaticamente al inicio de cada sesion es mas efectivo que documentarlas solo en el journal.
- **El patron Outbox es completamente mecanico una vez consolidado**: 14 tareas, zero ambiguedad, zero decisiones nuevas. Todo fue aplicar el template del ciclo #12. Esto valida que la arquitectura hexagonal + Outbox Pattern es un **patron repetible**: cada servicio nuevo se conecta al bus con el mismo checklist.
- **La diferencia entre "crear un patron" y "replicar un patron" es dramatica**: El ciclo #12 fue el mas denso del proyecto (Docker Compose, modulo Maven nuevo, 3 lecciones criticas, multiples errores). Este ciclo fue el mas rapido (14 tareas lineales, zero iteraciones). La inversion en crear un patron bien documentado se amortiza con cada replica.
- **definitions.json como fuente de verdad requiere mantenimiento activo**: Pre-declarar la topologia de los 4 servicios en el ciclo #12 fue una buena decision, pero quedo incompleta cuando Payment añadio el tercer evento (refund) en ciclos posteriores. La leccion: cuando se añaden domain events, actualizar definitions.json debe ser parte del checklist.
- **Dos servicios con Outbox real, dos pendientes**: Reservation y Payment publican eventos reales a RabbitMQ. Customer y Fleet siguen con logger no-op. Los connects de Customer y Fleet seran identicos a este change — mecanicos, sin sorpresas.

### Estado actual de la plataforma

```
customer-service/    (puerto 8181) ← event publisher: logger no-op
fleet-service/       (puerto 8183) ← event publisher: logger no-op
reservation-service/ (puerto 8183) ← event publisher: OUTBOX → RabbitMQ
payment-service/     (puerto 8184) ← event publisher: OUTBOX → RabbitMQ  ← NUEVO

common/              ← shared kernel, zero Spring
common-messaging/    ← outbox pattern, Spring JPA + AMQP
```

### Siguiente paso

Los dos servicios criticos para la SAGA (Reservation y Payment) ya publican eventos reales a RabbitMQ. El siguiente paso puede ser conectar Customer y Fleet al bus (misma mecanica) o ir directo a la **SAGA Orchestration** — el orquestador que coordinara el flujo end-to-end.

---

## Fecha: 2026-02-22

## Decimoseptimo Ciclo: Pre-SAGA Alignment — Alinear Inconsistencias Antes de la Coordinacion Distribuida

### Contexto

Con 4 microservicios funcionales (2 con Outbox real, 2 con logger no-op) y 392 tests pasando, el proyecto estaba listo *en apariencia* para entrar en la fase de SAGA. Pero una retrospectiva exhaustiva (`docs/retrospective-pre-saga.md`) — auditoria de los 19 modulos, topologia RabbitMQ, cobertura JaCoCo, y patrones de codigo — revelo 4 inconsistencias de prioridad alta que habria que resolver antes de que la coordinacion distribuida las amplificara.

Este ciclo tiene dos particularidades:

1. **Es el primer change retroactivo**: La implementacion se hizo directamente sin pasar por el flujo OpenSpec. Los artefactos (proposal.md, design.md, tasks.md, delta specs) se generaron *despues* de implementar y verificar, manteniendo la trazabilidad spec↔codigo sin retrasar el trabajo.
2. **Es puramente de alineacion**: No agrega funcionalidad nueva. Unifica patrones existentes y prepara infraestructura para los proximos changes.

### Que construimos

**4 tareas, 5 tests nuevos, 397 tests totales, BUILD SUCCESS**

#### Tarea 1: failureMessages — de comma-separated a JSON

```
reservation-service/reservation-infrastructure/.../mapper/
└── ReservationPersistenceMapper.java  ← MODIFICADO: ObjectMapper via constructor

reservation-service/reservation-container/.../config/
└── BeanConfiguration.java             ← MODIFICADO: factory method con ObjectMapper
```

- **Antes**: `String.join(",", failureMessages)` para serializar, `failureMessages.split(",")` para deserializar. Fragil: si un mensaje contiene una coma, se parte incorrectamente.
- **Despues**: `objectMapper.writeValueAsString(failureMessages)` / `objectMapper.readValue(json, new TypeReference<List<String>>(){})`. Robusto: JSON escapa correctamente cualquier caracter.
- **Patron identico** al que ya usaba `PaymentPersistenceMapper` (ciclo #15).
- **Consecuencia**: `ReservationPersistenceMapper` pasa de POJO plano a bean con dependencia. `BeanConfiguration` actualiza el factory method para inyectar `ObjectMapper`.

#### Tarea 2: Remover Flyway default-schema de Payment

```
payment-service/payment-container/src/main/resources/
└── application.yml  ← MODIFICADO: eliminada linea default-schema: payment
```

- **Antes**: Solo Payment definia `spring.flyway.default-schema: payment`. Los otros 3 servicios no lo definian (usan `public` por defecto).
- **Despues**: Ninguno define `default-schema`. Consistencia total.
- **Justificacion**: En database-per-service, el schema `public` es suficiente. El `default-schema` era un artefacto del ciclo #15 que intento resolver un problema de Docker Compose (base compartida) pero causo problemas con Testcontainers (ciclo #15, Error 2).

#### Tarea 3: Tests unitarios para OutboxPublisher

```
common-messaging/pom.xml                                  ← MODIFICADO: +spring-boot-starter-test
common-messaging/src/test/java/.../outbox/
└── OutboxPublisherTest.java                               ← NUEVO: 5 tests con Mockito
```

- `common-messaging` tenia **0 tests** — la logica critica del OutboxPublisher (polling, status transitions, retry, headers AMQP) solo se ejercitaba indirectamente via los ITs de Reservation y Payment.
- 5 unit tests cubren los caminos criticos:
  1. No-op cuando no hay eventos PENDING
  2. Publish exitoso marca PUBLISHED y guarda
  3. Fallo de send incrementa retry count
  4. Max retries (5) marca FAILED
  5. Headers AMQP correctos (X-Aggregate-Type, X-Aggregate-Id, messageId, contentType)
- **Decision**: Unit tests con mocks (no IT con Testcontainers). Los ITs end-to-end ya existen en cada servicio. Estos tests cubren la logica interna que los ITs no verifican directamente (retry count, headers).

#### Tarea 4: 4 command queues para SAGA orchestration

```
docker/rabbitmq/
└── definitions.json  ← MODIFICADO: +4 queues, +8 bindings
```

Nuevas queues:

| Queue | Exchange (receptor) | Routing Key | DLQ |
|-------|-------------------|-------------|-----|
| `customer.validate.command.queue` | `customer.exchange` | `customer.validate.command` | `customer.dlq` |
| `payment.process.command.queue` | `payment.exchange` | `payment.process.command` | `payment.dlq` |
| `payment.refund.command.queue` | `payment.exchange` | `payment.refund.command` | `payment.dlq` |
| `fleet.confirm.command.queue` | `fleet.exchange` | `fleet.confirm.command` | `fleet.dlq` |

- **Patron**: Cada command queue se bindea al exchange del servicio receptor (no del orquestador). Esto sigue la convencion de que cada servicio es dueno de su exchange.
- **Naming**: `{service}.{action}.command.queue` — distingue comandos de eventos por convencion.
- **DLQ**: Reutilizan la DLQ del servicio receptor (e.g., `customer.validate.command.dlq` → `customer.dlq`).
- **Topologia resultante**: 5 exchanges, 16 queues (8 eventos + 4 DLQ + 4 comandos), 24 bindings.

### Decisiones de Diseno Clave

#### Decision 1: JSON sin fallback comma-separated

- No hay datos legacy en produccion (POC). Un fallback añadiria complejidad sin beneficio.
- Si hubiera datos legacy, la migracion seria un script SQL: `UPDATE reservations SET failure_messages = '["' || replace(failure_messages, ',', '","') || '"]' WHERE failure_messages IS NOT NULL AND failure_messages NOT LIKE '[%'`.

#### Decision 2: Remover default-schema, no agregarlo a los demas

- Cuatro bases de datos separadas (`customer_db`, `fleet_db`, `reservation_db`, `payment_db`) — schema separation no aporta valor.
- Agregarla a los otros 3 significaria cambiar 3 application.yml + 3 application-test.yml + potencialmente ITs. Mucho ruido, cero beneficio.

#### Decision 3: Unit tests con mocks para OutboxPublisher — no IT

- Testcontainers para un modulo compartido (common-messaging) requeriria decidir contra cual servicio testear, o crear un contexto Spring artificial. Los mocks son mas limpios.
- Los ITs reales ya cubren el flujo end-to-end. Estos tests cubren el gap: logica interna (retry, status transitions, headers).

#### Decision 4: Command queues en exchange del receptor

- En **orquestacion**, el orquestador (Reservation) envia comandos. El receptor (Customer, Payment, Fleet) define *donde* los recibe.
- Si usaramos un `saga.exchange` centralizado, todos los servicios tendrian que saber de ese exchange — acoplamiento.
- Con exchange del receptor, el binding es transparente: `payment.exchange` → `payment.process.command.queue`. El orquestador solo necesita saber el exchange y routing key de destino.

### Workflow Retroactivo: Generar Artefactos OpenSpec Post-Implementacion

Este es el primer change donde los artefactos OpenSpec se crearon **despues** de implementar:

1. Se implementaron las 4 tareas y se verifico BUILD SUCCESS (397 tests).
2. Se generaron retroactivamente: `.openspec.yaml`, `proposal.md`, `design.md`, `tasks.md` (con todas las tareas marcadas `[x]`).
3. Se crearon 3 delta specs que documentan los cambios en capabilities existentes:
   - `reservation-jpa-persistence/spec.md` — MODIFIED: failureMessages JSON
   - `payment-container-assembly/spec.md` — MODIFIED: Flyway sin default-schema
   - `rabbitmq-topology/spec.md` — ADDED: command queues + bindings + DLQ bindings
4. Se verifico que los delta specs coinciden exactamente con el estado actual del codigo.
5. Se sincronizaron los delta specs al source of truth (`openspec/specs/`).
6. Se archivo como `2026-02-22-pre-saga-alignment`.

**Leccion**: El flujo OpenSpec puede usarse retroactivamente cuando la implementacion ya esta hecha. Los artefactos retroactivos son documentacion precisa (no especulativa) — documentan *lo que se hizo*, no lo que se *planea hacer*. La clave es que los delta specs se verifiquen contra el codigo real, no se escriban de memoria.

### Verificacion Final

- **397 tests, 0 fallos, 0 errores, 0 skipped**:
  - 392 tests previos intactos
  - 5 tests nuevos en `OutboxPublisherTest` (common-messaging)
- **BUILD SUCCESS** al primer intento.
- Delta specs verificados contra codigo fuente — zero discrepancias.
- Specs sincronizadas a `openspec/specs/` (3 capabilities actualizadas).

### Estado de la Topologia RabbitMQ Post-Alignment

```
Exchanges (5):  reservation, customer, payment, fleet, dlx

Event Queues (8):
  reservation.created.queue
  customer.validated.queue, customer.rejected.queue
  payment.completed.queue, payment.failed.queue, payment.refunded.queue
  fleet.confirmed.queue, fleet.rejected.queue

Command Queues (4):                         ← NUEVAS
  customer.validate.command.queue
  payment.process.command.queue
  payment.refund.command.queue
  fleet.confirm.command.queue

DLQ (4):  reservation.dlq, customer.dlq, payment.dlq, fleet.dlq
```

### Comparativa: Antes vs Despues del Alignment

| Aspecto | Antes | Despues |
|---------|-------|---------|
| failureMessages (Reservation) | Comma-separated | JSON (ObjectMapper) |
| failureMessages (Payment) | JSON (ObjectMapper) | JSON (ObjectMapper) — sin cambio |
| Flyway default-schema (Payment) | `payment` | No definido (usa `public`) |
| Flyway default-schema (otros 3) | No definido | No definido — sin cambio |
| OutboxPublisher tests | 0 | 5 unit tests |
| Command queues | 0 | 4 queues + 8 bindings |
| Total queues en definitions.json | 12 | 16 |
| Total bindings en definitions.json | 16 | 24 |
| Total tests plataforma | 392 | 397 |

### Reflexiones del decimoseptimo ciclo

- **La retrospectiva fue la herramienta correcta**: Sin la auditoria sistematica de los 19 modulos, las inconsistencias habrian pasado desapercibidas hasta que la SAGA las amplificara. Un mensaje con una coma en `failureMessages` habria causado un bug silencioso. La retrospectiva es mas valiosa *antes* de una fase de complejidad creciente, no despues.
- **El flujo retroactivo funciona pero es menos valioso que el flujo prospectivo**: Los artefactos generados post-implementacion son documentacion, no planificacion. No generaron las decisiones — las documentaron. El valor real de OpenSpec (pensar antes de implementar) solo se obtiene con el flujo completo. El flujo retroactivo es un *seguro de trazabilidad*, no un sustituto.
- **Las 4 tareas eran individualmente triviales pero colectivamente necesarias**: Ninguna de las 4 merecia su propio change. Pero dejarlas como deuda tecnica significaba entrar en SAGA con inconsistencias que complicarian el debugging. Agruparlas en un change de alineacion es el patron correcto: overhead minimo, maximo beneficio de consistencia.
- **La topologia de commands esta lista antes de necesitarla**: Las 4 command queues existen en definitions.json pero nadie las usa todavia. Esto es intencional: cuando `customer-outbox-and-messaging` implemente el `CustomerValidationListener`, la queue ya estara ahi. No habra que modificar definitions.json en cada change de messaging — solo en este, una vez.
- **El mapper POJO plano ya no existe en Reservation**: Con este cambio, `ReservationPersistenceMapper` sigue el mismo patron que `PaymentPersistenceMapper` — ambos requieren `ObjectMapper` via constructor. Customer y Fleet mantienen mappers POJO planos porque no tienen `failureMessages`. Si en el futuro lo necesitan, la conversion es mecanica.

### Siguiente paso

**Customer Outbox + Messaging** — conectar Customer Service al bus de mensajeria. Es el primer servicio participante de SAGA (no orquestador): necesita outbox infrastructure, nuevos domain events (`CustomerValidatedEvent`, `CustomerValidationFailedEvent`), un use case de validacion, y el primer `@RabbitListener` de toda la plataforma.

---

## Ciclo #18: customer-outbox-and-messaging (2026-02-28)

### Que se hizo

Conectar Customer Service al bus de mensajeria via Outbox Pattern. Customer Service es el primer servicio **participante** de SAGA (no orquestador): recibe un comando de validacion via RabbitMQ, ejecuta logica de negocio, y responde con un evento de exito o rechazo a traves del outbox.

### Cambios implementados

| Capa | Ficheros | Descripcion |
|------|----------|-------------|
| Domain | `CustomerValidatedEvent`, `CustomerRejectedEvent` | 2 nuevos records de SAGA response. Usan `reservationId` (UUID crudo) para correlacion cross-domain. |
| Application | `ValidateCustomerCommand`, `ValidateCustomerForReservationUseCase`, `CustomerApplicationService` | 6to interface implementado. Busca customer, valida estado ACTIVE, publica evento directamente (no via aggregate). |
| Infrastructure | `OutboxCustomerDomainEventPublisher` | Reemplaza el logger no-op. 6 instanceof checks, routing key auto-derivation, JSON via ObjectMapper, save a OutboxEventRepository. |
| Infrastructure | `RabbitMQConfig` | TopicExchange, 3 queues (validated, rejected, validate.command), per-queue DLQ routing keys, DirectExchange DLX. |
| Infrastructure | `CustomerValidationListener` | Primer `@RabbitListener` de la plataforma. Recibe raw Message, parsea JSON, invoca use case. |
| Container | `CustomerServiceApplication` | `@EntityScan` + `@EnableJpaRepositories` para cross-module JPA scanning de common-messaging. |
| Container | `application.yml` | RabbitMQ connection + listener retry (3 attempts, exponential backoff). |
| Container | `V2__create_outbox_events_table.sql` | Migracion identica a payment/reservation. |
| Container | `BeanConfiguration` | 6to use case bean registrado. |
| Tests | 8 unit tests dominio, 4 unit tests aplicacion, 5 ITs | OutboxPublisherIT, OutboxAtomicityIT, RabbitMQ Testcontainer en 3 ITs existentes. |

### Metricas

| Metrica | Antes | Despues |
|---------|-------|---------|
| Domain events Customer | 4 lifecycle | 4 lifecycle + 2 SAGA |
| Use cases Customer | 5 (CRUD + get) | 6 (+ValidateCustomerForReservation) |
| Event publisher | Logger no-op | OutboxCustomerDomainEventPublisher |
| RabbitMQ queues Customer | 0 | 3 + 1 DLQ |
| `@RabbitListener` plataforma | 0 | 1 |
| ITs Customer | 9 | 14 |
| Total ITs plataforma | ~39 | ~44 |

### Problemas encontrados y resueltos

#### 1. La odisea de Testcontainers + Docker Desktop 4.62.0

Este fue el problema mas complejo del ciclo y consumio la mayor parte del tiempo de debugging. Merece documentacion detallada porque afecta a **toda la plataforma**, no solo al Customer Service.

**Sintoma inicial**: Al ejecutar `mvn verify` en customer-container, todos los ITs fallaban con:

```
Could not find a valid Docker environment.
Please see logs and check configuration.
Attempted configurations were:
  NpipeSocketClientProviderStrategy: could not find docker endpoint
As no valid configuration was found, execution cannot continue.
```

Seguido por un cacheo agresivo: `"Previous attempts to find a Docker environment failed. Will not retry."`

**La pista falsa — el named pipe**: Docker Desktop 4.62.0 cambio el named pipe de `\\.\pipe\docker_engine` a `\\.\pipe\dockerDesktopLinuxEngine`. Esto nos llevo a investigar la ruta del pipe como causa raiz. Intentamos:

1. Configurar `~/.testcontainers.properties` con `docker.host=npipe:////./pipe/dockerDesktopLinuxEngine`
2. Setear la variable de entorno `DOCKER_HOST=npipe:////./pipe/dockerDesktopLinuxEngine`
3. Verificar que Docker respondia correctamente: `docker info` y `docker run hello-world` funcionaban perfecto

Nada de esto funciono. Testcontainers seguia cacheando el fallo y rechazando reintentar. Reiniciar Docker Desktop tampoco ayudo. Confirmar que el problema tambien afectaba a los ITs de Payment Service (que funcionaban en ciclos anteriores) descarto que fuera un problema de nuestro codigo nuevo.

**La investigacion online que revelo la causa real**: Buscando en GitHub issues de testcontainers-java encontramos:

- [Issue #11212](https://github.com/testcontainers/testcontainers-java/issues/11212): Docker 29.0.0 rompe Testcontainers porque requiere API version minima 1.44
- [Issue #11422](https://github.com/testcontainers/testcontainers-java/issues/11422): Confirmacion en Windows con NpipeSocketClientProviderStrategy, Status 400

**La causa raiz real**: No era el named pipe. Era la **version del Docker API**:

| Componente | Version | API |
|-----------|---------|-----|
| Docker Desktop 4.62.0 | Engine 29.2.1 | Minima 1.44 |
| Spring Boot 3.4.1 BOM | Testcontainers 1.20.4 | Default 1.32 |

Testcontainers 1.20.4 enviaba requests con API version 1.32. Docker Engine 29.x las rechazaba con **Status 400 (Bad Request)** porque ya no acepta versiones por debajo de 1.44. Testcontainers interpretaba esto como "Docker no disponible", cacheaba el fallo, y se negaba a reintentar.

**El fix aplicado (workaround inmediato)**: Crear `src/test/resources/docker-java.properties` con una sola linea:

```properties
api.version=1.44
```

Se creo en los 7 modulos que tienen ITs con Testcontainers:

- `customer-service/customer-container`
- `customer-service/customer-infrastructure`
- `payment-service/payment-container`
- `payment-service/payment-infrastructure`
- `reservation-service/reservation-container`
- `reservation-service/reservation-infrastructure`
- `fleet-service/fleet-container`
- `fleet-service/fleet-infrastructure`

**Resultado**: Los 14 ITs de Customer y los 16 de Payment pasaron inmediatamente. El `mvn verify` completo de toda la plataforma (19 modulos) paso en 8:18 min.

**Fix definitivo pendiente**: Overridear la version de Testcontainers en el parent POM:

```xml
<properties>
    <testcontainers.version>1.21.4</testcontainers.version>
</properties>
```

Testcontainers 1.21.4+ y 2.0.3+ incluyen el fix nativo para Docker 29.x (negociacion automatica de API version). Esto eliminaria la necesidad de los `docker-java.properties` en cada modulo. Lo dejamos como tarea para el proximo ciclo o como un mini-change de mantenimiento.

**Cronologia del troubleshooting**:

1. ITs fallan → sospechamos del named pipe cambiado
2. Configuramos `~/.testcontainers.properties` → no funciona
3. Configuramos `DOCKER_HOST` env var → no funciona
4. Reiniciamos Docker Desktop → no funciona
5. Verificamos que Payment ITs tambien fallan → descartamos error de codigo
6. Investigamos online → encontramos GitHub issues sobre Docker 29 API version
7. Creamos `docker-java.properties` con `api.version=1.44` → FUNCIONA
8. Extendemos el fix a todos los modulos → `mvn verify` completo pasa

**Moraleja**: El mensaje de error de Testcontainers ("could not find docker endpoint") es enganoso. No era que no encontrara Docker — lo encontraba, le hacia una peticion, Docker la rechazaba por version de API incorrecta, y Testcontainers lo reportaba como "Docker no disponible". Un mejor mensaje de error habria ahorrado horas de debugging.

#### 2. NoUniqueBeanDefinitionException en CustomerValidationListener

`CustomerValidationListener` tenia parametro `validateCustomerUseCase` que no coincidia con el nombre del bean `validateCustomerForReservationUseCase`. Spring no podia desambiguar entre el bean `customerApplicationService` (que implementa el interface) y el bean especifico del use case. Fix: renombrar parametro a `validateCustomerForReservationUseCase`.

#### 3. spring-rabbit-test no gestionado en BOM

Se intento agregar como dependencia test pero no esta en el parent BOM de Spring Boot 3.4.1 (no lo usa Payment tampoco). Se elimino.

### Lecciones aprendidas

- **SAGA events van con UUID crudo, no typed ID**: `reservationId` usa `java.util.UUID` (no `ReservationId`) para evitar dependencia cross-domain. Los typed IDs son para dentro del bounded context.
- **Los SAGA response events no pasan por el aggregate**: Se crean directamente en el application service con `eventPublisher.publish(List.of(event))`. No se usa `registerDomainEvent()` ni `clearDomainEvents()` porque no hay mutacion de estado en el aggregate.
- **El nombre del parametro del constructor importa**: Spring usa el nombre del parametro como fallback para desambiguar beans. Si hay N beans que implementan un interface, el parametro debe coincidir exactamente con el nombre del `@Bean` que se quiere inyectar.
- **Docker API version es una bomba de tiempo**: Cuando Docker Desktop se actualiza, puede romper Testcontainers silenciosamente. Hay que vigilar la version de Testcontainers y mantenerla compatible con Docker Engine. El workaround de `docker-java.properties` con `api.version=1.44` funciona, pero el fix real es mantener Testcontainers actualizado. **Pendiente**: override de `<testcontainers.version>1.21.4</testcontainers.version>` en el parent POM.
- **Los mensajes de error de Testcontainers son enganosos en Windows**: "Could not find docker endpoint" y "Previous attempts failed. Will not retry" no significan que Docker no este corriendo. Pueden significar que Docker rechazo la peticion por version de API incompatible (Status 400). Siempre verificar con `docker version` que API version requiere el engine antes de tocar configuracion de pipes o sockets.

### Siguiente paso

**Fleet Outbox + Messaging** — conectar Fleet Service al bus de mensajeria siguiendo el mismo patron que Customer. Sera el segundo servicio participante de SAGA.

---

## Ciclo #19: fleet-outbox-and-messaging (2026-03-06)

### Que se hizo

Conectar Fleet Service al bus de mensajeria via Outbox Pattern. Fleet es el segundo servicio **participante** de SAGA: recibe dos comandos via RabbitMQ (confirm availability y release reservation), ejecuta logica de negocio, y responde con eventos (confirmed/rejected/released) a traves del outbox. Tambien es el primer servicio con **compensacion**: el release command es idempotente y siempre publica FleetReleasedEvent sin tocar el aggregate.

### Cambios implementados

| Capa | Ficheros | Descripcion |
|------|----------|-------------|
| Domain | `FleetConfirmedEvent`, `FleetRejectedEvent`, `FleetReleasedEvent` | 3 nuevos records de SAGA response. Usan `reservationId` (UUID crudo) para correlacion. FleetRejectedEvent incluye `failureMessages: List<String>`. |
| Application | `ConfirmFleetAvailabilityCommand`, `ReleaseFleetReservationCommand`, 2 use case interfaces, `FleetApplicationService` | 6to y 7mo interfaces implementados. Confirm busca vehicle y valida ACTIVE; Release es idempotente (no toca aggregate). |
| Infrastructure | `OutboxFleetDomainEventPublisher` | Reemplaza el logger no-op. 7 instanceof checks, explicit Map para routing keys (no auto-derivation como Customer, por naming mixto Vehicle*/Fleet*). |
| Infrastructure | `RabbitMQConfig` | TopicExchange, 4 queues (confirmed, rejected, confirm.command, release.command), per-queue DLQ routing keys, DirectExchange DLX. |
| Infrastructure | `FleetConfirmationListener`, `FleetReleaseListener` | 2 `@RabbitListener`, uno por comando SAGA. Parsean raw Message con ObjectMapper. |
| Container | `FleetServiceApplication` | `@EntityScan` + `@EnableJpaRepositories` para cross-module JPA scanning de common-messaging. |
| Container | `application.yml` | RabbitMQ connection + listener retry (3 attempts, exponential backoff). |
| Container | `V2__create_outbox_events_table.sql` | Migracion identica a customer/payment/reservation. |
| Container | `BeanConfiguration` | 6to y 7mo use case beans registrados. |
| Platform | `definitions.json` | `fleet.release.command.queue` + binding + DLQ binding (total command queues: 5). |
| Tests | 9 unit tests dominio, 5 unit tests aplicacion, 5 ITs | OutboxPublisherIT, OutboxAtomicityIT, RabbitMQ Testcontainer en 3 ITs existentes. |

### Metricas

| Metrica | Antes | Despues |
|---------|-------|---------|
| Domain events Fleet | 4 lifecycle | 4 lifecycle + 3 SAGA |
| Use cases Fleet | 5 (CRUD + get) | 7 (+ConfirmFleetAvailability, +ReleaseFleetReservation) |
| Event publisher | Logger no-op | OutboxFleetDomainEventPublisher |
| RabbitMQ queues Fleet | 0 | 4 + 1 DLQ |
| `@RabbitListener` plataforma | 1 (Customer) | 3 (+2 Fleet) |
| ITs Fleet | 10 | 15 |
| Command queues en definitions.json | 4 | 5 |

### Problemas encontrados y resueltos

#### 1. UnnecessaryStubbingException en ReleaseFleetReservation tests

Los tests iniciales para `execute(ReleaseFleetReservationCommand)` stubbeaban `vehicleRepository.findById()`, pero la implementacion es un no-op sobre el aggregate — publica FleetReleasedEvent directamente sin buscar el vehicle. Mockito strict stubbing detecto los stubs innecesarios.

**Fix**: Eliminar los stubs del repository. Reemplazar `vehicleNotFoundStillPublishesFleetReleasedEvent` por `doesNotCallSaveOrClearDomainEvents` que verifica que `vehicleRepository.findById()` nunca se invoca.

**Moraleja**: Cuando un use case de compensacion es idempotente y no necesita el aggregate, los tests deben reflejar esa decision de diseno — no asumir que siempre se busca la entidad.

### Lecciones aprendidas

- **Fleet usa Map explicito para routing keys, no auto-derivation**: A diferencia de Customer/Payment donde los eventos se llaman `Customer*Event` y el routing key se deriva automaticamente del nombre de la clase, Fleet tiene naming mixto (`Vehicle*Event` para lifecycle, `Fleet*Event` para SAGA). Un `Map<Class, String>` explicito es mas claro y evita errores de derivacion.
- **Compensacion idempotente no requiere el aggregate**: `ReleaseFleetReservationUseCase` no hace `findById()` ni `save()`. Simplemente construye un `VehicleId` desde el command string y publica `FleetReleasedEvent`. Esto es correcto porque no hay modelo de `vehicle_reservations` todavia — el release solo notifica al orquestador.
- **Dos listeners, un servicio**: Fleet es el primer servicio con 2 `@RabbitListener` (confirm + release). Cada listener tiene su propia command queue, lo que permite scaling y DLQ independientes.
- **El patron outbox ya es mecanico**: Tercera vez implementandolo (tras Payment y Customer). El flujo es predecible: delete logger adapter → create outbox publisher → add RabbitMQConfig → add listeners → update container scanning → V2 migration → fix existing ITs con RabbitMQ container.
- **35 tasks en un solo ciclo es manejable**: Con el patron establecido y los servicios anteriores como referencia, 35 tasks se implementaron sin bloqueos significativos. La unica friccion fue el strict stubbing de Mockito (facil de resolver).

### Siguiente paso

**Reservation Outbox + Messaging refinement** o **SAGA Orchestrator** — el reservation-service ya tiene outbox, pero podria necesitar ajustes para los nuevos command/response queues de Fleet. El paso mas ambicioso seria empezar el orquestador SAGA en reservation-service.

---

## Ciclo #20: payment-saga-participation (2026-03-08)

### Que se hizo

Habilitar Payment Service para recibir comandos SAGA via RabbitMQ. A diferencia de Customer (#18) y Fleet (#19) que necesitaron toda la infraestructura de outbox desde cero, Payment ya tenia todo implementado desde el change #16. Este change solo agrega la **recepcion de comandos**: 2 listeners que delegan a los use cases existentes (`ProcessPaymentUseCase` y `RefundPaymentUseCase`).

### Cambios implementados

| Capa | Ficheros | Descripcion |
|------|----------|-------------|
| Infrastructure | `RabbitMQConfig` | +2 command queues (`payment.process.command.queue`, `payment.refund.command.queue`) con DLQ routing, +2 bindings a payment.exchange, +2 DLQ bindings. Total bindings: 10 (antes 6). |
| Infrastructure | `PaymentProcessListener` | `@RabbitListener` en `payment.process.command.queue`. Parsea JSON (reservationId, customerId, amount, currency), construye `ProcessPaymentCommand`, invoca use case. |
| Infrastructure | `PaymentRefundListener` | `@RabbitListener` en `payment.refund.command.queue`. Parsea JSON (reservationId), construye `RefundPaymentCommand`, invoca use case. |
| Container | `PaymentProcessListenerIT` | Envia raw Message al exchange, verifica Payment creado con status COMPLETED y outbox event publicado. |
| Container | `PaymentRefundListenerIT` | Crea Payment via use case, envia refund command, verifica transicion a REFUNDED y outbox event. |

### Metricas

| Metrica | Antes | Despues |
|---------|-------|---------|
| `@RabbitListener` Payment | 0 | 2 |
| `@RabbitListener` plataforma | 3 (1 Customer + 2 Fleet) | 5 (+2 Payment) |
| RabbitMQ bindings Payment | 6 (3 event + 3 DLQ) | 10 (+2 command + 2 command DLQ) |
| ITs Payment | 16 | 18 (+2 listener ITs) |
| Servicios participantes SAGA listos | 2 (Customer, Fleet) | 3 (Customer, Fleet, Payment) |

### Problemas encontrados y resueltos

#### 1. convertAndSend con String produce JSON envuelto

Al usar `rabbitTemplate.convertAndSend(exchange, routingKey, jsonString)` en los ITs, el message converter (Jackson o SimpleMessageConverter) envuelve el String de forma que `objectMapper.readTree(message.getBody())` no encuentra los campos esperados. `json.get("reservationId")` retorna null y el listener falla con NullPointerException.

**Fix**: Usar `rabbitTemplate.send()` con un `Message` construido manualmente via `MessageBuilder.withBody(payload.getBytes(UTF_8)).setContentType("application/json").build()`. Esto es consistente con como el `OutboxPublisher` envia mensajes en produccion.

**Moraleja**: En tests de listeners, siempre enviar mensajes como raw bytes con `rabbitTemplate.send()`, no como objetos con `convertAndSend()`. El message converter del test puede diferir del que se usa en produccion.

#### 2. aggregate_id en outbox_events es paymentId, no reservationId

El `OutboxPaymentDomainEventPublisher.extractAggregateId()` usa `e.paymentId().value().toString()` como aggregate_id. Los ITs inicialmente buscaban outbox events por `aggregate_id = reservationId`, que siempre daba 0 resultados.

**Fix**: Primero obtener el `paymentId` del registro en `payments` table via `SELECT id FROM payments WHERE reservation_id = ?::uuid`, luego usarlo para la query de outbox.

**Moraleja**: Verificar que campo usa el outbox publisher como aggregate_id antes de escribir assertions. No asumir que es el campo de correlacion SAGA.

### Lecciones aprendidas

- **Payment fue el change mas reducido de los 3 participantes**: Solo 12 tasks vs 30+ en Customer y Fleet. La diferencia es que Payment ya tenia toda la infraestructura de outbox y messaging. Solo faltaba la recepcion de comandos.
- **Reusar use cases existentes elimina complejidad**: A diferencia de Customer (que necesito `ValidateCustomerForReservationUseCase`) y Fleet (que necesito `ConfirmFleetAvailabilityUseCase` + `ReleaseFleetReservationUseCase`), Payment ya tenia los comandos correctos desde el change #14. `ProcessPaymentCommand(reservationId, customerId, amount, currency)` ya llevaba correlacion SAGA.
- **Patron de listeners ya es 100% mecanico**: Tercer servicio con el mismo patron exacto — `@Component`, `@RabbitListener(queues = "...")`, `Message` param, `objectMapper.readTree()`, construct command, invoke use case. Zero variacion entre servicios.
- **Los 3 servicios participantes estan listos para SAGA**: Customer, Fleet y Payment pueden recibir comandos y responder con eventos via outbox. El siguiente paso es el orquestador en Reservation Service.

### Siguiente paso

**SAGA Orchestrator** en reservation-service — el unico paso que queda antes de tener el flujo completo de reserva funcionando end-to-end.

---

## Ciclo #21: reservation-saga-orchestration (2026-03-08)

### Que se hizo

Implementar el SAGA Orchestrator completo en reservation-service. Este es el change mas ambicioso del proyecto: 43 tasks en 11 secciones que cruzan las 4 capas de la arquitectura hexagonal. El orquestador coordina el flujo secuencial Customer Validation → Payment Processing → Fleet Confirmation, con compensacion inversa (rollback) cuando un paso falla. Es el paso final que conecta los 3 servicios participantes (Customer, Fleet, Payment — ciclos #18-#20) en un flujo end-to-end funcional.

### Cambios implementados

| Capa | Ficheros | Descripcion |
|------|----------|-------------|
| Domain | `SagaStatus` | Enum con 5 estados (STARTED, PROCESSING, COMPENSATING, SUCCEEDED, FAILED) y `canTransitionTo()` via switch expression. |
| Domain | `SagaState` | Objeto de dominio puro Java. Factory methods `create()` y `reconstruct()`. Metodos de transicion: `beginProcessing`, `advanceToNextStep`, `markAsSucceeded`, `startCompensation`, `decrementStep`, `markAsFailed`. Version nullable para compatibilidad con JPA new-entity detection. |
| Domain | `SagaStateRepository` | Puerto de salida: `save(SagaState)`, `findById(UUID)`. |
| Domain | 30 unit tests | `SagaStatusTest` (11 tests) + `SagaStateTest` (19 tests, nested classes por operacion). |
| Application | `SagaStep<T>` | Interfaz generica: `process(T)`, `rollback(T)`, `getName()`, `hasCompensation()`. |
| Application | `ReservationSagaData` | Record con los 7 campos necesarios para todos los pasos SAGA. |
| Application | `SagaCommandPublisher` | Puerto de salida: `publish(exchange, routingKey, payload)`. |
| Application | `CustomerValidationStep` | Paso 0: envia a `customer.exchange`/`customer.validate.command`. Sin compensacion. |
| Application | `PaymentStep` | Paso 1: process envia `payment.process.command`, rollback envia `payment.refund.command`. Unico paso con compensacion. |
| Application | `FleetConfirmationStep` | Paso 2: envia a `fleet.exchange`/`fleet.confirm.command`. Sin compensacion. |
| Application | `ReservationSagaOrchestrator` | Clase central con 4 metodos `@Transactional`: `start()`, `handleStepSuccess()`, `handleStepFailure()`, `handleCompensationComplete()`. Logica de compensacion inversa via `findNextCompensatableStep()`. |
| Application | 20 unit tests | `SagaStepsTest` (12 tests) + `ReservationSagaOrchestratorTest` (8 tests, Mockito con LENIENT strictness). |
| Infrastructure | `SagaStateJpaEntity` | `@Entity` con `@Version` para optimistic locking. Separada del domain object (patron del proyecto). |
| Infrastructure | `SagaStateJpaRepository`, `SagaStatePersistenceMapper`, `SagaStateRepositoryAdapter` | Adaptador de persistencia completo. Usa `saveAndFlush()` para version correcta. |
| Infrastructure | `OutboxSagaCommandPublisher` | Implementa `SagaCommandPublisher` via outbox: escribe `OutboxEvent` con aggregateType "SAGA". |
| Infrastructure | `RabbitMQConfig` | +3 TopicExchanges participantes, +7 response queues con DLQ routing, +7 bindings. |
| Infrastructure | 7 Response Listeners | `CustomerValidated/RejectedResponseListener`, `PaymentCompleted/Failed/RefundedResponseListener`, `FleetConfirmed/RejectedResponseListener`. Cada uno parsea `Message.getBody()` via ObjectMapper y delega al orchestrator. |
| Container | `V3__create_saga_state_table.sql` | Tabla `saga_state` con PK `saga_id`, `@Version`, indice en status. |
| Container | `BeanConfiguration` | +5 beans nuevos (mapper, 3 steps, orchestrator). Updated `reservationApplicationService` con 4to parametro. |
| Container | `ReservationApplicationService` | Tras persist + event publishing, construye `ReservationSagaData` y llama `sagaOrchestrator.start()`. |
| Container | 5 ITs | `SagaStateRepositoryAdapterIT`, `ReservationSagaHappyPathIT`, `ReservationSagaCustomerRejectionIT`, `ReservationSagaPaymentFailureIT`, `ReservationSagaFleetRejectionIT`. |

### Metricas

| Metrica | Antes | Despues |
|---------|-------|---------|
| Unit tests reservation-service | 110 | 160 (+30 domain, +20 application) |
| ITs reservation-service | 15 | 20 (+5 SAGA ITs) |
| Total tests | 125 | 180 |
| `@RabbitListener` reservation-service | 0 | 7 (response listeners) |
| `@RabbitListener` plataforma | 5 (1 Customer + 2 Fleet + 2 Payment) | 12 (+7 Reservation) |
| RabbitMQ queues Reservation | 2 (created + DLQ) | 9 (+7 response queues) |
| SagaStep implementations | 0 | 3 (Customer, Payment, Fleet) |
| Tablas reservation-service | 3 (reservations, reservation_items, outbox_events) | 4 (+saga_state) |

### Problemas encontrados y resueltos

#### 1. JPA new-entity detection con @Version y ID asignado manualmente

`SagaState.create()` inicializaba version a `0L`. Pero JPA usa `@Version == null` para detectar entidades nuevas (persist) vs existentes (merge). Con version `0L`, JPA llamaba merge sobre una fila inexistente → `ObjectOptimisticLockingFailureException`.

**Fix**: Cambiar `SagaState.create()` para inicializar version a `null`. JPA entonces llama persist (correcto para INSERT), y tras el persist la version queda en 0. En el siguiente save, version 0 → merge correcto → version 1.

**Moraleja**: Con IDs asignados manualmente (UUID sin `@GeneratedValue`), JPA necesita `@Version == null` para distinguir INSERT de UPDATE. Esto aplica a cualquier entidad con PK manual y optimistic locking.

#### 2. saveAndFlush necesario para version visible inmediatamente

`SagaStateRepositoryAdapter.save()` usaba `jpaRepository.save()` que no fuerza flush. La version incrementada por `@Version` solo se refleja tras el flush SQL. El IT que verificaba version=1 despues del segundo save veia version=0.

**Fix**: Cambiar a `jpaRepository.saveAndFlush()`. El SQL UPDATE se ejecuta inmediatamente, actualizando el campo `@Version` en el objeto JPA.

**Moraleja**: Cuando se necesita leer el `@Version` actualizado inmediatamente despues de save (no al final del transaction), usar `saveAndFlush()`.

#### 3. Jackson2JsonMessageConverter double-serializa Strings en tests

El modulo `common-messaging` registra un `Jackson2JsonMessageConverter` como bean global. Cuando los ITs usaban `rabbitTemplate.convertAndSend(exchange, key, jsonString)`, Jackson serializaba el String como JSON string (envuelto en comillas). El listener recibia un `TextNode` en vez de un `ObjectNode`, y `json.get("reservationId")` retornaba null.

**Fix**: Cambiar los ITs para enviar `Map.of(...)` en vez de JSON strings. El `Jackson2JsonMessageConverter` serializa el Map como un JSON object correctamente. Alternativa: usar `rabbitTemplate.send()` con `MessageBuilder` (como hicimos en ciclo #20).

**Moraleja**: Con `Jackson2JsonMessageConverter` activo, nunca enviar JSON pre-serializado via `convertAndSend()`. Enviar objetos Java (Map, POJO) y dejar que el converter los serialice, o usar `send()` con raw bytes.

#### 4. Constructor de ReservationApplicationService cambio de 3 a 4 parametros

Al agregar `ReservationSagaOrchestrator` como 4ta dependencia, los tests existentes (`CreateTest`, `TrackTest`) dejaron de compilar.

**Fix**: Agregar `@Mock ReservationSagaOrchestrator sagaOrchestrator` en cada test y pasarlo como 4to argumento al constructor.

**Moraleja**: Todo cambio de constructor en un application service impacta sus tests. Revisar tests existentes inmediatamente despues de modificar la firma.

### Lecciones aprendidas

- **SAGA Orchestrator es el change mas complejo pero el patron es predecible**: 43 tasks suenan intimidante, pero el patron hexagonal descompone el problema en capas independientes. Domain primero (puro Java), luego application (logica de coordinacion), infrastructure (adaptadores), container (wiring + tests).
- **La compensacion inversa es elegante**: `findNextCompensatableStep(fromIndex)` camina hacia atras buscando pasos con `hasCompensation=true`. Solo PaymentStep tiene compensacion, asi que fleet rejection → payment refund → cancel. Customer rejection → cancel directo (sin compensacion). El codigo es generico y funciona para N pasos.
- **Los tests de SAGA son mas valiosos que los unitarios individuales**: Los 5 ITs cubren happy path, customer rejection, payment failure, fleet rejection (con compensacion), y persistencia de SagaState. Cada IT simula el flujo completo POST → mensajes → verificacion de estado final con Awaitility.
- **Jackson2JsonMessageConverter es una trampa recurrente**: Tercer ciclo donde el message converter causa problemas en tests (ver ciclo #20). La regla es clara: con Jackson converter, enviar objetos Java via `convertAndSend()` o raw bytes via `send()`. Nunca strings JSON pre-serializados.
- **`@Version null` para entidades nuevas con ID manual**: Leccion critica para JPA. Sin `@GeneratedValue`, la unica senyal de "nueva entidad" es `@Version == null`. Esto deberia documentarse como patron del proyecto para futuras entidades con PK asignado.
- **El flujo SAGA end-to-end funciona**: Reservation crea la saga → publica comando de validacion de customer via outbox → Customer responde con evento → listener delega al orchestrator → orquestador avanza al siguiente paso → y asi hasta confirmacion o compensacion. Los 3 servicios participantes y el orquestador estan conectados.

### Siguiente paso

El SAGA Orchestrator esta completo. Los posibles siguientes pasos son:
- **End-to-end testing** con los 4 servicios corriendo simultaneamente via Docker Compose
- **SAGA timeout/retry handling** — que pasa si un participante no responde?
- **Idempotencia de listeners** — evitar procesar el mismo mensaje dos veces
- **Monitoring/observability** — tracing distribuido para seguir una saga a traves de los servicios

---

## Ciclo #22: jacoco-permanent-coverage (2026-03-08)

### Que se hizo

Configurar JaCoCo como red de seguridad permanente en el build. Antes, JaCoCo solo se activaba con `-Pcoverage` y no tenia goal `check` — la cobertura se podia consultar pero nunca rompia el build. Ahora esta siempre activo con umbrales diferenciados por capa arquitectonica: domain/common al 80%, application al 75%, infrastructure al 60%. Los modulos container estan excluidos y las entidades JPA (data holders sin logica) no cuentan para el calculo.

### Cambios implementados

| Fichero | Descripcion |
|---------|-------------|
| `pom.xml` (parent) | JaCoCo movido de profile `coverage` a `<plugins>` activos. 6 executions en pluginManagement: `prepare-agent`, `report`, `prepare-agent-integration`, `report-integration`, `merge-results` (combina jacoco.exec + jacoco-it.exec), `check` (INSTRUCTION/COVEREDRATIO, minimum 0.80). Property `<jacoco.skip>false</jacoco.skip>`. Exclusiones globales: `**/entity/*JpaEntity.class`, `**/outbox/OutboxEvent.class`. Profile `coverage` eliminado. |
| 4x `*-container/pom.xml` | `<jacoco.skip>true</jacoco.skip>` en `<properties>`. |
| 4x `*-application/pom.xml` | Override del check execution con minimum `0.75`. |
| 4x `*-infrastructure/pom.xml` | Override del check execution con minimum `0.60`. |
| `CLAUDE.md` | Seccion Build & Run actualizada: JaCoCo siempre activo, sin `-Pcoverage`. |

### Metricas

| Metrica | Antes | Despues |
|---------|-------|---------|
| JaCoCo activacion | Solo con `-Pcoverage` | Siempre activo |
| Goal `check` | No existia | 14 modulos con check |
| Umbral domain/common | Sin umbral | 80% INSTRUCTION |
| Umbral application | Sin umbral | 75% INSTRUCTION |
| Umbral infrastructure | Sin umbral | 60% INSTRUCTION |
| Modulos excluidos | Ninguno | 4 containers (jacoco.skip) |
| Clases excluidas | Ninguna | 6 *JpaEntity + OutboxEvent |
| POMs modificados | 0 | 13 (1 parent + 4 container + 4 application + 4 infrastructure) |
| `mvn verify` resultado | BUILD SUCCESS (492 tests) | BUILD SUCCESS (492 tests + coverage check) |

### Decisiones de diseno relevantes

#### 1. Contador INSTRUCTION explicito

JaCoCo usa INSTRUCTION por defecto, pero dejarlo implicito es fragil ante cambios de version. Se especifica `<counter>INSTRUCTION</counter>` y `<value>COVEREDRATIO</value>` explicitamente en el XML para que la configuracion sea auto-documentada.

#### 2. Merge de datos unit + IT antes del check

Los modulos infrastructure no tienen tests unitarios — sus tests son ITs en el modulo container. Para que el check no falle con 0% en infrastructure (donde no existe `jacoco.exec`), se usa el goal `merge` que combina `jacoco.exec` + `jacoco-it.exec` en `jacoco-merged.exec`. El check corre sobre el merged. En modulos sin ITs (como domain), el merge simplemente usa el `jacoco.exec` disponible.

**Nota**: En la practica, los modulos infrastructure no tienen tests propios (ni unit ni IT) — los ITs viven en los container modules. El check de 60% es efectivamente un no-op hoy, pero se activara si se agregan tests directamente en infrastructure.

#### 3. Umbrales diferenciados via override en child POMs

JaCoCo no soporta umbrales condicionales por artifactId en `pluginManagement`. El parent define 80% como default. Los modulos application (75%) e infrastructure (60%) sobreescriben la configuracion del check en sus propios POMs. Los modulos domain y common heredan el default sin tocar nada.

#### 4. Exclusion de containers via property vs plugin block

Se evaluo `<skip>true</skip>` en bloque `<plugin>` vs property `jacoco.skip`. La property es una sola linea en `<properties>` — mas concisa que un bloque XML completo del plugin.

## Ciclo #23: archunit-architecture-tests (2026-03-08)

### Que se hizo

Nuevo modulo `architecture-tests` con tests ArchUnit que validan las boundaries de arquitectura hexagonal de forma automatica. Antes, las reglas (domain sin Spring, application sin infrastructure, dependencias inward-only) se cumplian por disciplina manual. Ahora son tests ejecutables que fallan el build si alguien viola una boundary.

### Cambios implementados

| Fichero | Descripcion |
|---------|-------------|
| `architecture-tests/pom.xml` | Nuevo modulo: parent POM, `jacoco.skip=true`, depende de 4x `*-infrastructure` + `common-messaging` + `archunit-junit5` (test). |
| `DomainPurityTest.java` | 5 reglas: domain no importa Spring, JPA, application, infrastructure, ni common-messaging. |
| `ApplicationIsolationTest.java` | 1 regla allowlist: application solo depende de domain, common, java, lombok, slf4j, jackson, spring-transaction. |
| `DependencyFlowTest.java` | 2 reglas: domain no depende de application/infrastructure, application no depende de infrastructure. |
| `pom.xml` (root) | `<module>architecture-tests</module>` al final de `<modules>`. |

### Metricas

| Metrica | Antes | Despues |
|---------|-------|---------|
| Enforcement de boundaries | Manual (disciplina) | Automatico (8 tests ArchUnit) |
| Tests de arquitectura | 0 | 8 (5 domain + 1 application + 2 flow) |
| Modulos en build | 19 | 20 (+architecture-tests) |
| Tiempo de build extra | 0s | ~5s (analisis estatico en memoria) |
| `mvn verify` resultado | BUILD SUCCESS (492 tests) | BUILD SUCCESS (500 tests) |

### Decisiones de diseno relevantes

#### 1. Allowlist expandida vs spec original

La spec original definia allowlist de application como: domain, common, java, lombok, spring-transaction. Al ejecutar los tests, ArchUnit detecto que el SAGA orchestrator en application usa `com.fasterxml.jackson` (serializacion de saga data) y `org.slf4j` (logging). Ambas son dependencias legitimas — Jackson es framework-agnostico y SLF4J es solo una API de logging. Se expandio el allowlist y se actualizaron specs y design.

#### 2. ImportOption.DoNotIncludeTests

Todas las clases de test usan `ImportOption.DoNotIncludeTests.class` para que ArchUnit solo analice codigo de produccion. Sin esto, las clases de test (que legitimamente importan Spring, JPA, etc.) generarian falsos positivos.

#### 3. Modulo dedicado vs tests distribuidos

Un solo modulo `architecture-tests` con todas las reglas, no tests en cada `*-container`. Esto evita duplicar las mismas reglas 4 veces. El modulo depende de los 4 `*-infrastructure` (transitivamente trae application + domain), asi que tiene todas las clases en classpath.

### Lecciones aprendidas

- **JaCoCo como quality gate permanente es el paso natural despues de estabilizar los tests**: Con 492 tests pasando, el riesgo ya no es "no tenemos tests" sino "alguien agrega codigo sin tests y la cobertura baja silenciosamente". El check automatico previene esa regresion.
- **Los umbrales diferenciados reflejan la realidad del proyecto**: Domain es logica pura (80% es facil), application tiene ceremony de mocking (75% es razonable), infrastructure tiene adapters triviales y tests costosos con Testcontainers (60% evita tests de bajo valor).
- **El merge de exec files resuelve el problema unit vs IT**: Sin merge, los modulos que solo tienen ITs fallarian el check unitario con 0%. El merge es transparente — si solo existe un archivo, lo usa.
- **Los mappers manuales deben estar cubiertos**: Inicialmente se penso excluirlos como "generados por MapStruct", pero son clases manuales con logica real de conversion (typed IDs, enums, listas). Incluirlos en cobertura tiene valor.
- **Este es el primer change puramente de build/tooling**: 13 POMs modificados, 0 ficheros Java. Todo es configuracion Maven. El workflow OpenSpec (proposal → design → specs → tasks) funciona igual de bien para changes de build que para features de codigo.

### Siguiente paso

Con JaCoCo como red de seguridad, los posibles siguientes pasos son:
- **End-to-end testing** con los 4 servicios via Docker Compose
- **SAGA timeout/retry handling** — que pasa si un participante no responde?
- **Idempotencia de listeners** — evitar procesar el mismo mensaje dos veces
- **Monitoring/observability** — MDC + tracing distribuido

---

## Ciclo #24: docker-compose-services (2026-03-08)

### Que se hizo

Se configuro Docker Compose para levantar la plataforma completa (4 microservicios + PostgreSQL + RabbitMQ) con un solo `docker compose up -d`. Incluye:

- **Actuator health**: `spring-boot-starter-actuator` en los 4 container POMs, solo endpoint `health` expuesto
- **Paketo images**: `mvn spring-boot:build-image` genera imagenes OCI sin Dockerfiles, naming `vehicle-rental/<service>:latest`
- **Docker Compose reescrito**: eliminado `profiles: [infra]`, 4 bloques de servicio con puertos, env vars, depends_on
- **Spring Boot 3.4.1 → 3.4.13**: upgrade necesario porque 3.4.1 hardcodeaba Docker API v1.24 (incompatible con Docker 29.x)
- **BeanConfiguration simplificados**: Spring 6.2 trackea runtime types, los wrappers de use cases causaban ambiguedad de beans
- **RabbitMQ definitions.json**: corregido `password_hash: "guest"` a `password: "guest"` (RabbitMQ 3.13 requiere hash base64 valido)

### Metricas

| Metrica | Antes | Despues |
|---------|-------|---------|
| Containers en compose | 2 (infra only) | 6 (infra + 4 servicios) |
| Comando para full stack | N/A (manual `mvn spring-boot:run` × 4) | `docker compose up -d` |
| Spring Boot version | 3.4.1 | 3.4.13 |
| `@Bean` en BeanConfigs | ~20 (con wrappers) | ~12 (solo servicios principales) |
| `mvn verify` resultado | BUILD SUCCESS (500 tests) | BUILD SUCCESS (500 tests) |

### Decisiones de diseno relevantes

#### 1. Sin Docker healthcheck en servicios (Paketo limitation)

Las imagenes Paketo (Bellsoft Liberica tiny stack) no incluyen curl, wget, bash ni ningun shell utility. No hay forma practica de ejecutar un healthcheck CMD-SHELL desde dentro del container. Se verifica health externamente con `curl localhost:<port>/actuator/health`. Infrastructure (postgres, rabbitmq) conserva healthchecks nativos.

#### 2. Spring Boot 3.4.13 — no solo un version bump

El upgrade a 3.4.13 trajo Spring Framework 6.2, que cambia como Spring resuelve tipos de beans en runtime. Antes, si un `@Bean` method devuelve `CustomerApplicationService`, Spring solo registra ese tipo concreto. Ahora Spring infiere **todos** los tipos que implementa (interfaces incluidas). Esto causo ambiguedad con los beans wrapper de use cases que devolvian la misma instancia. Solucion: eliminar los wrappers — Spring resuelve las interfaces directamente.

#### 3. RabbitMQ `password` vs `password_hash`

En definitions.json, usar `"password": "guest"` en vez de `"password_hash": "guest"` + `"hashing_algorithm"`. RabbitMQ 3.13 hashea internamente cuando recibe `password` en plaintext. El campo `password_hash` requiere un hash SHA-256 base64-encoded valido — `"guest"` literal no lo es.

### Lecciones aprendidas

- **Paketo images son MUY minimalistas**: No asumas que tienen herramientas CLI (curl, wget, bash, ls, java en PATH). Para healthchecks en Docker Compose, necesitas alternativas externas o un health-checker buildpack dedicado.
- **Los upgrades de Spring Boot menores pueden tener breaking changes sutiles**: 3.4.1 → 3.4.13 parece inocuo pero Spring 6.2 cambia semantica de bean resolution. Siempre ejecutar `mvn verify` despues de un upgrade.
- **definitions.json de RabbitMQ tiene trampas**: El campo `password_hash` NO acepta passwords en texto plano aunque el nombre sugiera que es un hash. Usa `password` para que RabbitMQ hashee internamente.
- **`docker compose down -v` es tu amigo para debugging de infra**: Cuando algo falla en RabbitMQ o PostgreSQL, los volumenes persistidos pueden tener estado corrupto. `-v` fuerza reinicio limpio.
- **El flujo OpenSpec funciona bien para changes de infraestructura**: Proposal → Design → Specs → Tasks aplica igual para Docker Compose que para features de codigo. Las decisiones D7-D9 (descubiertas durante implementacion) se documentaron retroactivamente en design.md.

### Siguiente paso

Con la plataforma levantando en Docker Compose, los posibles siguientes pasos son:
- **E2E testing con Bruno CLI** — colecciones .bru en git, tests contra los 4 servicios
- **SAGA timeout/retry handling** — que pasa si un participante no responde?
- **Idempotencia de listeners** — evitar procesar el mismo mensaje dos veces
