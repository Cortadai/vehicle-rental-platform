## Why

Payment Service usa un logger no-op (`PaymentDomainEventPublisherAdapter`) para "publicar" eventos de dominio. Los eventos (`PaymentCompletedEvent`, `PaymentFailedEvent`, `PaymentRefundedEvent`) se loguean y se pierden — no llegan a RabbitMQ, no pueden ser consumidos por la SAGA. Para que el orquestador de SAGA sepa si un pago fue exitoso, fallido o reembolsado, Payment necesita publicar eventos reales al bus de mensajería usando el Outbox Pattern que ya existe en `common-messaging`.

Toda la infraestructura pesada ya existe desde `reservation-outbox-and-messaging` (ciclo #12): `common-messaging` module, Docker Compose con PostgreSQL + RabbitMQ, `definitions.json` con topología de payment, `init-schemas.sql` con schema payment + payment_user. Este change solo "enchufa" Payment al bus existente.

## What Changes

- **Reemplazar** `PaymentDomainEventPublisherAdapter` (logger no-op) por `OutboxPaymentDomainEventPublisher` que escribe eventos al outbox en la misma transacción
- **Añadir** dependencia `common-messaging` al POM de `payment-infrastructure`
- **Añadir** `RabbitMQConfig` en payment-infrastructure: declara payment.exchange, 3 queues (completed, failed, refunded), DLQ, bindings — validación idempotente de la topología pre-declarada en definitions.json
- **Añadir** migración Flyway `V2__create_outbox_events_table.sql` en payment-container (misma tabla que Reservation)
- **Modificar** `PaymentServiceApplication` para añadir `@EntityScan` + `@EnableJpaRepositories` con `basePackages = "com.vehiclerental"` (lección del ciclo 12: sin esto, OutboxEvent no se detecta como entidad JPA)
- **Añadir** propiedades RabbitMQ a `application.yml`
- **Añadir** dependencias de test: `testcontainers-rabbitmq` + `awaitility`
- **Actualizar** `definitions.json`: añadir `payment.refunded.queue` + binding + DLQ binding (no existe aún, solo están completed y failed)
- **Añadir** Testcontainers RabbitMQ a todos los ITs existentes (lección del ciclo 12: una vez common-messaging está en classpath, `OutboxPublisher` requiere `RabbitTemplate`)
- **Crear** 2 integration tests: `OutboxAtomicityIT` + `OutboxPublisherIT`

## Capabilities

### New Capabilities
- `payment-outbox-publishing`: OutboxPaymentDomainEventPublisher que implementa PaymentDomainEventPublisher, serializa 3 tipos de eventos a JSON, escribe al outbox con routing keys payment.completed/payment.failed/payment.refunded
- `payment-rabbitmq-topology`: RabbitMQConfig declarando exchanges, queues (completed, failed, refunded + DLQ), y bindings para Payment Service

### Modified Capabilities
- `multi-module-build`: No — common-messaging ya está declarado en root POM
- `payment-container-assembly`: Añadir @EntityScan/@EnableJpaRepositories, propiedades RabbitMQ, migración V2, dependencias de test

## Impact

- **payment-infrastructure**: nuevo POM dependency, 2 clases nuevas (OutboxPaymentDomainEventPublisher, RabbitMQConfig), 1 clase eliminada (PaymentDomainEventPublisherAdapter)
- **payment-container**: migración V2, application.yml actualizado, PaymentServiceApplication con scanning, POM con test deps, ITs existentes necesitan RabbitMQ container, 2 ITs nuevos
- **docker/rabbitmq/definitions.json**: 1 queue nueva (payment.refunded.queue), 1 binding nueva, 1 DLQ binding nueva
- **Zero cambios** en: payment-domain, payment-application, common-messaging, docker-compose.yml, init-schemas.sql, root pom.xml, BeanConfiguration
