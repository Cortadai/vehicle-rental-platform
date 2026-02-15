package com.vehiclerental.fleet.application.mapper;

import com.vehiclerental.fleet.application.dto.response.VehicleResponse;
import com.vehiclerental.fleet.domain.model.aggregate.Vehicle;

public class FleetApplicationMapper {

    public VehicleResponse toResponse(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId().value().toString(),
                vehicle.getLicensePlate().value(),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.getCategory().name(),
                vehicle.getDailyRate().money().amount(),
                vehicle.getDailyRate().money().currency().getCurrencyCode(),
                vehicle.getDescription(),
                vehicle.getStatus().name(),
                vehicle.getCreatedAt());
    }
}
