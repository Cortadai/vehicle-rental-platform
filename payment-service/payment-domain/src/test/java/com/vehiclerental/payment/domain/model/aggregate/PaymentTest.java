package com.vehiclerental.payment.domain.model.aggregate;

import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.payment.domain.exception.PaymentDomainException;
import com.vehiclerental.payment.domain.model.vo.CustomerId;
import com.vehiclerental.payment.domain.model.vo.PaymentId;
import com.vehiclerental.payment.domain.model.vo.PaymentStatus;
import com.vehiclerental.payment.domain.model.vo.ReservationId;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final ReservationId RESERVATION_ID = new ReservationId(UUID.randomUUID());
    private static final CustomerId CUSTOMER_ID = new CustomerId(UUID.randomUUID());
    private static final Money AMOUNT = new Money(new BigDecimal("150.00"), EUR);

    @Test
    void successfulCreation() {
        var payment = Payment.create(RESERVATION_ID, CUSTOMER_ID, AMOUNT);

        assertThat(payment.getId()).isNotNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getDomainEvents()).isEmpty();
    }

    @Test
    void nullReservationIdRejected() {
        assertThatThrownBy(() -> Payment.create(null, CUSTOMER_ID, AMOUNT))
                .isInstanceOf(PaymentDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PAYMENT_RESERVATION_ID_REQUIRED");
    }

    @Test
    void nullCustomerIdRejected() {
        assertThatThrownBy(() -> Payment.create(RESERVATION_ID, null, AMOUNT))
                .isInstanceOf(PaymentDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PAYMENT_CUSTOMER_ID_REQUIRED");
    }

    @Test
    void nullAmountRejected() {
        assertThatThrownBy(() -> Payment.create(RESERVATION_ID, CUSTOMER_ID, null))
                .isInstanceOf(PaymentDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PAYMENT_AMOUNT_REQUIRED");
    }

    @Test
    void zeroAmountRejected() {
        var zeroAmount = new Money(new BigDecimal("0.00"), EUR);
        assertThatThrownBy(() -> Payment.create(RESERVATION_ID, CUSTOMER_ID, zeroAmount))
                .isInstanceOf(PaymentDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PAYMENT_AMOUNT_INVALID");
    }

    @Test
    void fieldsAccessibleAfterCreation() {
        var payment = Payment.create(RESERVATION_ID, CUSTOMER_ID, AMOUNT);

        assertThat(payment.getId()).isNotNull();
        assertThat(payment.getReservationId()).isEqualTo(RESERVATION_ID);
        assertThat(payment.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(payment.getAmount()).isEqualTo(AMOUNT);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getFailureMessages()).isEmpty();
        assertThat(payment.getCreatedAt()).isNotNull();
        assertThat(payment.getUpdatedAt()).isNotNull();
    }

    @Test
    void reconstructDoesNotEmitEvents() {
        var paymentId = new PaymentId(UUID.randomUUID());
        var now = Instant.now();

        var payment = Payment.reconstruct(paymentId, RESERVATION_ID, CUSTOMER_ID, AMOUNT,
                PaymentStatus.COMPLETED, List.of(), now, now);

        assertThat(payment.getDomainEvents()).isEmpty();
        assertThat(payment.getId()).isEqualTo(paymentId);
        assertThat(payment.getReservationId()).isEqualTo(RESERVATION_ID);
        assertThat(payment.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(payment.getAmount()).isEqualTo(AMOUNT);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void reconstructPreservesFailureMessages() {
        var paymentId = new PaymentId(UUID.randomUUID());
        var now = Instant.now();
        var failureMessages = List.of("Card declined", "Insufficient funds");

        var payment = Payment.reconstruct(paymentId, RESERVATION_ID, CUSTOMER_ID, AMOUNT,
                PaymentStatus.FAILED, failureMessages, now, now);

        assertThat(payment.getFailureMessages()).hasSize(2);
        assertThat(payment.getFailureMessages()).containsExactly("Card declined", "Insufficient funds");
    }

    @Test
    void noPublicConstructors() {
        var constructors = Payment.class.getDeclaredConstructors();

        assertThat(Arrays.stream(constructors).noneMatch(c -> Modifier.isPublic(c.getModifiers()))).isTrue();
    }
}
