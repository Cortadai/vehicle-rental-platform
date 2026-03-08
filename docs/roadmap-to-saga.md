# Roadmap to SAGA Orchestration

## Decision Log

| Decision | Rationale |
|----------|-----------|
| Orquestacion (no coreografia) | Commands via exchange del receptor — control centralizado del flujo |
| Flyway: sin default-schema | Todos los servicios usan `public` schema — consistencia |
| failureMessages: JSON unificado | Patron Payment con ObjectMapper — robusto ante comas en mensajes |
| Logging/MDC | Implementar durante SAGA, no durante messaging — evitar cambios prematuros |
| @JsonValue en typed IDs | No cambiar (funcional, decision diferida) — `{"value":"uuid"}` en payloads |

---

## Change 1: pre-saga-alignment [COMPLETADO]

- [x] Fix failureMessages comma-separated -> JSON en Reservation
- [x] Remover Flyway default-schema de Payment
- [x] Tests unitarios para OutboxPublisher (common-messaging)
- [x] Anadir 4 command queues a definitions.json

## Change 2: customer-outbox-and-messaging [COMPLETADO]

- [x] Outbox infrastructure (OutboxCustomerDomainEventPublisher reemplaza logger no-op)
- [x] @EntityScan + @EnableJpaRepositories + scanBasePackages
- [x] CustomerValidatedEvent + CustomerRejectedEvent (nombre alineado con FleetRejectedEvent)
- [x] ValidateCustomerForReservationUseCase (6to interface en CustomerApplicationService)
- [x] CustomerValidationListener (@RabbitListener — primero de la plataforma)
- [x] RabbitMQConfig (customer.exchange, 3 queues, per-queue DLQ routing keys)
- [x] RabbitMQContainer en todos los ITs (3 existentes + 2 nuevos)
- [x] OutboxPublisherIT + OutboxAtomicityIT
- [x] V2__create_outbox_events_table.sql
- [x] docker-java.properties api.version=1.44 (workaround Docker 29.x / Testcontainers 1.20.4)

## Change 3: fleet-outbox-and-messaging [COMPLETADO]

- [x] Outbox infrastructure (OutboxFleetDomainEventPublisher reemplaza logger no-op)
- [x] @EntityScan + @EnableJpaRepositories + scanBasePackages
- [x] FleetConfirmedEvent + FleetRejectedEvent + FleetReleasedEvent (3 SAGA events)
- [x] ConfirmFleetAvailabilityUseCase + ReleaseFleetReservationUseCase (6to y 7mo interface)
- [x] FleetConfirmationListener + FleetReleaseListener (2 @RabbitListeners)
- [x] RabbitMQConfig (fleet.exchange, 4 queues, per-queue DLQ routing keys)
- [x] RabbitMQContainer en todos los ITs (3 existentes + 2 nuevos)
- [x] OutboxPublisherIT + OutboxAtomicityIT
- [x] V2__create_outbox_events_table.sql
- [x] fleet.release.command.queue en definitions.json (5to command queue)

## Change 4: payment-saga-participation [COMPLETADO]

- [x] RabbitMQConfig: 2 command queues (process + refund) con DLQ routing
- [x] RabbitMQConfig: 2 bindings a payment.exchange + 2 DLQ bindings a dlx.exchange
- [x] PaymentProcessListener (@RabbitListener — reusa ProcessPaymentUseCase existente)
- [x] PaymentRefundListener (@RabbitListener — reusa RefundPaymentUseCase existente)
- [x] PaymentProcessListenerIT + PaymentRefundListenerIT (raw Message + Awaitility)
- [x] Todos los tests pasan (69 unit + 18 ITs)

## Change 5: reservation-saga-orchestration [COMPLETADO]

- [x] SagaStatus enum (STARTED→PROCESSING→SUCCEEDED/COMPENSATING→FAILED) con canTransitionTo()
- [x] SagaState domain object (create/reconstruct, 6 metodos de transicion, version nullable)
- [x] SagaStateRepository output port + SagaStateJpaEntity (@Version optimistic locking)
- [x] SagaStep<T> interface + 3 implementaciones (CustomerValidation, Payment, FleetConfirmation)
- [x] ReservationSagaOrchestrator (start, handleStepSuccess, handleStepFailure, handleCompensationComplete)
- [x] OutboxSagaCommandPublisher (comandos via outbox, aggregateType "SAGA")
- [x] 7 Response listeners en Reservation (validated/rejected × customer/payment/fleet + refunded)
- [x] RabbitMQConfig: 3 participant exchanges, 7 response queues, 7 bindings con DLQ
- [x] V3__create_saga_state_table.sql (saga_id UUID PK, version, indice en status)
- [x] BeanConfiguration: 5 nuevos beans + ReservationApplicationService con 4to parametro
- [x] Compensation flow funcional: Fleet rejected → Payment refund → cancel
- [x] 50 unit tests nuevos (30 domain + 20 application) — total 160 unit tests reservation
- [x] 5 SAGA ITs (happy path, customer rejection, payment failure, fleet rejection, saga state persistence)
- [x] Todos los tests pasan (160 unit + 20 ITs = 180 total reservation-service)

---

## SAGA Orchestration — COMPLETADA

El flujo SAGA end-to-end esta operativo:
1. POST /api/v1/reservations → crea Reservation + SagaState → publica customer.validate.command
2. Customer validated → avanza a Payment → publica payment.process.command
3. Payment completed → avanza a Fleet → publica fleet.confirm.command
4. Fleet confirmed → marca SAGA como SUCCEEDED, Reservation como CONFIRMED

Compensacion: Fleet rejected → rollback Payment (refund command) → cancel Reservation → SAGA FAILED

---

## Post-SAGA (changes dedicados)

- [ ] SAGA timeout/retry handling (que pasa si un participante no responde?)
- [ ] Idempotencia de listeners (evitar procesar el mismo mensaje dos veces)
- [ ] MDC/correlationId propagation (tracing distribuido)
- [ ] End-to-end testing con Docker Compose (4 servicios simultaneos)
- [ ] Mover spring-boot-starter-test a dependencyManagement (domain zero-Spring)
- [x] ArchUnit tests para boundaries hexagonales (change #23 — domain purity, application isolation, dependency flow)
- [ ] Indices en BD (status, email, category)
- [ ] OpenAPI documentation
- [ ] README para developers
- [ ] Evaluar MapStruct vs mappers manuales
- [x] JaCoCo permanente con umbrales (change #22 — 80/75/60% por capa, containers excluidos)
- [ ] Tests para ApiMetadata/ApiResponse en common
