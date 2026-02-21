package com.vehiclerental.payment.application.exception;

public class PaymentNotFoundException extends RuntimeException {

    private final String identifier;

    public PaymentNotFoundException(String identifier) {
        super("Payment not found with id: " + identifier);
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
