package com.vehiclerental.fleet.domain.port.output;

import com.vehiclerental.fleet.domain.model.aggregate.Vehicle;
import com.vehiclerental.fleet.domain.model.vo.VehicleId;

import java.util.Optional;

public interface VehicleRepository {

    Vehicle save(Vehicle vehicle);

    Optional<Vehicle> findById(VehicleId vehicleId);
}
