package com.vehiclerental.reservation.domain.event;

import com.vehiclerental.common.domain.event.DomainEvent;
import com.vehiclerental.reservation.domain.exception.ReservationDomainException;
import com.vehiclerental.reservation.domain.model.vo.ReservationId;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReservationCancelledEvent(
        UUID eventId,
        Instant occurredOn,
        ReservationId reservationId,
        List<String> failureMessages
) implements DomainEvent {

    public ReservationCancelledEvent {
        if (eventId == null) {
            throw new ReservationDomainException("eventId must not be null", "RESERVATION_EVENT_ID_NULL");
        }
        if (occurredOn == null) {
            throw new ReservationDomainException("occurredOn must not be null", "RESERVATION_EVENT_OCCURRED_ON_NULL");
        }
    }
}
