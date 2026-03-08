package com.vehiclerental.reservation.infrastructure.adapter.input.messaging.saga;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehiclerental.reservation.application.saga.ReservationSagaOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class PaymentFailedResponseListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentFailedResponseListener.class);

    private final ReservationSagaOrchestrator orchestrator;
    private final ObjectMapper objectMapper;

    public PaymentFailedResponseListener(ReservationSagaOrchestrator orchestrator,
                                         ObjectMapper objectMapper) {
        this.orchestrator = orchestrator;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "payment.failed.queue")
    public void handle(Message message) {
        JsonNode json = parseMessage(message);
        UUID reservationId = UUID.fromString(json.get("reservationId").asText());
        List<String> failureMessages = extractFailureMessages(json);

        log.warn("Received payment failed response for reservation {}: {}", reservationId, failureMessages);
        orchestrator.handleStepFailure(reservationId, "PAYMENT", failureMessages);
    }

    private List<String> extractFailureMessages(JsonNode json) {
        List<String> messages = new ArrayList<>();
        JsonNode failureNode = json.get("failureMessages");
        if (failureNode != null && failureNode.isArray()) {
            failureNode.forEach(node -> messages.add(node.asText()));
        }
        return messages;
    }

    private JsonNode parseMessage(Message message) {
        try {
            return objectMapper.readTree(message.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse payment failed response", e);
        }
    }
}
