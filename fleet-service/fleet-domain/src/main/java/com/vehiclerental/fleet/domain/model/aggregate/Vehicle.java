package com.vehiclerental.fleet.domain.model.aggregate;

import com.vehiclerental.common.domain.entity.AggregateRoot;
import com.vehiclerental.fleet.domain.event.VehicleActivatedEvent;
import com.vehiclerental.fleet.domain.event.VehicleRegisteredEvent;
import com.vehiclerental.fleet.domain.event.VehicleRetiredEvent;
import com.vehiclerental.fleet.domain.event.VehicleSentToMaintenanceEvent;
import com.vehiclerental.fleet.domain.exception.FleetDomainException;
import com.vehiclerental.fleet.domain.model.vo.*;

import java.time.Instant;
import java.time.Year;
import java.util.UUID;

public class Vehicle extends AggregateRoot<VehicleId> {

    private static final int MIN_YEAR = 1950;

    private final LicensePlate licensePlate;
    private final String make;
    private final String model;
    private final int year;
    private final VehicleCategory category;
    private final DailyRate dailyRate;
    private final String description;
    private final Instant createdAt;
    private VehicleStatus status;

    private Vehicle(VehicleId id, LicensePlate licensePlate, String make, String model,
                    int year, VehicleCategory category, DailyRate dailyRate, String description,
                    VehicleStatus status, Instant createdAt) {
        super(id);
        this.licensePlate = licensePlate;
        this.make = make;
        this.model = model;
        this.year = year;
        this.category = category;
        this.dailyRate = dailyRate;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Vehicle create(LicensePlate licensePlate, String make, String model,
                                 int year, VehicleCategory category, DailyRate dailyRate,
                                 String description) {
        validateLicensePlate(licensePlate);
        validateMake(make);
        validateModel(model);
        validateYear(year);
        validateCategory(category);
        validateDailyRate(dailyRate);
        validateDescription(description);

        var vehicleId = new VehicleId(UUID.randomUUID());
        var now = Instant.now();
        var vehicle = new Vehicle(vehicleId, licensePlate, make, model, year, category,
                dailyRate, description, VehicleStatus.ACTIVE, now);

        vehicle.registerDomainEvent(new VehicleRegisteredEvent(
                UUID.randomUUID(), now, vehicleId,
                licensePlate.value(), make, model, year, category,
                dailyRate.money(), description));

        return vehicle;
    }

    public static Vehicle reconstruct(VehicleId id, LicensePlate licensePlate, String make,
                                      String model, int year, VehicleCategory category,
                                      DailyRate dailyRate, String description,
                                      VehicleStatus status, Instant createdAt) {
        return new Vehicle(id, licensePlate, make, model, year, category,
                dailyRate, description, status, createdAt);
    }

    public void sendToMaintenance() {
        if (status != VehicleStatus.ACTIVE) {
            throw new FleetDomainException(
                    "Cannot send vehicle to maintenance in state " + status,
                    "VEHICLE_INVALID_STATE");
        }
        status = VehicleStatus.UNDER_MAINTENANCE;
        registerDomainEvent(new VehicleSentToMaintenanceEvent(UUID.randomUUID(), Instant.now(), getId()));
    }

    public void activate() {
        if (status != VehicleStatus.UNDER_MAINTENANCE) {
            throw new FleetDomainException(
                    "Cannot activate vehicle in state " + status,
                    "VEHICLE_INVALID_STATE");
        }
        status = VehicleStatus.ACTIVE;
        registerDomainEvent(new VehicleActivatedEvent(UUID.randomUUID(), Instant.now(), getId()));
    }

    public void retire() {
        if (status == VehicleStatus.RETIRED) {
            throw new FleetDomainException(
                    "Cannot retire vehicle in state " + status,
                    "VEHICLE_ALREADY_RETIRED");
        }
        status = VehicleStatus.RETIRED;
        registerDomainEvent(new VehicleRetiredEvent(UUID.randomUUID(), Instant.now(), getId()));
    }

    public boolean isAvailable() {
        return status == VehicleStatus.ACTIVE;
    }

    public LicensePlate getLicensePlate() {
        return licensePlate;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public int getYear() {
        return year;
    }

    public VehicleCategory getCategory() {
        return category;
    }

    public DailyRate getDailyRate() {
        return dailyRate;
    }

    public String getDescription() {
        return description;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static void validateLicensePlate(LicensePlate licensePlate) {
        if (licensePlate == null) {
            throw new FleetDomainException("License plate must not be null", "VEHICLE_LICENSE_PLATE_REQUIRED");
        }
    }

    private static void validateMake(String make) {
        if (make == null || make.isBlank()) {
            throw new FleetDomainException("Make must not be null or blank", "VEHICLE_MAKE_REQUIRED");
        }
    }

    private static void validateModel(String model) {
        if (model == null || model.isBlank()) {
            throw new FleetDomainException("Model must not be null or blank", "VEHICLE_MODEL_REQUIRED");
        }
    }

    private static void validateYear(int year) {
        int maxYear = Year.now().getValue() + 1;
        if (year < MIN_YEAR) {
            throw new FleetDomainException("Year must be at least " + MIN_YEAR, "VEHICLE_YEAR_TOO_LOW");
        }
        if (year > maxYear) {
            throw new FleetDomainException("Year must be at most " + maxYear, "VEHICLE_YEAR_TOO_HIGH");
        }
    }

    private static void validateCategory(VehicleCategory category) {
        if (category == null) {
            throw new FleetDomainException("Category must not be null", "VEHICLE_CATEGORY_REQUIRED");
        }
    }

    private static void validateDailyRate(DailyRate dailyRate) {
        if (dailyRate == null) {
            throw new FleetDomainException("Daily rate must not be null", "VEHICLE_DAILY_RATE_REQUIRED");
        }
    }

    private static void validateDescription(String description) {
        if (description != null && description.length() > 500) {
            throw new FleetDomainException("Description must be at most 500 characters", "VEHICLE_DESCRIPTION_TOO_LONG");
        }
    }
}
