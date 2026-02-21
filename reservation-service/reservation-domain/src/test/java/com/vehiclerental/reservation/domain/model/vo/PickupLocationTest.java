package com.vehiclerental.reservation.domain.model.vo;

import com.vehiclerental.reservation.domain.exception.ReservationDomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PickupLocationTest {

    @Test
    void validConstruction() {
        var location = new PickupLocation("123 Main St", "Madrid");

        assertThat(location.address()).isEqualTo("123 Main St");
        assertThat(location.city()).isEqualTo("Madrid");
    }

    @Test
    void nullAddressRejected() {
        assertThatThrownBy(() -> new PickupLocation(null, "Madrid"))
                .isInstanceOf(ReservationDomainException.class);
    }

    @Test
    void blankAddressRejected() {
        assertThatThrownBy(() -> new PickupLocation("", "Madrid"))
                .isInstanceOf(ReservationDomainException.class);
        assertThatThrownBy(() -> new PickupLocation("   ", "Madrid"))
                .isInstanceOf(ReservationDomainException.class);
    }

    @Test
    void nullCityRejected() {
        assertThatThrownBy(() -> new PickupLocation("123 Main St", null))
                .isInstanceOf(ReservationDomainException.class);
    }

    @Test
    void blankCityRejected() {
        assertThatThrownBy(() -> new PickupLocation("123 Main St", ""))
                .isInstanceOf(ReservationDomainException.class);
        assertThatThrownBy(() -> new PickupLocation("123 Main St", "   "))
                .isInstanceOf(ReservationDomainException.class);
    }
}
