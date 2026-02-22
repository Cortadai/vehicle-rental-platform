package com.vehiclerental.payment.infrastructure.adapter.output.persistence;

import com.vehiclerental.payment.infrastructure.adapter.output.persistence.entity.PaymentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, UUID> {

    Optional<PaymentJpaEntity> findByReservationId(UUID reservationId);
}
