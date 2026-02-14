package com.vehiclerental.customer.domain.exception;

import com.vehiclerental.common.domain.exception.DomainException;

public class CustomerDomainException extends DomainException {

    public CustomerDomainException(String message, String errorCode) {
        super(message, errorCode);
    }

    public CustomerDomainException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
