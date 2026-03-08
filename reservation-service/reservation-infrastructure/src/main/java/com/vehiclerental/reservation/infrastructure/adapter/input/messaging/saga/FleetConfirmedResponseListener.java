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
public class FleetConfirmedResponseListener {

    private static final Logger log = LoggerFactory.getLogger(FleetConfirmedResponseListener.class);

    private final ReservationSagaOrchestrator orchestrator;
    private final ObjectMapper objectMapper;

    public FleetConfirmedResponseListener(ReservationSagaOrchestrator orchestrator,
                                          ObjectMapper objectMapper) {
        this.orchestrator = orchestrator;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "fleet.confirmed.queue")
    public void handle(Message message) {
        JsonNode json = parseMessage(message);
        UUID reservationId = UUID.fromString(json.get("reservationId").asText());

        log.info("Received fleet confirmed response for reservation {}", reservationId);
        orchestrator.handleStepSuccess(reservationId, "FLEET_CONFIRMATION");
    }

    private JsonNode parseMessage(Message message) {
        try {
            return objectMapper.readTree(message.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse fleet confirmed response", e);
        }
    }
}
