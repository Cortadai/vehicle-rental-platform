package com.vehiclerental.payment.application.mapper;

import com.vehiclerental.payment.application.dto.response.PaymentResponse;
import com.vehiclerental.payment.domain.model.aggregate.Payment;

public class PaymentApplicationMapper {

    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId().value().toString(),
                payment.getReservationId().value().toString(),
                payment.getCustomerId().value().toString(),
                payment.getAmount().amount(),
                payment.getAmount().currency().getCurrencyCode(),
                payment.getStatus().name(),
                payment.getFailureMessages(),
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }
}
