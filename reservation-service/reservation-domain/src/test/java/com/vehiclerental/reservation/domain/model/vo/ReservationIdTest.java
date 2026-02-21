package com.vehiclerental.reservation.domain.model.vo;

import com.vehiclerental.reservation.domain.exception.ReservationDomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationIdTest {

    @Test
    void validConstruction() {
        UUID uuid = UUID.randomUUID();
        var reservationId = new ReservationId(uuid);

        assertThat(reservationId.value()).isEqualTo(uuid);
    }

    @Test
    void nullUuidRejected() {
        assertThatThrownBy(() -> new ReservationId(null))
                .isInstanceOf(ReservationDomainException.class);
    }

    @Test
    void equalityByValue() {
        UUID uuid = UUID.randomUUID();
        var id1 = new ReservationId(uuid);
        var id2 = new ReservationId(uuid);

        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }
}
