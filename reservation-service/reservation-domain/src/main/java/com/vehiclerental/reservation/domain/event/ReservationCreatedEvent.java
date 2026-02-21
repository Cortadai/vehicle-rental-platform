package com.vehiclerental.reservation.domain.event;

import com.vehiclerental.common.domain.event.DomainEvent;
import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.reservation.domain.exception.ReservationDomainException;
import com.vehiclerental.reservation.domain.model.vo.CustomerId;
import com.vehiclerental.reservation.domain.model.vo.DateRange;
import com.vehiclerental.reservation.domain.model.vo.PickupLocation;
import com.vehiclerental.reservation.domain.model.vo.ReservationId;
import com.vehiclerental.reservation.domain.model.vo.TrackingId;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReservationCreatedEvent(
        UUID eventId,
        Instant occurredOn,
        ReservationId reservationId,
        TrackingId trackingId,
        CustomerId customerId,
        Money totalPrice,
        DateRange dateRange,
        PickupLocation pickupLocation,
        PickupLocation returnLocation,
        List<ReservationItemSnapshot> items
) implements DomainEvent {

    public ReservationCreatedEvent {
        if (eventId == null) {
            throw new ReservationDomainException("eventId must not be null", "RESERVATION_EVENT_ID_NULL");
        }
        if (occurredOn == null) {
            throw new ReservationDomainException("occurredOn must not be null", "RESERVATION_EVENT_OCCURRED_ON_NULL");
        }
    }
}
