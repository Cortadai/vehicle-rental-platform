package com.vehiclerental.customer.application.service;

import com.vehiclerental.customer.application.dto.command.*;
import com.vehiclerental.customer.application.dto.response.CustomerResponse;
import com.vehiclerental.customer.application.exception.CustomerNotFoundException;
import com.vehiclerental.customer.application.mapper.CustomerApplicationMapper;
import com.vehiclerental.customer.application.port.input.*;
import com.vehiclerental.customer.application.port.output.CustomerDomainEventPublisher;
import com.vehiclerental.customer.domain.event.CustomerRejectedEvent;
import com.vehiclerental.customer.domain.event.CustomerValidatedEvent;
import com.vehiclerental.customer.domain.model.aggregate.Customer;
import com.vehiclerental.customer.domain.model.vo.CustomerId;
import com.vehiclerental.customer.domain.model.vo.CustomerStatus;
import com.vehiclerental.customer.domain.model.vo.Email;
import com.vehiclerental.customer.domain.model.vo.PhoneNumber;
import com.vehiclerental.customer.domain.port.output.CustomerRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CustomerApplicationService implements
        CreateCustomerUseCase,
        GetCustomerUseCase,
        SuspendCustomerUseCase,
        ActivateCustomerUseCase,
        DeleteCustomerUseCase,
        ValidateCustomerForReservationUseCase {

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

    @Override
    @Transactional
    public void execute(ValidateCustomerCommand command) {
        CustomerId customerId = new CustomerId(UUID.fromString(command.customerId()));
        UUID reservationId = UUID.fromString(command.reservationId());

        Optional<Customer> customerOpt = customerRepository.findById(customerId);

        if (customerOpt.isEmpty()) {
            var rejectedEvent = new CustomerRejectedEvent(
                    UUID.randomUUID(), Instant.now(), customerId, reservationId,
                    List.of("Customer not found: " + command.customerId()));
            eventPublisher.publish(List.of(rejectedEvent));
            return;
        }

        Customer customer = customerOpt.get();

        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            var rejectedEvent = new CustomerRejectedEvent(
                    UUID.randomUUID(), Instant.now(), customerId, reservationId,
                    List.of("Customer is not active, current status: " + customer.getStatus()));
            eventPublisher.publish(List.of(rejectedEvent));
            return;
        }

        var validatedEvent = new CustomerValidatedEvent(
                UUID.randomUUID(), Instant.now(), customerId, reservationId);
        eventPublisher.publish(List.of(validatedEvent));
    }

    private Customer findCustomerOrThrow(CustomerId customerId, String rawId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(rawId));
    }
}
