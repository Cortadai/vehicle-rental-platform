# Retrospectiva Pre-SAGA - Estado del Proyecto

**Fecha:** 2026-02-22
**Objetivo:** Auditar el estado real del codigo antes de entrar en la fase de coordinacion distribuida (SAGA Orchestration).

---

## 1. Inventario de Modulos

### Build: `mvn clean verify` — BUILD SUCCESS (5m 40s)

| # | Modulo | Estado | Tests | Tiempo |
|---|--------|--------|-------|--------|
| 1 | vehicle-rental-platform (parent) | OK | - | 0.5s |
| 2 | common | OK | 31 | 5.0s |
| 3 | common-messaging | OK | 0 | 1.8s |
| 4 | customer-domain | OK | 58 | 4.8s |
| 5 | customer-application | OK | 17 | 5.3s |
| 6 | customer-infrastructure | OK | 0 | 1.7s |
| 7 | customer-container | OK | 11 IT | 21.7s |
| 8 | fleet-domain | OK | 50 | 3.5s |
| 9 | fleet-application | OK | 17 | 11.5s |
| 10 | fleet-infrastructure | OK | 0 | 2.6s |
| 11 | fleet-container | OK | 12 IT | 26.9s |
| 12 | reservation-domain | OK | 80 | 7.2s |
| 13 | reservation-application | OK | 18 | 9.0s |
| 14 | reservation-infrastructure | OK | 0 | 2.1s |
| 15 | reservation-container | OK | 13 IT | 2m 00s |
| 16 | payment-domain | OK | 51 | 6.1s |
| 17 | payment-application | OK | 18 | 6.5s |
| 18 | payment-infrastructure | OK | 0 | 1.7s |
| 19 | payment-container | OK | 16 IT | 1m 41s |

**Total: 19 modulos, 392 tests, 0 fallos, 0 errores, 0 skipped.**

---

## 2. Inconsistencias entre Servicios

### 2.1 `spring.flyway.default-schema` en application.yml

| Servicio | Configuracion |
|----------|---------------|
| Customer | No definido (usa `public` por defecto) |
| Fleet | No definido (usa `public` por defecto) |
| Reservation | No definido (usa `public` por defecto) |
| **Payment** | **`default-schema: payment`** |

**Hallazgo:** Solo Payment define schema explicito. Esto es inconsistente — o todos lo definen o ninguno.

### 2.2 `@EntityScan` / `@EnableJpaRepositories` / `scanBasePackages`

| Servicio | scanBasePackages | @EntityScan | @EnableJpaRepositories | Estado |
|----------|------------------|-------------|----------------------|--------|
| Customer | -- | -- | -- | Default scanning |
| Fleet | -- | -- | -- | Default scanning |
| **Reservation** | `com.vehiclerental` | `com.vehiclerental` | `com.vehiclerental` | Full cross-module |
| **Payment** | `com.vehiclerental` | `com.vehiclerental` | `com.vehiclerental` | Full cross-module |

**Hallazgo:** Customer y Fleet no tienen las anotaciones de scanning cross-module. Funciona ahora porque no importan `common-messaging`, pero **se rompera** cuando se les agregue outbox. CLAUDE.md ya documenta esta leccion.

### 2.3 Patron de Event Publishing

| Servicio | Implementacion | Estrategia |
|----------|----------------|------------|
| Customer | `CustomerDomainEventPublisherAdapter` | Logger No-Op (solo log.info) |
| Fleet | `FleetDomainEventPublisherAdapter` | Logger No-Op (solo log.info) |
| Reservation | `OutboxReservationDomainEventPublisher` | Outbox Pattern (JSON -> outbox_events -> RabbitMQ) |
| Payment | `OutboxPaymentDomainEventPublisher` | Outbox Pattern (JSON -> outbox_events -> RabbitMQ) |

**Hallazgo:** Arquitectura en dos tiers. Customer/Fleet aun en modo stub.

### 2.4 Patron de `failureMessages` storage

| Servicio | Campo en dominio | Formato en BD | Mecanismo |
|----------|------------------|---------------|-----------|
| Customer | No tiene | N/A | N/A |
| Fleet | No tiene | N/A | N/A |
| **Reservation** | `List<String> failureMessages` | **Comma-separated String** | `String.join(",")` / `split(",")` |
| **Payment** | `List<String> failureMessages` | **JSON serializado** | `ObjectMapper.writeValueAsString()` / `readValue()` |

**Hallazgo critico:** Inconsistencia de serializacion entre Reservation (comma-separated, fragil si el mensaje contiene comas) y Payment (JSON, robusto). Deberia unificarse a JSON.

### 2.5 Patron de `getDomainEvents()` — defensive copy

| Servicio | Implementacion |
|----------|----------------|
| Todos | `Collections.unmodifiableList(domainEvents)` en `AggregateRoot` (comun) |

**Hallazgo:** Consistente. Todos usan la misma clase base `AggregateRoot` con vista inmutable. No usa `List.copyOf()` (que crearia una copia independiente). La vista inmutable es suficiente dado que `clearDomainEvents()` se llama siempre despues de publicar.

### 2.6 Testcontainers: RabbitMQContainer en ITs

| Servicio | RabbitMQContainer en ITs |
|----------|--------------------------|
| Customer | No |
| Fleet | No |
| Reservation | Si — todos los ITs (ServiceApplicationIT, ControllerIT, RepositoryAdapterIT, OutboxPublisherIT, OutboxAtomicityIT) |
| Payment | Si — todos los ITs (misma estructura que Reservation) |

**Hallazgo:** Correcto. Solo los servicios con `common-messaging` en classpath necesitan RabbitMQ.

### Tabla Resumen de Inconsistencias

| Aspecto | Customer | Fleet | Reservation | Payment |
|---------|----------|-------|-------------|---------|
| Flyway Schema | default | default | default | `payment` |
| Spring Scanning | default | default | full cross-module | full cross-module |
| Event Publishing | Logger No-Op | Logger No-Op | Outbox | Outbox |
| RabbitMQ en classpath | No | No | Si | Si |
| failureMessages | N/A | N/A | Comma-separated | JSON |
| getDomainEvents() | Unmodifiable | Unmodifiable | Unmodifiable | Unmodifiable |
| RabbitMQ en tests | No | No | Si | Si |

---

## 3. Topologia RabbitMQ

Fuente: `docker/rabbitmq/definitions.json`

### 3.1 Exchanges (5)

| Exchange | Tipo | Durable | Proposito |
|----------|------|---------|-----------|
| `reservation.exchange` | topic | Si | Eventos de Reservation |
| `customer.exchange` | topic | Si | Eventos de Customer |
| `payment.exchange` | topic | Si | Eventos de Payment |
| `fleet.exchange` | topic | Si | Eventos de Fleet |
| `dlx.exchange` | direct | Si | Dead Letter Exchange global |

### 3.2 Queues (12)

**Work Queues (8):**

| Queue | DLX Routing Key | Servicio |
|-------|-----------------|----------|
| `reservation.created.queue` | `reservation.created.dlq` | Reservation |
| `customer.validated.queue` | `customer.validated.dlq` | Customer |
| `customer.rejected.queue` | `customer.rejected.dlq` | Customer |
| `payment.completed.queue` | `payment.completed.dlq` | Payment |
| `payment.failed.queue` | `payment.failed.dlq` | Payment |
| `payment.refunded.queue` | `payment.refunded.dlq` | Payment |
| `fleet.confirmed.queue` | `fleet.confirmed.dlq` | Fleet |
| `fleet.rejected.queue` | `fleet.rejected.dlq` | Fleet |

**Dead Letter Queues (4):**

| DLQ | Recibe de |
|-----|-----------|
| `reservation.dlq` | reservation.created.queue |
| `customer.dlq` | customer.validated.queue, customer.rejected.queue |
| `payment.dlq` | payment.completed.queue, payment.failed.queue, payment.refunded.queue |
| `fleet.dlq` | fleet.confirmed.queue, fleet.rejected.queue |

### 3.3 Bindings (16)

**Work Queue Bindings (8):**

| Exchange -> Queue | Routing Key |
|-------------------|-------------|
| `reservation.exchange` -> `reservation.created.queue` | `reservation.created` |
| `customer.exchange` -> `customer.validated.queue` | `customer.validated` |
| `customer.exchange` -> `customer.rejected.queue` | `customer.rejected` |
| `payment.exchange` -> `payment.completed.queue` | `payment.completed` |
| `payment.exchange` -> `payment.failed.queue` | `payment.failed` |
| `payment.exchange` -> `payment.refunded.queue` | `payment.refunded` |
| `fleet.exchange` -> `fleet.confirmed.queue` | `fleet.confirmed` |
| `fleet.exchange` -> `fleet.rejected.queue` | `fleet.rejected` |

**DLQ Bindings (8):**
Cada work queue tiene un binding `dlx.exchange` -> `[servicio].dlq` con su routing key correspondiente.

### 3.4 Estado de Topologia por Servicio

| Servicio | RabbitMQConfig.java | application.yml RabbitMQ | @RabbitListener | Estado |
|----------|---------------------|--------------------------|-----------------|--------|
| **Reservation** | Si | Si | No | Outbox funcional, sin listeners |
| **Payment** | Si | Si | No | Outbox funcional, sin listeners |
| **Customer** | No | No | No | Solo topologia en definitions.json |
| **Fleet** | No | No | No | Solo topologia en definitions.json |

**Hallazgo:** La topologia esta pre-definida en `definitions.json` para los 4 servicios, pero solo Reservation y Payment tienen la configuracion de codigo (RabbitMQConfig + application.yml). Ningun servicio tiene `@RabbitListener` todavia.

---

## 4. Deuda Tecnica

### Prioridad Alta (Alinear antes/durante SAGA)

| # | Deuda | Impacto | Servicios |
|---|-------|---------|-----------|
| 1 | **failureMessages inconsistente**: Reservation usa comma-separated, Payment usa JSON | Bug potencial si un mensaje contiene comas | Reservation |
| 2 | **Customer/Fleet sin scanning cross-module**: Falta `@EntityScan` + `@EnableJpaRepositories` | Se rompera al agregar `common-messaging` | Customer, Fleet |
| 3 | **Flyway schema inconsistente**: Solo Payment define `default-schema` | Puede causar conflictos en schema compartido | Payment (o todos) |
| 4 | **`spring-boot-starter-test` en global dependencies**: Domain modules heredan Spring en test scope | Viola "zero Spring dependencies" en domain | Todos (root POM) |
| 5 | **Sin ArchUnit tests**: Las boundaries hexagonales no se enforzan programaticamente | Regresiones arquitectonicas silenciosas | Todos |
| 6 | **Sin listeners en ningun servicio**: Nadie consume eventos de RabbitMQ | Prerequisito para SAGA | Todos |

### Prioridad Media

| # | Deuda | Impacto |
|---|-------|---------|
| 7 | Sin indices en BD (status, email, category) | Performance en queries frecuentes |
| 8 | Infrastructure modules sin tests | 0 tests en 4 modulos infrastructure |
| 9 | `common-messaging` sin tests | 0 tests, toda la logica del OutboxPublisher sin cobertura directa |
| 10 | `ApiMetadata` y `ApiResponse` sin tests | 0% cobertura en common |
| 11 | Sin logging estructurado ni MDC | Dificil troubleshooting en produccion |

### Prioridad Baja

| # | Deuda | Impacto |
|---|-------|---------|
| 12 | Sin OpenAPI documentation | DX para consumidores de API |
| 13 | Sin README orientado a desarrolladores | Onboarding |
| 14 | MapStruct configurado pero no usado (mappers manuales) | Complejidad innecesaria en el POM |

---

## 5. Tareas Pendientes para los Proximos Changes

### 5.1 Customer Service — Readiness: 20%

**Tiene:**
- Domain events: `CustomerCreatedEvent`, `CustomerSuspendedEvent`, `CustomerActivatedEvent`, `CustomerDeletedEvent`
- Use cases: Create, Suspend, Activate, Delete, Get
- Event publisher (logger stub)

**Falta para ser participante SAGA:**
- [ ] Crear `OutboxCustomerDomainEventPublisher` (reemplazar logger stub)
- [ ] Flyway migration V2 para tabla `outbox_events`
- [ ] `RabbitMQConfig.java` con exchanges, queues, bindings
- [ ] `application.yml` con configuracion `spring.rabbitmq.*`
- [ ] `@EntityScan` + `@EnableJpaRepositories` + `scanBasePackages` en Application class
- [ ] RabbitMQContainer en todos los ITs
- [ ] **Nuevo domain event**: `CustomerValidatedEvent` (para SAGA)
- [ ] **Nuevo domain event**: `CustomerValidationFailedEvent` (para SAGA)
- [ ] **Nuevo use case**: `ValidateCustomerForReservationUseCase` (verifica que el customer exista y este activo)
- [ ] **Listener de entrada**: `@RabbitListener` que consuma request de validacion y ejecute el use case
- [ ] Publicar resultado via outbox: validated o rejected

### 5.2 Fleet Service — Readiness: 20%

**Tiene:**
- Domain events: `VehicleRegisteredEvent`, `VehicleActivatedEvent`, `VehicleSentToMaintenanceEvent`, `VehicleRetiredEvent`
- Use cases: Register, Activate, SendToMaintenance, Retire, Get
- Event publisher (logger stub)

**Falta para ser participante SAGA:**
- [ ] Crear `OutboxFleetDomainEventPublisher` (reemplazar logger stub)
- [ ] Flyway migration V2 para tabla `outbox_events`
- [ ] `RabbitMQConfig.java` con exchanges, queues, bindings
- [ ] `application.yml` con configuracion `spring.rabbitmq.*`
- [ ] `@EntityScan` + `@EnableJpaRepositories` + `scanBasePackages` en Application class
- [ ] RabbitMQContainer en todos los ITs
- [ ] **Nuevo domain event**: `FleetConfirmedEvent` (para SAGA)
- [ ] **Nuevo domain event**: `FleetRejectedEvent` (para SAGA)
- [ ] **Nuevo use case**: `ConfirmFleetAvailabilityUseCase` (verifica disponibilidad de vehiculos)
- [ ] **Nuevo use case**: `ReleaseFleetReservationUseCase` (compensacion: liberar vehiculos)
- [ ] **Listener de entrada**: `@RabbitListener` que consuma request de confirmacion y ejecute el use case
- [ ] Publicar resultado via outbox: confirmed o rejected

### 5.3 Reservation Service — Readiness: 60%

**Tiene:**
- Outbox pattern completo (outbox_events table + OutboxPublisher + RabbitMQConfig)
- Domain events: `ReservationCreatedEvent`, `ReservationCancelledEvent`
- States definidos: PENDING -> CUSTOMER_VALIDATED -> PAID -> CONFIRMED -> CANCELLING -> CANCELLED
- Use cases: CreateReservation, TrackReservation

**Falta para ser orquestador SAGA:**
- [ ] **SAGA Orchestrator**: `ReservationSagaOrchestrator` con maquina de estados
- [ ] **SAGA State persistence**: Tabla + entidad para tracking del progreso
- [ ] **SagaStep interface** + implementaciones: `CustomerValidationStep`, `PaymentStep`, `FleetConfirmationStep`
- [ ] **Listeners de respuesta**:
  - `@RabbitListener("customer.validated.queue")` -> avanzar a Payment step
  - `@RabbitListener("customer.rejected.queue")` -> cancelar reserva
  - `@RabbitListener("payment.completed.queue")` -> avanzar a Fleet step
  - `@RabbitListener("payment.failed.queue")` -> cancelar reserva
  - `@RabbitListener("fleet.confirmed.queue")` -> confirmar reserva
  - `@RabbitListener("fleet.rejected.queue")` -> compensar (refund payment) y cancelar
  - `@RabbitListener("payment.refunded.queue")` -> finalizar cancelacion
- [ ] **Compensating logic**: Orquestar refund de payment cuando fleet rechaza
- [ ] **Idempotency**: Verificar que eventos duplicados no causen doble procesamiento

### 5.4 Payment Service — Readiness: 70%

**Tiene:**
- Outbox pattern completo (outbox_events table + OutboxPublisher + RabbitMQConfig)
- Domain events: `PaymentCompletedEvent`, `PaymentFailedEvent`, `PaymentRefundedEvent`
- Use cases: ProcessPayment (con idempotency check), RefundPayment, GetPayment
- PaymentGateway simulado (SimulatedPaymentGateway)
- Topologia RabbitMQ completa (3 queues + DLQ)

**Falta para ser participante SAGA completo:**
- [ ] **Listener de entrada**: `@RabbitListener` que consuma "procesar pago para reserva X" y ejecute `ProcessPaymentUseCase`
- [ ] **Listener de compensacion**: `@RabbitListener` que consuma "refund pago para reserva X" y ejecute `RefundPaymentUseCase`
- [ ] Determinar que eventos triggerea cada listener (posiblemente `CustomerValidatedEvent` o un command dedicado)

---

## 6. Jackson Serialization de Typed IDs

### Formato de Serializacion

Todos los typed IDs son **Java records sin anotaciones Jackson**:

```java
public record PaymentId(UUID value) { ... }
public record ReservationId(UUID value) { ... }
public record CustomerId(UUID value) { ... }
public record VehicleId(UUID value) { ... }
public record TrackingId(UUID value) { ... }
```

- **Sin `@JsonValue`**: No aplana el UUID
- **Sin `@JsonCreator`**: No tiene constructor de deserializacion explicito
- **Sin custom serializers/deserializers**

### Formato Resultante por Capa

| Capa | Formato | Ejemplo |
|------|---------|---------|
| **Domain Events (Outbox payload)** | Objeto anidado | `{"paymentId": {"value": "uuid"}}` |
| **API Responses (REST)** | String plano | `{"paymentId": "uuid"}` |

Los response DTOs convierten a `String` antes de serializar:

```java
public record PaymentResponse(
    String paymentId,       // String, no PaymentId
    String reservationId,   // String, no ReservationId
    ...
) {}
```

### Consistencia

| Aspecto | Estado |
|---------|--------|
| Formato de typed IDs | Consistente entre servicios (todos `{"value":"uuid"}`) |
| Outbox serialization | `objectMapper.writeValueAsString(event)` — identico en Reservation y Payment |
| MessageConverter | `Jackson2JsonMessageConverter` con ObjectMapper default de Spring Boot |
| API responses | Consistente — todos convierten a String |

**Consideracion:** El formato anidado `{"value":"uuid"}` funciona pero es verboso. Agregar `@JsonValue` en los records aplainaria a `"uuid"`, reduciendo payload size. Esto es una decision de diseno, no un bug.

---

## 7. Logging

### 7.1 Inventario de Logging Calls

**Total: 9 llamadas a log en todo el codebase (excluyendo tests).**

#### Customer Service (2)

| Clase | Nivel | Mensaje |
|-------|-------|---------|
| `GlobalExceptionHandler` | ERROR | `Unexpected error` |
| `CustomerDomainEventPublisherAdapter` | INFO | `EVENT LOGGED (not published): {eventType} [eventId={id}]` |

#### Fleet Service (2)

| Clase | Nivel | Mensaje |
|-------|-------|---------|
| `GlobalExceptionHandler` | ERROR | `Unexpected error` |
| `FleetDomainEventPublisherAdapter` | INFO | `EVENT LOGGED (not published): {eventType} [eventId={id}]` |

#### Reservation Service (1)

| Clase | Nivel | Mensaje |
|-------|-------|---------|
| `GlobalExceptionHandler` | ERROR | `Unexpected error` |

#### Payment Service (2)

| Clase | Nivel | Mensaje |
|-------|-------|---------|
| `GlobalExceptionHandler` | ERROR | `Unexpected error` |
| `SimulatedPaymentGateway` | INFO | `Simulated payment gateway: charging {amount} {currency}` |

#### Common-Messaging (3)

| Clase | Nivel | Mensaje |
|-------|-------|---------|
| `OutboxPublisher` | DEBUG | `Published outbox event [id={}, type={}, aggregateId={}]` |
| `OutboxPublisher` | ERROR | `Outbox event FAILED after {} retries [...]` |
| `OutboxPublisher` | WARN | `Failed to publish outbox event [...]` |
| `OutboxCleanupScheduler` | INFO | `Outbox cleanup: deleted {} published events older than 7 days` |

### 7.2 MDC (Mapped Diagnostic Context)

**Estado: NO IMPLEMENTADO**

- No existe configuracion de MDC en el codigo
- Los docs (`docs/03-manejo-excepciones-enterprise.md`) documentan el patron recomendado pero no esta aplicado
- Los `GlobalExceptionHandler` no inyectan `traceId` en las respuestas de error

### 7.3 Correlation ID / Trace ID

**Estado: NO IMPLEMENTADO**

- No hay `correlationId`, `traceId`, ni `sagaId` en el codigo
- Es **critico** implementarlo antes de SAGA para trazar flujos distribuidos
- Los docs sugieren propagarlo via MDC + headers de RabbitMQ

### 7.4 Logback Configuration

**Estado: NO EXISTE**

- Sin `logback.xml` ni `logback-spring.xml` en ningun servicio
- Usa el default de Spring Boot (INFO level, console output basico)
- Los docs (`docs/06-configuracion-application-properties.md`) tienen template de logback-spring.xml con perfiles dev/prod

### 7.5 Application.yml Logging

**Estado: NO CONFIGURADO**

- Ningun servicio define seccion `logging:` en su `application.yml`

---

## 8. Cobertura de Tests (JaCoCo)

### Resumen Ejecutivo

| Metrica | Valor |
|---------|-------|
| **Total tests** | 392 |
| **Total modulos** | 19 |
| **Cobertura global (instrucciones)** | 96.7% (3667/3792) |
| **Cobertura global (branches)** | 95.5% (233/244) |
| **Cobertura global (lineas)** | 96.6% (805/833) |

### 8.1 Cobertura por Modulo

#### Domain Modules

| Modulo | Tests | Lineas | Branches | Estado |
|--------|-------|--------|----------|--------|
| customer-domain | 58 | 100% (107/107) | 100% (52/52) | Excelente |
| fleet-domain | 50 | 94.1% (128/136) | 87.1% (54/62) | Bueno |
| reservation-domain | 80 | 100% (159/159) | 100% (64/64) | Excelente |
| payment-domain | 51 | 100% (93/93) | 100% (36/36) | Excelente |

#### Application Modules

| Modulo | Tests | Lineas | Branches | Estado |
|--------|-------|--------|----------|--------|
| customer-application | 17 | 98.3% (57/58) | 75% (3/4) | Muy bueno |
| fleet-application | 17 | 98.5% (64/65) | N/A | Muy bueno |
| reservation-application | 18 | 100% (61/61) | N/A | Excelente |
| payment-application | 18 | 98.1% (52/53) | 100% (4/4) | Muy bueno |

#### Infrastructure Modules

| Modulo | Tests | Lineas | Branches | Estado |
|--------|-------|--------|----------|--------|
| customer-infrastructure | 0 | Sin reporte | Sin reporte | Sin cobertura directa |
| fleet-infrastructure | 0 | Sin reporte | Sin reporte | Sin cobertura directa |
| reservation-infrastructure | 0 | Sin reporte | Sin reporte | Sin cobertura directa |
| payment-infrastructure | 0 | Sin reporte | Sin reporte | Sin cobertura directa |

#### Container Modules (Integration Tests)

| Modulo | Tests | Lineas | Branches | Estado |
|--------|-------|--------|----------|--------|
| customer-container | 11 IT | 83.3% (10/12) | N/A | Bueno |
| fleet-container | 12 IT | 83.3% (10/12) | N/A | Bueno |
| reservation-container | 13 IT | 77.8% (7/9) | N/A | Aceptable |
| payment-container | 16 IT | 77.8% (7/9) | N/A | Aceptable |

#### Common Modules

| Modulo | Tests | Lineas | Branches | Estado |
|--------|-------|--------|----------|--------|
| common | 31 | 84.7% (50/59) | 90.9% (20/22) | Bueno |
| common-messaging | 0 | Sin reporte | Sin reporte | Sin cobertura directa |

### 8.2 Clases con Menor Cobertura

| Clase | Modulo | Cobertura | Causa |
|-------|--------|-----------|-------|
| `ApiMetadata` | common | 0% (0/7 lineas) | Sin tests |
| `ApiResponse` | common | 0% (0/2 lineas) | Sin tests |
| `VehicleActivatedEvent` | fleet-domain | 66.7% (4/6) | Evento no testeado completamente |
| `VehicleRetiredEvent` | fleet-domain | 66.7% (4/6) | Evento no testeado completamente |
| `VehicleSentToMaintenanceEvent` | fleet-domain | 66.7% (4/6) | Evento no testeado completamente |
| `*NotFoundException` (3 clases) | application (x3) | 75% (3/4) | Constructor overload no usado |
| `*ServiceApplication` (4 clases) | container (x4) | 33.3% (1/3) | Main method, tipico |

### 8.3 Modulos sin Cobertura

5 modulos no generaron reporte JaCoCo (0 tests directos):

1. **common-messaging** — Contiene `OutboxPublisher`, `OutboxCleanupScheduler`, `MessageConverterConfig`. Logica critica sin tests unitarios propios (se ejercita indirectamente via ITs de container).
2. **customer-infrastructure** — Controllers, repositories, mappers, event publisher
3. **fleet-infrastructure** — Controllers, repositories, mappers, event publisher
4. **reservation-infrastructure** — Controllers, repositories, mappers, event publisher, outbox publisher
5. **payment-infrastructure** — Controllers, repositories, mappers, event publisher, outbox publisher

### 8.4 Recomendacion sobre JaCoCo

La cobertura actual es **excelente en domain/application** (96-100%) y **aceptable en container** (77-83%). Los gaps principales estan en infrastructure (sin tests directos, cubiertos parcialmente por ITs) y common-messaging.

**Recomendacion:** No integrar JaCoCo al build permanente todavia. Agregarlo como change dedicado junto con umbrales minimos cuando se haya completado la fase de messaging (customer-messaging + fleet-messaging).

---

## Anexo: Items de MEJORAS-PENDIENTES.md

Para referencia, estos items ya estaban documentados antes de esta auditoria:

1. **[Alta]** Mover `spring-boot-starter-test` a dependencyManagement
2. **[Alta]** Agregar tests de ArchUnit
3. **[Media]** Agregar indices a tablas
4. **[Media]** Agregar docker-compose.yml (ya hecho parcialmente)
5. **[Media]** Considerar MapStruct para mappers
6. **[Baja]** Agregar OpenAPI documentation
7. **[Baja]** Agregar README con instrucciones de build
8. **[Baja]** Actualizar journal de aprendizaje
