package com.vehiclerental.payment.application.mapper;

import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.payment.application.dto.response.PaymentResponse;
import com.vehiclerental.payment.domain.model.aggregate.Payment;
import com.vehiclerental.payment.domain.model.vo.CustomerId;
import com.vehiclerental.payment.domain.model.vo.PaymentId;
import com.vehiclerental.payment.domain.model.vo.PaymentStatus;
import com.vehiclerental.payment.domain.model.vo.ReservationId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentApplicationMapperTest {

    private final PaymentApplicationMapper mapper = new PaymentApplicationMapper();

    @Test
    void toResponseMapsAllFieldsCorrectly() {
        var paymentId = new PaymentId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        var reservationId = new ReservationId(UUID.fromString("660e8400-e29b-41d4-a716-446655440000"));
        var customerId = new CustomerId(UUID.fromString("770e8400-e29b-41d4-a716-446655440000"));
        var amount = new Money(new BigDecimal("150.00"), Currency.getInstance("USD"));
        var createdAt = Instant.parse("2024-01-15T10:30:00Z");
        var updatedAt = Instant.parse("2024-01-15T10:31:00Z");

        var payment = Payment.reconstruct(
                paymentId, reservationId, customerId, amount,
                PaymentStatus.COMPLETED, List.of(), createdAt, updatedAt);

        PaymentResponse response = mapper.toResponse(payment);

        assertThat(response.paymentId()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        assertThat(response.reservationId()).isEqualTo("660e8400-e29b-41d4-a716-446655440000");
        assertThat(response.customerId()).isEqualTo("770e8400-e29b-41d4-a716-446655440000");
        assertThat(response.amount()).isEqualTo(new BigDecimal("150.00"));
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.failureMessages()).isEmpty();
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void toResponseMapsFailureMessagesForFailedPayment() {
        var payment = Payment.reconstruct(
                new PaymentId(UUID.randomUUID()),
                new ReservationId(UUID.randomUUID()),
                new CustomerId(UUID.randomUUID()),
                new Money(new BigDecimal("100.00"), Currency.getInstance("EUR")),
                PaymentStatus.FAILED,
                List.of("Card declined", "Insufficient funds"),
                Instant.now(), Instant.now());

        PaymentResponse response = mapper.toResponse(payment);

        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.failureMessages()).containsExactly("Card declined", "Insufficient funds");
    }

    @Test
    void toResponseReturnsEmptyListNotNullForNonFailedPayment() {
        var payment = Payment.reconstruct(
                new PaymentId(UUID.randomUUID()),
                new ReservationId(UUID.randomUUID()),
                new CustomerId(UUID.randomUUID()),
                new Money(new BigDecimal("50.00"), Currency.getInstance("USD")),
                PaymentStatus.PENDING,
                List.of(),
                Instant.now(), Instant.now());

        PaymentResponse response = mapper.toResponse(payment);

        assertThat(response.failureMessages()).isNotNull();
        assertThat(response.failureMessages()).isEmpty();
    }
}
