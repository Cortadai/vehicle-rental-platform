# Síntesis Arquitectónica: Vehicle Rental Platform

> **Proyecto**: POC de Microservicios con Arquitectura Hexagonal, SAGA, Outbox Pattern y RabbitMQ
> **Dominio**: Plataforma de Alquiler de Vehículos
> **Stack**: Java 21, Spring Boot 3.x, PostgreSQL, RabbitMQ, Docker Compose
> **Objetivo**: Aprendizaje profundo de patrones enterprise + evaluación de OpenSpec como herramienta SDD
> **Autor**: David — Analista/Programador/Arquitecto

---

## 1. Visión General del Sistema

### 1.1 Descripción

Sistema de alquiler de vehículos compuesto por 4 microservicios que se comunican mediante eventos a través de RabbitMQ. El flujo transaccional completo para procesar una reserva requiere: validación del cliente, procesamiento del pago y confirmación de disponibilidad en la flota. Se implementa el patrón SAGA Orchestration con compensaciones automáticas y Outbox Pattern para garantizar consistencia eventual.

### 1.2 Los 4 Microservicios

| Servicio | Rol | Responsabilidad |
|----------|-----|-----------------|
| **Reservation Service** | Coordinador SAGA | Punto de entrada REST. Crea reservas, coordina el flujo SAGA entre los demás servicios, gestiona el ciclo de vida completo de la reserva |
| **Customer Service** | Validación | Verifica existencia del cliente, estado activo, licencia de conducir válida y elegibilidad para reservar |
| **Payment Service** | Procesamiento de pago | Ejecuta el cargo al cliente. Soporta compensación (reembolso) si pasos posteriores fallan |
| **Fleet Service** | Gestión de flota | Confirma disponibilidad del vehículo para las fechas solicitadas, reserva el vehículo en la flota. Equivalente al "Restaurant Service" del curso de referencia |

### 1.3 Flujo de Datos: Proceso de Reserva (Happy Path)

```
Cliente HTTP (Postman)
    │
    ▼
┌─────────────────────┐
│  Reservation Service │ ◄── Coordinador SAGA
│  (REST API)          │
└──────────┬──────────┘
           │
           │ ① Crear reserva en estado PENDING (DB local)
           │ ② Publicar ReservationCreatedEvent → Outbox → RabbitMQ
           │
           ▼
┌─────────────────────┐
│  Customer Service    │
│                      │ ③ Consume evento, valida cliente
│                      │ ④ Persiste validación en DB local
│                      │ ⑤ Publica CustomerValidatedEvent → Outbox → RabbitMQ
└─────────────────────┘
           │
           ▼
┌─────────────────────┐
│  Reservation Service │
│                      │ ⑥ Consume evento, actualiza reserva a CUSTOMER_VALIDATED
│                      │ ⑦ Publica ReservationCustomerValidatedEvent → Outbox → RabbitMQ
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  Payment Service     │
│                      │ ⑧ Consume evento, procesa pago
│                      │ ⑨ Persiste pago en DB local
│                      │ ⑩ Publica PaymentCompletedEvent → Outbox → RabbitMQ
└─────────────────────┘
           │
           ▼
┌─────────────────────┐
│  Reservation Service │
│                      │ ⑪ Consume evento, actualiza reserva a PAID
│                      │ ⑫ Publica ReservationPaidEvent → Outbox → RabbitMQ
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  Fleet Service       │
│                      │ ⑬ Consume evento, verifica disponibilidad del vehículo
│                      │ ⑭ Reserva vehículo en DB local
│                      │ ⑮ Publica FleetConfirmedEvent → Outbox → RabbitMQ
└─────────────────────┘
           │
           ▼
┌─────────────────────┐
│  Reservation Service │
│                      │ ⑯ Consume evento, actualiza reserva a CONFIRMED
│                      │     → Estado final: CONFIRMED
│                      │     → GET /reservations/{trackingId} devuelve CONFIRMED
└─────────────────────┘
```

### 1.4 Flujo de Compensación (Unhappy Paths)

**Escenario A: Fallo en validación de cliente**

```
Customer Service → CustomerValidationFailedEvent
    → Reservation Service actualiza estado a CANCELLED
    → No hay compensación adicional (no se ha cobrado ni reservado nada)
```

**Escenario B: Fallo en pago**

```
Payment Service → PaymentFailedEvent
    → Reservation Service actualiza estado a CANCELLED
    → No hay compensación adicional (el cliente estaba validado pero no se reservó vehículo)
```

**Escenario C: Fallo en confirmación de flota (vehículo no disponible)**

```
Fleet Service → FleetConfirmationFailedEvent
    → Reservation Service:
        1. Actualiza estado a CANCELLING
        2. Publica ReservationCancellingEvent (solicita reembolso)
    → Payment Service:
        1. Procesa reembolso
        2. Publica PaymentRefundedEvent
    → Reservation Service:
        1. Actualiza estado a CANCELLED
```

---

## 2. Arquitectura Hexagonal (Puertos y Adaptadores)

### 2.1 Principios Fundamentales

Cada microservicio sigue estrictamente la arquitectura hexagonal. La capa de dominio es el centro del software, completamente independiente de la infraestructura. Las dependencias apuntan **siempre hacia adentro** (hacia el dominio). Se aplica el Principio de Inversión de Dependencias para que los módulos de bajo nivel (base de datos, mensajería) sean plugins intercambiables del dominio.

**Beneficios clave para esta POC:**
- Desarrollar y testear la lógica de dominio sin base de datos ni RabbitMQ reales
- Cambiar de PostgreSQL a otra DB sin tocar la lógica de negocio
- Cambiar de RabbitMQ a otro broker sin afectar el dominio
- Cada capa desplegable y testeable de forma independiente

### 2.2 Estructura de Capas por Servicio

```
reservation-service/
├── domain/                          ← NÚCLEO (sin dependencias externas)
│   ├── model/                       ← Entities, Aggregate Roots, Value Objects
│   │   ├── Reservation.java        ← Aggregate Root
│   │   ├── ReservationItem.java    ← Entity
│   │   ├── Vehicle.java            ← Entity (referencia desde dominio)
│   │   └── vo/                     ← Value Objects
│   │       ├── ReservationId.java
│   │       ├── CustomerId.java
│   │       ├── VehicleId.java
│   │       ├── TrackingId.java
│   │       ├── Money.java
│   │       ├── DateRange.java
│   │       ├── PickupLocation.java
│   │       └── ReservationStatus.java
│   ├── event/                       ← Domain Events
│   │   ├── ReservationCreatedEvent.java
│   │   ├── ReservationPaidEvent.java
│   │   └── ReservationCancelledEvent.java
│   ├── service/                     ← Domain Service
│   │   └── ReservationDomainService.java
│   └── exception/                   ← Domain Exceptions
│       └── ReservationDomainException.java
│
├── application/                     ← ORQUESTACIÓN (casos de uso)
│   ├── port/
│   │   ├── in/                     ← Puertos de Entrada (interfaces)
│   │   │   ├── CreateReservationUseCase.java
│   │   │   ├── TrackReservationUseCase.java
│   │   │   └── PaymentResponseListener.java
│   │   └── out/                    ← Puertos de Salida (interfaces)
│   │       ├── ReservationRepository.java
│   │       ├── CustomerRepository.java
│   │       ├── ReservationMessagePublisher.java
│   │       └── OutboxRepository.java
│   ├── service/                    ← Implementación de casos de uso
│   │   ├── ReservationApplicationService.java
│   │   └── ReservationTrackCommandHandler.java
│   ├── saga/                       ← SAGA Steps
│   │   ├── ReservationPaymentSaga.java
│   │   └── ReservationFleetSaga.java
│   ├── outbox/                     ← Outbox schedulers
│   │   ├── PaymentOutboxScheduler.java
│   │   └── FleetOutboxScheduler.java
│   ├── dto/                        ← Commands y Responses
│   │   ├── CreateReservationCommand.java
│   │   ├── CreateReservationResponse.java
│   │   └── TrackReservationResponse.java
│   └── mapper/
│       └── ReservationDataMapper.java
│
└── infrastructure/                  ← ADAPTADORES (plugins intercambiables)
    ├── adapter/
    │   ├── in/                     ← Adaptadores Primarios
    │   │   └── web/
    │   │       ├── ReservationController.java
    │   │       └── GlobalExceptionHandler.java
    │   └── out/                    ← Adaptadores Secundarios
    │       ├── persistence/
    │       │   ├── ReservationJpaEntity.java
    │       │   ├── ReservationJpaRepository.java
    │       │   ├── ReservationRepositoryAdapter.java
    │       │   └── OutboxJpaRepository.java
    │       └── messaging/
    │           ├── publisher/
    │           │   └── ReservationEventPublisher.java
    │           └── listener/
    │               ├── PaymentResponseRabbitListener.java
    │               └── FleetResponseRabbitListener.java
    └── config/
        ├── RabbitMQConfig.java
        └── BeanConfiguration.java   ← Registro de beans de dominio (sin @Service en dominio)
```

### 2.3 Regla de Dependencias (Inversión)

```
┌─────────────────────────────────────────────────┐
│                INFRASTRUCTURE                    │
│  (Controllers, JPA Entities, RabbitMQ Listeners) │
│                                                  │
│   Depende de ──►  APPLICATION                    │
│                   (Use Cases, DTOs, Mappers)      │
│                                                  │
│                   Depende de ──►  DOMAIN          │
│                                   (Entities, VOs, │
│                                    Domain Events,  │
│                                    Domain Service) │
│                                                    │
│                   DOMAIN NO DEPENDE DE NADA        │
└────────────────────────────────────────────────────┘
```

**Decisión clave**: No usamos anotaciones de Spring (`@Service`, `@Component`) en las clases de dominio. En su lugar, registramos los beans de dominio manualmente en `BeanConfiguration.java` dentro de infrastructure. Esto garantiza que el dominio es 100% independiente del framework.

---

## 3. Domain-Driven Design (DDD)

### 3.1 Conceptos Aplicados

| Concepto | Aplicación en la POC |
|----------|---------------------|
| **Aggregate Root** | `Reservation` — punto de entrada al agregado, garantiza estado consistente |
| **Entity** | Objetos con identidad única: `Reservation`, `ReservationItem`, `Vehicle`, `Customer` |
| **Value Object** | Objetos inmutables sin identidad: `Money`, `DateRange`, `ReservationId`, `PickupLocation` |
| **Domain Event** | Notificaciones entre bounded contexts: `ReservationCreatedEvent`, `ReservationPaidEvent` |
| **Domain Service** | Lógica que abarca múltiples aggregates: `ReservationDomainService` |
| **Application Service** | Orquestación, transacciones, mapeo de datos: `ReservationApplicationService` |
| **Bounded Context** | Cada microservicio = un bounded context con su propio modelo de dominio |

### 3.2 Modelo de Dominio: Reservation Service

**Aggregate: Reservation Processing**

```
┌─────────────────────────────────────────────────┐
│            Reservation Aggregate                 │
│                                                  │
│  ┌──────────────────────────┐                    │
│  │ «Aggregate Root»         │                    │
│  │ Reservation              │                    │
│  │─────────────────────────│                     │
│  │ - reservationId: ReservationId                │
│  │ - customerId: CustomerId                      │
│  │ - trackingId: TrackingId                      │
│  │ - pickupLocation: PickupLocation              │
│  │ - returnLocation: PickupLocation              │
│  │ - dateRange: DateRange                        │
│  │ - totalPrice: Money                           │
│  │ - status: ReservationStatus                   │
│  │ - items: List<ReservationItem>                │
│  │ - failureMessages: List<String>               │
│  │─────────────────────────│                     │
│  │ + validateReservation()                       │
│  │ + initializeReservation()                     │
│  │ + pay()                                       │
│  │ + confirm()                                   │
│  │ + initCancel()                                │
│  │ + cancel()                                    │
│  └──────────┬───────────────┘                    │
│             │ 1..*                               │
│  ┌──────────▼───────────────┐                    │
│  │ «Entity»                 │                    │
│  │ ReservationItem          │                    │
│  │─────────────────────────│                     │
│  │ - itemId: ReservationItemId                   │
│  │ - vehicle: Vehicle                            │
│  │ - days: int                                   │
│  │ - dailyRate: Money                            │
│  │ - subtotal: Money                             │
│  └──────────────────────────┘                    │
│                                                  │
│  ┌──────────────────────────┐                    │
│  │ «Entity»                 │                    │
│  │ Vehicle                  │                    │
│  │─────────────────────────│                     │
│  │ - vehicleId: VehicleId                        │
│  │ - name: String                                │
│  │ - category: VehicleCategory                   │
│  │ - dailyRate: Money                            │
│  └──────────────────────────┘                    │
└─────────────────────────────────────────────────┘
```

**Agregados de soporte (en el dominio del Reservation Service):**

```
┌────────────────────┐       ┌────────────────────────┐
│ «Aggregate Root»   │       │ «Aggregate Root»       │
│ Customer           │       │ Fleet                  │
│───────────────────│        │───────────────────────│ │
│ - customerId       │       │ - fleetId              │
│ - hasValidLicense  │       │ - vehicles: List<Vehicle>│
│ - isActive         │       │ - isActive             │
└────────────────────┘       └────────────────────────┘
```

### 3.3 Value Objects

```java
// Money — inmutable, con lógica de negocio
public record Money(BigDecimal amount) {
    public Money {
        if (amount == null) amount = BigDecimal.ZERO;
        amount = amount.setScale(2, RoundingMode.HALF_EVEN);
    }
    public boolean isGreaterThanZero() { return amount.compareTo(BigDecimal.ZERO) > 0; }
    public boolean isGreaterThan(Money other) { return amount.compareTo(other.amount) > 0; }
    public Money add(Money other) { return new Money(amount.add(other.amount)); }
    public Money subtract(Money other) { return new Money(amount.subtract(other.amount)); }
    public Money multiply(int multiplier) { return new Money(amount.multiply(BigDecimal.valueOf(multiplier))); }
}

// DateRange — rango de fechas de la reserva
public record DateRange(LocalDate pickupDate, LocalDate returnDate) {
    public DateRange {
        if (pickupDate == null || returnDate == null)
            throw new ReservationDomainException("Dates cannot be null");
        if (!returnDate.isAfter(pickupDate))
            throw new ReservationDomainException("Return date must be after pickup date");
    }
    public int totalDays() { return (int) ChronoUnit.DAYS.between(pickupDate, returnDate); }
}

// PickupLocation — ubicación de recogida/devolución
public record PickupLocation(UUID id, String address, String city, String postalCode) {
    public PickupLocation {
        if (address == null || address.isBlank())
            throw new ReservationDomainException("Address cannot be empty");
    }
}

// IDs tipados — contexto semántico
public record ReservationId(UUID value) {}
public record CustomerId(UUID value) {}
public record VehicleId(UUID value) {}
public record TrackingId(UUID value) {}
public record ReservationItemId(Long value) {}
```

### 3.4 Domain Events

```java
// Evento base
public abstract class ReservationDomainEvent {
    private final Reservation reservation;
    private final ZonedDateTime createdAt;
}

// Eventos concretos
public class ReservationCreatedEvent extends ReservationDomainEvent { }  // status = PENDING
public class ReservationPaidEvent extends ReservationDomainEvent { }     // status = PAID
public class ReservationCancelledEvent extends ReservationDomainEvent { } // status = CANCELLED / CANCELLING
```

### 3.5 Estados de la Reserva (Ciclo de Vida)

```
                    ┌──────────┐
        ┌──────────►│ CANCELLED│◄────────────────────────────┐
        │           └──────────┘                              │
        │                ▲                                    │
        │                │ (compensación completada)          │
        │           ┌────┴─────┐                              │
        │           │CANCELLING│◄──────────┐                  │
        │           └──────────┘           │                  │
        │                                  │                  │
   (validación    (pago              (flota no         
    fallida)       fallido)           disponible)       
        │                │                 │                  
   ┌────┴──┐      ┌──────┴─────────┐  ┌───┴──┐       ┌──────────┐
   │PENDING├─────►│CUSTOMER_VALID. ├─►│ PAID ├──────►│CONFIRMED │
   └───────┘      └────────────────┘  └──────┘       └──────────┘
      ①                 ②                ③               ④
```

**Métodos de transición en la Aggregate Root `Reservation`:**

| Método | De → A | Cuándo |
|--------|--------|--------|
| `initializeReservation()` | (nuevo) → PENDING | Al crear la reserva |
| `validateCustomer()` | PENDING → CUSTOMER_VALIDATED | Cliente validado OK |
| `pay()` | CUSTOMER_VALIDATED → PAID | Pago procesado OK |
| `confirm()` | PAID → CONFIRMED | Flota confirma disponibilidad |
| `initCancel()` | PAID → CANCELLING | Flota no disponible, requiere reembolso |
| `cancel()` | PENDING/CUSTOMER_VALIDATED/CANCELLING → CANCELLED | Fallo en cualquier punto o compensación completada |

---

## 4. Patrón SAGA (Orchestration)

### 4.1 Enfoque

Usamos **SAGA Orchestration** donde el Reservation Service actúa como coordinador central. El coordinador inicia la saga enviando el primer evento, recibe respuestas de cada servicio, y decide el siguiente paso o la compensación.

Cada paso del SAGA implementa una interfaz `SagaStep` con dos métodos: `process()` para ejecutar el paso y `rollback()` para compensar en caso de fallo posterior.

### 4.2 SAGA Steps

```java
public interface SagaStep<T> {
    void process(T data);
    void rollback(T data);
}
```

**Step 1 — Customer Validation** (sin compensación necesaria):

```
process():  Publica ReservationCreatedEvent → Customer Service valida
rollback(): No requiere compensación (solo lectura)
```

**Step 2 — Payment**:

```
process():  Publica ReservationCustomerValidatedEvent → Payment Service cobra
rollback(): Publica ReservationCancellingEvent → Payment Service reembolsa
```

**Step 3 — Fleet Confirmation**:

```
process():  Publica ReservationPaidEvent → Fleet Service reserva vehículo
rollback(): Publica FleetCancellationEvent → Fleet Service libera vehículo
```

### 4.3 Tabla resumen de compensaciones

| Paso que falla | Compensación necesaria | Acción |
|----------------|----------------------|--------|
| Customer validation | Ninguna | Cancelar reserva directamente |
| Payment | Ninguna | No se ha reservado vehículo, cancelar reserva |
| Fleet confirmation | Reembolsar pago | Payment Service procesa refund → Reservation se cancela |

---

## 5. Outbox Pattern

### 5.1 El Problema que Resuelve

En el patrón SAGA, cada servicio necesita hacer dos cosas: actualizar su base de datos local Y publicar un evento a RabbitMQ. Estas son dos operaciones distintas que no se pueden meter en una sola transacción ACID (dual write problem).

Si primero haces commit en DB y luego publicas, el publish puede fallar → el sistema queda inconsistente. Si primero publicas y luego haces commit, el commit puede fallar → publicaste un evento sobre algo que no ocurrió. El Outbox Pattern resuelve esto usando una sola transacción ACID.

### 5.2 Cómo Funciona

```
┌──────────────────────────────────────────────────────────────┐
│                    TRANSACCIÓN ACID LOCAL                      │
│                                                                │
│  1. Actualizar tabla de negocio (ej: reservations)            │
│  2. Insertar evento en tabla outbox_events (misma DB)          │
│                                                                │
│  → Ambas operaciones en la MISMA transacción                   │
│  → Si una falla, ambas hacen rollback                          │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                    SCHEDULER (cada 500ms)                      │
│                                                                │
│  1. Lee eventos PENDING de outbox_events                       │
│  2. Publica cada evento a RabbitMQ                             │
│  3. Marca evento como PUBLISHED solo si RabbitMQ confirma      │
│  4. Limpia eventos antiguos ya publicados                      │
│                                                                │
└──────────────────────────────────────────────────────────────┘
```

### 5.3 Tabla Outbox

Cada microservicio tiene su propia tabla `outbox_events` en su base de datos local:

```sql
CREATE TABLE outbox_events (
    id              BIGSERIAL PRIMARY KEY,
    aggregate_type  VARCHAR(50)  NOT NULL,  -- 'RESERVATION', 'PAYMENT'
    aggregate_id    VARCHAR(36)  NOT NULL,  -- UUID del agregado
    event_type      VARCHAR(100) NOT NULL,  -- 'RESERVATION_CREATED'
    payload         TEXT         NOT NULL,  -- JSON del evento
    routing_key     VARCHAR(100) NOT NULL,  -- 'reservation.created'
    exchange        VARCHAR(100) NOT NULL,  -- 'reservation.exchange'
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',  -- PENDING | PUBLISHED | FAILED
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    published_at    TIMESTAMP,
    retry_count     INT          NOT NULL DEFAULT 0,
    saga_id         VARCHAR(36),           -- ID de la SAGA para trazabilidad
    saga_status     VARCHAR(20),           -- Estado de la SAGA en este servicio
    reservation_status VARCHAR(20)          -- Estado de la reserva en este momento
);
```

### 5.4 Garantías del Outbox

- **Atomicidad**: Evento se crea si y solo si la operación de negocio se completa
- **Idempotencia**: Se usan optimistic locks y constraints de BD para evitar duplicados
- **Orden**: Los eventos se leen por `created_at ASC`, manteniendo orden causal
- **Reintento**: Eventos FAILED se reintentan con backoff. Tras N intentos → Dead Letter
- **Limpieza**: Scheduler limpia eventos PUBLISHED con antigüedad > X horas

---

## 6. Mensajería con RabbitMQ

### 6.1 Topología de Exchanges y Queues

```
┌─────────────────────────────────────────────────────────────────────┐
│                         RabbitMQ                                     │
│                                                                      │
│  ┌─────────────────────┐                                            │
│  │ reservation.exchange │ (Topic Exchange)                           │
│  │─────────────────────│                                            │
│  │ reservation.created ─────► customer.validation.request.queue      │
│  │ reservation.customer.validated ──► payment.request.queue          │
│  │ reservation.paid ───────► fleet.confirmation.request.queue        │
│  │ reservation.cancelling ─► payment.refund.request.queue            │
│  └─────────────────────┘                                            │
│                                                                      │
│  ┌─────────────────────┐                                            │
│  │ customer.exchange    │ (Topic Exchange)                           │
│  │─────────────────────│                                            │
│  │ customer.validated ──────► reservation.customer.response.queue    │
│  │ customer.validation.failed ► reservation.customer.response.queue  │
│  └─────────────────────┘                                            │
│                                                                      │
│  ┌─────────────────────┐                                            │
│  │ payment.exchange     │ (Topic Exchange)                           │
│  │─────────────────────│                                            │
│  │ payment.completed ───────► reservation.payment.response.queue     │
│  │ payment.failed ──────────► reservation.payment.response.queue     │
│  │ payment.refunded ────────► reservation.payment.response.queue     │
│  └─────────────────────┘                                            │
│                                                                      │
│  ┌─────────────────────┐                                            │
│  │ fleet.exchange       │ (Topic Exchange)                           │
│  │─────────────────────│                                            │
│  │ fleet.confirmed ─────────► reservation.fleet.response.queue       │
│  │ fleet.confirmation.failed ► reservation.fleet.response.queue      │
│  └─────────────────────┘                                            │
│                                                                      │
│  ┌─────────────────────┐                                            │
│  │ dlx.exchange         │ (Dead Letter Exchange)                     │
│  │─────────────────────│                                            │
│  │ *.dlq ──────────────────► *.dead-letter.queue                    │
│  └─────────────────────┘                                            │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 6.2 Convenciones de Naming

| Elemento | Patrón | Ejemplo |
|----------|--------|---------|
| Exchange | `{servicio}.exchange` | `reservation.exchange` |
| Routing Key | `{servicio}.{acción}` | `payment.completed` |
| Queue | `{servicio-destino}.{contexto}.queue` | `reservation.payment.response.queue` |
| DLQ | `{queue-original}.dlq` | `reservation.payment.response.queue.dlq` |

### 6.3 Estructura de Mensajes

Todos los mensajes incluyen headers de metadata para trazabilidad:

```json
{
  "headers": {
    "X-Event-Type": "RESERVATION_CREATED",
    "X-Source-Service": "reservation-service",
    "X-Correlation-Id": "uuid-saga-id",
    "X-Timestamp": "2025-01-15T10:30:00Z"
  },
  "body": {
    "reservationId": "uuid",
    "customerId": "uuid",
    "totalPrice": 450.00,
    "dateRange": {
      "pickupDate": "2025-02-01",
      "returnDate": "2025-02-04"
    },
    "items": [
      {
        "vehicleId": "uuid",
        "dailyRate": 150.00,
        "days": 3
      }
    ]
  }
}
```

---

## 7. Stack Tecnológico

| Tecnología | Versión | Uso |
|-----------|---------|-----|
| Java | 21 | Virtual Threads habilitados |
| Spring Boot | 3.x (última estable) | Framework base |
| Spring AMQP | (gestionado por Spring Boot) | Integración con RabbitMQ |
| Spring Data JPA | (gestionado por Spring Boot) | Persistencia |
| PostgreSQL | 16+ | Base de datos (una instancia, múltiples schemas) |
| RabbitMQ | 3.13+ | Broker de mensajería |
| Docker Compose | — | Orquestación local de contenedores |
| Flyway | (gestionado por Spring Boot) | Migraciones de base de datos |
| MapStruct | 1.5.x | Mapping entre DTOs y entidades |
| Lombok | (gestionado por Spring Boot) | Reducción de boilerplate |
| Testcontainers | (gestionado por Spring Boot) | Tests de integración |
| JUnit 5 + Mockito | (gestionado por Spring Boot) | Testing |

### 7.1 Estructura Maven Multi-Módulo

```
vehicle-rental-platform/
├── pom.xml                                  ← Parent POM
├── common/                                  ← Módulo compartido
│   └── pom.xml
├── reservation-service/
│   ├── reservation-domain/                  ← Domain + Application
│   │   └── pom.xml
│   ├── reservation-infrastructure/          ← Adaptadores (DB, Messaging, Web)
│   │   └── pom.xml
│   └── reservation-container/               ← Spring Boot main class, une todo
│       └── pom.xml
├── customer-service/
│   ├── customer-domain/
│   ├── customer-infrastructure/
│   └── customer-container/
├── payment-service/
│   ├── payment-domain/
│   ├── payment-infrastructure/
│   └── payment-container/
├── fleet-service/
│   ├── fleet-domain/
│   ├── fleet-infrastructure/
│   └── fleet-container/
└── docker-compose.yml
```

**Módulo `common`** contiene solo: base classes de Entity/AggregateRoot, Value Objects comunes (Money), Domain Event base, excepciones base, DTOs de respuesta estándar (ApiResponse, PageResponse).

**Módulo `*-domain`** contiene: modelo de dominio, application services, puertos (interfaces), SAGA steps, outbox schedulers. Sin dependencias de Spring Framework en la capa de dominio pura.

**Módulo `*-infrastructure`** contiene: JPA entities, repositories, RabbitMQ listeners/publishers, REST controllers, configuración.

**Módulo `*-container`** contiene: clase main de Spring Boot, BeanConfiguration (registro manual de beans de dominio), application.yml.

---

## 8. Base de Datos

### 8.1 Estrategia

Una instancia de PostgreSQL con **schemas separados** por servicio (patrón database-per-service a nivel lógico):

```
PostgreSQL Instance
├── schema: reservation    ← Reservation Service
├── schema: customer       ← Customer Service
├── schema: payment        ← Payment Service
└── schema: fleet          ← Fleet Service
```

Cada schema tiene su propia tabla `outbox_events` y tablas de negocio.

### 8.2 Migraciones

Flyway por servicio, cada uno gestiona sus propias migraciones dentro de su schema.

---

## 9. Docker Compose

```yaml
# Servicios de infraestructura
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: admin
    ports:
      - "5432:5432"
    volumes:
      - ./init-schemas.sql:/docker-entrypoint-initdb.d/init.sql

  rabbitmq:
    image: rabbitmq:3.13-management-alpine
    ports:
      - "5672:5672"    # AMQP
      - "15672:15672"  # Management UI
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest

  # Los 4 microservicios se añaden cuando estén implementados
```

---

## 10. Reglas de Negocio del Dominio

### 10.1 Reservation Service

- El precio total debe coincidir con la suma de (dailyRate × days) de todos los items
- Las fechas de reserva deben ser futuras (pickup > hoy)
- La fecha de devolución debe ser posterior a la de recogida
- Un cliente no puede tener más de 3 reservas activas simultáneamente
- El estado solo puede transicionar según el diagrama de estados definido

### 10.2 Customer Service

- El cliente debe existir y estar activo
- El cliente debe tener una licencia de conducir válida (no expirada)
- Verificación simple: consulta de estado en base de datos local

### 10.3 Payment Service

- El monto del pago debe coincidir con el totalPrice de la reserva
- Simulación de procesamiento de pago (no integración real con pasarela)
- Soporte de reembolso completo como compensación SAGA
- Idempotencia: no procesar el mismo pago dos veces (verificar en outbox)

### 10.4 Fleet Service

- El vehículo debe existir en la flota y estar activo
- El vehículo debe estar disponible para el rango de fechas solicitado (sin solapamiento)
- Al confirmar, el vehículo se marca como reservado para esas fechas
- Al compensar, la reserva del vehículo se libera

---

## 11. Fases de Implementación con OpenSpec

### Fase 1 — Walking Skeleton (Reservation Service + Hexagonal)

Un solo servicio con la arquitectura hexagonal completa. CRUD de reservas con reglas de dominio. PostgreSQL + Docker Compose básico. Sin mensajería aún.

**OpenSpec change:** `openspec/changes/phase-1-reservation-hexagonal/`

### Fase 2 — Customer Service + RabbitMQ + Outbox

Segundo servicio hexagonal. Introducir RabbitMQ para comunicación. Implementar Outbox Pattern en Reservation Service. Flujo: crear reserva → validar cliente → actualizar estado.

**OpenSpec change:** `openspec/changes/phase-2-customer-messaging-outbox/`

### Fase 3 — Payment Service + SAGA Steps

Tercer servicio. Implementar SagaStep interface con process/rollback. Flujo completo con compensación de pago. Outbox en Payment Service.

**OpenSpec change:** `openspec/changes/phase-3-payment-saga/`

### Fase 4 — Fleet Service + SAGA Completo

Cuarto y último servicio. SAGA completo con los 3 steps. Escenarios de fallo end-to-end. Outbox en Fleet Service. Testing de compensaciones.

**OpenSpec change:** `openspec/changes/phase-4-fleet-saga-complete/`

---

## 12. Testing Strategy

### 12.1 Enfoque: Test-First en Dominio, Test-After en Infraestructura

En un sistema con arquitectura hexagonal + SAGA + Outbox, la pirámide de tests clásica (75% unit / 20% integration / 5% E2E) necesita adaptarse. Los bugs más difíciles en esta arquitectura no están en la lógica pura — están en las costuras entre capas: serialización de eventos, transaccionalidad del outbox, idempotencia, orden de mensajes.

**Estrategia por capa:**

- **Dominio → Test-First**: Escribir tests ANTES de implementar. Los escenarios GIVEN/WHEN/THEN de las specs de OpenSpec mapean directamente a tests de dominio. Esto refuerza la comprensión de cada patrón durante el aprendizaje.
- **Application → Test-After con mocks**: Implementar primero, testear después. Los tests verifican orquestación correcta (que se llaman los puertos en el orden correcto).
- **Infrastructure → Test-After con Testcontainers**: Implementar primero, testear después contra infraestructura real. Aquí el valor está en verificar que las costuras funcionan, no en predecir la implementación.

**Integración con OpenSpec (`tasks.md`):**

```markdown
## Phase 1: Domain (Test-First)
- [ ] 1.1 Write unit tests for Reservation aggregate (create, validate, state transitions)
- [ ] 1.2 Implement Reservation aggregate root to pass tests
- [ ] 1.3 Write unit tests for ReservationDomainService
- [ ] 1.4 Implement ReservationDomainService to pass tests

## Phase 2: Application Layer (Test-After)
- [ ] 2.1 Implement CreateReservationCommandHandler
- [ ] 2.2 Write unit tests for CommandHandler (mock ports)

## Phase 3: Infrastructure (Test-After with Testcontainers)
- [ ] 3.1 Implement JPA adapter
- [ ] 3.2 Implement Outbox persistence
- [ ] 3.3 Write integration tests for persistence + outbox atomicity
- [ ] 3.4 Implement RabbitMQ publisher adapter
- [ ] 3.5 Write integration tests for messaging flow
```

### 12.2 Pirámide de Tests Adaptada

```
              /\
             /  \        SAGA Flow Tests (15%)
            /    \       Flujo SAGA multi-step con DB + RabbitMQ reales
           /──────\
          /        \     Integration Tests por Adaptador (30%)
         /          \    JPA, Outbox atomicidad, RabbitMQ listeners
        /────────────\
       /              \  Application Service Tests (15%)
      /                \ Orquestación con mocks de puertos
     /──────────────────\
    /                    \ Domain Tests (40%)
   /                      \ Aggregate Root, entities, VOs, domain service
  /────────────────────────\
```

### 12.3 Capa 1 — Domain Tests (~40%, Test-First)

**Qué se testea**: Lógica de negocio pura de la Aggregate Root, entities, value objects y domain service. Sin Spring, sin mocks de infraestructura.

**Por qué Test-First**: Los escenarios de las specs OpenSpec se traducen directamente a tests. Escribirlos primero obliga a pensar en el comportamiento esperado antes de codificar, reforzando el aprendizaje de cada patrón.

**Herramientas**: JUnit 5, AssertJ. Sin Mockito (no hay dependencias que mockear).

**Convenciones**: Archivo `*Test.java`, estructura given-when-then, tests anidados con `@Nested`.

**Ejemplo — Reservation Aggregate Root:**

```java
@DisplayName("Reservation Aggregate Root")
class ReservationTest {

    @Nested
    @DisplayName("when creating a new reservation")
    class WhenCreatingReservation {

        @Test
        @DisplayName("should initialize with PENDING status")
        void shouldInitializeWithPendingStatus() {
            // given
            Reservation reservation = buildValidReservation();
            // when
            reservation.validateReservation();
            reservation.initializeReservation();
            // then
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
            assertThat(reservation.getTrackingId()).isNotNull();
        }

        @Test
        @DisplayName("should reject when total price doesn't match item calculations")
        void shouldRejectWhenPriceMismatch() {
            // given
            Reservation reservation = buildReservationWithWrongPrice();
            // when/then
            assertThatThrownBy(() -> reservation.validateReservation())
                .isInstanceOf(ReservationDomainException.class)
                .hasMessageContaining("Total price");
        }

        @Test
        @DisplayName("should reject when pickup date is in the past")
        void shouldRejectWhenPickupDateInPast() {
            // given/when/then
            assertThatThrownBy(() -> new DateRange(LocalDate.now().minusDays(1), LocalDate.now().plusDays(3)))
                .isInstanceOf(ReservationDomainException.class);
        }
    }

    @Nested
    @DisplayName("state transitions")
    class StateTransitions {

        @Test
        @DisplayName("should transition PENDING → CUSTOMER_VALIDATED")
        void shouldTransitionToCustomerValidated() {
            // given
            Reservation reservation = buildPendingReservation();
            // when
            reservation.validateCustomer();
            // then
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CUSTOMER_VALIDATED);
        }

        @Test
        @DisplayName("should reject pay() when status is not CUSTOMER_VALIDATED")
        void shouldRejectPayWhenNotCustomerValidated() {
            // given
            Reservation reservation = buildPendingReservation();
            // when/then
            assertThatThrownBy(() -> reservation.pay())
                .isInstanceOf(ReservationDomainException.class);
        }

        @Test
        @DisplayName("should transition PAID → CANCELLING when fleet unavailable")
        void shouldTransitionToCancellingWhenFleetUnavailable() {
            // given
            Reservation reservation = buildPaidReservation();
            // when
            reservation.initCancel(List.of("Vehicle not available for requested dates"));
            // then
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLING);
            assertThat(reservation.getFailureMessages()).contains("Vehicle not available for requested dates");
        }
    }
}
```

**Ejemplo — Value Object Money:**

```java
class MoneyTest {

    @Test
    @DisplayName("should calculate correct subtotal")
    void shouldCalculateSubtotal() {
        Money dailyRate = new Money(new BigDecimal("50.00"));
        Money result = dailyRate.multiply(3);
        assertThat(result).isEqualTo(new Money(new BigDecimal("150.00")));
    }

    @Test
    @DisplayName("should be immutable - add returns new instance")
    void shouldBeImmutable() {
        Money a = new Money(new BigDecimal("100.00"));
        Money b = new Money(new BigDecimal("50.00"));
        Money sum = a.add(b);
        assertThat(sum).isNotSameAs(a);
        assertThat(a.getAmount()).isEqualByComparingTo("100.00");
    }
}
```

### 12.4 Capa 2 — Application Service Tests (~15%, Test-After)

**Qué se testea**: Orquestación del command handler — que llame al domain service, luego al repository, luego al outbox, en el orden correcto.

**Por qué Test-After**: Estos tests verifican cableado, no lógica. Escribirlos antes de implementar aporta poco valor porque estás prediciendo el orden de llamadas a mocks.

**Herramientas**: JUnit 5, Mockito (BDD style), AssertJ.

**Convenciones**: Archivo `*Test.java`. Se mockean los puertos de salida (repositories, publishers). NO se mockea el domain service ni las entities.

**Qué mockear vs qué no:**

| Mockear | NO mockear |
|---------|------------|
| `ReservationRepository` (puerto de salida) | `Reservation` (aggregate root) |
| `ReservationMessagePublisher` (puerto de salida) | `ReservationDomainService` (lógica real) |
| `OutboxRepository` (puerto de salida) | Value Objects (`Money`, `DateRange`) |
| `CustomerRepository` (puerto de salida) | Domain Events |

**Ejemplo:**

```java
@ExtendWith(MockitoExtension.class)
class ReservationApplicationServiceTest {

    @InjectMocks
    private ReservationApplicationService service;

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private OutboxRepository outboxRepository;
    @Spy
    private ReservationDomainService domainService = new ReservationDomainServiceImpl();
    @Spy
    private ReservationDataMapper mapper = new ReservationDataMapper();

    @Test
    @DisplayName("should create reservation and save outbox event")
    void shouldCreateReservationAndSaveOutboxEvent() {
        // given
        CreateReservationCommand command = buildValidCommand();
        given(customerRepository.findById(any())).willReturn(Optional.of(buildActiveCustomer()));
        given(reservationRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        CreateReservationResponse response = service.createReservation(command);

        // then
        assertThat(response.status()).isEqualTo(ReservationStatus.PENDING);
        then(reservationRepository).should().save(any(Reservation.class));
        then(outboxRepository).should().save(any(OutboxEvent.class));
    }
}
```

### 12.5 Capa 3 — Integration Tests por Adaptador (~30%, Test-After)

**Qué se testea**: Que los adaptadores funcionan contra infraestructura real. El mayor valor está en verificar las costuras: serialización JPA, transaccionalidad del Outbox, deserialización de mensajes RabbitMQ, routing de exchanges.

**Por qué Test-After con Testcontainers**: Mockear la base de datos elimina el valor del test. El Outbox Pattern solo tiene sentido testearlo con una transacción ACID real. RabbitMQ solo valida bindings y routing con un broker real.

**Herramientas**: JUnit 5, Testcontainers (`@ServiceConnection`), `@DataJpaTest` (slice tests JPA), `@SpringBootTest` (tests de messaging).

**Convenciones**: Archivo `*IT.java`. Base class compartida con `@Testcontainers`.

**Ejemplo — Outbox atomicidad (el test más crítico de la POC):**

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class OutboxAtomicityIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ReservationApplicationService service;
    @Autowired
    private ReservationJpaRepository reservationRepo;
    @Autowired
    private OutboxJpaRepository outboxRepo;

    @Test
    @DisplayName("should persist reservation AND outbox event in same transaction")
    void shouldPersistBothAtomically() {
        // given
        CreateReservationCommand command = buildValidCommand();

        // when
        CreateReservationResponse response = service.createReservation(command);

        // then — ambos existen en BD
        assertThat(reservationRepo.findByTrackingId(response.trackingId())).isPresent();
        assertThat(outboxRepo.findByAggregateId(response.reservationId().toString()))
            .isPresent()
            .hasValueSatisfying(event -> {
                assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
                assertThat(event.getEventType()).isEqualTo("RESERVATION_CREATED");
            });
    }

    @Test
    @DisplayName("should rollback both reservation AND outbox on domain validation failure")
    void shouldRollbackBothOnFailure() {
        // given
        CreateReservationCommand invalidCommand = buildCommandWithInvalidPrice();

        // when/then
        assertThatThrownBy(() -> service.createReservation(invalidCommand))
            .isInstanceOf(ReservationDomainException.class);

        // then — ninguno existe en BD
        assertThat(reservationRepo.count()).isZero();
        assertThat(outboxRepo.count()).isZero();
    }
}
```

**Ejemplo — RabbitMQ listener:**

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class PaymentResponseListenerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management");

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ReservationJpaRepository reservationRepo;

    @Test
    @DisplayName("should update reservation to PAID when payment completed event received")
    void shouldUpdateToPaidOnPaymentCompleted() {
        // given — reservation en estado CUSTOMER_VALIDATED en BD
        ReservationJpaEntity reservation = persistReservationInState(CUSTOMER_VALIDATED);
        PaymentCompletedEvent event = new PaymentCompletedEvent(reservation.getId(), /*...*/);

        // when
        rabbitTemplate.convertAndSend("payment.exchange", "payment.completed", event);

        // then — esperar procesamiento asíncrono
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            ReservationJpaEntity updated = reservationRepo.findById(reservation.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(ReservationStatus.PAID);
        });
    }
}
```

### 12.6 Capa 4 — SAGA Flow Tests (~15%, Test-After)

**Qué se testea**: Flujo completo de un SAGA step, incluyendo compensaciones. Simula el ida y vuelta: publicar evento → listener lo procesa → verifica estado en DB → verifica evento de respuesta en outbox.

**Por qué es necesario**: Los tests de capas 1-3 verifican cada pieza aislada. Los SAGA Flow Tests verifican que las piezas encajan correctamente en un flujo multi-step.

**Herramientas**: JUnit 5, Testcontainers (PostgreSQL + RabbitMQ), Awaitility.

**Convenciones**: Archivo `*SagaIT.java`. No requieren los 4 servicios corriendo — testean un servicio completo con DB y RabbitMQ reales.

**Ejemplo — Compensación de pago cuando flota no disponible:**

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class ReservationFleetSagaIT {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @Container @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management");

    @Autowired private RabbitTemplate rabbitTemplate;
    @Autowired private ReservationJpaRepository reservationRepo;
    @Autowired private OutboxJpaRepository outboxRepo;

    @Test
    @DisplayName("should initiate payment rollback when fleet confirmation fails")
    void shouldInitiatePaymentRollbackOnFleetFailure() {
        // given — reservation en estado PAID
        ReservationJpaEntity reservation = persistReservationInState(PAID);

        FleetConfirmationFailedEvent event = new FleetConfirmationFailedEvent(
            reservation.getId(), List.of("Vehicle not available")
        );

        // when — Fleet Service notifica fallo
        rabbitTemplate.convertAndSend("fleet.exchange", "fleet.confirmation.failed", event);

        // then — Reservation pasa a CANCELLING y genera evento de rollback en outbox
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            ReservationJpaEntity updated = reservationRepo.findById(reservation.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(ReservationStatus.CANCELLING);

            List<OutboxEventEntity> outboxEvents = outboxRepo.findByAggregateId(reservation.getId().toString());
            assertThat(outboxEvents)
                .extracting(OutboxEventEntity::getEventType)
                .contains("RESERVATION_CANCELLING");  // → solicita reembolso a Payment
        });
    }
}
```

### 12.7 Configuración Maven (Surefire + Failsafe)

```xml
<!-- Unit tests (*Test.java) → mvn test -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <includes><include>**/*Test.java</include></includes>
        <excludes><exclude>**/*IT.java</exclude></excludes>
    </configuration>
</plugin>

<!-- Integration tests (*IT.java) → mvn verify -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <configuration>
        <includes><include>**/*IT.java</include></includes>
    </configuration>
</plugin>
```

### 12.8 Resumen de Convenciones

| Convención | Valor |
|------------|-------|
| Tests unitarios | `*Test.java` |
| Tests integración | `*IT.java` |
| Tests SAGA flow | `*SagaIT.java` |
| Estructura de test | given-when-then con comentarios |
| Assertions | AssertJ (fluent API) |
| Mocks | Mockito BDD (`given`/`then`) |
| Organización | `@Nested` classes por escenario |
| DisplayName | Siempre presente, en inglés, descriptivo |
| Base de datos test | Testcontainers PostgreSQL (`@ServiceConnection`) |
| Mensajería test | Testcontainers RabbitMQ (`@ServiceConnection`) |
| Async assertions | Awaitility para tests con listeners |
| Profile | `@ActiveProfiles("test")` en todos los ITs |

---

## 13. Fuentes de Referencia

- **Curso Udemy**: Transcripciones filtradas de secciones 1-3, 5-9 (adaptadas de Kafka a RabbitMQ, de food ordering a vehicle rental)
- **Best Practices MDs**: 16 documentos de buenas prácticas enterprise Java/Spring Boot (especialmente relevantes: 01-Estructura, 02-Organización Paquetes, 05-Convenciones, 07-Maven, 08-REST APIs, 13-RabbitMQ Patterns)
- **OpenSpec Framework**: https://openspec.dev — herramienta SDD para gestionar specs como documentación viva en el repositorio
