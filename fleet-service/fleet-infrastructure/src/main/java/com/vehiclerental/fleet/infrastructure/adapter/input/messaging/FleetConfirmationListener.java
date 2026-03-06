package com.vehiclerental.fleet.infrastructure.adapter.input.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehiclerental.fleet.application.dto.command.ConfirmFleetAvailabilityCommand;
import com.vehiclerental.fleet.application.port.input.ConfirmFleetAvailabilityUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class FleetConfirmationListener {

    private static final Logger log = LoggerFactory.getLogger(FleetConfirmationListener.class);

    private final ConfirmFleetAvailabilityUseCase confirmFleetAvailabilityUseCase;
    private final ObjectMapper objectMapper;

    public FleetConfirmationListener(ConfirmFleetAvailabilityUseCase confirmFleetAvailabilityUseCase,
                                     ObjectMapper objectMapper) {
        this.confirmFleetAvailabilityUseCase = confirmFleetAvailabilityUseCase;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "fleet.confirm.command.queue")
    public void handle(Message message) {
        log.info("Received fleet confirmation command");

        JsonNode json = parseMessage(message);
        String vehicleId = json.get("vehicleId").asText();
        String reservationId = json.get("reservationId").asText();
        String pickupDate = json.get("pickupDate").asText();
        String returnDate = json.get("returnDate").asText();

        var command = new ConfirmFleetAvailabilityCommand(vehicleId, reservationId, pickupDate, returnDate);
        confirmFleetAvailabilityUseCase.execute(command);

        log.info("Fleet confirmation command processed for vehicleId={}, reservationId={}", vehicleId, reservationId);
    }

    private JsonNode parseMessage(Message message) {
        try {
            return objectMapper.readTree(message.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse fleet confirmation command message", e);
        }
    }
}
