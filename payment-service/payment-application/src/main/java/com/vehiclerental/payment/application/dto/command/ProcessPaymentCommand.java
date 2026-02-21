package com.vehiclerental.payment.application.dto.command;

import java.math.BigDecimal;

public record ProcessPaymentCommand(String reservationId, String customerId, BigDecimal amount, String currency) {
}
