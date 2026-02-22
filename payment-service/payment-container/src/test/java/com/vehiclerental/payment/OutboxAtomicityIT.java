package com.vehiclerental.payment;

import com.vehiclerental.payment.application.dto.command.ProcessPaymentCommand;
import com.vehiclerental.payment.application.port.input.ProcessPaymentUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
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
    private ProcessPaymentUseCase processPaymentUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void paymentAndOutboxEventPersistedInSameTransaction() {
        var command = new ProcessPaymentCommand(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                new BigDecimal("150.00"),
                "USD");

        processPaymentUseCase.execute(command);

        Integer paymentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payments", Integer.class);
        Integer outboxCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events", Integer.class);

        assertThat(paymentCount).isGreaterThanOrEqualTo(1);
        assertThat(outboxCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    void bothRollBackOnDomainValidationFailure() {
        // Zero amount triggers domain validation failure
        var invalidCommand = new ProcessPaymentCommand(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                new BigDecimal("0.00"),
                "USD");

        // Clear tables to ensure clean state
        jdbcTemplate.execute("DELETE FROM outbox_events");
        jdbcTemplate.execute("DELETE FROM payments");

        assertThatThrownBy(() -> processPaymentUseCase.execute(invalidCommand))
                .isInstanceOf(RuntimeException.class);

        Integer paymentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payments", Integer.class);
        Integer outboxCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events", Integer.class);

        assertThat(paymentCount).isZero();
        assertThat(outboxCount).isZero();
    }
}
