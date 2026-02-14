package com.vehiclerental.customer.application.exception;

public class CustomerNotFoundException extends RuntimeException {

    private final String customerId;

    public CustomerNotFoundException(String customerId) {
        super("Customer not found with id: " + customerId);
        this.customerId = customerId;
    }

    public String getCustomerId() {
        return customerId;
    }
}
