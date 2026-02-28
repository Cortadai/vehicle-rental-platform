package com.vehiclerental.customer.domain.event;

import com.vehiclerental.common.domain.event.DomainEvent;
import com.vehiclerental.customer.domain.exception.CustomerDomainException;
import com.vehiclerental.customer.domain.model.vo.CustomerId;

import java.time.Instant;
import java.util.UUID;

public record CustomerValidatedEvent(
        UUID eventId,
        Instant occurredOn,
        CustomerId customerId,
        UUID reservationId
) implements DomainEvent {

    public CustomerValidatedEvent {
        if (eventId == null) {
            throw new CustomerDomainException("eventId must not be null", "CUSTOMER_EVENT_ID_NULL");
        }
        if (occurredOn == null) {
            throw new CustomerDomainException("occurredOn must not be null", "CUSTOMER_EVENT_OCCURRED_ON_NULL");
        }
    }
}
