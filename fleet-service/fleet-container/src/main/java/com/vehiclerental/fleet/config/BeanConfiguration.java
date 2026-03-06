package com.vehiclerental.fleet.config;

import com.vehiclerental.fleet.application.mapper.FleetApplicationMapper;
import com.vehiclerental.fleet.application.port.input.*;
import com.vehiclerental.fleet.application.port.output.FleetDomainEventPublisher;
import com.vehiclerental.fleet.application.service.FleetApplicationService;
import com.vehiclerental.fleet.domain.port.output.VehicleRepository;
import com.vehiclerental.fleet.infrastructure.adapter.output.persistence.mapper.VehiclePersistenceMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public VehiclePersistenceMapper vehiclePersistenceMapper() {
        return new VehiclePersistenceMapper();
    }

    @Bean
    public FleetApplicationMapper fleetApplicationMapper() {
        return new FleetApplicationMapper();
    }

    @Bean
    public FleetApplicationService fleetApplicationService(
            VehicleRepository vehicleRepository,
            FleetDomainEventPublisher eventPublisher,
            FleetApplicationMapper mapper) {
        return new FleetApplicationService(vehicleRepository, eventPublisher, mapper);
    }

    @Bean
    public RegisterVehicleUseCase registerVehicleUseCase(FleetApplicationService service) {
        return service;
    }

    @Bean
    public GetVehicleUseCase getVehicleUseCase(FleetApplicationService service) {
        return service;
    }

    @Bean
    public SendToMaintenanceUseCase sendToMaintenanceUseCase(FleetApplicationService service) {
        return service;
    }

    @Bean
    public ActivateVehicleUseCase activateVehicleUseCase(FleetApplicationService service) {
        return service;
    }

    @Bean
    public RetireVehicleUseCase retireVehicleUseCase(FleetApplicationService service) {
        return service;
    }

    @Bean
    public ConfirmFleetAvailabilityUseCase confirmFleetAvailabilityUseCase(FleetApplicationService service) {
        return service;
    }

    @Bean
    public ReleaseFleetReservationUseCase releaseFleetReservationUseCase(FleetApplicationService service) {
        return service;
    }
}
