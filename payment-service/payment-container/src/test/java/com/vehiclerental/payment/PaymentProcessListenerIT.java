package com.vehiclerental.payment;

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

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class PaymentProcessListenerIT {

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void listenerProcessesPaymentCommandFromQueue() {
        String reservationId = UUID.randomUUID().toString();
        String customerId = UUID.randomUUID().toString();

        String payload = """
                {
                    "reservationId": "%s",
                    "customerId": "%s",
                    "amount": 250.00,
                    "currency": "USD"
                }
                """.formatted(reservationId, customerId);

        Message message = MessageBuilder
                .withBody(payload.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .build();

        rabbitTemplate.send("payment.exchange", "payment.process.command", message);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM payments WHERE reservation_id = ?::uuid",
                    Integer.class, reservationId);
            assertThat(count).isEqualTo(1);
        });

        String paymentId = jdbcTemplate.queryForObject(
                "SELECT id FROM payments WHERE reservation_id = ?::uuid",
                String.class, reservationId);

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM payments WHERE reservation_id = ?::uuid",
                String.class, reservationId);
        assertThat(status).isEqualTo("COMPLETED");

        Integer outboxCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE aggregate_id = ?",
                Integer.class, paymentId);
        assertThat(outboxCount).isGreaterThanOrEqualTo(1);
    }
}
