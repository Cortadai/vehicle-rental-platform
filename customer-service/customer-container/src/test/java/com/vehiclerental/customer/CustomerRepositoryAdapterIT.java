package com.vehiclerental.customer;

import com.vehiclerental.customer.domain.model.aggregate.Customer;
import com.vehiclerental.customer.domain.model.vo.CustomerId;
import com.vehiclerental.customer.domain.model.vo.Email;
import com.vehiclerental.customer.domain.model.vo.PhoneNumber;
import com.vehiclerental.customer.domain.port.output.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class CustomerRepositoryAdapterIT {

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void saveAndFindByIdRoundTrip() {
        var customer = Customer.create("John", "Doe",
                new Email("john.roundtrip@example.com"), new PhoneNumber("+1234567890"));
        customer.clearDomainEvents();

        var saved = customerRepository.save(customer);

        var found = customerRepository.findById(saved.getId());

        assertThat(found).isPresent();
        var loaded = found.get();
        assertThat(loaded.getId()).isEqualTo(saved.getId());
        assertThat(loaded.getFirstName()).isEqualTo("John");
        assertThat(loaded.getLastName()).isEqualTo("Doe");
        assertThat(loaded.getEmail().value()).isEqualTo("john.roundtrip@example.com");
        assertThat(loaded.getPhone().value()).isEqualTo("+1234567890");
        assertThat(loaded.getStatus().name()).isEqualTo("ACTIVE");
        assertThat(loaded.getCreatedAt()).isCloseTo(saved.getCreatedAt(), within(1, ChronoUnit.MICROS));
    }

    @Test
    void findByIdReturnsEmptyForNonExisting() {
        var nonExistingId = new CustomerId(UUID.randomUUID());

        var result = customerRepository.findById(nonExistingId);

        assertThat(result).isEmpty();
    }

    @Test
    void savesPersistsAllFieldsIncludingNullPhone() {
        var customer = Customer.create("Jane", "Smith",
                new Email("jane.nullphone@example.com"), null);
        customer.clearDomainEvents();

        var saved = customerRepository.save(customer);
        var found = customerRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getPhone()).isNull();
    }
}
