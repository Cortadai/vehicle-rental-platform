package com.vehiclerental.reservation.domain.model.entity;

import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.reservation.domain.exception.ReservationDomainException;
import com.vehiclerental.reservation.domain.model.vo.VehicleId;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationItemTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final VehicleId VEHICLE_ID = new VehicleId(UUID.randomUUID());
    private static final Money DAILY_RATE = new Money(new BigDecimal("100.00"), EUR);

    @Test
    void successfulCreation() {
        var item = ReservationItem.create(VEHICLE_ID, DAILY_RATE, 3);

        assertThat(item.getId()).isNotNull();
        assertThat(item.getVehicleId()).isEqualTo(VEHICLE_ID);
        assertThat(item.getDailyRate()).isEqualTo(DAILY_RATE);
        assertThat(item.getDays()).isEqualTo(3);
        assertThat(item.getSubtotal()).isEqualTo(new Money(new BigDecimal("300.00"), EUR));
    }

    @Test
    void singleDayCalculation() {
        var item = ReservationItem.create(VEHICLE_ID, DAILY_RATE, 1);

        assertThat(item.getSubtotal()).isEqualTo(DAILY_RATE);
    }

    @Test
    void nullVehicleIdRejected() {
        assertThatThrownBy(() -> ReservationItem.create(null, DAILY_RATE, 3))
                .isInstanceOf(ReservationDomainException.class);
    }

    @Test
    void nullDailyRateRejected() {
        assertThatThrownBy(() -> ReservationItem.create(VEHICLE_ID, null, 3))
                .isInstanceOf(ReservationDomainException.class);
    }

    @Test
    void zeroDailyRateAmountRejected() {
        var zeroDailyRate = new Money(new BigDecimal("0.00"), EUR);
        assertThatThrownBy(() -> ReservationItem.create(VEHICLE_ID, zeroDailyRate, 3))
                .isInstanceOf(ReservationDomainException.class);
    }

    @Test
    void zeroDaysRejected() {
        assertThatThrownBy(() -> ReservationItem.create(VEHICLE_ID, DAILY_RATE, 0))
                .isInstanceOf(ReservationDomainException.class);
    }

    @Test
    void negativeDaysRejected() {
        assertThatThrownBy(() -> ReservationItem.create(VEHICLE_ID, DAILY_RATE, -1))
                .isInstanceOf(ReservationDomainException.class);
    }

    @Test
    void reconstructPreservesAllFields() {
        UUID id = UUID.randomUUID();
        var subtotal = new Money(new BigDecimal("300.00"), EUR);

        var item = ReservationItem.reconstruct(id, VEHICLE_ID, DAILY_RATE, 3, subtotal);

        assertThat(item.getId()).isEqualTo(id);
        assertThat(item.getVehicleId()).isEqualTo(VEHICLE_ID);
        assertThat(item.getDailyRate()).isEqualTo(DAILY_RATE);
        assertThat(item.getDays()).isEqualTo(3);
        assertThat(item.getSubtotal()).isEqualTo(subtotal);
    }

    @Test
    void noPublicConstructors() {
        var constructors = ReservationItem.class.getDeclaredConstructors();

        assertThat(Arrays.stream(constructors).noneMatch(c -> Modifier.isPublic(c.getModifiers()))).isTrue();
    }
}
