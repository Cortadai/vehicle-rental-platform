# Área 18: SAGA Orchestration Pattern — Transacciones Distribuidas

> **Audiencia**: Desarrolladores junior/mid (guía detallada) + Seniors (referencia rápida)
> **Stack**: Spring Boot 3.4.x, Java 21+, RabbitMQ 3.13+, PostgreSQL
> **Prerrequisito**: Haber leído el Área 13 (Mensajería RabbitMQ + Outbox Pattern)

---

## 1. Fundamentos del Patrón SAGA

### Referencia Rápida (Seniors)

| Concepto | Descripción |
|----------|-------------|
| Problema | Transacciones ACID no existen entre microservicios |
| Solución | Secuencia de transacciones locales + compensaciones si algo falla |
| Orchestration | Un orquestador central coordina los steps |
| Choreography | Cada servicio escucha eventos y decide el siguiente paso |
| Outbox | La SAGA NUNCA publica a RabbitMQ directamente — siempre vía Outbox |

### Guía Detallada (Junior/Mid)

#### El Problema: Transacciones Distribuidas

En un monolito, una operación como "crear pedido" se ejecuta en una sola transacción de base de datos:

```
BEGIN TRANSACTION
  1. Validar cliente     → tabla clientes
  2. Cobrar pago         → tabla pagos
  3. Reservar flota      → tabla flota
  4. Crear pedido        → tabla pedidos
COMMIT  ← todo o nada
```

En microservicios, cada paso vive en un servicio diferente con su propia base de datos. No puedes hacer un `COMMIT` que abarque 4 bases de datos distintas. Si el paso 3 (flota) falla después de que el paso 2 (pago) ya hizo commit, tienes dinero cobrado y ningún servicio asignado.

**El patrón SAGA resuelve esto**: ejecuta una secuencia de transacciones locales. Si un paso falla, ejecuta **compensaciones** en orden inverso para deshacer los pasos anteriores.

#### Orchestration vs Choreography

```
ORCHESTRATION                          CHOREOGRAPHY

┌─────────────┐                        ┌──────┐  evento  ┌──────┐  evento  ┌──────┐
│ Orchestrator │                        │ Svc A │────────▶│ Svc B │────────▶│ Svc C │
│   (central)  │                        └──────┘         └──────┘         └──────┘
└──────┬───────┘                           ▲                                  │
       │                                   └──────────── compensación ────────┘
       ├──▶ Step 1 (Svc A)
       ├──▶ Step 2 (Svc B)                Cada servicio decide qué hacer
       └──▶ Step 3 (Svc C)                al recibir un evento.

El orquestador decide                     No hay coordinador central.
el siguiente paso.
```

| Aspecto | Orchestration | Choreography |
|---------|--------------|--------------|
| Coordinación | Centralizada (un orquestador) | Descentralizada (cada servicio decide) |
| Visibilidad del flujo | Alta — el flujo está en un solo lugar | Baja — el flujo está distribuido en N servicios |
| Acoplamiento | Orquestador conoce todos los pasos | Servicios se conocen entre sí vía eventos |
| Complejidad con +5 steps | Manejable | Exponencial (event spaghetti) |
| Debugging | Fácil — sagaId centralizado | Difícil — hay que rastrear eventos entre servicios |
| Punto de fallo único | Sí — el orquestador | No |
| Mejor para | Flujos complejos, +3 steps, compensaciones | Flujos simples, 2-3 steps, bajo acoplamiento |

**¿Por qué elegimos Orchestration?** Para flujos enterprise con 3+ pasos, compensaciones complejas y necesidad de auditoría, el orquestador centralizado es más mantenible. El flujo completo está en una sola clase, no distribuido en N listeners.

#### Relación con el Outbox Pattern

La SAGA **NUNCA** publica eventos directamente a RabbitMQ. Cada step de la SAGA guarda su evento en la tabla `outbox_events` (documentada en el Área 13). El `OutboxPublisher` scheduler se encarga de la publicación real.

```
SAGA Step                    Outbox                      RabbitMQ
    │                          │                            │
    ├── process(data) ────────▶│ INSERT outbox_event        │
    │   (misma tx DB)          │ status=PENDING             │
    │                          │                            │
    │                          ├── Scheduler (500ms) ──────▶│ publish
    │                          │   status=PUBLISHED         │
```

**Por qué**: Si la SAGA publicara directamente a RabbitMQ, tendríamos el problema de dual-write: la DB se actualiza pero el mensaje no se envía (o viceversa). Con el Outbox, la persistencia del evento y la actualización del estado de la SAGA están en la misma transacción de base de datos.

---

## 2. Diseño de la SAGA

### Referencia Rápida (Seniors)

```
SagaStep<T>                    → Interfaz: process() + rollback()
SagaStatus (enum)              → STARTED, PROCESSING, COMPENSATING, SUCCEEDED, FAILED
SagaState (entity)             → Tracking en DB: sagaId, currentStep, status, payload
OrderSagaOrchestrator          → Coordina steps basándose en respuestas
```

**Steps del ejemplo**:
```
Step 1: CustomerValidation  → Valida que el cliente existe y tiene crédito
Step 2: Payment             → Cobra el monto al cliente
Step 3: FleetConfirmation   → Asigna un vehículo/servicio
```

### Guía Detallada (Junior/Mid)

### 2.1. Interfaz SagaStep

✅ **Hacer**: Definir una interfaz genérica que encapsule proceso y compensación

```java
package com.acme.order.application.saga;

/**
 * Representa un paso individual dentro de una SAGA.
 * Cada step tiene dos operaciones:
 *   - process: ejecuta la acción del paso (vía Outbox)
 *   - rollback: compensa/deshace la acción (vía Outbox)
 *
 * @param <T> Tipo de datos que necesita el step para ejecutarse
 */
public interface SagaStep<T> {

    /**
     * Ejecuta la acción principal del paso.
     * NO publica a RabbitMQ directamente — guarda en el Outbox.
     */
    void process(T data);

    /**
     * Compensa/deshace la acción realizada por process().
     * Se invoca cuando un paso posterior falla.
     */
    void rollback(T data);

    /**
     * Nombre del step para logging y tracking.
     */
    String getName();
}
```

### 2.2. Máquina de Estados de la SAGA

✅ **Hacer**: Modelar los estados con un enum y transiciones válidas

```java
package com.acme.order.application.saga;

/**
 * Estados posibles de una SAGA.
 *
 * Flujo normal:    STARTED → PROCESSING → SUCCEEDED
 * Flujo con fallo: STARTED → PROCESSING → COMPENSATING → FAILED
 */
public enum SagaStatus {

    STARTED,        // SAGA creada, ningún step ejecutado aún
    PROCESSING,     // Al menos un step en ejecución
    COMPENSATING,   // Un step falló, ejecutando compensaciones
    SUCCEEDED,      // Todos los steps completados exitosamente
    FAILED;         // Compensaciones completadas (o SAGA terminada con error)

    /**
     * Valida si la transición de estado es válida.
     */
    public boolean canTransitionTo(SagaStatus target) {
        return switch (this) {
            case STARTED      -> target == PROCESSING;
            case PROCESSING   -> target == COMPENSATING || target == SUCCEEDED;
            case COMPENSATING -> target == FAILED;
            case SUCCEEDED, FAILED -> false;  // Estados terminales
        };
    }
}
```

**Diagrama de transiciones**:
```
             ┌──────────┐
             │ STARTED  │
             └────┬─────┘
                  │ primer step inicia
                  ▼
             ┌──────────┐
             │PROCESSING│
             └──┬────┬──┘
    todos OK    │    │  un step falla
                ▼    ▼
         ┌─────────┐  ┌─────────────┐
         │SUCCEEDED│  │COMPENSATING │
         └─────────┘  └──────┬──────┘
                             │ compensaciones completas
                             ▼
                       ┌──────────┐
                       │  FAILED  │
                       └──────────┘
```

### 2.3. SagaState — Entidad de Tracking

✅ **Hacer**: Persistir el estado de cada SAGA en la base de datos para tracking y recuperación

```java
package com.acme.order.application.saga;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Persiste el estado de una SAGA individual.
 * Permite consultar, debuggear y recuperar SAGAs en cualquier momento.
 *
 * @Version para optimistic locking — previene procesamiento concurrente
 * del mismo step por mensajes duplicados.
 */
@Entity
@Table(name = "saga_state")
public class SagaState {

    @Id
    @Column(name = "saga_id", updatable = false)
    private UUID sagaId;

    @Column(name = "saga_type", nullable = false)
    private String sagaType;    // "ORDER_CREATION"

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SagaStatus status;

    @Column(name = "current_step", nullable = false)
    private int currentStep;    // Índice del step actual (0-based)

    @Column(name = "total_steps", nullable = false)
    private int totalSteps;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;     // JSON con los datos del flujo

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;       // Optimistic locking

    protected SagaState() {}

    public static SagaState create(String sagaType, int totalSteps, String payload) {
        var state = new SagaState();
        state.sagaId = UUID.randomUUID();
        state.sagaType = sagaType;
        state.status = SagaStatus.STARTED;
        state.currentStep = 0;
        state.totalSteps = totalSteps;
        state.payload = payload;
        state.createdAt = Instant.now();
        state.updatedAt = Instant.now();
        return state;
    }

    public void advanceToNextStep() {
        this.currentStep++;
        this.updatedAt = Instant.now();
        if (this.status == SagaStatus.STARTED) {
            this.status = SagaStatus.PROCESSING;
        }
    }

    public void markAsSucceeded() {
        this.status = SagaStatus.SUCCEEDED;
        this.updatedAt = Instant.now();
    }

    public void startCompensation(String reason) {
        this.status = SagaStatus.COMPENSATING;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public void markAsFailed() {
        this.status = SagaStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    public void decrementStep() {
        this.currentStep--;
        this.updatedAt = Instant.now();
    }

    // Getters
    public UUID getSagaId() { return sagaId; }
    public String getSagaType() { return sagaType; }
    public SagaStatus getStatus() { return status; }
    public int getCurrentStep() { return currentStep; }
    public int getTotalSteps() { return totalSteps; }
    public String getPayload() { return payload; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
```

**SQL de la tabla**:
```sql
CREATE TABLE saga_state (
    saga_id        UUID PRIMARY KEY,
    saga_type      VARCHAR(100) NOT NULL,
    status         VARCHAR(20) NOT NULL,
    current_step   INT NOT NULL DEFAULT 0,
    total_steps    INT NOT NULL,
    payload        TEXT NOT NULL,
    failure_reason TEXT,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version        BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_saga_state_status ON saga_state(status);
CREATE INDEX idx_saga_state_type_status ON saga_state(saga_type, status);
CREATE INDEX idx_saga_state_updated ON saga_state(updated_at);
```

### 2.4. SagaState Repository

```java
package com.acme.order.application.saga;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SagaStateRepository extends JpaRepository<SagaState, UUID> {

    List<SagaState> findByStatus(SagaStatus status);

    List<SagaState> findBySagaTypeAndStatus(String sagaType, SagaStatus status);

    /**
     * Encuentra SAGAs "stuck" — en estado PROCESSING o COMPENSATING
     * sin actualización durante más de N minutos.
     */
    @Query("SELECT s FROM SagaState s WHERE s.status IN ('PROCESSING', 'COMPENSATING') " +
           "AND s.updatedAt < :threshold")
    List<SagaState> findStuckSagas(Instant threshold);

    /**
     * Cuenta SAGAs por estado para métricas.
     */
    @Query("SELECT s.status, COUNT(s) FROM SagaState s " +
           "WHERE s.sagaType = :sagaType GROUP BY s.status")
    List<Object[]> countBySagaTypeGroupByStatus(String sagaType);
}
```

---

## 3. Implementación Detallada

### Referencia Rápida (Seniors)

```
OrderSagaOrchestrator
├── start(CreateOrderCommand)     → crea SagaState + ejecuta step 0
├── handleStepResult(sagaId, stepName, success, data)
│   ├── success=true  → advanceToNextStep() o markAsSucceeded()
│   └── success=false → startCompensation() + rollback steps
└── Cada step usa el Outbox para comunicarse con otros servicios
```

### Guía Detallada (Junior/Mid)

### 3.1. Steps Concretos

Cada step sabe cómo ejecutar su acción y cómo compensarla. Interactúa con el Outbox, nunca directamente con RabbitMQ.

```java
package com.acme.order.application.saga.step;

import com.acme.order.application.saga.SagaStep;
import com.acme.order.application.saga.dto.OrderSagaData;
import com.acme.order.infrastructure.outbox.OutboxEvent;
import com.acme.order.infrastructure.outbox.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;

/**
 * Step 1: Valida que el cliente existe, está activo, y tiene crédito.
 * Publica un comando al Customer Service vía Outbox.
 */
public class CustomerValidationStep implements SagaStep<OrderSagaData> {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public CustomerValidationStep(OutboxEventRepository outboxRepository,
                                  ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(OrderSagaData data) {
        var payload = serializePayload(new ValidateCustomerCommand(
                data.sagaId().toString(),
                data.customerId(),
                data.totalAmount()
        ));

        outboxRepository.save(OutboxEvent.builder()
                .aggregateType("SAGA")
                .aggregateId(data.sagaId().toString())
                .eventType("VALIDATE_CUSTOMER_COMMAND")
                .payload(payload)
                .routingKey("saga.customer.validate")
                .exchange("saga.exchange")
                .status(OutboxEvent.OutboxStatus.PENDING)
                .createdAt(Instant.now())
                .retryCount(0)
                .build());
    }

    @Override
    public void rollback(OrderSagaData data) {
        // Step 1 es validación, no modifica estado → no requiere compensación real.
        // Pero registramos el evento para trazabilidad.
        var payload = serializePayload(new CustomerValidationRollbackCommand(
                data.sagaId().toString(),
                data.customerId()
        ));

        outboxRepository.save(OutboxEvent.builder()
                .aggregateType("SAGA")
                .aggregateId(data.sagaId().toString())
                .eventType("CUSTOMER_VALIDATION_ROLLBACK")
                .payload(payload)
                .routingKey("saga.customer.rollback")
                .exchange("saga.exchange")
                .status(OutboxEvent.OutboxStatus.PENDING)
                .createdAt(Instant.now())
                .retryCount(0)
                .build());
    }

    @Override
    public String getName() {
        return "CUSTOMER_VALIDATION";
    }

    private String serializePayload(Object command) {
        try {
            return objectMapper.writeValueAsString(command);
        } catch (Exception e) {
            throw new RuntimeException("Error serializando comando SAGA", e);
        }
    }

    // Commands internos
    record ValidateCustomerCommand(String sagaId, String customerId, java.math.BigDecimal amount) {}
    record CustomerValidationRollbackCommand(String sagaId, String customerId) {}
}
```

```java
package com.acme.order.application.saga.step;

import com.acme.order.application.saga.SagaStep;
import com.acme.order.application.saga.dto.OrderSagaData;
import com.acme.order.infrastructure.outbox.OutboxEvent;
import com.acme.order.infrastructure.outbox.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;

/**
 * Step 2: Cobra el pago al cliente.
 * La compensación emite un reembolso.
 */
public class PaymentStep implements SagaStep<OrderSagaData> {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public PaymentStep(OutboxEventRepository outboxRepository,
                       ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(OrderSagaData data) {
        var payload = serializePayload(new ProcessPaymentCommand(
                data.sagaId().toString(),
                data.customerId(),
                data.orderId(),
                data.totalAmount(),
                data.currency()
        ));

        outboxRepository.save(OutboxEvent.builder()
                .aggregateType("SAGA")
                .aggregateId(data.sagaId().toString())
                .eventType("PROCESS_PAYMENT_COMMAND")
                .payload(payload)
                .routingKey("saga.payment.process")
                .exchange("saga.exchange")
                .status(OutboxEvent.OutboxStatus.PENDING)
                .createdAt(Instant.now())
                .retryCount(0)
                .build());
    }

    @Override
    public void rollback(OrderSagaData data) {
        // Compensación: reembolsar el pago
        var payload = serializePayload(new RefundPaymentCommand(
                data.sagaId().toString(),
                data.customerId(),
                data.orderId(),
                data.totalAmount(),
                data.currency(),
                "SAGA_COMPENSATION"
        ));

        outboxRepository.save(OutboxEvent.builder()
                .aggregateType("SAGA")
                .aggregateId(data.sagaId().toString())
                .eventType("REFUND_PAYMENT_COMMAND")
                .payload(payload)
                .routingKey("saga.payment.refund")
                .exchange("saga.exchange")
                .status(OutboxEvent.OutboxStatus.PENDING)
                .createdAt(Instant.now())
                .retryCount(0)
                .build());
    }

    @Override
    public String getName() {
        return "PAYMENT";
    }

    private String serializePayload(Object command) {
        try {
            return objectMapper.writeValueAsString(command);
        } catch (Exception e) {
            throw new RuntimeException("Error serializando comando SAGA", e);
        }
    }

    record ProcessPaymentCommand(String sagaId, String customerId, String orderId,
                                 java.math.BigDecimal amount, String currency) {}
    record RefundPaymentCommand(String sagaId, String customerId, String orderId,
                                java.math.BigDecimal amount, String currency, String reason) {}
}
```

```java
package com.acme.order.application.saga.step;

import com.acme.order.application.saga.SagaStep;
import com.acme.order.application.saga.dto.OrderSagaData;
import com.acme.order.infrastructure.outbox.OutboxEvent;
import com.acme.order.infrastructure.outbox.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;

/**
 * Step 3: Confirma la asignación de flota/vehículo.
 * La compensación libera la reserva de flota.
 */
public class FleetConfirmationStep implements SagaStep<OrderSagaData> {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public FleetConfirmationStep(OutboxEventRepository outboxRepository,
                                 ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(OrderSagaData data) {
        var payload = serializePayload(new ConfirmFleetCommand(
                data.sagaId().toString(),
                data.orderId(),
                data.shippingAddress(),
                data.requestedDate()
        ));

        outboxRepository.save(OutboxEvent.builder()
                .aggregateType("SAGA")
                .aggregateId(data.sagaId().toString())
                .eventType("CONFIRM_FLEET_COMMAND")
                .payload(payload)
                .routingKey("saga.fleet.confirm")
                .exchange("saga.exchange")
                .status(OutboxEvent.OutboxStatus.PENDING)
                .createdAt(Instant.now())
                .retryCount(0)
                .build());
    }

    @Override
    public void rollback(OrderSagaData data) {
        var payload = serializePayload(new ReleaseFleetCommand(
                data.sagaId().toString(),
                data.orderId(),
                "SAGA_COMPENSATION"
        ));

        outboxRepository.save(OutboxEvent.builder()
                .aggregateType("SAGA")
                .aggregateId(data.sagaId().toString())
                .eventType("RELEASE_FLEET_COMMAND")
                .payload(payload)
                .routingKey("saga.fleet.release")
                .exchange("saga.exchange")
                .status(OutboxEvent.OutboxStatus.PENDING)
                .createdAt(Instant.now())
                .retryCount(0)
                .build());
    }

    @Override
    public String getName() {
        return "FLEET_CONFIRMATION";
    }

    private String serializePayload(Object command) {
        try {
            return objectMapper.writeValueAsString(command);
        } catch (Exception e) {
            throw new RuntimeException("Error serializando comando SAGA", e);
        }
    }

    record ConfirmFleetCommand(String sagaId, String orderId,
                               String address, String requestedDate) {}
    record ReleaseFleetCommand(String sagaId, String orderId, String reason) {}
}
```

### 3.2. DTO de Datos de la SAGA

```java
package com.acme.order.application.saga.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Datos que fluyen a través de todos los steps de la SAGA.
 * Inmutable (record). Se serializa como JSON en SagaState.payload.
 */
public record OrderSagaData(
        UUID sagaId,
        String orderId,
        String customerId,
        BigDecimal totalAmount,
        String currency,
        String shippingAddress,
        String requestedDate
) {
    public static OrderSagaData create(String orderId, String customerId,
                                       BigDecimal totalAmount, String currency,
                                       String shippingAddress, String requestedDate) {
        return new OrderSagaData(
                UUID.randomUUID(), orderId, customerId,
                totalAmount, currency, shippingAddress, requestedDate
        );
    }

    public OrderSagaData withSagaId(UUID newSagaId) {
        return new OrderSagaData(
                newSagaId, orderId, customerId,
                totalAmount, currency, shippingAddress, requestedDate
        );
    }
}
```

### 3.3. Resultado de un Step

```java
package com.acme.order.application.saga.dto;

import java.util.UUID;

/**
 * Respuesta recibida de un servicio externo para un step de SAGA.
 * Cada servicio publica este resultado cuando termina de procesar un comando.
 */
public record SagaStepResult(
        UUID sagaId,
        String stepName,
        boolean success,
        String message,
        String data         // JSON opcional con datos adicionales
) {}
```

### 3.4. Coordinador SAGA (Orchestrator)

✅ **Hacer**: El orquestador coordina los steps. Solo tiene lógica de coordinación, NO de negocio.

```java
package com.acme.order.application.saga;

import com.acme.order.application.saga.dto.OrderSagaData;
import com.acme.order.application.saga.dto.SagaStepResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Orquestador de la SAGA de creación de pedido.
 *
 * Responsabilidades:
 *  1. Iniciar la SAGA (crear SagaState + ejecutar primer step)
 *  2. Recibir resultados de steps y decidir el siguiente paso
 *  3. Ejecutar compensaciones en cascada si un step falla
 *
 * NO contiene lógica de negocio — solo coordinación.
 * Registrado como bean en BeanConfiguration (sin @Service).
 */
public class OrderSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestrator.class);
    private static final String SAGA_TYPE = "ORDER_CREATION";

    private final List<SagaStep<OrderSagaData>> steps;
    private final SagaStateRepository sagaStateRepository;
    private final ObjectMapper objectMapper;

    public OrderSagaOrchestrator(List<SagaStep<OrderSagaData>> steps,
                                 SagaStateRepository sagaStateRepository,
                                 ObjectMapper objectMapper) {
        this.steps = steps;
        this.sagaStateRepository = sagaStateRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Inicia una nueva SAGA. Crea el SagaState y ejecuta el primer step.
     *
     * @return sagaId para tracking
     */
    @Transactional
    public UUID start(OrderSagaData data) {
        log.info("Iniciando SAGA para orderId={}", data.orderId());

        var sagaState = SagaState.create(
                SAGA_TYPE,
                steps.size(),
                serialize(data)
        );
        sagaStateRepository.save(sagaState);

        var sagaData = data.withSagaId(sagaState.getSagaId());
        updatePayload(sagaState, sagaData);

        // Ejecutar primer step
        executeCurrentStep(sagaState, sagaData);

        log.info("SAGA iniciada: sagaId={}, primer step={}",
                sagaState.getSagaId(), steps.get(0).getName());

        return sagaState.getSagaId();
    }

    /**
     * Recibe el resultado de un step y decide la siguiente acción.
     * Llamado por el Application Service cuando recibe una respuesta de RabbitMQ.
     */
    @Transactional
    public void handleStepResult(SagaStepResult result) {
        log.info("SAGA resultado recibido: sagaId={}, step={}, success={}",
                result.sagaId(), result.stepName(), result.success());

        var sagaState = sagaStateRepository.findById(result.sagaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "SAGA no encontrada: " + result.sagaId()));

        var sagaData = deserialize(sagaState.getPayload());

        // Usar pattern matching de Java 21 para decidir la acción
        switch (sagaState.getStatus()) {
            case PROCESSING, STARTED -> {
                if (result.success()) {
                    handleSuccessfulStep(sagaState, sagaData);
                } else {
                    handleFailedStep(sagaState, sagaData, result.message());
                }
            }
            case COMPENSATING -> {
                handleCompensationStepCompleted(sagaState, sagaData);
            }
            case SUCCEEDED, FAILED -> {
                log.warn("SAGA {} en estado terminal {}, ignorando resultado",
                        sagaState.getSagaId(), sagaState.getStatus());
            }
        }
    }

    /**
     * Step exitoso → avanzar al siguiente o marcar como completada.
     */
    private void handleSuccessfulStep(SagaState sagaState, OrderSagaData data) {
        sagaState.advanceToNextStep();

        if (sagaState.getCurrentStep() >= sagaState.getTotalSteps()) {
            // Todos los steps completados
            sagaState.markAsSucceeded();
            sagaStateRepository.save(sagaState);
            log.info("SAGA completada exitosamente: sagaId={}", sagaState.getSagaId());
        } else {
            // Ejecutar siguiente step
            sagaStateRepository.save(sagaState);
            executeCurrentStep(sagaState, data);
            log.info("SAGA avanzando al step {}/{}: sagaId={}, step={}",
                    sagaState.getCurrentStep() + 1, sagaState.getTotalSteps(),
                    sagaState.getSagaId(), steps.get(sagaState.getCurrentStep()).getName());
        }
    }

    /**
     * Step fallido → iniciar compensación de los steps anteriores (en orden inverso).
     */
    private void handleFailedStep(SagaState sagaState, OrderSagaData data, String reason) {
        log.warn("SAGA step falló: sagaId={}, step={}, razón={}",
                sagaState.getSagaId(),
                steps.get(sagaState.getCurrentStep()).getName(),
                reason);

        sagaState.startCompensation(reason);

        // Compensar desde el step anterior al que falló (el que falló no se ejecutó exitosamente)
        int stepToCompensate = sagaState.getCurrentStep() - 1;
        sagaState.decrementStep();  // Apuntar al step que hay que compensar

        if (stepToCompensate >= 0) {
            sagaStateRepository.save(sagaState);
            rollbackCurrentStep(sagaState, data);
        } else {
            // No hay steps que compensar (falló el primero)
            sagaState.markAsFailed();
            sagaStateRepository.save(sagaState);
            log.info("SAGA fallida (sin compensaciones): sagaId={}", sagaState.getSagaId());
        }
    }

    /**
     * Compensación de un step completada → continuar compensando el anterior.
     */
    private void handleCompensationStepCompleted(SagaState sagaState, OrderSagaData data) {
        log.info("Compensación completada para step: sagaId={}, step={}",
                sagaState.getSagaId(),
                steps.get(sagaState.getCurrentStep()).getName());

        sagaState.decrementStep();

        if (sagaState.getCurrentStep() >= 0) {
            // Compensar el step anterior
            sagaStateRepository.save(sagaState);
            rollbackCurrentStep(sagaState, data);
        } else {
            // Todas las compensaciones completadas
            sagaState.markAsFailed();
            sagaStateRepository.save(sagaState);
            log.info("SAGA compensaciones completas: sagaId={}", sagaState.getSagaId());
        }
    }

    // --- Métodos auxiliares ---

    private void executeCurrentStep(SagaState sagaState, OrderSagaData data) {
        var step = steps.get(sagaState.getCurrentStep());
        step.process(data);
    }

    private void rollbackCurrentStep(SagaState sagaState, OrderSagaData data) {
        var step = steps.get(sagaState.getCurrentStep());
        step.rollback(data);
    }

    private void updatePayload(SagaState sagaState, OrderSagaData data) {
        sagaState = sagaStateRepository.findById(sagaState.getSagaId()).orElseThrow();
        // Actualizar payload en la misma tx
    }

    private String serialize(OrderSagaData data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new RuntimeException("Error serializando SAGA data", e);
        }
    }

    private OrderSagaData deserialize(String json) {
        try {
            return objectMapper.readValue(json, OrderSagaData.class);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializando SAGA data", e);
        }
    }
}
```

### 3.5. Listeners que Alimentan al Coordinador

✅ **Hacer**: Los listeners de RabbitMQ reciben respuestas de servicios externos y delegan al orquestador

```java
package com.acme.order.infrastructure.adapter.input.messaging;

import com.acme.order.application.saga.OrderSagaOrchestrator;
import com.acme.order.application.saga.dto.SagaStepResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener que recibe respuestas de los servicios participantes
 * y las delega al orquestador de la SAGA.
 *
 * Cada servicio (Customer, Payment, Fleet) publica su resultado
 * en una queue dedicada de respuesta.
 */
@Component
public class SagaResponseListener {

    private static final Logger log = LoggerFactory.getLogger(SagaResponseListener.class);

    private final OrderSagaOrchestrator orchestrator;

    public SagaResponseListener(OrderSagaOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @RabbitListener(queues = "${saga.queues.customer-validation-response}")
    public void onCustomerValidationResponse(SagaStepResult result) {
        log.info("Customer validation response: sagaId={}, success={}",
                result.sagaId(), result.success());
        orchestrator.handleStepResult(result);
    }

    @RabbitListener(queues = "${saga.queues.payment-response}")
    public void onPaymentResponse(SagaStepResult result) {
        log.info("Payment response: sagaId={}, success={}",
                result.sagaId(), result.success());
        orchestrator.handleStepResult(result);
    }

    @RabbitListener(queues = "${saga.queues.fleet-response}")
    public void onFleetConfirmationResponse(SagaStepResult result) {
        log.info("Fleet confirmation response: sagaId={}, success={}",
                result.sagaId(), result.success());
        orchestrator.handleStepResult(result);
    }
}
```

### 3.6. Registro de Beans

```java
package com.acme.order.config;

import com.acme.order.application.saga.OrderSagaOrchestrator;
import com.acme.order.application.saga.SagaStateRepository;
import com.acme.order.application.saga.SagaStep;
import com.acme.order.application.saga.dto.OrderSagaData;
import com.acme.order.application.saga.step.CustomerValidationStep;
import com.acme.order.application.saga.step.FleetConfirmationStep;
import com.acme.order.application.saga.step.PaymentStep;
import com.acme.order.infrastructure.outbox.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SagaBeanConfiguration {

    @Bean
    public CustomerValidationStep customerValidationStep(
            OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        return new CustomerValidationStep(outboxRepository, objectMapper);
    }

    @Bean
    public PaymentStep paymentStep(
            OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        return new PaymentStep(outboxRepository, objectMapper);
    }

    @Bean
    public FleetConfirmationStep fleetConfirmationStep(
            OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        return new FleetConfirmationStep(outboxRepository, objectMapper);
    }

    /**
     * El orden de los steps en la lista define el orden de ejecución
     * y el orden inverso de compensación.
     */
    @Bean
    public OrderSagaOrchestrator orderSagaOrchestrator(
            CustomerValidationStep customerValidationStep,
            PaymentStep paymentStep,
            FleetConfirmationStep fleetConfirmationStep,
            SagaStateRepository sagaStateRepository,
            ObjectMapper objectMapper) {

        List<SagaStep<OrderSagaData>> steps = List.of(
                customerValidationStep,     // Step 0
                paymentStep,                // Step 1
                fleetConfirmationStep       // Step 2
        );

        return new OrderSagaOrchestrator(steps, sagaStateRepository, objectMapper);
    }
}
```

---

## 4. Manejo de Compensaciones

### Referencia Rápida (Seniors)

```
Fallo en Step 3 (Fleet):
  → Compensar Step 2 (Payment): emitir reembolso
  → Compensar Step 1 (Customer): liberar crédito reservado
  → Compensar Step 0 (si aplica)
  → Marcar SAGA como FAILED

El orden de compensación es INVERSO al de ejecución.
```

### Guía Detallada (Junior/Mid)

### 4.1. Happy Path (Todo Exitoso)

```
┌───────────┐
│  Cliente   │  POST /api/v1/orders
└─────┬─────┘
      │
      ▼
┌───────────┐  ① start()  ┌───────────────────┐
│  Order    │────────────▶│ SagaOrchestrator  │
│  Service  │             └────────┬──────────┘
└───────────┘                      │
                                   │ process()
                    ┌──────────────┼──────────────┐
                    ▼              ▼              ▼
              ┌──────────┐  ┌──────────┐  ┌──────────┐
              │Step 1    │  │Step 2    │  │Step 3    │
              │Customer  │  │Payment   │  │Fleet     │
              │Validation│  │          │  │Confirm   │
              └────┬─────┘  └────┬─────┘  └────┬─────┘
                   │             │             │
                   ▼             ▼             ▼
              ┌──────────┐  ┌──────────┐  ┌──────────┐
              │ Outbox   │  │ Outbox   │  │ Outbox   │
              │→ Customer│  │→ Payment │  │→ Fleet   │
              │  Service │  │  Service │  │  Service │
              └────┬─────┘  └────┬─────┘  └────┬─────┘
                   │ ✅          │ ✅          │ ✅
                   └─────────────┴─────────────┘
                              │
                              ▼
                    SAGA Status = SUCCEEDED
```

### 4.2. Compensation Path (Step 3 Falla)

```
Flujo ejecutado:
  Step 1 ✅ Customer validado
  Step 2 ✅ Pago cobrado ($150.00)
  Step 3 ❌ Fleet no disponible → FALLA

Compensaciones (orden inverso):
  Rollback Step 2: Reembolsar $150.00 al cliente
  Rollback Step 1: Liberar reserva de crédito (si aplica)

SAGA Status = FAILED
```

```
┌───────────────────┐
│ SagaOrchestrator  │◀── Step 3 FALLÓ: "No hay flota disponible"
└────────┬──────────┘
         │
         │ startCompensation("No hay flota disponible")
         │
         ├──▶ Step 2.rollback() ──▶ Outbox: REFUND_PAYMENT_COMMAND
         │                          │
         │    ◀── Payment refunded ─┘
         │
         ├──▶ Step 1.rollback() ──▶ Outbox: CUSTOMER_VALIDATION_ROLLBACK
         │                          │
         │    ◀── Rollback done ───┘
         │
         └──▶ markAsFailed()
              SAGA Status = FAILED
              failureReason = "No hay flota disponible"
```

### 4.3. Compensaciones Parciales

No todos los steps requieren compensación real. Una **validación** (Step 1) puede no tener side effects que deshacer. La compensación sigue siendo definida para mantener la simetría del patrón, pero puede ser un no-op.

```java
// Step que NO necesita compensación real
@Override
public void rollback(OrderSagaData data) {
    // No-op: la validación no modifica estado
    // Pero registramos para trazabilidad
    log.info("Rollback customer validation (no-op): sagaId={}", data.sagaId());
}

// Step que SÍ necesita compensación real
@Override
public void rollback(OrderSagaData data) {
    // Emitir comando de reembolso — esto SÍ tiene efecto
    outboxRepository.save(OutboxEvent.builder()
            .eventType("REFUND_PAYMENT_COMMAND")
            // ...
            .build());
}
```

---

## 5. Idempotencia

### Referencia Rápida (Seniors)

```
1. Verificar en saga_state si el step ya fue procesado (sagaId + stepName)
2. Usar @Version (optimistic locking) para prevenir procesamiento concurrente
3. Constraint UNIQUE en outbox para evitar duplicar comandos del mismo step
```

### Guía Detallada (Junior/Mid)

### 5.1. Por Qué la Idempotencia es Crítica en SAGA

En una SAGA, un mensaje de respuesta puede llegar duplicado porque:
- RabbitMQ reentrega el mensaje tras un timeout de ack
- El servicio externo reintenta la publicación de su respuesta
- El Outbox publica el mismo evento dos veces por un race condition

Si no manejas duplicados, podrías:
- Avanzar dos pasos en vez de uno
- Ejecutar una compensación dos veces (doble reembolso)
- Crear SAGAs duplicadas

### 5.2. Estrategia: Optimistic Locking + Verificación de Estado

```java
// En OrderSagaOrchestrator.handleStepResult()
@Transactional
public void handleStepResult(SagaStepResult result) {
    var sagaState = sagaStateRepository.findById(result.sagaId())
            .orElseThrow(() -> new IllegalArgumentException(
                    "SAGA no encontrada: " + result.sagaId()));

    // IDEMPOTENCIA: verificar que el step corresponde al step actual
    var expectedStep = steps.get(sagaState.getCurrentStep());
    if (!expectedStep.getName().equals(result.stepName())) {
        log.warn("Step duplicado o desordenado ignorado: sagaId={}, " +
                 "esperado={}, recibido={}",
                result.sagaId(), expectedStep.getName(), result.stepName());
        return;  // Ignorar — ya fue procesado
    }

    // IDEMPOTENCIA: verificar que la SAGA está en un estado que acepta resultados
    if (sagaState.getStatus() == SagaStatus.SUCCEEDED
            || sagaState.getStatus() == SagaStatus.FAILED) {
        log.warn("SAGA en estado terminal, ignorando: sagaId={}, status={}",
                sagaState.getSagaId(), sagaState.getStatus());
        return;
    }

    // Procesar resultado...
    // @Version en SagaState garantiza que si dos mensajes llegan
    // al mismo tiempo, el segundo obtendrá OptimisticLockException
}
```

### 5.3. Constraint de Base de Datos para Outbox

```sql
-- Prevenir que un step de SAGA genere duplicados en el Outbox
CREATE UNIQUE INDEX idx_outbox_saga_step ON outbox_events(aggregate_id, event_type)
    WHERE aggregate_type = 'SAGA' AND status = 'PENDING';
```

Esto garantiza que si el mismo step se ejecuta dos veces (por un retry), el segundo `INSERT` fallará con un constraint violation, que puedes manejar silenciosamente.

### 5.4. Manejo del OptimisticLockException

```java
package com.acme.order.infrastructure.adapter.input.messaging;

import com.acme.order.application.saga.OrderSagaOrchestrator;
import com.acme.order.application.saga.dto.SagaStepResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Component
public class SagaResultHandler {

    private static final Logger log = LoggerFactory.getLogger(SagaResultHandler.class);

    private final OrderSagaOrchestrator orchestrator;

    public SagaResultHandler(OrderSagaOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Maneja el resultado de un step con protección de idempotencia.
     * Si el optimistic lock falla, significa que otro thread/instancia
     * ya procesó este resultado → se ignora silenciosamente.
     */
    public void handleResult(SagaStepResult result) {
        try {
            orchestrator.handleStepResult(result);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.info("Mensaje duplicado detectado (optimistic lock): sagaId={}, step={}",
                    result.sagaId(), result.stepName());
            // Ignorar — otro thread ya procesó este resultado
        }
    }
}
```

---

## 6. Escenarios de Fallo Detallados

### Referencia Rápida (Seniors)

| Step que falla | Steps exitosos previos | Compensaciones requeridas | Estado final |
|---------------|----------------------|--------------------------|-------------|
| Step 1 (Customer) | Ninguno | Ninguna | FAILED |
| Step 2 (Payment) | Step 1 ✅ | Rollback Step 1 | FAILED |
| Step 3 (Fleet) | Step 1 ✅, Step 2 ✅ | Rollback Step 2, Rollback Step 1 | FAILED |
| Ninguno (todo OK) | Step 1 ✅, Step 2 ✅, Step 3 ✅ | Ninguna | SUCCEEDED |

### Guía Detallada (Junior/Mid)

### 6.1. Tabla Completa de Escenarios

| # | Escenario | Qué falla | Estado previo | Compensaciones | Estado final |
|---|-----------|-----------|---------------|----------------|-------------|
| 1 | Happy path | Nada | — | Ninguna | SUCCEEDED |
| 2 | Customer inválido | Step 1 process | STARTED | Ninguna (no hay pasos previos) | FAILED |
| 3 | Pago rechazado | Step 2 process | PROCESSING | Rollback Step 1 | FAILED |
| 4 | Flota no disponible | Step 3 process | PROCESSING | Rollback Step 2 → Rollback Step 1 | FAILED |
| 5 | Timeout Customer | Step 1 no responde | PROCESSING | Detectado por stuck scheduler | FAILED |
| 6 | Timeout Payment | Step 2 no responde | PROCESSING | Detectado por stuck scheduler → Rollback Step 1 | FAILED |
| 7 | Timeout Fleet | Step 3 no responde | PROCESSING | Detectado por stuck scheduler → Rollback Step 2, 1 | FAILED |
| 8 | Fallo en compensación Payment | Step 2 rollback | COMPENSATING | Retry + DLQ si persiste | COMPENSATING (stuck) |
| 9 | Mensaje duplicado | Step ya procesado | PROCESSING | Ignorado (idempotencia) | Sin cambio |
| 10 | SAGA duplicada | Outbox duplicate | STARTED | Constraint violation → ignorado | Sin cambio |

### 6.2. Timeout Handling — Detección de SAGAs "Stuck"

✅ **Hacer**: Implementar un scheduler que detecta SAGAs que llevan demasiado tiempo en PROCESSING o COMPENSATING

```java
package com.acme.order.infrastructure.scheduler;

import com.acme.order.application.saga.OrderSagaOrchestrator;
import com.acme.order.application.saga.SagaState;
import com.acme.order.application.saga.SagaStateRepository;
import com.acme.order.application.saga.dto.SagaStepResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Detecta SAGAs que llevan demasiado tiempo sin actualización.
 * Esto puede pasar cuando un servicio no responde o el mensaje se pierde.
 *
 * Estrategia: si una SAGA está en PROCESSING/COMPENSATING sin
 * actualización por más de 5 minutos, se considera "stuck".
 */
@Component
public class StuckSagaDetector {

    private static final Logger log = LoggerFactory.getLogger(StuckSagaDetector.class);
    private static final int STUCK_THRESHOLD_MINUTES = 5;

    private final SagaStateRepository sagaStateRepository;
    private final OrderSagaOrchestrator orchestrator;

    public StuckSagaDetector(SagaStateRepository sagaStateRepository,
                             OrderSagaOrchestrator orchestrator) {
        this.sagaStateRepository = sagaStateRepository;
        this.orchestrator = orchestrator;
    }

    @Scheduled(fixedDelay = 60_000)  // Cada minuto
    public void detectStuckSagas() {
        Instant threshold = Instant.now().minus(STUCK_THRESHOLD_MINUTES, ChronoUnit.MINUTES);
        List<SagaState> stuckSagas = sagaStateRepository.findStuckSagas(threshold);

        if (stuckSagas.isEmpty()) {
            return;
        }

        log.warn("Detectadas {} SAGAs stuck (sin actualización por > {} min)",
                stuckSagas.size(), STUCK_THRESHOLD_MINUTES);

        for (SagaState saga : stuckSagas) {
            handleStuckSaga(saga);
        }
    }

    private void handleStuckSaga(SagaState saga) {
        log.warn("SAGA stuck detectada: sagaId={}, type={}, status={}, " +
                 "step={}/{}, lastUpdate={}",
                saga.getSagaId(), saga.getSagaType(), saga.getStatus(),
                saga.getCurrentStep(), saga.getTotalSteps(), saga.getUpdatedAt());

        // Tratar como fallo del step actual → iniciar compensación
        var failureResult = new SagaStepResult(
                saga.getSagaId(),
                "STUCK_DETECTION",
                false,
                "SAGA stuck — sin respuesta por " + STUCK_THRESHOLD_MINUTES + " minutos",
                null
        );

        try {
            orchestrator.handleStepResult(failureResult);
        } catch (Exception e) {
            log.error("Error manejando SAGA stuck: sagaId={}", saga.getSagaId(), e);
        }
    }
}
```

### 6.3. Fallo Durante la Compensación

Cuando un rollback falla, el Outbox reintentará la publicación del comando de compensación. Si después de N reintentos sigue fallando, el evento va a la DLQ.

```
Compensación falla
       │
       ▼
┌──────────────┐     ┌─────────┐     ┌──────┐
│ Outbox retry │────▶│ RabbitMQ│────▶│ Svc  │ ← Service caído
│ (hasta 5x)   │     │         │     │      │
└──────┬───────┘     └─────────┘     └──────┘
       │ fallo persistente
       ▼
┌──────────────┐
│ Outbox FAILED│ ← Requiere intervención manual
└──────────────┘
```

**Acción recomendada**: alertar al equipo de operaciones. Las compensaciones fallidas requieren intervención manual (procesar el reembolso manualmente, verificar el estado en el servicio externo, etc.).

---

## 7. Errores Comunes y Anti-patterns

### 7.1. Publicar Eventos Directamente (sin Outbox)

❌ **Anti-pattern**: El step publica directamente a RabbitMQ

```java
// MAL — dual-write: si la DB falla después del publish, hay inconsistencia
public class PaymentStep implements SagaStep<OrderSagaData> {

    private final RabbitTemplate rabbitTemplate;  // ❌ Dependencia directa

    @Override
    public void process(OrderSagaData data) {
        rabbitTemplate.convertAndSend("saga.exchange",
                "saga.payment.process", data);   // ❌ Publicación directa
    }
}
```

✅ **Correcto**: Guardar en el Outbox (misma transacción DB)

```java
// BIEN — el step guarda en el Outbox, el scheduler publica
public class PaymentStep implements SagaStep<OrderSagaData> {

    private final OutboxEventRepository outboxRepository;  // ✅ Outbox

    @Override
    public void process(OrderSagaData data) {
        outboxRepository.save(OutboxEvent.builder()
                .eventType("PROCESS_PAYMENT_COMMAND")
                .routingKey("saga.payment.process")
                // ...
                .build());  // ✅ Persistido en misma tx
    }
}
```

### 7.2. No Implementar Idempotencia

❌ **Anti-pattern**: Procesar cada mensaje sin verificar duplicados

```java
// MAL — si llega el mismo resultado dos veces, avanza dos steps
@RabbitListener(queues = "saga.payment.response")
public void onPaymentResponse(SagaStepResult result) {
    orchestrator.handleStepResult(result);  // ❌ Sin verificación de duplicado
}
```

✅ **Correcto**: Verificar estado actual + optimistic locking

```java
// BIEN — verificar que el step corresponde al esperado
public void handleStepResult(SagaStepResult result) {
    var saga = sagaStateRepository.findById(result.sagaId()).orElseThrow();

    // Verificar que el resultado corresponde al step actual
    if (!steps.get(saga.getCurrentStep()).getName().equals(result.stepName())) {
        log.warn("Resultado duplicado/desordenado ignorado");
        return;  // ✅ Idempotente
    }
    // ... procesar
}
```

### 7.3. Compensaciones No Inversas

❌ **Anti-pattern**: La compensación no deshace exactamente la operación original

```java
// MAL — el process cobra $150, pero el rollback reembolsa $100
@Override
public void process(OrderSagaData data) {
    // Cobra $150 al cliente
    outboxRepository.save(/* PROCESS_PAYMENT: amount = data.totalAmount() */);
}

@Override
public void rollback(OrderSagaData data) {
    // Reembolsa solo $100 (¿por qué?)
    outboxRepository.save(/* REFUND_PAYMENT: amount = 100 */);  // ❌
}
```

✅ **Correcto**: La compensación es la operación inversa exacta

```java
// BIEN — reembolsa exactamente el monto cobrado
@Override
public void rollback(OrderSagaData data) {
    outboxRepository.save(/* REFUND_PAYMENT: amount = data.totalAmount() */);  // ✅
}
```

### 7.4. Steps con Efectos No Compensables

❌ **Anti-pattern**: Un step envía un email o llama a una API externa irreversible

```java
// MAL — ¿cómo "descancelamos" un email ya enviado?
public class NotificationStep implements SagaStep<OrderSagaData> {

    @Override
    public void process(OrderSagaData data) {
        emailService.sendOrderConfirmation(data);  // ❌ Irreversible
    }

    @Override
    public void rollback(OrderSagaData data) {
        // ¿Enviar "disculpe, ignore el email anterior"? 😬
    }
}
```

✅ **Correcto**: Los efectos no compensables van DESPUÉS de que la SAGA complete exitosamente

```java
// BIEN — enviar email solo después de que TODA la SAGA termine OK
@TransactionalEventListener(phase = AFTER_COMMIT)
public void onSagaSucceeded(SagaCompletedEvent event) {
    if (event.status() == SagaStatus.SUCCEEDED) {
        emailService.sendOrderConfirmation(event.data());  // ✅ Solo si todo fue OK
    }
}
```

### 7.5. No Trackear el Estado de la SAGA

❌ **Anti-pattern**: No persistir el estado de la SAGA

```java
// MAL — el estado de la SAGA vive solo en memoria
public class OrderSagaOrchestrator {
    private final Map<UUID, Integer> currentSteps = new ConcurrentHashMap<>();  // ❌

    // Si el servicio se reinicia, se pierde todo el estado
    // Imposible saber qué SAGAs están activas en producción
}
```

✅ **Correcto**: Persistir en tabla `saga_state` con todos los campos necesarios

```java
// BIEN — estado persistido en DB
public class OrderSagaOrchestrator {
    private final SagaStateRepository sagaStateRepository;  // ✅ Persistencia

    // Consultable, auditable, recuperable ante reinicios
}
```

### 7.6. Orchestrator con Lógica de Negocio

❌ **Anti-pattern**: Poner validaciones o cálculos en el orquestador

```java
// MAL — el orquestador decide lógica de negocio
public void handleStepResult(SagaStepResult result) {
    if (result.success()) {
        // Calcular descuento basado en el tipo de cliente ❌
        BigDecimal discount = calculateDiscount(sagaData.customerId());
        sagaData = sagaData.withDiscount(discount);  // ❌ Lógica de negocio
    }
}
```

✅ **Correcto**: El orquestador SOLO coordina. La lógica de negocio vive en los servicios participantes.

```java
// BIEN — solo coordinación
public void handleStepResult(SagaStepResult result) {
    if (result.success()) {
        handleSuccessfulStep(sagaState, sagaData);  // ✅ Avanzar o completar
    } else {
        handleFailedStep(sagaState, sagaData, result.message());  // ✅ Compensar
    }
}
```

---

## 8. Monitorización y Operaciones

### Referencia Rápida (Seniors)

```sql
-- SAGAs activas
SELECT * FROM saga_state WHERE status IN ('PROCESSING', 'COMPENSATING');

-- SAGAs stuck (sin actualización por > 5 min)
SELECT * FROM saga_state WHERE status IN ('PROCESSING', 'COMPENSATING')
  AND updated_at < NOW() - INTERVAL '5 MINUTES';

-- Métricas por estado
SELECT status, COUNT(*) FROM saga_state GROUP BY status;
```

### Guía Detallada (Junior/Mid)

### 8.1. Consulta del Estado de SAGAs Activas

```java
package com.acme.order.infrastructure.adapter.input.rest;

import com.acme.order.application.saga.SagaState;
import com.acme.order.application.saga.SagaStateRepository;
import com.acme.order.application.saga.SagaStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Endpoint de monitorización para consultar el estado de las SAGAs.
 * Para uso interno/operaciones, no exponer a clientes externos.
 */
@RestController
@RequestMapping("/internal/sagas")
public class SagaMonitorController {

    private final SagaStateRepository sagaStateRepository;

    public SagaMonitorController(SagaStateRepository sagaStateRepository) {
        this.sagaStateRepository = sagaStateRepository;
    }

    @GetMapping
    public ResponseEntity<List<SagaState>> listByStatus(
            @RequestParam(defaultValue = "PROCESSING") SagaStatus status) {
        return ResponseEntity.ok(sagaStateRepository.findByStatus(status));
    }

    @GetMapping("/{sagaId}")
    public ResponseEntity<SagaState> getById(@PathVariable UUID sagaId) {
        return sagaStateRepository.findById(sagaId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/metrics")
    public ResponseEntity<List<Object[]>> metrics(
            @RequestParam(defaultValue = "ORDER_CREATION") String sagaType) {
        return ResponseEntity.ok(
                sagaStateRepository.countBySagaTypeGroupByStatus(sagaType));
    }
}
```

### 8.2. Logs Estructurados con saga_id como Correlation ID

✅ **Hacer**: Incluir el `sagaId` en todos los logs del flujo SAGA

```java
// En cada método del Orchestrator y de los Steps
import org.slf4j.MDC;

public void handleStepResult(SagaStepResult result) {
    MDC.put("sagaId", result.sagaId().toString());
    try {
        log.info("Procesando resultado: step={}, success={}",
                result.stepName(), result.success());
        // ... lógica
    } finally {
        MDC.remove("sagaId");
    }
}
```

**logback-spring.xml** (patrón con sagaId):
```xml
<pattern>%d{ISO8601} [%thread] %-5level %logger{36} sagaId=%X{sagaId} - %msg%n</pattern>
```

**Ejemplo de output**:
```
2025-01-15T14:30:00 [main] INFO  OrderSagaOrchestrator sagaId=a1b2c3d4 - Procesando resultado: step=PAYMENT, success=true
2025-01-15T14:30:00 [main] INFO  OrderSagaOrchestrator sagaId=a1b2c3d4 - SAGA avanzando al step 3/3: step=FLEET_CONFIRMATION
2025-01-15T14:30:05 [main] WARN  OrderSagaOrchestrator sagaId=a1b2c3d4 - SAGA step falló: step=FLEET_CONFIRMATION, razón=No hay flota disponible
2025-01-15T14:30:05 [main] INFO  OrderSagaOrchestrator sagaId=a1b2c3d4 - Compensación iniciada, rollback Step 2
```

### 8.3. Métricas Recomendadas

| Métrica | Tipo | Descripción |
|---------|------|-------------|
| `saga.started.total` | Counter | Total de SAGAs iniciadas |
| `saga.succeeded.total` | Counter | Total de SAGAs completadas exitosamente |
| `saga.failed.total` | Counter | Total de SAGAs que terminaron en FAILED |
| `saga.compensated.total` | Counter | Total de compensaciones ejecutadas |
| `saga.stuck.current` | Gauge | Cantidad de SAGAs stuck en este momento |
| `saga.duration.seconds` | Histogram | Tiempo total de ejecución de la SAGA |
| `saga.step.duration.seconds` | Histogram | Tiempo de ejecución por step |

```java
package com.acme.order.application.saga;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.springframework.stereotype.Component;

@Component
public class SagaMetrics {

    private final Counter sagaStarted;
    private final Counter sagaSucceeded;
    private final Counter sagaFailed;
    private final Counter sagaCompensated;
    private final Timer sagaDuration;

    public SagaMetrics(MeterRegistry registry) {
        this.sagaStarted = Counter.builder("saga.started.total")
                .tag("type", "ORDER_CREATION")
                .description("Total SAGAs iniciadas")
                .register(registry);

        this.sagaSucceeded = Counter.builder("saga.succeeded.total")
                .tag("type", "ORDER_CREATION")
                .description("Total SAGAs exitosas")
                .register(registry);

        this.sagaFailed = Counter.builder("saga.failed.total")
                .tag("type", "ORDER_CREATION")
                .description("Total SAGAs fallidas")
                .register(registry);

        this.sagaCompensated = Counter.builder("saga.compensated.total")
                .tag("type", "ORDER_CREATION")
                .description("Total compensaciones ejecutadas")
                .register(registry);

        this.sagaDuration = Timer.builder("saga.duration.seconds")
                .tag("type", "ORDER_CREATION")
                .description("Duración total de la SAGA")
                .register(registry);
    }

    public void recordStarted() { sagaStarted.increment(); }
    public void recordSucceeded() { sagaSucceeded.increment(); }
    public void recordFailed() { sagaFailed.increment(); }
    public void recordCompensation() { sagaCompensated.increment(); }
    public Timer.Sample startTimer() { return Timer.start(); }
    public void stopTimer(Timer.Sample sample) { sample.stop(sagaDuration); }
}
```

---

## 9. Checklist de Implementación

### Diseño

- [ ] Definir los steps de la SAGA con process() y rollback() para cada uno
- [ ] Cada compensación es exactamente la inversa de su operación
- [ ] Efectos no compensables (emails, notificaciones) van DESPUÉS de SAGA SUCCEEDED
- [ ] Máquina de estados definida con transiciones válidas

### Base de Datos

- [ ] Tabla `saga_state` creada con todos los campos (sagaId, status, currentStep, payload, version)
- [ ] Índices en `status` y `updated_at` para queries de monitorización y stuck detection
- [ ] `@Version` (optimistic locking) en `SagaState` para prevenir procesamiento concurrente
- [ ] Constraint UNIQUE en outbox para evitar comandos duplicados por step

### Implementación

- [ ] `SagaStep<T>` interfaz genérica con `process()`, `rollback()`, `getName()`
- [ ] Steps concretos que interactúan con el Outbox, NO con RabbitMQ directamente
- [ ] `OrderSagaOrchestrator` con `start()` y `handleStepResult()`
- [ ] Orquestador sin lógica de negocio — solo coordinación
- [ ] Steps registrados como beans en `SagaBeanConfiguration`
- [ ] Orden de steps en la lista = orden de ejecución = inverso de compensación

### Idempotencia

- [ ] Verificar que el `stepName` del resultado corresponde al step actual
- [ ] Ignorar resultados para SAGAs en estado terminal (SUCCEEDED, FAILED)
- [ ] `OptimisticLockException` manejada silenciosamente (mensaje duplicado)
- [ ] Constraint en outbox para prevenir comandos duplicados

### Listeners

- [ ] Un listener por queue de respuesta (customer, payment, fleet)
- [ ] Cada listener delega al `SagaResultHandler` (con manejo de OptimisticLock)
- [ ] MDC con `sagaId` para correlación de logs

### Operaciones

- [ ] Stuck detection scheduler ejecutándose cada minuto
- [ ] Threshold configurable para detección (default: 5 minutos)
- [ ] Endpoint `/internal/sagas` para consultar estado
- [ ] Métricas Micrometer: started, succeeded, failed, compensated, duration
- [ ] Log pattern incluye `sagaId` como campo estructurado

### Integración con Outbox (Área 13)

- [ ] Los steps usan `OutboxEventRepository` para guardar comandos
- [ ] `OutboxPublisher` scheduler publica los comandos a RabbitMQ
- [ ] `aggregate_type = 'SAGA'` y `aggregate_id = sagaId` para trazabilidad
- [ ] Eventos de compensación también pasan por el Outbox

---

*Documento generado con Context7 - Spring Boot 3.4.x, RabbitMQ 3.13+*
