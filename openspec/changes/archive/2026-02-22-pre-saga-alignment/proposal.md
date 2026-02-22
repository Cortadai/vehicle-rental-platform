## Why

La retrospectiva pre-SAGA (`docs/retrospective-pre-saga.md`) identifico 4 inconsistencias entre servicios que deben alinearse antes de implementar SAGA orchestration. Estas inconsistencias — si no se corrigen — causarian bugs en runtime (failureMessages con comas), fallos de Flyway (schema inconsistente), falta de cobertura critica (OutboxPublisher sin tests), y topologia incompleta para orquestacion (sin command queues).

Este change es puramente de alineacion: no agrega funcionalidad nueva, solo unifica patrones existentes y prepara la infraestructura para los proximos changes (customer-outbox-and-messaging, fleet-outbox-and-messaging, reservation-saga-orchestration).

## What Changes

- **Unificar failureMessages a JSON**: Reemplazar serializacion comma-separated en `ReservationPersistenceMapper` por JSON via `ObjectMapper` (mismo patron que `PaymentPersistenceMapper`). El mapper ahora requiere `ObjectMapper` inyectado via constructor.
- **Remover Flyway default-schema en Payment**: Eliminar `spring.flyway.default-schema: payment` de `application.yml` de Payment Service. Los otros 3 servicios no lo definen — Flyway usa `public` por defecto.
- **Agregar unit tests para OutboxPublisher**: Crear `OutboxPublisherTest` en `common-messaging` con 5 tests unitarios usando Mockito (no Testcontainers). Agregar `spring-boot-starter-test` como dependencia test al POM.
- **Agregar 4 command queues a definitions.json**: Crear queues para comandos SAGA: `customer.validate.command.queue`, `payment.process.command.queue`, `payment.refund.command.queue`, `fleet.confirm.command.queue`. Cada una con DLQ routing al DLQ del servicio receptor. Bindings via exchange del receptor.

## Capabilities

### New Capabilities

Ninguna — este change no introduce capacidades nuevas.

### Modified Capabilities

- `reservation-jpa-persistence`: ReservationPersistenceMapper cambia de comma-separated a JSON para failureMessages. Requiere ObjectMapper inyectado.
- `payment-container-assembly`: application.yml sin `spring.flyway.default-schema`.
- `rabbitmq-topology`: 4 command queues con DLQ y bindings en definitions.json.

## Impact

- **reservation-infrastructure**: `ReservationPersistenceMapper` — cambio de serializacion, nuevo constructor con `ObjectMapper`
- **reservation-container**: `BeanConfiguration` — factory method de `ReservationPersistenceMapper` ahora recibe `ObjectMapper`
- **payment-container**: `application.yml` — remover linea `default-schema: payment`
- **common-messaging**: `pom.xml` — agregar `spring-boot-starter-test` (test scope). Nuevo test: `OutboxPublisherTest`
- **docker/rabbitmq/definitions.json**: 4 queues nuevas, 8 bindings nuevos (4 work + 4 DLQ)
- **Zero cambios** en: domain modules, application modules, payment-infrastructure, customer-service, fleet-service, docker-compose.yml
