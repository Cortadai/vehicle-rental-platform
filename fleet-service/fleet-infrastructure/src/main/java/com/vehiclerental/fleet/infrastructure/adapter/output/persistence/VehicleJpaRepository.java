package com.vehiclerental.fleet.infrastructure.adapter.output.persistence;

import com.vehiclerental.fleet.infrastructure.adapter.output.persistence.entity.VehicleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VehicleJpaRepository extends JpaRepository<VehicleJpaEntity, UUID> {
}
