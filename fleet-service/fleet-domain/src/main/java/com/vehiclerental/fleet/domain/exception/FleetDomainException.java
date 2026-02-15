package com.vehiclerental.fleet.domain.exception;

import com.vehiclerental.common.domain.exception.DomainException;

public class FleetDomainException extends DomainException {

    public FleetDomainException(String message, String errorCode) {
        super(message, errorCode);
    }

    public FleetDomainException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
