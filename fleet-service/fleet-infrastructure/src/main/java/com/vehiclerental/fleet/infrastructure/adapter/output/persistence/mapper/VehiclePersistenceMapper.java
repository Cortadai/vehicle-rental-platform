package com.vehiclerental.fleet.infrastructure.adapter.output.persistence.mapper;

import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.fleet.domain.model.aggregate.Vehicle;
import com.vehiclerental.fleet.domain.model.vo.DailyRate;
import com.vehiclerental.fleet.domain.model.vo.LicensePlate;
import com.vehiclerental.fleet.domain.model.vo.VehicleCategory;
import com.vehiclerental.fleet.domain.model.vo.VehicleId;
import com.vehiclerental.fleet.domain.model.vo.VehicleStatus;
import com.vehiclerental.fleet.infrastructure.adapter.output.persistence.entity.VehicleJpaEntity;

import java.util.Currency;

public class VehiclePersistenceMapper {

    public VehicleJpaEntity toJpaEntity(Vehicle vehicle) {
        var entity = new VehicleJpaEntity();
        entity.setId(vehicle.getId().value());
        entity.setLicensePlate(vehicle.getLicensePlate().value());
        entity.setMake(vehicle.getMake());
        entity.setModel(vehicle.getModel());
        entity.setYear(vehicle.getYear());
        entity.setCategory(vehicle.getCategory().name());
        entity.setDailyRateAmount(vehicle.getDailyRate().money().amount());
        entity.setDailyRateCurrency(vehicle.getDailyRate().money().currency().getCurrencyCode());
        entity.setDescription(vehicle.getDescription());
        entity.setStatus(vehicle.getStatus().name());
        entity.setCreatedAt(vehicle.getCreatedAt());
        return entity;
    }

    public Vehicle toDomainEntity(VehicleJpaEntity entity) {
        return Vehicle.reconstruct(
                new VehicleId(entity.getId()),
                new LicensePlate(entity.getLicensePlate()),
                entity.getMake(),
                entity.getModel(),
                entity.getYear(),
                VehicleCategory.valueOf(entity.getCategory()),
                new DailyRate(new Money(entity.getDailyRateAmount(),
                        Currency.getInstance(entity.getDailyRateCurrency()))),
                entity.getDescription(),
                VehicleStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt());
    }
}
