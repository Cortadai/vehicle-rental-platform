package com.vehiclerental.payment.domain.exception;

import com.vehiclerental.common.domain.exception.DomainException;

public class PaymentDomainException extends DomainException {

    public PaymentDomainException(String message, String errorCode) {
        super(message, errorCode);
    }

    public PaymentDomainException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
