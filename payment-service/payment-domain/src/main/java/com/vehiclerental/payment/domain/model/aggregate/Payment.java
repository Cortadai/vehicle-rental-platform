package com.vehiclerental.payment.domain.model.aggregate;

import com.vehiclerental.common.domain.entity.AggregateRoot;
import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.payment.domain.event.PaymentCompletedEvent;
import com.vehiclerental.payment.domain.event.PaymentFailedEvent;
import com.vehiclerental.payment.domain.event.PaymentRefundedEvent;
import com.vehiclerental.payment.domain.exception.PaymentDomainException;
import com.vehiclerental.payment.domain.model.vo.CustomerId;
import com.vehiclerental.payment.domain.model.vo.PaymentId;
import com.vehiclerental.payment.domain.model.vo.PaymentStatus;
import com.vehiclerental.payment.domain.model.vo.ReservationId;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Payment extends AggregateRoot<PaymentId> {

    private final ReservationId reservationId;
    private final CustomerId customerId;
    private final Money amount;
    private final Instant createdAt;
    private PaymentStatus status;
    private List<String> failureMessages;
    private Instant updatedAt;

    private Payment(PaymentId id, ReservationId reservationId, CustomerId customerId,
                    Money amount, PaymentStatus status, List<String> failureMessages,
                    Instant createdAt, Instant updatedAt) {
        super(id);
        this.reservationId = reservationId;
        this.customerId = customerId;
        this.amount = amount;
        this.status = status;
        this.failureMessages = failureMessages;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Payment create(ReservationId reservationId, CustomerId customerId, Money amount) {
        if (reservationId == null) {
            throw new PaymentDomainException("reservationId must not be null", "PAYMENT_RESERVATION_ID_REQUIRED");
        }
        if (customerId == null) {
            throw new PaymentDomainException("customerId must not be null", "PAYMENT_CUSTOMER_ID_REQUIRED");
        }
        if (amount == null) {
            throw new PaymentDomainException("amount must not be null", "PAYMENT_AMOUNT_REQUIRED");
        }
        if (amount.amount().signum() <= 0) {
            throw new PaymentDomainException("amount must be positive", "PAYMENT_AMOUNT_INVALID");
        }

        var paymentId = new PaymentId(UUID.randomUUID());
        var now = Instant.now();

        return new Payment(paymentId, reservationId, customerId, amount,
                PaymentStatus.PENDING, List.of(), now, now);
    }

    public static Payment reconstruct(PaymentId id, ReservationId reservationId, CustomerId customerId,
                                      Money amount, PaymentStatus status, List<String> failureMessages,
                                      Instant createdAt, Instant updatedAt) {
        return new Payment(id, reservationId, customerId, amount, status,
                List.copyOf(failureMessages), createdAt, updatedAt);
    }

    public void complete() {
        if (status != PaymentStatus.PENDING) {
            throw new PaymentDomainException(
                    "Cannot complete payment in state " + status, "PAYMENT_INVALID_STATE");
        }
        status = PaymentStatus.COMPLETED;
        updatedAt = Instant.now();
        registerDomainEvent(new PaymentCompletedEvent(
                UUID.randomUUID(), Instant.now(), getId(), reservationId, customerId, amount));
    }

    public void fail(List<String> failureMessages) {
        if (failureMessages == null || failureMessages.isEmpty()) {
            throw new PaymentDomainException(
                    "failureMessages must not be null or empty", "PAYMENT_FAILURE_MESSAGES_REQUIRED");
        }
        if (status != PaymentStatus.PENDING) {
            throw new PaymentDomainException(
                    "Cannot fail payment in state " + status, "PAYMENT_INVALID_STATE");
        }
        this.failureMessages = List.copyOf(failureMessages);
        status = PaymentStatus.FAILED;
        updatedAt = Instant.now();
        registerDomainEvent(new PaymentFailedEvent(
                UUID.randomUUID(), Instant.now(), getId(), reservationId, List.copyOf(failureMessages)));
    }

    public void refund() {
        if (status != PaymentStatus.COMPLETED) {
            throw new PaymentDomainException(
                    "Cannot refund payment in state " + status, "PAYMENT_INVALID_STATE");
        }
        status = PaymentStatus.REFUNDED;
        updatedAt = Instant.now();
        registerDomainEvent(new PaymentRefundedEvent(
                UUID.randomUUID(), Instant.now(), getId(), reservationId, amount));
    }

    public ReservationId getReservationId() {
        return reservationId;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public Money getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public List<String> getFailureMessages() {
        return Collections.unmodifiableList(failureMessages);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
