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
