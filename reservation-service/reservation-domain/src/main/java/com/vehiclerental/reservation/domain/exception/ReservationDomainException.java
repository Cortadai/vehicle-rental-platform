package com.vehiclerental.reservation.domain.exception;

import com.vehiclerental.common.domain.exception.DomainException;

public class ReservationDomainException extends DomainException {

    public ReservationDomainException(String message, String errorCode) {
        super(message, errorCode);
    }

    public ReservationDomainException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
