package com.vehiclerental.fleet.application.service;

import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.fleet.application.dto.command.*;
import com.vehiclerental.fleet.application.dto.response.VehicleResponse;
import com.vehiclerental.fleet.application.exception.VehicleNotFoundException;
import com.vehiclerental.fleet.application.mapper.FleetApplicationMapper;
import com.vehiclerental.fleet.application.port.input.*;
import com.vehiclerental.fleet.application.port.output.FleetDomainEventPublisher;
import com.vehiclerental.fleet.domain.model.aggregate.Vehicle;
import com.vehiclerental.fleet.domain.model.vo.DailyRate;
import com.vehiclerental.fleet.domain.model.vo.LicensePlate;
import com.vehiclerental.fleet.domain.model.vo.VehicleCategory;
import com.vehiclerental.fleet.domain.model.vo.VehicleId;
import com.vehiclerental.fleet.domain.port.output.VehicleRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;
import java.util.UUID;

public class FleetApplicationService implements
        RegisterVehicleUseCase,
        GetVehicleUseCase,
        SendToMaintenanceUseCase,
        ActivateVehicleUseCase,
        RetireVehicleUseCase {

    private final VehicleRepository vehicleRepository;
    private final FleetDomainEventPublisher eventPublisher;
    private final FleetApplicationMapper mapper;

    public FleetApplicationService(VehicleRepository vehicleRepository,
                                   FleetDomainEventPublisher eventPublisher,
                                   FleetApplicationMapper mapper) {
        this.vehicleRepository = vehicleRepository;
        this.eventPublisher = eventPublisher;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public VehicleResponse execute(RegisterVehicleCommand command) {
        LicensePlate licensePlate = new LicensePlate(command.licensePlate());
        VehicleCategory category = VehicleCategory.valueOf(command.category());
        DailyRate dailyRate = new DailyRate(new Money(command.dailyRateAmount(),
                Currency.getInstance(command.dailyRateCurrency())));

        Vehicle vehicle = Vehicle.create(licensePlate, command.make(), command.model(),
                command.year(), category, dailyRate, command.description());
        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        eventPublisher.publish(savedVehicle.getDomainEvents());
        savedVehicle.clearDomainEvents();

        return mapper.toResponse(savedVehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse execute(GetVehicleCommand command) {
        VehicleId vehicleId = new VehicleId(UUID.fromString(command.vehicleId()));
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(command.vehicleId()));

        return mapper.toResponse(vehicle);
    }

    @Override
    @Transactional
    public void execute(SendToMaintenanceCommand command) {
        VehicleId vehicleId = new VehicleId(UUID.fromString(command.vehicleId()));
        Vehicle vehicle = findVehicleOrThrow(vehicleId, command.vehicleId());

        vehicle.sendToMaintenance();
        vehicleRepository.save(vehicle);
        eventPublisher.publish(vehicle.getDomainEvents());
        vehicle.clearDomainEvents();
    }

    @Override
    @Transactional
    public void execute(ActivateVehicleCommand command) {
        VehicleId vehicleId = new VehicleId(UUID.fromString(command.vehicleId()));
        Vehicle vehicle = findVehicleOrThrow(vehicleId, command.vehicleId());

        vehicle.activate();
        vehicleRepository.save(vehicle);
        eventPublisher.publish(vehicle.getDomainEvents());
        vehicle.clearDomainEvents();
    }

    @Override
    @Transactional
    public void execute(RetireVehicleCommand command) {
        VehicleId vehicleId = new VehicleId(UUID.fromString(command.vehicleId()));
        Vehicle vehicle = findVehicleOrThrow(vehicleId, command.vehicleId());

        vehicle.retire();
        vehicleRepository.save(vehicle);
        eventPublisher.publish(vehicle.getDomainEvents());
        vehicle.clearDomainEvents();
    }

    private Vehicle findVehicleOrThrow(VehicleId vehicleId, String rawId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(rawId));
    }
}
