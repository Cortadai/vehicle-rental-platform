payment-event-publisher
=======================

**SUPERSEDED** by `payment-outbox-publishing` (change: payment-outbox-and-messaging, cycle #16).

The logger no-op `PaymentDomainEventPublisherAdapter` was replaced by `OutboxPaymentDomainEventPublisher` which persists events to the outbox table. See `payment-outbox-publishing/spec.md` for the current specification.
