## Why

Fleet Service usa un logger no-op (`FleetDomainEventPublisherAdapter`) para "publicar" eventos de dominio. Los 4 eventos existentes (`VehicleRegisteredEvent`, `VehicleSentToMaintenanceEvent`, `VehicleActivatedEvent`, `VehicleRetiredEvent`) se loguean y se pierden — no llegan a RabbitMQ. Fleet es el último servicio participante de la SAGA que falta por conectar al bus: necesita recibir comandos de confirmación de disponibilidad y de liberación (compensación), ejecutar la lógica, y responder con eventos de resultado vía outbox. Sin outbox, listeners ni eventos SAGA, el orquestador no puede coordinar el paso final del flujo de reserva ni compensar cuando algo falla después de Fleet.

Este change convierte a Fleet de servicio aislado a participante activo del bus de mensajería. Sigue el patrón establecido en `customer-outbox-and-messaging` (ciclo #18), con una diferencia clave: Fleet tiene **dos** command queues (confirm + release/compensación) mientras que Customer solo tiene una (validate). Con Fleet conectado, los 4 servicios estarán listos para `reservation-saga-orchestration`.

## What Changes

### Outbox Infrastructure (mecánico, réplica de Customer/Payment)
- **Añadir** dependencia `common-messaging` al POM de `fleet-infrastructure`
- **Reemplazar** `FleetDomainEventPublisherAdapter` (logger no-op) por `OutboxFleetDomainEventPublisher` que escribe eventos al outbox en la misma transacción
- **Añadir** `RabbitMQConfig` en fleet-infrastructure: declara `fleet.exchange`, 2 event queues (confirmed, rejected), 2 command queues (`fleet.confirm.command.queue` + `fleet.release.command.queue`), DLQ (`fleet.dlq`), y todos los bindings
- **Añadir** migración Flyway `V2__create_outbox_events_table.sql` en fleet-container
- **Modificar** `FleetServiceApplication` para añadir `scanBasePackages`, `@EntityScan` y `@EnableJpaRepositories` con `basePackages = "com.vehiclerental"`
- **Añadir** propiedades `spring.rabbitmq.*` a `application.yml`, incluyendo `listener.simple.retry` (enabled: true, 3 attempts, backoff exponencial)
- **Añadir** dependencias de test: `testcontainers-rabbitmq` + `awaitility`
- **Añadir** Testcontainers RabbitMQ a todos los ITs existentes (3 ITs: FleetServiceApplicationIT, VehicleControllerIT, VehicleRepositoryAdapterIT)
- **Actualizar** `definitions.json`: añadir `fleet.release.command.queue` + binding a `fleet.exchange` + DLQ binding (la queue de confirm ya existe del pre-saga-alignment, la de release no)

### SAGA Participation — Confirmación (happy path)
- **Añadir** 2 nuevos domain events: `FleetConfirmedEvent` y `FleetRejectedEvent` en `fleet-domain`
- **Añadir** use case: `ConfirmFleetAvailabilityUseCase` en `fleet-application` — verifica que el vehículo exista y esté ACTIVE. El `ConfirmFleetAvailabilityCommand` lleva `vehicleId` + `reservationId` + `pickupDate` + `returnDate` (las fechas viajan en el command para logging/futuro uso; la validación real de disponibilidad por fechas se simplifica a verificar status ACTIVE dado que el dominio actual no tiene modelo de reservas de vehículo — ver nota de diseño abajo)
- **Añadir** `FleetConfirmationListener` en `fleet-infrastructure` — `@RabbitListener` que consume de `fleet.confirm.command.queue`, ejecuta el use case de confirmación

### SAGA Participation — Compensación (unhappy path)
- **Añadir** use case: `ReleaseFleetReservationUseCase` en `fleet-application` — libera la reserva del vehículo cuando el orquestador envía compensación. El `ReleaseFleetReservationCommand` lleva `vehicleId` + `reservationId`
- **Añadir** domain event: `FleetReleasedEvent` en `fleet-domain` — confirma que la liberación se completó (el orquestador necesita esta respuesta para avanzar al siguiente paso de compensación o finalizar)
- **Añadir** `FleetReleaseListener` en `fleet-infrastructure` — `@RabbitListener` que consume de `fleet.release.command.queue`, ejecuta el use case de liberación. Listener separado del de confirmación porque son queues distintas con contratos distintos
- Error handling en ambos listeners: business failures → evento de respuesta normal (rejected/released); infrastructure failures → Spring Retry → DLQ

### Nota de diseño: Disponibilidad por fechas (simplificada)
La síntesis arquitectónica dice que Fleet debe verificar disponibilidad "para el rango de fechas solicitado (sin solapamiento)". Implementar esto requiere un modelo de `vehicle_reservations` (vehicleId, reservationId, pickupDate, returnDate) con queries de solapamiento. Para este change, simplificamos: Fleet solo verifica que el vehículo exista y esté ACTIVE (mismo nivel que Customer: existencia + status). El modelo de disponibilidad por fechas es candidato para un change post-SAGA. Las fechas viajan en el command para que cuando se implemente la lógica real, no haya que cambiar el contrato del mensaje.

### Integration Tests
- **Crear** `OutboxPublisherIT`: verifica que el scheduler publica eventos PENDING a RabbitMQ
- **Crear** `OutboxAtomicityIT`: verifica atomicidad entre entidad de negocio y outbox event

## Capabilities

### New Capabilities
- `fleet-outbox-publishing`: OutboxFleetDomainEventPublisher que implementa FleetDomainEventPublisher, serializa 7 tipos de eventos (4 lifecycle + 3 SAGA) a JSON, escribe al outbox con routing keys derivadas
- `fleet-rabbitmq-topology`: RabbitMQConfig declarando exchange, 2 event queues (confirmed, rejected), 2 command queues (confirm.command, release.command), DLQ, y todos los bindings para Fleet Service
- `fleet-saga-participation`: ConfirmFleetAvailabilityUseCase + ReleaseFleetReservationUseCase + FleetConfirmationListener + FleetReleaseListener. Confirmación verifica existencia + status ACTIVE. Liberación es no-op sobre el aggregate (sin modelo de reservas de vehículo) pero emite FleetReleasedEvent para que el orquestador avance
- `fleet-saga-events`: Tres nuevos domain events — `FleetConfirmedEvent` (vehicleId + reservationId), `FleetRejectedEvent` (vehicleId + reservationId + failureMessages), `FleetReleasedEvent` (vehicleId + reservationId) — para comunicación SAGA

### Modified Capabilities
- `fleet-event-publisher`: Reemplaza el logger no-op por OutboxFleetDomainEventPublisher basado en Outbox Pattern. La interface del output port no cambia.
- `fleet-container-assembly`: Añadir scanning annotations, propiedades RabbitMQ, migración V2, dependencias de test, RabbitMQ container en ITs existentes
- `fleet-domain-events`: Añadir FleetConfirmedEvent, FleetRejectedEvent y FleetReleasedEvent al catálogo de eventos del dominio
- `rabbitmq-topology`: Añadir `fleet.release.command.queue` + bindings a `definitions.json`

## Impact

- **fleet-domain**: 3 clases nuevas (eventos SAGA). Zero cambios en clases existentes.
- **fleet-application**: 2 interfaces nuevas (ConfirmFleetAvailabilityUseCase, ReleaseFleetReservationUseCase), implementación en FleetApplicationService (pasa de 5 a 7 interfaces), 2 commands nuevos. Zero cambios en use cases existentes.
- **fleet-infrastructure**: POM + dependency nueva, 4 clases nuevas (OutboxFleetDomainEventPublisher, RabbitMQConfig, FleetConfirmationListener, FleetReleaseListener), 1 clase eliminada (FleetDomainEventPublisherAdapter)
- **fleet-container**: migración V2, application.yml actualizado, Application class con scanning, POM con test deps, ITs existentes necesitan RabbitMQ container, 2 ITs nuevos
- **definitions.json**: 1 queue nueva (fleet.release.command.queue) + 2 bindings (exchange + DLQ)
- **Zero cambios** en: common, common-messaging, docker-compose.yml, root pom.xml, otros servicios
