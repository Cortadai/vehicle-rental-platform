## Why

Customer Service usa un logger no-op (`CustomerDomainEventPublisherAdapter`) para "publicar" eventos de dominio. Los 4 eventos existentes (`CustomerCreatedEvent`, `CustomerSuspendedEvent`, `CustomerActivatedEvent`, `CustomerDeletedEvent`) se loguean y se pierden — no llegan a RabbitMQ. Ademas, Customer es el primer servicio participante de la SAGA: necesita recibir un comando de validacion ("¿existe este customer y esta activo?"), ejecutar la logica, y responder con un evento de resultado via el bus. Sin outbox, listeners ni eventos SAGA, el orquestador no puede coordinar la validacion de clientes en el flujo de reserva.

Este change convierte a Customer de servicio aislado a participante activo del bus de mensajeria. Es el primer servicio en implementar el patron completo de participante SAGA: recibir comando via `@RabbitListener` + responder via Outbox + publicar eventos existentes al bus.

## What Changes

### Outbox Infrastructure (mecanico, replica de Payment)
- **Añadir** dependencia `common-messaging` al POM de `customer-infrastructure`
- **Reemplazar** `CustomerDomainEventPublisherAdapter` (logger no-op) por `OutboxCustomerDomainEventPublisher` que escribe eventos al outbox en la misma transaccion
- **Añadir** `RabbitMQConfig` en customer-infrastructure: declara `customer.exchange`, event queues (validated, rejected), command queue (`customer.validate.command.queue`), DLQ (`customer.dlq`), y todos los bindings — validacion idempotente de la topologia pre-declarada en `definitions.json`. A diferencia de Payment/Reservation (que solo declaran event queues salientes), Customer tambien declara la queue de comando entrante porque es participante SAGA
- **Añadir** migracion Flyway `V2__create_outbox_events_table.sql` en customer-container
- **Modificar** `CustomerServiceApplication` para añadir `scanBasePackages`, `@EntityScan` y `@EnableJpaRepositories` con `basePackages = "com.vehiclerental"`
- **Añadir** propiedades `spring.rabbitmq.*` a `application.yml`, incluyendo `listener.simple.retry` (enabled: true, 3 attempts, backoff exponencial) — patron de error handling para el primer `@RabbitListener`
- **Añadir** dependencias de test: `testcontainers-rabbitmq` + `awaitility`
- **Añadir** Testcontainers RabbitMQ a todos los ITs existentes (3 ITs: ServiceApplicationIT, ControllerIT, RepositoryAdapterIT)

### SAGA Participation (novedad — primer listener de la plataforma)
- **Añadir** 2 nuevos domain events: `CustomerValidatedEvent` y `CustomerRejectedEvent` en `customer-domain`
- **Añadir** nuevo use case: `ValidateCustomerForReservationUseCase` en `customer-application` — verifica que el customer exista y este activo. El `ValidateCustomerCommand` lleva `customerId` + `reservationId` (correlacion SAGA). Los eventos de respuesta (`CustomerValidatedEvent`, `CustomerRejectedEvent`) tambien llevan `reservationId` para que el orquestador correlacione la respuesta con la reserva
- **Añadir** `CustomerValidationListener` en `customer-infrastructure` — `@RabbitListener` que consume de `customer.validate.command.queue`, ejecuta el use case, y el resultado se publica via outbox (validated o rejected). Error handling: business failures (customer not found, not active) producen `CustomerRejectedEvent` como respuesta normal; infrastructure failures (DB down, JSON error) → exception → Spring Retry (3 attempts, backoff exponencial configurado en application.yml) → DLQ

### Integration Tests
- **Crear** `OutboxPublisherIT`: verifica que el scheduler publica eventos PENDING a RabbitMQ
- **Crear** `OutboxAtomicityIT`: verifica atomicidad entre entidad de negocio y outbox event

## Capabilities

### New Capabilities
- `customer-outbox-publishing`: OutboxCustomerDomainEventPublisher que implementa CustomerDomainEventPublisher, serializa 6 tipos de eventos (4 existentes + 2 SAGA) a JSON, escribe al outbox con routing keys `customer.created`, `customer.suspended`, `customer.activated`, `customer.deleted`, `customer.validated`, `customer.rejected`
- `customer-rabbitmq-topology`: RabbitMQConfig declarando exchange, 2 event queues (validated, rejected), 1 command queue (validate.command), DLQ, y todos los bindings para Customer Service — primera RabbitMQConfig que declara una queue de comando entrante
- `customer-saga-participation`: ValidateCustomerForReservationUseCase + CustomerValidationListener que recibe comandos de validacion via RabbitMQ y responde con CustomerValidatedEvent o CustomerRejectedEvent via outbox. Incluye Spring Retry + DLQ para error handling de infraestructura
- `customer-saga-events`: Dos nuevos domain events — `CustomerValidatedEvent` (customerId + reservationId) y `CustomerRejectedEvent` (customerId + reservationId + failureMessages) — para comunicacion SAGA. El naming Validated/Rejected es consistente con el par Confirmed/Rejected de Fleet y permite auto-derivacion de routing keys sin special cases

### Modified Capabilities
- `customer-event-publisher`: Reemplaza el logger no-op por OutboxCustomerDomainEventPublisher basado en Outbox Pattern. La interface del output port no cambia.
- `customer-container-assembly`: Añadir scanning annotations, propiedades RabbitMQ, migracion V2, dependencias de test, RabbitMQ container en ITs existentes
- `customer-domain-events`: Añadir CustomerValidatedEvent y CustomerRejectedEvent al catalogo de eventos del dominio. A diferencia de los 4 eventos existentes (que son lifecycle events del aggregate), estos son SAGA response events que llevan `reservationId` (UUID raw, no typed ID cross-domain) para correlacion

## Impact

- **customer-domain**: 2 clases nuevas (eventos SAGA). Zero cambios en clases existentes.
- **customer-application**: 1 interface nueva (ValidateCustomerForReservationUseCase), implementacion en CustomerApplicationService, 1 command nuevo. Zero cambios en use cases existentes.
- **customer-infrastructure**: POM + dependency nueva, 3 clases nuevas (OutboxCustomerDomainEventPublisher, RabbitMQConfig, CustomerValidationListener), 1 clase eliminada (CustomerDomainEventPublisherAdapter)
- **customer-container**: migracion V2, application.yml actualizado, Application class con scanning, POM con test deps, ITs existentes necesitan RabbitMQ container, 2 ITs nuevos
- **Zero cambios** en: common, common-messaging, docker-compose.yml, definitions.json (command queues ya existen del pre-saga-alignment), root pom.xml, otros servicios
