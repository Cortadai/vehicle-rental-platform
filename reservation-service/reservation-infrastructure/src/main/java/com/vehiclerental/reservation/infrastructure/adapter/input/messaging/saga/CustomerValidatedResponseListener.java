package com.vehiclerental.reservation.infrastructure.adapter.input.messaging.saga;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehiclerental.reservation.application.saga.ReservationSagaOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CustomerValidatedResponseListener {

    private static final Logger log = LoggerFactory.getLogger(CustomerValidatedResponseListener.class);

    private final ReservationSagaOrchestrator orchestrator;
    private final ObjectMapper objectMapper;

    public CustomerValidatedResponseListener(ReservationSagaOrchestrator orchestrator,
                                             ObjectMapper objectMapper) {
        this.orchestrator = orchestrator;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "customer.validated.queue")
    public void handle(Message message) {
        JsonNode json = parseMessage(message);
        UUID reservationId = UUID.fromString(json.get("reservationId").asText());

        log.info("Received customer validated response for reservation {}", reservationId);
        orchestrator.handleStepSuccess(reservationId, "CUSTOMER_VALIDATION");
    }

    private JsonNode parseMessage(Message message) {
        try {
            return objectMapper.readTree(message.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse customer validated response", e);
        }
    }
}
