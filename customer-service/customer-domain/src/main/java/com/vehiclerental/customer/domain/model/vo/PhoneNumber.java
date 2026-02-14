package com.vehiclerental.customer.domain.model.vo;

import com.vehiclerental.customer.domain.exception.CustomerDomainException;

import java.util.regex.Pattern;

public record PhoneNumber(String value) {

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\+?[0-9() -]+$");

    public PhoneNumber {
        if (value == null) {
            throw new CustomerDomainException("Phone number must not be null", "CUSTOMER_PHONE_NULL");
        }
        if (value.isBlank()) {
            throw new CustomerDomainException("Phone number must not be blank", "CUSTOMER_PHONE_BLANK");
        }
        if (value.length() < 3) {
            throw new CustomerDomainException("Phone number too short: " + value, "CUSTOMER_PHONE_TOO_SHORT");
        }
        if (value.length() > 20) {
            throw new CustomerDomainException("Phone number too long: " + value, "CUSTOMER_PHONE_TOO_LONG");
        }
        if (!PHONE_PATTERN.matcher(value).matches()) {
            throw new CustomerDomainException("Phone number format is invalid: " + value, "CUSTOMER_PHONE_INVALID");
        }
    }
}
