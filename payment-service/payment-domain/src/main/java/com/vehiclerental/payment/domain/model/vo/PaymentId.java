package com.vehiclerental.payment.domain.model.vo;

import com.vehiclerental.payment.domain.exception.PaymentDomainException;

import java.util.UUID;

public record PaymentId(UUID value) {

    public PaymentId {
        if (value == null) {
            throw new PaymentDomainException("PaymentId value must not be null", "PAYMENT_ID_NULL");
        }
    }
}
