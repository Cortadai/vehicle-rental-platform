package com.vehiclerental.customer.application.dto.command;

public record CreateCustomerCommand(String firstName, String lastName, String email, String phone) {
}
