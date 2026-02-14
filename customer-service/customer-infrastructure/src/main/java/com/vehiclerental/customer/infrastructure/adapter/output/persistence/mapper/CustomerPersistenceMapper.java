package com.vehiclerental.customer.infrastructure.adapter.output.persistence.mapper;

import com.vehiclerental.customer.domain.model.aggregate.Customer;
import com.vehiclerental.customer.domain.model.vo.CustomerId;
import com.vehiclerental.customer.domain.model.vo.CustomerStatus;
import com.vehiclerental.customer.domain.model.vo.Email;
import com.vehiclerental.customer.domain.model.vo.PhoneNumber;
import com.vehiclerental.customer.infrastructure.adapter.output.persistence.entity.CustomerJpaEntity;

public class CustomerPersistenceMapper {

    public CustomerJpaEntity toJpaEntity(Customer customer) {
        var entity = new CustomerJpaEntity();
        entity.setId(customer.getId().value());
        entity.setFirstName(customer.getFirstName());
        entity.setLastName(customer.getLastName());
        entity.setEmail(customer.getEmail().value());
        entity.setPhone(customer.getPhone() != null ? customer.getPhone().value() : null);
        entity.setStatus(customer.getStatus().name());
        entity.setCreatedAt(customer.getCreatedAt());
        return entity;
    }

    public Customer toDomainEntity(CustomerJpaEntity entity) {
        return Customer.reconstruct(
                new CustomerId(entity.getId()),
                entity.getFirstName(),
                entity.getLastName(),
                new Email(entity.getEmail()),
                entity.getPhone() != null ? new PhoneNumber(entity.getPhone()) : null,
                CustomerStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt());
    }
}
