package com.vehiclerental.payment.domain.event;

import com.vehiclerental.common.domain.event.DomainEvent;
import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.payment.domain.exception.PaymentDomainException;
import com.vehiclerental.payment.domain.model.vo.CustomerId;
import com.vehiclerental.payment.domain.model.vo.PaymentId;
import com.vehiclerental.payment.domain.model.vo.ReservationId;

import java.time.Instant;
import java.util.UUID;

public record PaymentCompletedEvent(
        UUID eventId,
        Instant occurredOn,
        PaymentId paymentId,
        ReservationId reservationId,
        CustomerId customerId,
        Money amount
) implements DomainEvent {

    public PaymentCompletedEvent {
        if (eventId == null) {
            throw new PaymentDomainException("eventId must not be null", "PAYMENT_EVENT_ID_NULL");
        }
        if (occurredOn == null) {
            throw new PaymentDomainException("occurredOn must not be null", "PAYMENT_EVENT_OCCURRED_ON_NULL");
        }
    }
}
