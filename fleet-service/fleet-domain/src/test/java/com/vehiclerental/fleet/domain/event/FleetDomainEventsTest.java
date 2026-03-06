package com.vehiclerental.fleet.domain.event;

import com.vehiclerental.common.domain.event.DomainEvent;
import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.fleet.domain.exception.FleetDomainException;
import com.vehiclerental.fleet.domain.model.vo.VehicleCategory;
import com.vehiclerental.fleet.domain.model.vo.VehicleId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FleetDomainEventsTest {

    private static final VehicleId VEHICLE_ID = new VehicleId(UUID.randomUUID());
    private static final UUID RESERVATION_ID = UUID.randomUUID();
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
        assertThat(FleetConfirmedEvent.class).isRecord();
        assertThat(FleetRejectedEvent.class).isRecord();
        assertThat(FleetReleasedEvent.class).isRecord();
    }

    // --- FleetConfirmedEvent tests ---

    @Test
    void fleetConfirmedEventFieldsAccessible() {
        var event = new FleetConfirmedEvent(UUID.randomUUID(), Instant.now(), VEHICLE_ID, RESERVATION_ID);

        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredOn()).isNotNull();
        assertThat(event.vehicleId()).isEqualTo(VEHICLE_ID);
        assertThat(event.reservationId()).isEqualTo(RESERVATION_ID);
    }

    @Test
    void fleetConfirmedEventImplementsDomainEvent() {
        assertThat(DomainEvent.class).isAssignableFrom(FleetConfirmedEvent.class);
    }

    @Test
    void fleetConfirmedEventNullEventIdThrows() {
        assertThatThrownBy(() -> new FleetConfirmedEvent(null, Instant.now(), VEHICLE_ID, RESERVATION_ID))
                .isInstanceOf(FleetDomainException.class);
    }

    @Test
    void fleetConfirmedEventNullOccurredOnThrows() {
        assertThatThrownBy(() -> new FleetConfirmedEvent(UUID.randomUUID(), null, VEHICLE_ID, RESERVATION_ID))
                .isInstanceOf(FleetDomainException.class);
    }

    // --- FleetRejectedEvent tests ---

    @Test
    void fleetRejectedEventFieldsAccessible() {
        var failures = List.of("Vehicle not found: 123");
        var event = new FleetRejectedEvent(UUID.randomUUID(), Instant.now(), VEHICLE_ID, RESERVATION_ID, failures);

        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredOn()).isNotNull();
        assertThat(event.vehicleId()).isEqualTo(VEHICLE_ID);
        assertThat(event.reservationId()).isEqualTo(RESERVATION_ID);
        assertThat(event.failureMessages()).containsExactly("Vehicle not found: 123");
    }

    @Test
    void fleetRejectedEventImplementsDomainEvent() {
        assertThat(DomainEvent.class).isAssignableFrom(FleetRejectedEvent.class);
    }

    @Test
    void fleetRejectedEventNullEventIdThrows() {
        assertThatThrownBy(() -> new FleetRejectedEvent(null, Instant.now(), VEHICLE_ID, RESERVATION_ID, List.of()))
                .isInstanceOf(FleetDomainException.class);
    }

    @Test
    void fleetRejectedEventNullOccurredOnThrows() {
        assertThatThrownBy(() -> new FleetRejectedEvent(UUID.randomUUID(), null, VEHICLE_ID, RESERVATION_ID, List.of()))
                .isInstanceOf(FleetDomainException.class);
    }

    // --- FleetReleasedEvent tests ---

    @Test
    void fleetReleasedEventFieldsAccessible() {
        var event = new FleetReleasedEvent(UUID.randomUUID(), Instant.now(), VEHICLE_ID, RESERVATION_ID);

        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredOn()).isNotNull();
        assertThat(event.vehicleId()).isEqualTo(VEHICLE_ID);
        assertThat(event.reservationId()).isEqualTo(RESERVATION_ID);
    }

    @Test
    void fleetReleasedEventImplementsDomainEvent() {
        assertThat(DomainEvent.class).isAssignableFrom(FleetReleasedEvent.class);
    }

    @Test
    void fleetReleasedEventNullEventIdThrows() {
        assertThatThrownBy(() -> new FleetReleasedEvent(null, Instant.now(), VEHICLE_ID, RESERVATION_ID))
                .isInstanceOf(FleetDomainException.class);
    }

    @Test
    void fleetReleasedEventNullOccurredOnThrows() {
        assertThatThrownBy(() -> new FleetReleasedEvent(UUID.randomUUID(), null, VEHICLE_ID, RESERVATION_ID))
                .isInstanceOf(FleetDomainException.class);
    }

    @Test
    void allSevenEventsImplementDomainEvent() {
        assertThat(DomainEvent.class).isAssignableFrom(VehicleRegisteredEvent.class);
        assertThat(DomainEvent.class).isAssignableFrom(VehicleSentToMaintenanceEvent.class);
        assertThat(DomainEvent.class).isAssignableFrom(VehicleActivatedEvent.class);
        assertThat(DomainEvent.class).isAssignableFrom(VehicleRetiredEvent.class);
        assertThat(DomainEvent.class).isAssignableFrom(FleetConfirmedEvent.class);
        assertThat(DomainEvent.class).isAssignableFrom(FleetRejectedEvent.class);
        assertThat(DomainEvent.class).isAssignableFrom(FleetReleasedEvent.class);
    }
}
