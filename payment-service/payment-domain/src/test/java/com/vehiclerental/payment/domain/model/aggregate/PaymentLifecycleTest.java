package com.vehiclerental.payment.domain.model.aggregate;

import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.payment.domain.event.PaymentCompletedEvent;
import com.vehiclerental.payment.domain.event.PaymentFailedEvent;
import com.vehiclerental.payment.domain.event.PaymentRefundedEvent;
import com.vehiclerental.payment.domain.exception.PaymentDomainException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentLifecycleTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final ReservationId RESERVATION_ID = new ReservationId(UUID.randomUUID());
    private static final CustomerId CUSTOMER_ID = new CustomerId(UUID.randomUUID());
    private static final Money AMOUNT = new Money(new BigDecimal("150.00"), EUR);

    private Payment createPendingPayment() {
        var payment = Payment.create(RESERVATION_ID, CUSTOMER_ID, AMOUNT);
        payment.clearDomainEvents();
        return payment;
    }

    private Payment createCompletedPayment() {
        var payment = createPendingPayment();
        payment.complete();
        payment.clearDomainEvents();
        return payment;
    }

    private Payment createFailedPayment() {
        var payment = createPendingPayment();
        payment.fail(List.of("Card declined"));
        payment.clearDomainEvents();
        return payment;
    }

    private Payment createRefundedPayment() {
        var payment = createCompletedPayment();
        payment.refund();
        payment.clearDomainEvents();
        return payment;
    }

    // --- complete ---

    @Test
    void completeFromPending() {
        var payment = createPendingPayment();
        var updatedAtBefore = payment.getUpdatedAt();

        payment.complete();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getDomainEvents()).hasSize(1);
        assertThat(payment.getDomainEvents().get(0)).isInstanceOf(PaymentCompletedEvent.class);
        assertThat(payment.getUpdatedAt()).isAfterOrEqualTo(updatedAtBefore);
    }

    @Test
    void completeFromCompletedThrows() {
        assertThatThrownBy(() -> createCompletedPayment().complete())
                .isInstanceOf(PaymentDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PAYMENT_INVALID_STATE");
    }

    @Test
    void completeFromFailedThrows() {
        assertThatThrownBy(() -> createFailedPayment().complete())
                .isInstanceOf(PaymentDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PAYMENT_INVALID_STATE");
    }

    @Test
    void completeFromRefundedThrows() {
        assertThatThrownBy(() -> createRefundedPayment().complete())
                .isInstanceOf(PaymentDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PAYMENT_INVALID_STATE");
    }

    // --- fail ---

    @Test
    void failFromPending() {
        var payment = createPendingPayment();
        var updatedAtBefore = payment.getUpdatedAt();

        payment.fail(List.of("Card declined"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureMessages()).containsExactly("Card declined");
        assertThat(payment.getDomainEvents()).hasSize(1);
        assertThat(payment.getDomainEvents().get(0)).isInstanceOf(PaymentFailedEvent.class);
        assertThat(payment.getUpdatedAt()).isAfterOrEqualTo(updatedAtBefore);
    }

    @Test
    void failFromCompletedThrows() {
        assertThatThrownBy(() -> createCompletedPayment().fail(List.of("error")))
                .isInstanceOf(PaymentDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PAYMENT_INVALID_STATE");
    }

    @Test
    void failFromFailedThrows() {
        assertThatThrownBy(() -> createFailedPayment().fail(List.of("error")))
                .isInstanceOf(PaymentDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PAYMENT_INVALID_STATE");
    }

    @Test
    void failFromRefundedThrows() {
        assertThatThrownBy(() -> createRefundedPayment().fail(List.of("error")))
                .isInstanceOf(PaymentDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PAYMENT_INVALID_STATE");
    }

    @Test
    void failWithNullMessagesRejected() {
        var payment = createPendingPayment();
        assertThatThrownBy(() -> payment.fail(null))
                .isInstanceOf(PaymentDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PAYMENT_FAILURE_MESSAGES_REQUIRED");
    }

    @Test
    void failWithEmptyMessagesRejected() {
        var payment = createPendingPayment();
        assertThatThrownBy(() -> payment.fail(List.of()))
                .isInstanceOf(PaymentDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PAYMENT_FAILURE_MESSAGES_REQUIRED");
    }

    // --- refund ---

    @Test
    void refundFromCompleted() {
        var payment = createCompletedPayment();
        var updatedAtBefore = payment.getUpdatedAt();

        payment.refund();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getDomainEvents()).hasSize(1);
        assertThat(payment.getDomainEvents().get(0)).isInstanceOf(PaymentRefundedEvent.class);
        assertThat(payment.getUpdatedAt()).isAfterOrEqualTo(updatedAtBefore);
    }

    @Test
    void refundFromPendingThrows() {
        assertThatThrownBy(() -> createPendingPayment().refund())
                .isInstanceOf(PaymentDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PAYMENT_INVALID_STATE");
    }

    @Test
    void refundFromFailedThrows() {
        assertThatThrownBy(() -> createFailedPayment().refund())
                .isInstanceOf(PaymentDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PAYMENT_INVALID_STATE");
    }

    @Test
    void refundFromRefundedThrows() {
        assertThatThrownBy(() -> createRefundedPayment().refund())
                .isInstanceOf(PaymentDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PAYMENT_INVALID_STATE");
    }
}
