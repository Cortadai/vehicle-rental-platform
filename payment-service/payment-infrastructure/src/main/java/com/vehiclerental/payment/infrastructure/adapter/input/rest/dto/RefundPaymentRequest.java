package com.vehiclerental.payment.infrastructure.adapter.input.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record RefundPaymentRequest(@NotBlank String reservationId) {
}
