package com.vehiclerental.reservation;

import com.vehiclerental.reservation.application.dto.command.CreateReservationCommand;
import com.vehiclerental.reservation.application.port.input.CreateReservationUseCase;
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
import java.util.List;
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
    private CreateReservationUseCase createReservationUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void reservationAndOutboxEventPersistedInSameTransaction() {
        var command = new CreateReservationCommand(
                UUID.randomUUID().toString(),
                "100 Main St", "New York",
                "200 Oak Ave", "Boston",
                "2026-06-01", "2026-06-05",
                "USD",
                List.of(new CreateReservationCommand.CreateReservationItemCommand(
                        UUID.randomUUID().toString(),
                        new BigDecimal("50.00"),
                        4))
        );

        createReservationUseCase.execute(command);

        Integer reservationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservations", Integer.class);
        Integer outboxCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events", Integer.class);

        assertThat(reservationCount).isGreaterThanOrEqualTo(1);
        assertThat(outboxCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    void bothRollBackOnDomainValidationFailure() {
        // Return date before pickup date triggers domain validation failure
        var invalidCommand = new CreateReservationCommand(
                UUID.randomUUID().toString(),
                "100 Main St", "New York",
                "200 Oak Ave", "Boston",
                "2026-06-10", "2026-06-05",
                "USD",
                List.of(new CreateReservationCommand.CreateReservationItemCommand(
                        UUID.randomUUID().toString(),
                        new BigDecimal("50.00"),
                        4))
        );

        // Clear tables to ensure clean state
        jdbcTemplate.execute("DELETE FROM outbox_events");
        jdbcTemplate.execute("DELETE FROM reservation_items");
        jdbcTemplate.execute("DELETE FROM reservations");

        assertThatThrownBy(() -> createReservationUseCase.execute(invalidCommand))
                .isInstanceOf(RuntimeException.class);

        Integer reservationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservations", Integer.class);
        Integer outboxCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events", Integer.class);

        assertThat(reservationCount).isZero();
        assertThat(outboxCount).isZero();
    }
}
