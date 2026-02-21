package com.vehiclerental.reservation.domain.model.aggregate;

import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.reservation.domain.event.ReservationCancelledEvent;
import com.vehiclerental.reservation.domain.exception.ReservationDomainException;
import com.vehiclerental.reservation.domain.model.entity.ReservationItem;
import com.vehiclerental.reservation.domain.model.vo.CustomerId;
import com.vehiclerental.reservation.domain.model.vo.DateRange;
import com.vehiclerental.reservation.domain.model.vo.PickupLocation;
import com.vehiclerental.reservation.domain.model.vo.ReservationStatus;
import com.vehiclerental.reservation.domain.model.vo.VehicleId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationLifecycleTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final CustomerId CUSTOMER_ID = new CustomerId(UUID.randomUUID());
    private static final PickupLocation PICKUP_LOCATION = new PickupLocation("123 Main St", "Madrid");
    private static final PickupLocation RETURN_LOCATION = new PickupLocation("456 Oak Ave", "Barcelona");
    private static final DateRange DATE_RANGE = new DateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 4));
    private static final Money DAILY_RATE = new Money(new BigDecimal("100.00"), EUR);

    private Reservation createPendingReservation() {
        var items = List.of(ReservationItem.create(new VehicleId(UUID.randomUUID()), DAILY_RATE, 3));
        var reservation = Reservation.create(CUSTOMER_ID, PICKUP_LOCATION, RETURN_LOCATION, DATE_RANGE, items);
        reservation.clearDomainEvents();
        return reservation;
    }

    private Reservation createCustomerValidatedReservation() {
        var reservation = createPendingReservation();
        reservation.validateCustomer();
        reservation.clearDomainEvents();
        return reservation;
    }

    private Reservation createPaidReservation() {
        var reservation = createCustomerValidatedReservation();
        reservation.pay();
        reservation.clearDomainEvents();
        return reservation;
    }

    private Reservation createConfirmedReservation() {
        var reservation = createPaidReservation();
        reservation.confirm();
        reservation.clearDomainEvents();
        return reservation;
    }

    private Reservation createCancellingReservation() {
        var reservation = createPaidReservation();
        reservation.initCancel(List.of("Fleet unavailable"));
        reservation.clearDomainEvents();
        return reservation;
    }

    private Reservation createCancelledReservation() {
        var reservation = createPendingReservation();
        reservation.cancel();
        reservation.clearDomainEvents();
        return reservation;
    }

    // --- validateCustomer ---

    @Test
    void validateCustomerFromPending() {
        var reservation = createPendingReservation();
        var updatedAtBefore = reservation.getUpdatedAt();

        reservation.validateCustomer();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CUSTOMER_VALIDATED);
        assertThat(reservation.getUpdatedAt()).isAfterOrEqualTo(updatedAtBefore);
    }

    @Test
    void validateCustomerFromNonPendingThrows() {
        assertThatThrownBy(() -> createCustomerValidatedReservation().validateCustomer())
                .isInstanceOf(ReservationDomainException.class);
        assertThatThrownBy(() -> createPaidReservation().validateCustomer())
                .isInstanceOf(ReservationDomainException.class);
        assertThatThrownBy(() -> createConfirmedReservation().validateCustomer())
                .isInstanceOf(ReservationDomainException.class);
        assertThatThrownBy(() -> createCancellingReservation().validateCustomer())
                .isInstanceOf(ReservationDomainException.class);
        assertThatThrownBy(() -> createCancelledReservation().validateCustomer())
                .isInstanceOf(ReservationDomainException.class);
    }

    // --- pay ---

    @Test
    void payFromCustomerValidated() {
        var reservation = createCustomerValidatedReservation();
        var updatedAtBefore = reservation.getUpdatedAt();

        reservation.pay();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PAID);
        assertThat(reservation.getUpdatedAt()).isAfterOrEqualTo(updatedAtBefore);
    }

    @Test
    void payFromNonCustomerValidatedThrows() {
        assertThatThrownBy(() -> createPendingReservation().pay())
                .isInstanceOf(ReservationDomainException.class);
        assertThatThrownBy(() -> createPaidReservation().pay())
                .isInstanceOf(ReservationDomainException.class);
        assertThatThrownBy(() -> createConfirmedReservation().pay())
                .isInstanceOf(ReservationDomainException.class);
        assertThatThrownBy(() -> createCancellingReservation().pay())
                .isInstanceOf(ReservationDomainException.class);
        assertThatThrownBy(() -> createCancelledReservation().pay())
                .isInstanceOf(ReservationDomainException.class);
    }

    // --- confirm ---

    @Test
    void confirmFromPaid() {
        var reservation = createPaidReservation();
        var updatedAtBefore = reservation.getUpdatedAt();

        reservation.confirm();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservation.getUpdatedAt()).isAfterOrEqualTo(updatedAtBefore);
    }

    @Test
    void confirmFromNonPaidThrows() {
        assertThatThrownBy(() -> createPendingReservation().confirm())
                .isInstanceOf(ReservationDomainException.class);
        assertThatThrownBy(() -> createCustomerValidatedReservation().confirm())
                .isInstanceOf(ReservationDomainException.class);
        assertThatThrownBy(() -> createConfirmedReservation().confirm())
                .isInstanceOf(ReservationDomainException.class);
        assertThatThrownBy(() -> createCancellingReservation().confirm())
                .isInstanceOf(ReservationDomainException.class);
        assertThatThrownBy(() -> createCancelledReservation().confirm())
                .isInstanceOf(ReservationDomainException.class);
    }

    // --- initCancel ---

    @Test
    void initCancelFromPaid() {
        var reservation = createPaidReservation();
        var updatedAtBefore = reservation.getUpdatedAt();

        reservation.initCancel(List.of("Fleet unavailable"));

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLING);
        assertThat(reservation.getFailureMessages()).containsExactly("Fleet unavailable");
        assertThat(reservation.getUpdatedAt()).isAfterOrEqualTo(updatedAtBefore);
    }

    @Test
    void initCancelFromNonPaidThrows() {
        assertThatThrownBy(() -> createPendingReservation().initCancel(List.of("error")))
                .isInstanceOf(ReservationDomainException.class);
        assertThatThrownBy(() -> createCustomerValidatedReservation().initCancel(List.of("error")))
                .isInstanceOf(ReservationDomainException.class);
        assertThatThrownBy(() -> createConfirmedReservation().initCancel(List.of("error")))
                .isInstanceOf(ReservationDomainException.class);
        assertThatThrownBy(() -> createCancellingReservation().initCancel(List.of("error")))
                .isInstanceOf(ReservationDomainException.class);
        assertThatThrownBy(() -> createCancelledReservation().initCancel(List.of("error")))
                .isInstanceOf(ReservationDomainException.class);
    }

    @Test
    void initCancelWithNullMessagesRejected() {
        var reservation = createPaidReservation();
        assertThatThrownBy(() -> reservation.initCancel(null))
                .isInstanceOf(ReservationDomainException.class);
    }

    @Test
    void initCancelWithEmptyMessagesRejected() {
        var reservation = createPaidReservation();
        assertThatThrownBy(() -> reservation.initCancel(List.of()))
                .isInstanceOf(ReservationDomainException.class);
    }

    // --- cancel ---

    @Test
    void cancelFromPending() {
        var reservation = createPendingReservation();

        reservation.cancel();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.getDomainEvents()).hasSize(1);
        assertThat(reservation.getDomainEvents().get(0)).isInstanceOf(ReservationCancelledEvent.class);
        var event = (ReservationCancelledEvent) reservation.getDomainEvents().get(0);
        assertThat(event.failureMessages()).isEmpty();
    }

    @Test
    void cancelFromCustomerValidated() {
        var reservation = createCustomerValidatedReservation();

        reservation.cancel();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.getDomainEvents()).hasSize(1);
        assertThat(reservation.getDomainEvents().get(0)).isInstanceOf(ReservationCancelledEvent.class);
    }

    @Test
    void cancelFromCancellingPreservesFailureMessages() {
        var reservation = createCancellingReservation();

        reservation.cancel();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.getDomainEvents()).hasSize(1);
        var event = (ReservationCancelledEvent) reservation.getDomainEvents().get(0);
        assertThat(event.failureMessages()).containsExactly("Fleet unavailable");
    }

    @Test
    void cancelFromConfirmedRejected() {
        assertThatThrownBy(() -> createConfirmedReservation().cancel())
                .isInstanceOf(ReservationDomainException.class);
    }

    @Test
    void cancelFromPaidRejected() {
        assertThatThrownBy(() -> createPaidReservation().cancel())
                .isInstanceOf(ReservationDomainException.class);
    }

    @Test
    void cancelFromCancelledRejected() {
        assertThatThrownBy(() -> createCancelledReservation().cancel())
                .isInstanceOf(ReservationDomainException.class);
    }
}
