## REMOVED Requirements

### Requirement: CustomerDomainEventPublisherAdapter implements output port
**Reason**: Replaced by OutboxCustomerDomainEventPublisher which persists events to the outbox table instead of logging them. The logger no-op served as a placeholder until messaging infrastructure was available.
**Migration**: Delete `CustomerDomainEventPublisherAdapter`. The new `OutboxCustomerDomainEventPublisher` (defined in `customer-outbox-publishing` spec) implements the same `CustomerDomainEventPublisher` interface as a `@Component`, so Spring auto-detects the replacement.

### Requirement: No messaging infrastructure dependency
**Reason**: The replacement adapter (`OutboxCustomerDomainEventPublisher`) intentionally depends on `OutboxEventRepository` and Jackson `ObjectMapper` for outbox persistence and JSON serialization. The constraint of no messaging dependency is no longer applicable.
**Migration**: The new adapter lives in the same package `com.vehiclerental.customer.infrastructure.adapter.output.event` and uses `common-messaging` types.

## MODIFIED Requirements

### Requirement: Infrastructure layer only

The event publisher adapter SHALL live in `com.vehiclerental.customer.infrastructure.adapter.output.event`. The implementation class is now `OutboxCustomerDomainEventPublisher` (replacing the deleted `CustomerDomainEventPublisherAdapter`).

#### Scenario: Outbox publisher is in correct package

- **WHEN** OutboxCustomerDomainEventPublisher is inspected
- **THEN** it SHALL be in package `com.vehiclerental.customer.infrastructure.adapter.output.event`
