package com.vehiclerental.payment.application.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PaymentResponse(
        String paymentId,
        String reservationId,
        String customerId,
        BigDecimal amount,
        String currency,
        String status,
        List<String> failureMessages,
        Instant createdAt,
        Instant updatedAt) {
}
