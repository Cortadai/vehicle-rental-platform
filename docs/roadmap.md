# Roadmap to SAGA Orchestration

## Decision Log

| Decision | Rationale |
|----------|-----------|
| Orquestacion (no coreografia) | Commands via exchange del receptor — control centralizado del flujo |
| Flyway: sin default-schema | Todos los servicios usan `public` schema — consistencia |
| failureMessages: JSON unificado | Patron Payment con ObjectMapper — robusto ante comas en mensajes |
| Logging/MDC | Implementar durante SAGA, no durante messaging — evitar cambios prematuros |
| @JsonValue en typed IDs | No cambiar (funcional, decision diferida) — `{"value":"uuid"}` en payloads |
| Mappers manuales (no MapStruct) | Prioridad en claridad y aprendizaje sobre reduccion de boilerplate. 9 mappers (426 LOC) — los complejos (Reservation/Payment persistence) usan ObjectMapper, MapStruct necesitaria @AfterMapping igualmente verboso. MapStruct recomendado para proyectos de produccion |
| starter-test en parent `<dependencies>` | Se evaluo mover a `<dependencyManagement>` pero el coste (13 POMs modificados) supera el beneficio. ArchUnit ya protege la boundary domain-sin-Spring. `<scope>test</scope>` no contamina el classpath de produccion |
| MapStruct eliminado del POM | Estaba declarado en properties, dependencyManagement y compiler plugin pero ningun fichero Java lo importaba. Limpiado como peso muerto tras decidir mantener mappers manuales |
| SAGA timeout/retry e idempotencia diferidos | El POC demuestra los patrones arquitectonicos (orchestration, compensation, outbox). Timeout/retry e idempotencia son concerns operacionales que anaden complejidad sin aportar al objetivo de aprendizaje. En produccion: scheduled job que detecte SAGAs stuck > N minutos, y deduplicacion por messageId en listeners |
| OpenAPI sin @Schema en DTOs | Springdoc 2.x con Java records genera specs razonables por introspeccion (tipos, nombres, @NotBlank → required). @Schema aportaria descriptions/examples pero requeriria swagger-annotations en application y common, rompiendo la pureza de capas. swagger-annotations-jakarta solo en infrastructure (donde viven los controllers) |

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

- [x] SAGA timeout/retry handling — DIFERIDO (ver Decision Log)
- [x] Idempotencia de listeners — DIFERIDO (ver Decision Log)
- [ ] MDC/correlationId propagation (tracing distribuido)
- [x] Docker Compose con 4 servicios — Paketo images, Actuator health, Spring Boot 3.4.13 (change #24)
- [x] Bruno E2E tests — coleccion Bruno + happy path SAGA validado + bugfix serializacion payment events (change #25)
- [x] Mover spring-boot-starter-test a dependencyManagement — DESCARTADO (ver Decision Log)
- [x] ArchUnit tests para boundaries hexagonales (change #23 — domain purity, application isolation, dependency flow)
- [x] Indices en BD — migraciones Flyway V3/V4 en fleet, payment, reservation (change #26)
- [x] OpenAPI documentation — springdoc-openapi + Swagger UI en 4 servicios, sin @Schema (change #28)
- [x] README para developers — actualizado con Swagger UI, Bruno E2E, eliminado project-overview.md redundante
- [x] Evaluar MapStruct vs mappers manuales — DESCARTADO, MapStruct eliminado del POM (ver Decision Log)
- [x] JaCoCo permanente con umbrales (change #22 — 80/75/60% por capa, containers excluidos)
- [x] Tests para ApiMetadata/ApiResponse en common (7 tests directos)
- [x] E2E de compensation flow — Bruno e2e/compensation con fleet rejection + payment refund + CANCELLED (change #27)
