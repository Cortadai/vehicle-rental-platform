package com.vehiclerental.fleet.domain.event;

import com.vehiclerental.common.domain.event.DomainEvent;
import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.fleet.domain.model.vo.VehicleCategory;
import com.vehiclerental.fleet.domain.model.vo.VehicleId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FleetDomainEventsTest {

    private static final VehicleId VEHICLE_ID = new VehicleId(UUID.randomUUID());
    private static final Currency EUR = Currency.getInstance("EUR");

    @Test
    void vehicleRegisteredEventFieldsAccessible() {
        var money = new Money(new BigDecimal("50.00"), EUR);
        var event = new VehicleRegisteredEvent(
                UUID.randomUUID(), Instant.now(), VEHICLE_ID,
                "1234-BCD", "Toyota", "Corolla", 2023,
                VehicleCategory.SEDAN, money, "GPS integrado");

        assertThat(event.vehicleId()).isEqualTo(VEHICLE_ID);
        assertThat(event.licensePlate()).isEqualTo("1234-BCD");
        assertThat(event.make()).isEqualTo("Toyota");
        assertThat(event.model()).isEqualTo("Corolla");
        assertThat(event.year()).isEqualTo(2023);
        assertThat(event.category()).isEqualTo(VehicleCategory.SEDAN);
        assertThat(event.dailyRate()).isEqualTo(money);
        assertThat(event.description()).isEqualTo("GPS integrado");
    }

    @Test
    void vehicleRegisteredEventNullDescriptionAccepted() {
        var money = new Money(new BigDecimal("50.00"), EUR);
        var event = new VehicleRegisteredEvent(
                UUID.randomUUID(), Instant.now(), VEHICLE_ID,
                "1234-BCD", "Toyota", "Corolla", 2023,
                VehicleCategory.SEDAN, money, null);

        assertThat(event.description()).isNull();
    }

    @Test
    void allEventsImplementDomainEvent() {
        assertThat(DomainEvent.class).isAssignableFrom(VehicleRegisteredEvent.class);
        assertThat(DomainEvent.class).isAssignableFrom(VehicleSentToMaintenanceEvent.class);
        assertThat(DomainEvent.class).isAssignableFrom(VehicleActivatedEvent.class);
        assertThat(DomainEvent.class).isAssignableFrom(VehicleRetiredEvent.class);
    }

    @Test
    void vehicleSentToMaintenanceEventCarriesVehicleId() {
        var event = new VehicleSentToMaintenanceEvent(UUID.randomUUID(), Instant.now(), VEHICLE_ID);

        assertThat(event.vehicleId()).isEqualTo(VEHICLE_ID);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredOn()).isNotNull();
    }

    @Test
    void vehicleActivatedEventCarriesVehicleId() {
        var event = new VehicleActivatedEvent(UUID.randomUUID(), Instant.now(), VEHICLE_ID);

        assertThat(event.vehicleId()).isEqualTo(VEHICLE_ID);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredOn()).isNotNull();
    }

    @Test
    void vehicleRetiredEventCarriesVehicleId() {
        var event = new VehicleRetiredEvent(UUID.randomUUID(), Instant.now(), VEHICLE_ID);

        assertThat(event.vehicleId()).isEqualTo(VEHICLE_ID);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredOn()).isNotNull();
    }

    @Test
    void eventsAreRecords() {
        assertThat(VehicleRegisteredEvent.class).isRecord();
        assertThat(VehicleSentToMaintenanceEvent.class).isRecord();
        assertThat(VehicleActivatedEvent.class).isRecord();
        assertThat(VehicleRetiredEvent.class).isRecord();
    }
}
