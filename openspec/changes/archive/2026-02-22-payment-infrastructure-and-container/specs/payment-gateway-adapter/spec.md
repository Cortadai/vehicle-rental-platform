payment-gateway-adapter
=======================

Purpose
-------

Infrastructure adapter implementing the `PaymentGateway` output port from the application layer. Provides a `SimulatedPaymentGateway` that always returns success. This is the first external-system adapter in the platform — Customer and Fleet only have persistence and event-publishing adapters.

## ADDED Requirements

### Requirement: SimulatedPaymentGateway implements PaymentGateway output port

SimulatedPaymentGateway SHALL implement `PaymentGateway` from the application layer. It SHALL always return a successful result.

#### Scenario: Charge always succeeds

- **WHEN** `charge(Money amount)` is called with any valid amount
- **THEN** it SHALL return `new PaymentGatewayResult(true, List.of())`

#### Scenario: Charge logs the attempt

- **WHEN** `charge(Money amount)` is called
- **THEN** it SHALL log the charge attempt (amount and currency) using SLF4J at INFO level before returning

#### Scenario: Adapter is a Spring component

- **WHEN** SimulatedPaymentGateway is inspected
- **THEN** it SHALL be annotated with `@Component`

### Requirement: SimulatedPaymentGateway has no external dependencies

SimulatedPaymentGateway SHALL NOT depend on any external payment library (Stripe SDK, PayPal SDK, etc.). It SHALL only use the domain `Money` type and application `PaymentGatewayResult` type.

#### Scenario: No external payment imports

- **WHEN** SimulatedPaymentGateway imports are inspected
- **THEN** it SHALL NOT import any type from `com.stripe.*`, `com.paypal.*`, or any external payment SDK

### Requirement: Gateway is injectable into PaymentApplicationService

SimulatedPaymentGateway SHALL be discoverable by Spring's component scan in the infrastructure layer. BeanConfiguration SHALL inject it into `PaymentApplicationService` via constructor.

#### Scenario: BeanConfiguration wires gateway to application service

- **WHEN** BeanConfiguration creates the `PaymentApplicationService` bean
- **THEN** it SHALL pass the `PaymentGateway` (resolved to `SimulatedPaymentGateway`) as a constructor argument

Constraint: Infrastructure layer only
--------------------------------------

SimulatedPaymentGateway SHALL live in `com.vehiclerental.payment.infrastructure.adapter.output.gateway`.
