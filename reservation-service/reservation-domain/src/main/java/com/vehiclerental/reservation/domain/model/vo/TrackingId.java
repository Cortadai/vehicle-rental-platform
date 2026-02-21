package com.vehiclerental.reservation.domain.model.vo;

import com.vehiclerental.reservation.domain.exception.ReservationDomainException;

import java.util.UUID;

public record TrackingId(UUID value) {

    public TrackingId {
        if (value == null) {
            throw new ReservationDomainException("TrackingId value must not be null", "TRACKING_ID_NULL");
        }
    }
}
