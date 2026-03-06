package com.vehiclerental.fleet.domain.event;

import com.vehiclerental.common.domain.event.DomainEvent;
import com.vehiclerental.fleet.domain.exception.FleetDomainException;
import com.vehiclerental.fleet.domain.model.vo.VehicleId;

import java.time.Instant;
import java.util.UUID;

public record FleetReleasedEvent(
        UUID eventId,
        Instant occurredOn,
        VehicleId vehicleId,
        UUID reservationId
) implements DomainEvent {

    public FleetReleasedEvent {
        if (eventId == null) {
            throw new FleetDomainException("eventId must not be null", "FLEET_EVENT_ID_NULL");
        }
        if (occurredOn == null) {
            throw new FleetDomainException("occurredOn must not be null", "FLEET_EVENT_OCCURRED_ON_NULL");
        }
    }
}
