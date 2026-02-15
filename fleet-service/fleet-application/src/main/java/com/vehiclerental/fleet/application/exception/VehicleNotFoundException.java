package com.vehiclerental.fleet.application.exception;

public class VehicleNotFoundException extends RuntimeException {

    private final String vehicleId;

    public VehicleNotFoundException(String vehicleId) {
        super("Vehicle not found with id: " + vehicleId);
        this.vehicleId = vehicleId;
    }

    public String getVehicleId() {
        return vehicleId;
    }
}
