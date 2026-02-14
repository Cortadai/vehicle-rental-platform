package com.vehiclerental.customer.infrastructure.adapter.output.persistence;

import com.vehiclerental.customer.infrastructure.adapter.output.persistence.entity.CustomerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerJpaRepository extends JpaRepository<CustomerJpaEntity, UUID> {
}
