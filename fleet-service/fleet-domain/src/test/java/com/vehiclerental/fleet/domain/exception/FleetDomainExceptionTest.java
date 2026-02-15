package com.vehiclerental.fleet.domain.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FleetDomainExceptionTest {

    @Test
    void errorCodeAccessible() {
        var exception = new FleetDomainException("Vehicle not found", "VEHICLE_NOT_FOUND");

        assertThat(exception.getErrorCode()).isEqualTo("VEHICLE_NOT_FOUND");
    }

    @Test
    void messageAccessible() {
        var exception = new FleetDomainException("Vehicle not found", "VEHICLE_NOT_FOUND");

        assertThat(exception.getMessage()).isEqualTo("Vehicle not found");
    }

    @Test
    void constructorWithCause() {
        var cause = new RuntimeException("underlying error");
        var exception = new FleetDomainException("Wrapped error", "INTERNAL_ERROR", cause);

        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getErrorCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(exception.getMessage()).isEqualTo("Wrapped error");
    }
}
