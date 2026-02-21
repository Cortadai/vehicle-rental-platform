package com.vehiclerental.payment.domain.model.vo;

import com.vehiclerental.payment.domain.exception.PaymentDomainException;

import java.util.UUID;

public record CustomerId(UUID value) {

    public CustomerId {
        if (value == null) {
            throw new PaymentDomainException("CustomerId value must not be null", "CUSTOMER_ID_NULL");
        }
    }
}
