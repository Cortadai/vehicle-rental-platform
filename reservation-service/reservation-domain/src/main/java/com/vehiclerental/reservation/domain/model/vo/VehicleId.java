package com.vehiclerental.reservation.domain.model.vo;

import com.vehiclerental.reservation.domain.exception.ReservationDomainException;

import java.util.UUID;

public record VehicleId(UUID value) {

    public VehicleId {
        if (value == null) {
            throw new ReservationDomainException("VehicleId value must not be null", "VEHICLE_ID_NULL");
        }
    }
}
