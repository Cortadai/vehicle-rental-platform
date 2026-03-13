package com.vehiclerental.payment.domain.event;

import com.vehiclerental.common.domain.event.DomainEvent;
import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.payment.domain.exception.PaymentDomainException;
import com.vehiclerental.payment.domain.model.vo.CustomerId;
import com.vehiclerental.payment.domain.model.vo.PaymentId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentDomainEventsTest {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final Instant OCCURRED_ON = Instant.now();
    private static final PaymentId PAYMENT_ID = new PaymentId(UUID.randomUUID());
    private static final UUID RESERVATION_ID = UUID.randomUUID();
    private static final CustomerId CUSTOMER_ID = new CustomerId(UUID.randomUUID());
    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Money AMOUNT = new Money(new BigDecimal("150.00"), EUR);

    // --- PaymentCompletedEvent ---

    @Test
    void completedEventFieldsAccessible() {
        var event = new PaymentCompletedEvent(EVENT_ID, OCCURRED_ON, PAYMENT_ID,
                RESERVATION_ID, CUSTOMER_ID, AMOUNT);

        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.occurredOn()).isEqualTo(OCCURRED_ON);
        assertThat(event.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(event.reservationId()).isEqualTo(RESERVATION_ID);
        assertThat(event.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(event.amount()).isEqualTo(AMOUNT);
    }

    @Test
    void completedEventNullEventIdThrows() {
        assertThatThrownBy(() -> new PaymentCompletedEvent(null, OCCURRED_ON, PAYMENT_ID,
                RESERVATION_ID, CUSTOMER_ID, AMOUNT))
                .isInstanceOf(PaymentDomainException.class);
    }

    @Test
    void completedEventNullOccurredOnThrows() {
        assertThatThrownBy(() -> new PaymentCompletedEvent(EVENT_ID, null, PAYMENT_ID,
                RESERVATION_ID, CUSTOMER_ID, AMOUNT))
                .isInstanceOf(PaymentDomainException.class);
    }

    @Test
    void completedEventImplementsDomainEvent() {
        var event = new PaymentCompletedEvent(EVENT_ID, OCCURRED_ON, PAYMENT_ID,
                RESERVATION_ID, CUSTOMER_ID, AMOUNT);

        assertThat(event).isInstanceOf(DomainEvent.class);
    }

    @Test
    void completedEventIsRecord() {
        assertThat(PaymentCompletedEvent.class).isRecord();
    }

    // --- PaymentFailedEvent ---

    @Test
    void failedEventFieldsAccessible() {
        var failureMessages = List.of("Card declined", "Insufficient funds");
        var event = new PaymentFailedEvent(EVENT_ID, OCCURRED_ON, PAYMENT_ID,
                RESERVATION_ID, failureMessages);

        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.occurredOn()).isEqualTo(OCCURRED_ON);
        assertThat(event.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(event.reservationId()).isEqualTo(RESERVATION_ID);
        assertThat(event.failureMessages()).isEqualTo(failureMessages);
    }

    @Test
    void failedEventNullEventIdThrows() {
        assertThatThrownBy(() -> new PaymentFailedEvent(null, OCCURRED_ON, PAYMENT_ID,
                RESERVATION_ID, List.of("error")))
                .isInstanceOf(PaymentDomainException.class);
    }

    @Test
    void failedEventNullOccurredOnThrows() {
        assertThatThrownBy(() -> new PaymentFailedEvent(EVENT_ID, null, PAYMENT_ID,
                RESERVATION_ID, List.of("error")))
                .isInstanceOf(PaymentDomainException.class);
    }

    @Test
    void failedEventImplementsDomainEvent() {
        var event = new PaymentFailedEvent(EVENT_ID, OCCURRED_ON, PAYMENT_ID,
                RESERVATION_ID, List.of("error"));

        assertThat(event).isInstanceOf(DomainEvent.class);
    }

    @Test
    void failedEventIsRecord() {
        assertThat(PaymentFailedEvent.class).isRecord();
    }

    // --- PaymentRefundedEvent ---

    @Test
    void refundedEventFieldsAccessible() {
        var event = new PaymentRefundedEvent(EVENT_ID, OCCURRED_ON, PAYMENT_ID,
                RESERVATION_ID, AMOUNT);

        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.occurredOn()).isEqualTo(OCCURRED_ON);
        assertThat(event.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(event.reservationId()).isEqualTo(RESERVATION_ID);
        assertThat(event.amount()).isEqualTo(AMOUNT);
    }

    @Test
    void refundedEventNullEventIdThrows() {
        assertThatThrownBy(() -> new PaymentRefundedEvent(null, OCCURRED_ON, PAYMENT_ID,
                RESERVATION_ID, AMOUNT))
                .isInstanceOf(PaymentDomainException.class);
    }

    @Test
    void refundedEventNullOccurredOnThrows() {
        assertThatThrownBy(() -> new PaymentRefundedEvent(EVENT_ID, null, PAYMENT_ID,
                RESERVATION_ID, AMOUNT))
                .isInstanceOf(PaymentDomainException.class);
    }

    @Test
    void refundedEventImplementsDomainEvent() {
        var event = new PaymentRefundedEvent(EVENT_ID, OCCURRED_ON, PAYMENT_ID,
                RESERVATION_ID, AMOUNT);

        assertThat(event).isInstanceOf(DomainEvent.class);
    }

    @Test
    void refundedEventIsRecord() {
        assertThat(PaymentRefundedEvent.class).isRecord();
    }
}
