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
