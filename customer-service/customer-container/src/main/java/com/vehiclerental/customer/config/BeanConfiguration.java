package com.vehiclerental.customer.config;

import com.vehiclerental.customer.application.mapper.CustomerApplicationMapper;
import com.vehiclerental.customer.application.port.output.CustomerDomainEventPublisher;
import com.vehiclerental.customer.application.service.CustomerApplicationService;
import com.vehiclerental.customer.domain.port.output.CustomerRepository;
import com.vehiclerental.customer.infrastructure.adapter.output.persistence.mapper.CustomerPersistenceMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class BeanConfiguration {

    @Bean
    public CustomerPersistenceMapper customerPersistenceMapper() {
        return new CustomerPersistenceMapper();
    }

    @Bean
    public CustomerApplicationMapper customerApplicationMapper() {
        return new CustomerApplicationMapper();
    }

    @Bean
    public CustomerApplicationService customerApplicationService(
            CustomerRepository customerRepository,
            CustomerDomainEventPublisher eventPublisher,
            CustomerApplicationMapper mapper) {
        return new CustomerApplicationService(customerRepository, eventPublisher, mapper);
    }
}
