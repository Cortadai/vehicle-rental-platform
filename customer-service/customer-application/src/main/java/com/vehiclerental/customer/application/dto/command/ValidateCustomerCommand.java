package com.vehiclerental.customer.application.dto.command;

public record ValidateCustomerCommand(String customerId, String reservationId) {
}
