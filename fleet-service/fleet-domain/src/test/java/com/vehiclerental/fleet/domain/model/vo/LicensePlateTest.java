package com.vehiclerental.fleet.domain.model.vo;

import com.vehiclerental.fleet.domain.exception.FleetDomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LicensePlateTest {

    @Test
    void validPlateAccepted() {
        var plate = new LicensePlate("1234-BCD");

        assertThat(plate.value()).isEqualTo("1234-BCD");
    }

    @Test
    void spacesAccepted() {
        var plate = new LicensePlate("ABC 1234");

        assertThat(plate.value()).isEqualTo("ABC 1234");
    }

    @Test
    void nullRejected() {
        assertThatThrownBy(() -> new LicensePlate(null))
                .isInstanceOf(FleetDomainException.class);
    }

    @Test
    void blankRejected() {
        assertThatThrownBy(() -> new LicensePlate(""))
                .isInstanceOf(FleetDomainException.class);
        assertThatThrownBy(() -> new LicensePlate("   "))
                .isInstanceOf(FleetDomainException.class);
    }

    @Test
    void tooShortRejected() {
        assertThatThrownBy(() -> new LicensePlate("A"))
                .isInstanceOf(FleetDomainException.class);
    }

    @Test
    void tooLongRejected() {
        assertThatThrownBy(() -> new LicensePlate("ABCDEFGHIJKLMNOP"))
                .isInstanceOf(FleetDomainException.class);
    }

    @Test
    void specialCharactersRejected() {
        assertThatThrownBy(() -> new LicensePlate("AB@#123"))
                .isInstanceOf(FleetDomainException.class);
    }
}
