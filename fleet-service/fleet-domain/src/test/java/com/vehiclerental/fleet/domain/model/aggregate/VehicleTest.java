package com.vehiclerental.fleet.domain.model.aggregate;

import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.fleet.domain.event.VehicleRegisteredEvent;
import com.vehiclerental.fleet.domain.exception.FleetDomainException;
import com.vehiclerental.fleet.domain.model.vo.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VehicleTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final LicensePlate VALID_PLATE = new LicensePlate("1234-BCD");
    private static final DailyRate VALID_RATE = new DailyRate(new Money(new BigDecimal("50.00"), EUR));

    @Test
    void successfulCreation() {
        var vehicle = Vehicle.create(VALID_PLATE, "Toyota", "Corolla", 2023,
                VehicleCategory.SEDAN, VALID_RATE, "GPS integrado");

        assertThat(vehicle.getId()).isNotNull();
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.ACTIVE);
        assertThat(vehicle.getDomainEvents()).hasSize(1);
        assertThat(vehicle.getDomainEvents().get(0)).isInstanceOf(VehicleRegisteredEvent.class);
    }

    @Test
    void nullMakeRejected() {
        assertThatThrownBy(() -> Vehicle.create(VALID_PLATE, null, "Corolla", 2023,
                VehicleCategory.SEDAN, VALID_RATE, null))
                .isInstanceOf(FleetDomainException.class);
    }

    @Test
    void blankMakeRejected() {
        assertThatThrownBy(() -> Vehicle.create(VALID_PLATE, "", "Corolla", 2023,
                VehicleCategory.SEDAN, VALID_RATE, null))
                .isInstanceOf(FleetDomainException.class);
        assertThatThrownBy(() -> Vehicle.create(VALID_PLATE, "   ", "Corolla", 2023,
                VehicleCategory.SEDAN, VALID_RATE, null))
                .isInstanceOf(FleetDomainException.class);
    }

    @Test
    void nullModelRejected() {
        assertThatThrownBy(() -> Vehicle.create(VALID_PLATE, "Toyota", null, 2023,
                VehicleCategory.SEDAN, VALID_RATE, null))
                .isInstanceOf(FleetDomainException.class);
    }

    @Test
    void blankModelRejected() {
        assertThatThrownBy(() -> Vehicle.create(VALID_PLATE, "Toyota", "", 2023,
                VehicleCategory.SEDAN, VALID_RATE, null))
                .isInstanceOf(FleetDomainException.class);
    }

    @Test
    void nullLicensePlateRejected() {
        assertThatThrownBy(() -> Vehicle.create(null, "Toyota", "Corolla", 2023,
                VehicleCategory.SEDAN, VALID_RATE, null))
                .isInstanceOf(FleetDomainException.class);
    }

    @Test
    void nullCategoryRejected() {
        assertThatThrownBy(() -> Vehicle.create(VALID_PLATE, "Toyota", "Corolla", 2023,
                null, VALID_RATE, null))
                .isInstanceOf(FleetDomainException.class);
    }

    @Test
    void nullDailyRateRejected() {
        assertThatThrownBy(() -> Vehicle.create(VALID_PLATE, "Toyota", "Corolla", 2023,
                VehicleCategory.SEDAN, null, null))
                .isInstanceOf(FleetDomainException.class);
    }

    @Test
    void yearBelowMinimumRejected() {
        assertThatThrownBy(() -> Vehicle.create(VALID_PLATE, "Toyota", "Corolla", 1949,
                VehicleCategory.SEDAN, VALID_RATE, null))
                .isInstanceOf(FleetDomainException.class);
    }

    @Test
    void yearAboveMaximumRejected() {
        int futureYear = java.time.Year.now().getValue() + 2;
        assertThatThrownBy(() -> Vehicle.create(VALID_PLATE, "Toyota", "Corolla", futureYear,
                VehicleCategory.SEDAN, VALID_RATE, null))
                .isInstanceOf(FleetDomainException.class);
    }

    @Test
    void nullDescriptionAccepted() {
        var vehicle = Vehicle.create(VALID_PLATE, "Toyota", "Corolla", 2023,
                VehicleCategory.SEDAN, VALID_RATE, null);

        assertThat(vehicle.getDescription()).isNull();
    }

    @Test
    void descriptionExceeding500CharsRejected() {
        var longDescription = "A".repeat(501);
        assertThatThrownBy(() -> Vehicle.create(VALID_PLATE, "Toyota", "Corolla", 2023,
                VehicleCategory.SEDAN, VALID_RATE, longDescription))
                .isInstanceOf(FleetDomainException.class);
    }

    @Test
    void fieldsAccessibleAfterCreation() {
        var vehicle = Vehicle.create(VALID_PLATE, "Toyota", "Corolla", 2023,
                VehicleCategory.SEDAN, VALID_RATE, "GPS integrado");

        assertThat(vehicle.getLicensePlate()).isEqualTo(VALID_PLATE);
        assertThat(vehicle.getMake()).isEqualTo("Toyota");
        assertThat(vehicle.getModel()).isEqualTo("Corolla");
        assertThat(vehicle.getYear()).isEqualTo(2023);
        assertThat(vehicle.getCategory()).isEqualTo(VehicleCategory.SEDAN);
        assertThat(vehicle.getDailyRate()).isEqualTo(VALID_RATE);
        assertThat(vehicle.getDescription()).isEqualTo("GPS integrado");
        assertThat(vehicle.getCreatedAt()).isNotNull();
    }

    @Test
    void reconstructDoesNotEmitEvents() {
        var vehicleId = new VehicleId(UUID.randomUUID());
        var createdAt = Instant.now();

        var vehicle = Vehicle.reconstruct(vehicleId, VALID_PLATE, "Toyota", "Corolla", 2023,
                VehicleCategory.SEDAN, VALID_RATE, "GPS integrado", VehicleStatus.ACTIVE, createdAt);

        assertThat(vehicle.getDomainEvents()).isEmpty();
        assertThat(vehicle.getId()).isEqualTo(vehicleId);
        assertThat(vehicle.getLicensePlate()).isEqualTo(VALID_PLATE);
        assertThat(vehicle.getMake()).isEqualTo("Toyota");
        assertThat(vehicle.getModel()).isEqualTo("Corolla");
        assertThat(vehicle.getYear()).isEqualTo(2023);
        assertThat(vehicle.getCategory()).isEqualTo(VehicleCategory.SEDAN);
        assertThat(vehicle.getDailyRate()).isEqualTo(VALID_RATE);
        assertThat(vehicle.getDescription()).isEqualTo("GPS integrado");
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.ACTIVE);
        assertThat(vehicle.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void noPublicConstructors() {
        var constructors = Vehicle.class.getDeclaredConstructors();

        assertThat(Arrays.stream(constructors).noneMatch(c -> Modifier.isPublic(c.getModifiers()))).isTrue();
    }
}
