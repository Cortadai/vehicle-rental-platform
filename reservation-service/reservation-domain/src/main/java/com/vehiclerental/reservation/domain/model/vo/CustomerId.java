package com.vehiclerental.reservation.domain.model.vo;

import com.vehiclerental.reservation.domain.exception.ReservationDomainException;

import java.util.UUID;

public record CustomerId(UUID value) {

    public CustomerId {
        if (value == null) {
            throw new ReservationDomainException("CustomerId value must not be null", "CUSTOMER_ID_NULL");
        }
    }
}
