package com.vehiclerental.payment;

import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.payment.domain.model.aggregate.Payment;
import com.vehiclerental.payment.domain.model.vo.CustomerId;
import com.vehiclerental.payment.domain.model.vo.PaymentId;
import com.vehiclerental.payment.domain.model.vo.ReservationId;
import com.vehiclerental.payment.domain.port.output.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class PaymentRepositoryAdapterIT {

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void saveAndFindByIdRoundTrip() {
        var payment = Payment.create(
                new ReservationId(UUID.randomUUID()),
                new CustomerId(UUID.randomUUID()),
                new Money(new BigDecimal("100.00"), Currency.getInstance("USD")));
        payment.complete();
        payment.clearDomainEvents();

        var saved = paymentRepository.save(payment);

        var found = paymentRepository.findById(saved.getId());

        assertThat(found).isPresent();
        var loaded = found.get();
        assertThat(loaded.getId()).isEqualTo(saved.getId());
        assertThat(loaded.getReservationId()).isEqualTo(payment.getReservationId());
        assertThat(loaded.getCustomerId()).isEqualTo(payment.getCustomerId());
        assertThat(loaded.getAmount().amount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(loaded.getAmount().currency()).isEqualTo(Currency.getInstance("USD"));
        assertThat(loaded.getStatus().name()).isEqualTo("COMPLETED");
        assertThat(loaded.getFailureMessages()).isEmpty();
        assertThat(loaded.getCreatedAt()).isCloseTo(saved.getCreatedAt(), within(1, ChronoUnit.MICROS));
        assertThat(loaded.getUpdatedAt()).isCloseTo(saved.getUpdatedAt(), within(1, ChronoUnit.MICROS));
    }

    @Test
    void findByIdReturnsEmptyForNonExisting() {
        var nonExistingId = new PaymentId(UUID.randomUUID());

        var result = paymentRepository.findById(nonExistingId);

        assertThat(result).isEmpty();
    }

    @Test
    void findByReservationIdReturnsPayment() {
        var reservationId = new ReservationId(UUID.randomUUID());
        var payment = Payment.create(
                reservationId,
                new CustomerId(UUID.randomUUID()),
                new Money(new BigDecimal("250.50"), Currency.getInstance("EUR")));
        payment.complete();
        payment.clearDomainEvents();

        paymentRepository.save(payment);

        var found = paymentRepository.findByReservationId(reservationId);

        assertThat(found).isPresent();
        assertThat(found.get().getReservationId()).isEqualTo(reservationId);
    }

    @Test
    void findByReservationIdReturnsEmptyForNonExisting() {
        var nonExistingReservationId = new ReservationId(UUID.randomUUID());

        var result = paymentRepository.findByReservationId(nonExistingReservationId);

        assertThat(result).isEmpty();
    }

    @Test
    void savePersistsAllFieldsIncludingFailureMessagesJsonSerialization() {
        var payment = Payment.create(
                new ReservationId(UUID.randomUUID()),
                new CustomerId(UUID.randomUUID()),
                new Money(new BigDecimal("75.00"), Currency.getInstance("USD")));
        payment.fail(List.of("Gateway timeout", "Insufficient funds"));
        payment.clearDomainEvents();

        var saved = paymentRepository.save(payment);
        var found = paymentRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getStatus().name()).isEqualTo("FAILED");
        assertThat(found.get().getFailureMessages()).containsExactly("Gateway timeout", "Insufficient funds");
    }

    @Test
    void uniqueConstraintOnReservationIdPreventsDuplicatePayments() {
        var reservationId = new ReservationId(UUID.randomUUID());

        var payment1 = Payment.create(
                reservationId,
                new CustomerId(UUID.randomUUID()),
                new Money(new BigDecimal("100.00"), Currency.getInstance("USD")));
        payment1.complete();
        payment1.clearDomainEvents();
        paymentRepository.save(payment1);

        var payment2 = Payment.create(
                reservationId,
                new CustomerId(UUID.randomUUID()),
                new Money(new BigDecimal("200.00"), Currency.getInstance("USD")));
        payment2.complete();
        payment2.clearDomainEvents();

        assertThatThrownBy(() -> paymentRepository.save(payment2))
                .isInstanceOf(Exception.class);
    }
}
