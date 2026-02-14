package com.vehiclerental.customer.domain.model.vo;

import com.vehiclerental.customer.domain.exception.CustomerDomainException;

import java.util.regex.Pattern;

public record Email(String value) {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public Email {
        if (value == null) {
            throw new CustomerDomainException("Email must not be null", "CUSTOMER_EMAIL_NULL");
        }
        if (value.isBlank()) {
            throw new CustomerDomainException("Email must not be blank", "CUSTOMER_EMAIL_BLANK");
        }
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new CustomerDomainException("Email format is invalid: " + value, "CUSTOMER_EMAIL_INVALID");
        }
    }
}
