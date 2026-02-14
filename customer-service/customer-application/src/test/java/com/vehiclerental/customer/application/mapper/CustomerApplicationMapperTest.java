package com.vehiclerental.customer.application.mapper;

import com.vehiclerental.customer.application.dto.response.CustomerResponse;
import com.vehiclerental.customer.domain.model.aggregate.Customer;
import com.vehiclerental.customer.domain.model.vo.CustomerId;
import com.vehiclerental.customer.domain.model.vo.CustomerStatus;
import com.vehiclerental.customer.domain.model.vo.Email;
import com.vehiclerental.customer.domain.model.vo.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerApplicationMapperTest {

    private final CustomerApplicationMapper mapper = new CustomerApplicationMapper();

    @Test
    void toResponseMapsAllFieldsCorrectly() {
        var customerId = new CustomerId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        var email = new Email("john@example.com");
        var phone = new PhoneNumber("+1234567890");
        var createdAt = Instant.parse("2024-01-15T10:30:00Z");

        var customer = Customer.reconstruct(
                customerId, "John", "Doe", email, phone, CustomerStatus.ACTIVE, createdAt);

        CustomerResponse response = mapper.toResponse(customer);

        assertThat(response.customerId()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        assertThat(response.firstName()).isEqualTo("John");
        assertThat(response.lastName()).isEqualTo("Doe");
        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.phone()).isEqualTo("+1234567890");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void toResponseMapsNullPhoneAsNull() {
        var customerId = new CustomerId(UUID.randomUUID());
        var email = new Email("jane@example.com");
        var createdAt = Instant.now();

        var customer = Customer.reconstruct(
                customerId, "Jane", "Smith", email, null, CustomerStatus.ACTIVE, createdAt);

        CustomerResponse response = mapper.toResponse(customer);

        assertThat(response.phone()).isNull();
    }
}
