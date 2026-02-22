package com.vehiclerental.payment.infrastructure.adapter.input.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProcessPaymentRequest(
        @NotBlank String reservationId,
        @NotBlank String customerId,
        @NotNull BigDecimal amount,
        @NotBlank String currency) {
}
