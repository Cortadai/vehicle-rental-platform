package com.vehiclerental.reservation.infrastructure.adapter.output.persistence.saga;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SagaStateJpaRepository extends JpaRepository<SagaStateJpaEntity, UUID> {
}
