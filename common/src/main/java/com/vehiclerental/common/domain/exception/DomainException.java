package com.vehiclerental.common.domain.exception;

import java.util.Objects;

public abstract class DomainException extends RuntimeException {

    private final String errorCode;

    protected DomainException(String message, String errorCode) {
        super(message);
        this.errorCode = validateErrorCode(errorCode);
    }

    protected DomainException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = validateErrorCode(errorCode);
    }

    public String getErrorCode() {
        return errorCode;
    }

    private static String validateErrorCode(String errorCode) {
        Objects.requireNonNull(errorCode, "errorCode must not be null");
        if (errorCode.isBlank()) {
            throw new IllegalArgumentException("errorCode must not be blank");
        }
        return errorCode;
    }
}
