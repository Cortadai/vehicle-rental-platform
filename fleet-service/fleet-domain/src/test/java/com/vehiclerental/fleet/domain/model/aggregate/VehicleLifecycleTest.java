package com.vehiclerental.fleet.domain.model.aggregate;

import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.fleet.domain.event.VehicleActivatedEvent;
import com.vehiclerental.fleet.domain.event.VehicleRetiredEvent;
import com.vehiclerental.fleet.domain.event.VehicleSentToMaintenanceEvent;
import com.vehiclerental.fleet.domain.exception.FleetDomainException;
import com.vehiclerental.fleet.domain.model.vo.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VehicleLifecycleTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final LicensePlate VALID_PLATE = new LicensePlate("1234-BCD");
    private static final DailyRate VALID_RATE = new DailyRate(new Money(new BigDecimal("50.00"), EUR));

    private Vehicle createActiveVehicle() {
        var vehicle = Vehicle.create(VALID_PLATE, "Toyota", "Corolla", 2023,
                VehicleCategory.SEDAN, VALID_RATE, null);
        vehicle.clearDomainEvents();
        return vehicle;
    }

    private Vehicle createUnderMaintenanceVehicle() {
        var vehicle = createActiveVehicle();
        vehicle.sendToMaintenance();
        vehicle.clearDomainEvents();
        return vehicle;
    }

    private Vehicle createRetiredVehicle() {
        var vehicle = createActiveVehicle();
        vehicle.retire();
        vehicle.clearDomainEvents();
        return vehicle;
    }

    // --- sendToMaintenance ---

    @Test
    void sendToMaintenanceOnActive() {
        var vehicle = createActiveVehicle();

        vehicle.sendToMaintenance();

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.UNDER_MAINTENANCE);
        assertThat(vehicle.getDomainEvents()).hasSize(1);
        assertThat(vehicle.getDomainEvents().get(0)).isInstanceOf(VehicleSentToMaintenanceEvent.class);
    }

    @Test
    void sendToMaintenanceOnNonActiveThrows() {
        var underMaintenance = createUnderMaintenanceVehicle();
        assertThatThrownBy(underMaintenance::sendToMaintenance)
                .isInstanceOf(FleetDomainException.class);

        var retired = createRetiredVehicle();
        assertThatThrownBy(retired::sendToMaintenance)
                .isInstanceOf(FleetDomainException.class);
    }

    // --- activate ---

    @Test
    void activateOnUnderMaintenance() {
        var vehicle = createUnderMaintenanceVehicle();

        vehicle.activate();

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.ACTIVE);
        assertThat(vehicle.getDomainEvents()).hasSize(1);
        assertThat(vehicle.getDomainEvents().get(0)).isInstanceOf(VehicleActivatedEvent.class);
    }

    @Test
    void activateOnNonMaintenanceThrows() {
        var active = createActiveVehicle();
        assertThatThrownBy(active::activate)
                .isInstanceOf(FleetDomainException.class);

        var retired = createRetiredVehicle();
        assertThatThrownBy(retired::activate)
                .isInstanceOf(FleetDomainException.class);
    }

    // --- retire ---

    @Test
    void retireActiveVehicle() {
        var vehicle = createActiveVehicle();

        vehicle.retire();

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.RETIRED);
        assertThat(vehicle.getDomainEvents()).hasSize(1);
        assertThat(vehicle.getDomainEvents().get(0)).isInstanceOf(VehicleRetiredEvent.class);
    }

    @Test
    void retireUnderMaintenanceVehicle() {
        var vehicle = createUnderMaintenanceVehicle();

        vehicle.retire();

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.RETIRED);
        assertThat(vehicle.getDomainEvents()).hasSize(1);
        assertThat(vehicle.getDomainEvents().get(0)).isInstanceOf(VehicleRetiredEvent.class);
    }

    @Test
    void retireAlreadyRetiredThrows() {
        var vehicle = createRetiredVehicle();

        assertThatThrownBy(vehicle::retire)
                .isInstanceOf(FleetDomainException.class);
    }

    // --- isAvailable ---

    @Test
    void isAvailableTrueForActive() {
        var vehicle = createActiveVehicle();

        assertThat(vehicle.isAvailable()).isTrue();
    }

    @Test
    void isAvailableFalseForNonActive() {
        assertThat(createUnderMaintenanceVehicle().isAvailable()).isFalse();
        assertThat(createRetiredVehicle().isAvailable()).isFalse();
    }

    // --- Exception details ---

    @Test
    void exceptionCarriesErrorCode() {
        var retired = createRetiredVehicle();

        assertThatThrownBy(retired::sendToMaintenance)
                .isInstanceOf(FleetDomainException.class)
                .satisfies(ex -> assertThat(((FleetDomainException) ex).getErrorCode()).isNotBlank());
    }

    @Test
    void exceptionMessageIncludesCurrentState() {
        var retired = createRetiredVehicle();

        assertThatThrownBy(retired::sendToMaintenance)
                .isInstanceOf(FleetDomainException.class)
                .hasMessageContaining("RETIRED");
    }
}
