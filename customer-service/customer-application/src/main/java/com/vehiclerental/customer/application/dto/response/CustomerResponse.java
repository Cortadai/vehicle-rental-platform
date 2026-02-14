package com.vehiclerental.customer.application.dto.response;

import java.time.Instant;

public record CustomerResponse(
        String customerId,
        String firstName,
        String lastName,
        String email,
        String phone,
        String status,
        Instant createdAt) {
}
