package com.vehiclerental.reservation.domain.exception;

import com.vehiclerental.common.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationDomainExceptionTest {

    @Test
    void errorCodeAccessible() {
        var exception = new ReservationDomainException("something went wrong", "RESERVATION_ERROR");

        assertThat(exception.getErrorCode()).isEqualTo("RESERVATION_ERROR");
    }

    @Test
    void messageAccessible() {
        var exception = new ReservationDomainException("something went wrong", "RESERVATION_ERROR");

        assertThat(exception.getMessage()).isEqualTo("something went wrong");
    }

    @Test
    void constructorWithCause() {
        var cause = new RuntimeException("root cause");
        var exception = new ReservationDomainException("wrapped", "RESERVATION_ERROR", cause);

        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getErrorCode()).isEqualTo("RESERVATION_ERROR");
        assertThat(exception.getMessage()).isEqualTo("wrapped");
    }

    @Test
    void extendsDomainException() {
        var exception = new ReservationDomainException("test", "CODE");

        assertThat(exception).isInstanceOf(DomainException.class);
    }
}
