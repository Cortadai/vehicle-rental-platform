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
