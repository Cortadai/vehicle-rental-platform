package com.vehiclerental.fleet.application.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VehicleNotFoundExceptionTest {

    @Test
    void messageContainsVehicleId() {
        var vehicleId = "abc-123";

        var exception = new VehicleNotFoundException(vehicleId);

        assertThat(exception.getMessage()).contains(vehicleId);
    }

    @Test
    void extendsRuntimeException() {
        var exception = new VehicleNotFoundException("some-id");

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void doesNotExtendFleetDomainException() {
        var exception = new VehicleNotFoundException("some-id");

        assertThat(exception)
                .isNotInstanceOf(com.vehiclerental.fleet.domain.exception.FleetDomainException.class);
    }
}
