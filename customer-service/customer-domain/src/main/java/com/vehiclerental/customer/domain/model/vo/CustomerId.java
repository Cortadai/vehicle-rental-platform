package com.vehiclerental.customer.domain.model.vo;

import com.vehiclerental.customer.domain.exception.CustomerDomainException;

import java.util.UUID;

public record CustomerId(UUID value) {

    public CustomerId {
        if (value == null) {
            throw new CustomerDomainException("CustomerId value must not be null", "CUSTOMER_ID_NULL");
        }
    }
}
