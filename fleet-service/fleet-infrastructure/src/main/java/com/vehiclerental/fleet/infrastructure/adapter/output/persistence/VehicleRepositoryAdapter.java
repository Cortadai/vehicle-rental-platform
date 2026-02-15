package com.vehiclerental.fleet.infrastructure.adapter.output.persistence;

import com.vehiclerental.fleet.domain.model.aggregate.Vehicle;
import com.vehiclerental.fleet.domain.model.vo.VehicleId;
import com.vehiclerental.fleet.domain.port.output.VehicleRepository;
import com.vehiclerental.fleet.infrastructure.adapter.output.persistence.mapper.VehiclePersistenceMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class VehicleRepositoryAdapter implements VehicleRepository {

    private final VehicleJpaRepository jpaRepository;
    private final VehiclePersistenceMapper mapper;

    public VehicleRepositoryAdapter(VehicleJpaRepository jpaRepository,
                                    VehiclePersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Vehicle save(Vehicle vehicle) {
        var jpaEntity = mapper.toJpaEntity(vehicle);
        var savedEntity = jpaRepository.save(jpaEntity);
        return mapper.toDomainEntity(savedEntity);
    }

    @Override
    public Optional<Vehicle> findById(VehicleId id) {
        return jpaRepository.findById(id.value())
                .map(mapper::toDomainEntity);
    }
}
