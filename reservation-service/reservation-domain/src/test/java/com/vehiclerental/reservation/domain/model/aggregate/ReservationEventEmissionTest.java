package com.vehiclerental.reservation.domain.model.aggregate;

import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.reservation.domain.model.entity.ReservationItem;
import com.vehiclerental.reservation.domain.model.vo.CustomerId;
import com.vehiclerental.reservation.domain.model.vo.DateRange;
import com.vehiclerental.reservation.domain.model.vo.PickupLocation;
import com.vehiclerental.reservation.domain.model.vo.VehicleId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationEventEmissionTest {

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

    @Test
    void validateCustomerDoesNotRegisterNewEvents() {
        var reservation = createPendingReservation();

        reservation.validateCustomer();

        assertThat(reservation.getDomainEvents()).isEmpty();
    }

    @Test
    void payDoesNotRegisterEvents() {
        var reservation = createPendingReservation();
        reservation.validateCustomer();
        reservation.clearDomainEvents();

        reservation.pay();

        assertThat(reservation.getDomainEvents()).isEmpty();
    }

    @Test
    void confirmDoesNotRegisterEvents() {
        var reservation = createPendingReservation();
        reservation.validateCustomer();
        reservation.pay();
        reservation.clearDomainEvents();

        reservation.confirm();

        assertThat(reservation.getDomainEvents()).isEmpty();
    }

    @Test
    void initCancelDoesNotRegisterEvents() {
        var reservation = createPendingReservation();
        reservation.validateCustomer();
        reservation.pay();
        reservation.clearDomainEvents();

        reservation.initCancel(List.of("Fleet unavailable"));

        assertThat(reservation.getDomainEvents()).isEmpty();
    }
}
