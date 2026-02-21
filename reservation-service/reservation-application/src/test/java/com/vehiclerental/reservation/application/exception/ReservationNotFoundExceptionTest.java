package com.vehiclerental.reservation.application.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationNotFoundExceptionTest {

    @Test
    void messageContainsTrackingId() {
        String trackingId = "abc-123-def";

        var exception = new ReservationNotFoundException(trackingId);

        assertThat(exception.getMessage()).contains(trackingId);
        assertThat(exception.getTrackingId()).isEqualTo(trackingId);
    }

    @Test
    void extendsRuntimeExceptionNotDomainException() {
        var exception = new ReservationNotFoundException("some-id");

        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception.getClass().getSuperclass()).isEqualTo(RuntimeException.class);
    }
}
