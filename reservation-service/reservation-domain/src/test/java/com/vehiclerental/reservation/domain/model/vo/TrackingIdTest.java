package com.vehiclerental.reservation.domain.model.vo;

import com.vehiclerental.reservation.domain.exception.ReservationDomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrackingIdTest {

    @Test
    void validConstruction() {
        UUID uuid = UUID.randomUUID();
        var trackingId = new TrackingId(uuid);

        assertThat(trackingId.value()).isEqualTo(uuid);
    }

    @Test
    void nullUuidRejected() {
        assertThatThrownBy(() -> new TrackingId(null))
                .isInstanceOf(ReservationDomainException.class);
    }

    @Test
    void equalityByValue() {
        UUID uuid = UUID.randomUUID();
        var id1 = new TrackingId(uuid);
        var id2 = new TrackingId(uuid);

        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }
}
