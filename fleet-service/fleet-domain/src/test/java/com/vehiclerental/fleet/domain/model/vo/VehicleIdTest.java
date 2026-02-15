package com.vehiclerental.fleet.domain.model.vo;

import com.vehiclerental.fleet.domain.exception.FleetDomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VehicleIdTest {

    @Test
    void validConstruction() {
        var uuid = UUID.randomUUID();

        var vehicleId = new VehicleId(uuid);

        assertThat(vehicleId.value()).isEqualTo(uuid);
    }

    @Test
    void nullUuidRejected() {
        assertThatThrownBy(() -> new VehicleId(null))
                .isInstanceOf(FleetDomainException.class);
    }

    @Test
    void equalityByValue() {
        var uuid = UUID.randomUUID();

        var id1 = new VehicleId(uuid);
        var id2 = new VehicleId(uuid);

        assertThat(id1).isEqualTo(id2);
    }
}
