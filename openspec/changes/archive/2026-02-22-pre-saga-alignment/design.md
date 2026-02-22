## Context

La retrospectiva pre-SAGA audito los 19 modulos del proyecto y encontro 4 inconsistencias de prioridad alta que deben resolverse antes de entrar en la fase de coordinacion distribuida (SAGA Orchestration). El build tiene 392 tests pasando, pero hay deuda tecnica que crearia problemas al integrar los proximos changes.

Estado actual:
- Reservation y Payment ya tienen Outbox Pattern funcional con RabbitMQ
- Customer y Fleet siguen con logger no-op stubs
- La topologia en `definitions.json` tiene queues de eventos pero no de comandos
- `failureMessages` usa serializacion diferente entre Reservation (comma-separated) y Payment (JSON)
- Solo Payment define `spring.flyway.default-schema` — los otros 3 no
- `common-messaging` tiene 0 tests unitarios (logica critica cubierta solo indirectamente por ITs)

## Goals / Non-Goals

**Goals:**
- Unificar la serializacion de `failureMessages` a JSON en todos los servicios que lo usan (Reservation, para alinear con Payment)
- Remover la configuracion `spring.flyway.default-schema` de Payment para consistencia con los otros 3 servicios
- Agregar cobertura de tests unitarios para `OutboxPublisher` en `common-messaging`
- Preparar la topologia RabbitMQ con command queues necesarias para SAGA orchestration

**Non-Goals:**
- Agregar outbox a Customer o Fleet (changes separados: `customer-outbox-and-messaging`, `fleet-outbox-and-messaging`)
- Implementar el SAGA orchestrator (change separado: `reservation-saga-orchestration`)
- Agregar MDC/logging estructurado (se hara durante SAGA)
- Agregar `@RabbitListener` en ningun servicio (responsabilidad de cada change de messaging/SAGA)
- Modificar domain modules (los typed IDs y domain events quedan igual)

## Decisions

### Decision 1: failureMessages JSON sin fallback comma-separated

**Elegido**: Migrar directamente a JSON via `ObjectMapper.writeValueAsString()` / `readValue()` sin logica de fallback para el formato comma-separated anterior.

**Alternativa rechazada**: Agregar fallback que detecte si el string comienza con `[` (JSON) o no (comma-separated). Esto es un POC sin datos legacy en produccion — no hay registros existentes con formato comma-separated que necesiten leerse.

**Consecuencia**: `ReservationPersistenceMapper` requiere `ObjectMapper` inyectado via constructor. `BeanConfiguration` debe actualizar el factory method para pasar `ObjectMapper`. El patron es identico al de `PaymentPersistenceMapper` (lineas 19-75).

### Decision 2: Remover Flyway default-schema en Payment — no agregar a los demas

**Elegido**: Eliminar `spring.flyway.default-schema: payment` de `application.yml` de Payment. Cada servicio usa una base de datos separada (`payment_db`, `customer_db`, etc.), asi que todas las tablas viven en el schema `public` — no hay conflicto.

**Alternativa rechazada**: Agregar `default-schema` a los otros 3 servicios. Anade complejidad sin beneficio — en un esquema de database-per-service, el schema `public` es suficiente.

**Consecuencia**: Flyway en Payment se comportara igual que en Customer, Fleet y Reservation.

### Decision 3: OutboxPublisher tests como unit tests con mocks — no IT

**Elegido**: Crear `OutboxPublisherTest` en `common-messaging` usando Mockito para mockear `OutboxEventRepository`, `RabbitTemplate`, y `TransactionTemplate`. Agregar `spring-boot-starter-test` al POM de `common-messaging`.

**Alternativa rechazada**: Integration tests con Testcontainers PostgreSQL + RabbitMQ. No es necesario — los ITs de cada servicio (OutboxPublisherIT, OutboxAtomicityIT) ya cubren el flujo end-to-end. Los unit tests cubren la logica interna: retry count, status transitions, headers AMQP.

**Consecuencia**: 5 tests nuevos cubriendo: no-op con 0 eventos, publish exitoso, retry en fallo, max retries marca FAILED, headers AMQP correctos.

### Decision 4: Command queues en exchange del receptor — orquestacion, no coreografia

**Elegido**: Las 4 command queues se bindean al exchange del servicio receptor:
- `customer.validate.command.queue` → `customer.exchange` con routing key `customer.validate.command`
- `payment.process.command.queue` → `payment.exchange` con routing key `payment.process.command`
- `payment.refund.command.queue` → `payment.exchange` con routing key `payment.refund.command`
- `fleet.confirm.command.queue` → `fleet.exchange` con routing key `fleet.confirm.command`

**Razon**: En orquestacion, el orquestador (Reservation) envia comandos al exchange del servicio que debe ejecutarlos. El receptor es dueno de su exchange y define las queues que consume. Esto es coherente con el patron existente donde cada servicio tiene su propio exchange.

**Alternativa rechazada**: Un `saga.exchange` centralizado. Mezcla concerns y rompe la independencia de exchanges por servicio.

**Consecuencia**: Cada command queue tiene DLQ routing a la DLQ del servicio receptor (e.g., `customer.validate.command.dlq` → `customer.dlq`). Las DLQs existentes se reusan.

## Risks / Trade-offs

- **[Riesgo] Datos legacy con comma-separated failureMessages**: Mitigacion — POC sin datos de produccion, no hay registros existentes que migrar.
- **[Trade-off] Unit tests vs IT para OutboxPublisher**: Los unit tests con mocks no verifican la integracion real con PostgreSQL/RabbitMQ, pero eso ya esta cubierto por los ITs de cada servicio.
- **[Riesgo] Command queues sin consumers**: Las queues existiran en RabbitMQ pero nadie las consume todavia. No es un problema — los mensajes simplemente se acumularian si alguien publicara a ellas (que nadie hace aun).

## Open Questions

Ninguna — las 4 tareas son mecanicas y los patrones estan consolidados.
