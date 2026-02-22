package com.vehiclerental.payment;

import com.vehiclerental.common.messaging.outbox.OutboxEvent;
import com.vehiclerental.common.messaging.outbox.OutboxEventRepository;
import com.vehiclerental.common.messaging.outbox.OutboxStatus;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class OutboxPublisherIT {

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void schedulerPublishesPendingEventToRabbitMQ() {
        String payload = "{\"eventId\":\"" + UUID.randomUUID() + "\",\"test\":true}";

        OutboxEvent outboxEvent = OutboxEvent.create(
                "PAYMENT",
                UUID.randomUUID().toString(),
                "PaymentCompletedEvent",
                payload,
                "payment.completed",
                "payment.exchange"
        );
        outboxEventRepository.save(outboxEvent);

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            OutboxEvent updated = outboxEventRepository.findById(outboxEvent.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
            assertThat(updated.getPublishedAt()).isNotNull();
        });

        Object message = rabbitTemplate.receiveAndConvert("payment.completed.queue", 2000);
        assertThat(message).isNotNull();
    }
}
