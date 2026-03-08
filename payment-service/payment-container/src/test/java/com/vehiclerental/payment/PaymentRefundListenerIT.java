package com.vehiclerental.payment;

import com.vehiclerental.payment.application.dto.command.ProcessPaymentCommand;
import com.vehiclerental.payment.application.port.input.ProcessPaymentUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class PaymentRefundListenerIT {

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProcessPaymentUseCase processPaymentUseCase;

    @Test
    void listenerRefundsExistingPaymentFromQueue() {
        String reservationId = UUID.randomUUID().toString();
        String customerId = UUID.randomUUID().toString();

        // Create a completed payment first
        var command = new ProcessPaymentCommand(reservationId, customerId, new BigDecimal("300.00"), "USD");
        processPaymentUseCase.execute(command);

        // Verify payment exists as COMPLETED
        String statusBefore = jdbcTemplate.queryForObject(
                "SELECT status FROM payments WHERE reservation_id = ?::uuid",
                String.class, reservationId);
        assertThat(statusBefore).isEqualTo("COMPLETED");

        // Send refund command via RabbitMQ as raw JSON
        String payload = """
                {
                    "reservationId": "%s"
                }
                """.formatted(reservationId);

        Message message = MessageBuilder
                .withBody(payload.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .build();

        rabbitTemplate.send("payment.exchange", "payment.refund.command", message);

        // Wait for listener to process the refund
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            String status = jdbcTemplate.queryForObject(
                    "SELECT status FROM payments WHERE reservation_id = ?::uuid",
                    String.class, reservationId);
            assertThat(status).isEqualTo("REFUNDED");
        });

        // Verify outbox has refund event (aggregate_id is paymentId, not reservationId)
        String paymentId = jdbcTemplate.queryForObject(
                "SELECT id FROM payments WHERE reservation_id = ?::uuid",
                String.class, reservationId);
        Integer outboxCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE aggregate_id = ? AND event_type = 'PaymentRefundedEvent'",
                Integer.class, paymentId);
        assertThat(outboxCount).isGreaterThanOrEqualTo(1);
    }
}
