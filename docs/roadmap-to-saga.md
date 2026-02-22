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

## Change 2: customer-outbox-and-messaging [PENDIENTE]

- [ ] Outbox infrastructure (mecanico, copia de Payment)
- [ ] @EntityScan + @EnableJpaRepositories + scanBasePackages
- [ ] CustomerValidatedEvent + CustomerValidationFailedEvent
- [ ] ValidateCustomerForReservationUseCase
- [ ] CustomerValidationListener (@RabbitListener)
- [ ] RabbitMQContainer en todos los ITs
- [ ] OutboxPublisherIT + OutboxAtomicityIT

## Change 3: fleet-outbox-and-messaging [PENDIENTE]

- [ ] Outbox infrastructure (mecanico, copia de Payment)
- [ ] @EntityScan + @EnableJpaRepositories + scanBasePackages
- [ ] FleetConfirmedEvent + FleetRejectedEvent
- [ ] ConfirmFleetAvailabilityUseCase + ReleaseFleetReservationUseCase
- [ ] FleetConfirmationListener (@RabbitListener)
- [ ] RabbitMQContainer en todos los ITs

## Change 4: reservation-saga-orchestration [PENDIENTE]

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
