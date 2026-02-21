package com.vehiclerental.reservation.infrastructure.adapter.output.persistence;

import com.vehiclerental.reservation.infrastructure.adapter.output.persistence.entity.ReservationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReservationJpaRepository extends JpaRepository<ReservationJpaEntity, UUID> {

    Optional<ReservationJpaEntity> findByTrackingId(UUID trackingId);
}
