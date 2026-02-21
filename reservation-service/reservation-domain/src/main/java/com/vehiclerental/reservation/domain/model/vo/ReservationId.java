package com.vehiclerental.reservation.domain.model.vo;

import com.vehiclerental.reservation.domain.exception.ReservationDomainException;

import java.util.UUID;

public record ReservationId(UUID value) {

    public ReservationId {
        if (value == null) {
            throw new ReservationDomainException("ReservationId value must not be null", "RESERVATION_ID_NULL");
        }
    }
}
