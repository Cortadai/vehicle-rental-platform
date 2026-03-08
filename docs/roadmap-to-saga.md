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

## Change 5: reservation-saga-orchestration [PENDIENTE]

- [ ] SagaOrchestrator + SagaState + SagaStep interface
- [ ] Response listeners en Reservation
- [ ] Compensation flows (Fleet rechaza -> refund Payment -> cancel)
- [ ] MDC/correlationId propagation
- [ ] SAGA integration tests con Awaitility

## Post-SAGA (change dedicado)

- [ ] Mover spring-boot-starter-test a dependencyManagement (domain zero-Spring)
- [ ] ArchUnit tests para boundaries hexagonales
- [ ] Indices en BD (status, email, category)
- [ ] OpenAPI documentation
- [ ] README para developers
- [ ] Evaluar MapStruct vs mappers manuales
- [ ] JaCoCo permanente con umbrales
- [ ] Tests para ApiMetadata/ApiResponse en common
