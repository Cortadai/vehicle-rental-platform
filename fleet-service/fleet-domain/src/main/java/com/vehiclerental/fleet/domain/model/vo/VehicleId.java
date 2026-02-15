package com.vehiclerental.fleet.domain.model.vo;

import com.vehiclerental.fleet.domain.exception.FleetDomainException;

import java.util.UUID;

public record VehicleId(UUID value) {

    public VehicleId {
        if (value == null) {
            throw new FleetDomainException("VehicleId value must not be null", "VEHICLE_ID_NULL");
        }
    }
}
