package com.vehiclerental.fleet;

import com.vehiclerental.fleet.application.dto.command.RegisterVehicleCommand;
import com.vehiclerental.fleet.application.port.input.RegisterVehicleUseCase;
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
    private RegisterVehicleUseCase registerVehicleUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void vehicleAndOutboxEventPersistedInSameTransaction() {
        var command = new RegisterVehicleCommand(
                "ATOM-001-XX", "Toyota", "Corolla", 2024,
                "SEDAN", new BigDecimal("45.00"), "USD", "Atomicity test");

        registerVehicleUseCase.execute(command);

        Integer vehicleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vehicles", Integer.class);
        Integer outboxCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events", Integer.class);

        assertThat(vehicleCount).isGreaterThanOrEqualTo(1);
        assertThat(outboxCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    void bothRollBackOnDomainValidationFailure() {
        jdbcTemplate.execute("DELETE FROM outbox_events");
        jdbcTemplate.execute("DELETE FROM vehicles");

        // Empty license plate triggers domain validation failure
        var invalidCommand = new RegisterVehicleCommand(
                "", "Toyota", "Corolla", 2024,
                "SEDAN", new BigDecimal("45.00"), "USD", "Should fail");

        assertThatThrownBy(() -> registerVehicleUseCase.execute(invalidCommand))
                .isInstanceOf(RuntimeException.class);

        Integer vehicleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vehicles", Integer.class);
        Integer outboxCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events", Integer.class);

        assertThat(vehicleCount).isZero();
        assertThat(outboxCount).isZero();
    }
}
