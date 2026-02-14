package com.vehiclerental.customer.infrastructure.adapter.output.persistence;

import com.vehiclerental.customer.domain.model.aggregate.Customer;
import com.vehiclerental.customer.domain.model.vo.CustomerId;
import com.vehiclerental.customer.domain.port.output.CustomerRepository;
import com.vehiclerental.customer.infrastructure.adapter.output.persistence.mapper.CustomerPersistenceMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CustomerRepositoryAdapter implements CustomerRepository {

    private final CustomerJpaRepository jpaRepository;
    private final CustomerPersistenceMapper mapper;

    public CustomerRepositoryAdapter(CustomerJpaRepository jpaRepository,
                                     CustomerPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Customer save(Customer customer) {
        var jpaEntity = mapper.toJpaEntity(customer);
        var savedEntity = jpaRepository.save(jpaEntity);
        return mapper.toDomainEntity(savedEntity);
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return jpaRepository.findById(id.value())
                .map(mapper::toDomainEntity);
    }
}
