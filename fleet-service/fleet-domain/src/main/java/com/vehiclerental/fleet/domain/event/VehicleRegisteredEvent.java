package com.vehiclerental.fleet.domain.event;

import com.vehiclerental.common.domain.event.DomainEvent;
import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.fleet.domain.exception.FleetDomainException;
import com.vehiclerental.fleet.domain.model.vo.VehicleCategory;
import com.vehiclerental.fleet.domain.model.vo.VehicleId;

import java.time.Instant;
import java.util.UUID;

public record VehicleRegisteredEvent(
        UUID eventId,
        Instant occurredOn,
        VehicleId vehicleId,
        String licensePlate,
        String make,
        String model,
        int year,
        VehicleCategory category,
        Money dailyRate,
        String description
) implements DomainEvent {

    public VehicleRegisteredEvent {
        if (eventId == null) {
            throw new FleetDomainException("eventId must not be null", "FLEET_EVENT_ID_NULL");
        }
        if (occurredOn == null) {
            throw new FleetDomainException("occurredOn must not be null", "FLEET_EVENT_OCCURRED_ON_NULL");
        }
    }
}
