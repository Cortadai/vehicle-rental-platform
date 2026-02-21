package com.vehiclerental.payment.domain.model.vo;

import com.vehiclerental.payment.domain.exception.PaymentDomainException;

import java.util.UUID;

public record ReservationId(UUID value) {

    public ReservationId {
        if (value == null) {
            throw new PaymentDomainException("ReservationId value must not be null", "RESERVATION_ID_NULL");
        }
    }
}
