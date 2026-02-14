package com.vehiclerental.customer.domain.model.aggregate;

import com.vehiclerental.common.domain.entity.AggregateRoot;
import com.vehiclerental.customer.domain.event.CustomerActivatedEvent;
import com.vehiclerental.customer.domain.event.CustomerCreatedEvent;
import com.vehiclerental.customer.domain.event.CustomerDeletedEvent;
import com.vehiclerental.customer.domain.event.CustomerSuspendedEvent;
import com.vehiclerental.customer.domain.exception.CustomerDomainException;
import com.vehiclerental.customer.domain.model.vo.CustomerId;
import com.vehiclerental.customer.domain.model.vo.CustomerStatus;
import com.vehiclerental.customer.domain.model.vo.Email;
import com.vehiclerental.customer.domain.model.vo.PhoneNumber;

import java.time.Instant;
import java.util.UUID;

public class Customer extends AggregateRoot<CustomerId> {

    private final String firstName;
    private final String lastName;
    private final Email email;
    private final PhoneNumber phone;
    private final Instant createdAt;
    private CustomerStatus status;

    private Customer(CustomerId id, String firstName, String lastName, Email email,
                     PhoneNumber phone, CustomerStatus status, Instant createdAt) {
        super(id);
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Customer create(String firstName, String lastName, Email email, PhoneNumber phone) {
        validateFirstName(firstName);
        validateLastName(lastName);
        if (email == null) {
            throw new CustomerDomainException("Email must not be null", "CUSTOMER_EMAIL_REQUIRED");
        }

        var customerId = new CustomerId(UUID.randomUUID());
        var now = Instant.now();
        var customer = new Customer(customerId, firstName, lastName, email, phone,
                CustomerStatus.ACTIVE, now);

        customer.registerDomainEvent(new CustomerCreatedEvent(
                UUID.randomUUID(), now, customerId, firstName, lastName, email));

        return customer;
    }

    public static Customer reconstruct(CustomerId id, String firstName, String lastName,
                                       Email email, PhoneNumber phone,
                                       CustomerStatus status, Instant createdAt) {
        return new Customer(id, firstName, lastName, email, phone, status, createdAt);
    }

    public void suspend() {
        if (status != CustomerStatus.ACTIVE) {
            throw new CustomerDomainException(
                    "Cannot suspend customer in state " + status,
                    "CUSTOMER_INVALID_STATE");
        }
        status = CustomerStatus.SUSPENDED;
        registerDomainEvent(new CustomerSuspendedEvent(UUID.randomUUID(), Instant.now(), getId()));
    }

    public void activate() {
        if (status != CustomerStatus.SUSPENDED) {
            throw new CustomerDomainException(
                    "Cannot activate customer in state " + status,
                    "CUSTOMER_INVALID_STATE");
        }
        status = CustomerStatus.ACTIVE;
        registerDomainEvent(new CustomerActivatedEvent(UUID.randomUUID(), Instant.now(), getId()));
    }

    public void delete() {
        if (status == CustomerStatus.DELETED) {
            throw new CustomerDomainException(
                    "Cannot delete customer in state " + status,
                    "CUSTOMER_ALREADY_DELETED");
        }
        status = CustomerStatus.DELETED;
        registerDomainEvent(new CustomerDeletedEvent(UUID.randomUUID(), Instant.now(), getId()));
    }

    public boolean isActive() {
        return status == CustomerStatus.ACTIVE;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Email getEmail() {
        return email;
    }

    public PhoneNumber getPhone() {
        return phone;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static void validateFirstName(String firstName) {
        if (firstName == null || firstName.isBlank()) {
            throw new CustomerDomainException("First name must not be null or blank", "CUSTOMER_FIRST_NAME_REQUIRED");
        }
    }

    private static void validateLastName(String lastName) {
        if (lastName == null || lastName.isBlank()) {
            throw new CustomerDomainException("Last name must not be null or blank", "CUSTOMER_LAST_NAME_REQUIRED");
        }
    }
}
