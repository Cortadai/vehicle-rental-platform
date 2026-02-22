package com.vehiclerental.common.messaging.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private TransactionTemplate transactionTemplate;

    private OutboxPublisher outboxPublisher;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        outboxPublisher = new OutboxPublisher(outboxEventRepository, rabbitTemplate, transactionTemplate);

        lenient().doAnswer(invocation -> {
            Consumer<org.springframework.transaction.TransactionStatus> action =
                    invocation.getArgument(0, Consumer.class);
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    @DisplayName("No pending events results in no-op")
    void noPendingEvents_noOp() {
        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of());

        outboxPublisher.publishPendingEvents();

        verify(rabbitTemplate, never()).send(any(), any(), any(Message.class));
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Successful publish marks event as PUBLISHED and saves")
    void successfulPublish_marksPublishedAndSaves() {
        OutboxEvent event = OutboxEvent.create(
                "Reservation", "res-123", "ReservationCreatedEvent",
                "{\"id\":\"res-123\"}", "reservation.created", "reservation.exchange");
        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));

        outboxPublisher.publishPendingEvents();

        verify(rabbitTemplate).send(eq("reservation.exchange"), eq("reservation.created"), any(Message.class));
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();
        verify(outboxEventRepository).save(event);
    }

    @Test
    @DisplayName("Send failure increments retry count and saves")
    void sendFailure_incrementsRetryCountAndSaves() {
        OutboxEvent event = OutboxEvent.create(
                "Payment", "pay-456", "PaymentCompletedEvent",
                "{\"id\":\"pay-456\"}", "payment.completed", "payment.exchange");
        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));
        doThrow(new RuntimeException("Connection refused"))
                .when(rabbitTemplate).send(any(), any(), any(Message.class));

        outboxPublisher.publishPendingEvents();

        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        verify(outboxEventRepository).save(event);
    }

    @Test
    @DisplayName("Max retries (5) marks event as FAILED")
    void maxRetries_marksEventAsFailed() {
        OutboxEvent event = OutboxEvent.create(
                "Payment", "pay-789", "PaymentFailedEvent",
                "{\"id\":\"pay-789\"}", "payment.failed", "payment.exchange");
        // Simulate 4 previous retries
        for (int i = 0; i < 4; i++) {
            event.incrementRetryCount();
        }
        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));
        doThrow(new RuntimeException("Connection refused"))
                .when(rabbitTemplate).send(any(), any(), any(Message.class));

        outboxPublisher.publishPendingEvents();

        assertThat(event.getRetryCount()).isEqualTo(5);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        verify(outboxEventRepository).save(event);
    }

    @Test
    @DisplayName("AMQP message has correct headers (X-Aggregate-Type, X-Aggregate-Id, messageId)")
    void correctAmqpHeaders() {
        OutboxEvent event = OutboxEvent.create(
                "Fleet", "fleet-001", "FleetConfirmedEvent",
                "{\"vehicleId\":\"v-1\"}", "fleet.confirmed", "fleet.exchange");
        // Use reflection to set ID since it's normally assigned by JPA
        try {
            var idField = OutboxEvent.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(event, 42L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));

        outboxPublisher.publishPendingEvents();

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq("fleet.exchange"), eq("fleet.confirmed"), messageCaptor.capture());

        Message message = messageCaptor.getValue();
        assertThat((String) message.getMessageProperties().getHeader("X-Aggregate-Type")).isEqualTo("Fleet");
        assertThat((String) message.getMessageProperties().getHeader("X-Aggregate-Id")).isEqualTo("fleet-001");
        assertThat(message.getMessageProperties().getMessageId()).isEqualTo("42");
        assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(new String(message.getBody())).isEqualTo("{\"vehicleId\":\"v-1\"}");
    }
}
