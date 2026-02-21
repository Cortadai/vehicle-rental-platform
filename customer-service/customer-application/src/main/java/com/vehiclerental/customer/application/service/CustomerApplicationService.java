package com.vehiclerental.customer.application.service;

import com.vehiclerental.customer.application.dto.command.*;
import com.vehiclerental.customer.application.dto.response.CustomerResponse;
import com.vehiclerental.customer.application.exception.CustomerNotFoundException;
import com.vehiclerental.customer.application.mapper.CustomerApplicationMapper;
import com.vehiclerental.customer.application.port.input.*;
import com.vehiclerental.customer.application.port.output.CustomerDomainEventPublisher;
import com.vehiclerental.customer.domain.model.aggregate.Customer;
import com.vehiclerental.customer.domain.model.vo.CustomerId;
import com.vehiclerental.customer.domain.model.vo.Email;
import com.vehiclerental.customer.domain.model.vo.PhoneNumber;
import com.vehiclerental.customer.domain.port.output.CustomerRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class CustomerApplicationService implements
        CreateCustomerUseCase,
        GetCustomerUseCase,
        SuspendCustomerUseCase,
        ActivateCustomerUseCase,
        DeleteCustomerUseCase {

    private final CustomerRepository customerRepository;
    private final CustomerDomainEventPublisher eventPublisher;
    private final CustomerApplicationMapper mapper;

    public CustomerApplicationService(CustomerRepository customerRepository,
                                      CustomerDomainEventPublisher eventPublisher,
                                      CustomerApplicationMapper mapper) {
        this.customerRepository = customerRepository;
        this.eventPublisher = eventPublisher;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public CustomerResponse execute(CreateCustomerCommand command) {
        Email email = new Email(command.email());
        PhoneNumber phone = command.phone() != null ? new PhoneNumber(command.phone()) : null;

        Customer customer = Customer.create(command.firstName(), command.lastName(), email, phone);
        Customer savedCustomer = customerRepository.save(customer);
        eventPublisher.publish(customer.getDomainEvents());
        customer.clearDomainEvents();

        return mapper.toResponse(savedCustomer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse execute(GetCustomerCommand command) {
        CustomerId customerId = new CustomerId(UUID.fromString(command.customerId()));
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(command.customerId()));

        return mapper.toResponse(customer);
    }

    @Override
    @Transactional
    public void execute(SuspendCustomerCommand command) {
        CustomerId customerId = new CustomerId(UUID.fromString(command.customerId()));
        Customer customer = findCustomerOrThrow(customerId, command.customerId());

        customer.suspend();
        customerRepository.save(customer);
        eventPublisher.publish(customer.getDomainEvents());
        customer.clearDomainEvents();
    }

    @Override
    @Transactional
    public void execute(ActivateCustomerCommand command) {
        CustomerId customerId = new CustomerId(UUID.fromString(command.customerId()));
        Customer customer = findCustomerOrThrow(customerId, command.customerId());

        customer.activate();
        customerRepository.save(customer);
        eventPublisher.publish(customer.getDomainEvents());
        customer.clearDomainEvents();
    }

    @Override
    @Transactional
    public void execute(DeleteCustomerCommand command) {
        CustomerId customerId = new CustomerId(UUID.fromString(command.customerId()));
        Customer customer = findCustomerOrThrow(customerId, command.customerId());

        customer.delete();
        customerRepository.save(customer);
        eventPublisher.publish(customer.getDomainEvents());
        customer.clearDomainEvents();
    }

    private Customer findCustomerOrThrow(CustomerId customerId, String rawId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(rawId));
    }
}
