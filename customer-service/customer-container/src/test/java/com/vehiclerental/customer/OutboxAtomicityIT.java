package com.vehiclerental.customer;

import com.vehiclerental.customer.application.dto.command.CreateCustomerCommand;
import com.vehiclerental.customer.application.port.input.CreateCustomerUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class OutboxAtomicityIT {

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Autowired
    private CreateCustomerUseCase createCustomerUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void customerAndOutboxEventPersistedInSameTransaction() {
        String uniqueEmail = "atomicity-" + UUID.randomUUID() + "@example.com";
        var command = new CreateCustomerCommand("John", "Doe", uniqueEmail, "+1234567890");

        createCustomerUseCase.execute(command);

        Integer customerCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customers", Integer.class);
        Integer outboxCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events", Integer.class);

        assertThat(customerCount).isGreaterThanOrEqualTo(1);
        assertThat(outboxCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    void bothRollBackOnDomainValidationFailure() {
        // Empty email triggers domain validation failure
        jdbcTemplate.execute("DELETE FROM outbox_events");
        jdbcTemplate.execute("DELETE FROM customers");

        var invalidCommand = new CreateCustomerCommand("John", "Doe", "", "+1234567890");

        assertThatThrownBy(() -> createCustomerUseCase.execute(invalidCommand))
                .isInstanceOf(RuntimeException.class);

        Integer customerCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customers", Integer.class);
        Integer outboxCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events", Integer.class);

        assertThat(customerCount).isZero();
        assertThat(outboxCount).isZero();
    }
}
