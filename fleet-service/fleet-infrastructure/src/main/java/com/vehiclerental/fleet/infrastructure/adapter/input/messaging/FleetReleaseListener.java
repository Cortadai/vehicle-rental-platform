package com.vehiclerental.fleet.infrastructure.adapter.input.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehiclerental.fleet.application.dto.command.ReleaseFleetReservationCommand;
import com.vehiclerental.fleet.application.port.input.ReleaseFleetReservationUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class FleetReleaseListener {

    private static final Logger log = LoggerFactory.getLogger(FleetReleaseListener.class);

    private final ReleaseFleetReservationUseCase releaseFleetReservationUseCase;
    private final ObjectMapper objectMapper;

    public FleetReleaseListener(ReleaseFleetReservationUseCase releaseFleetReservationUseCase,
                                ObjectMapper objectMapper) {
        this.releaseFleetReservationUseCase = releaseFleetReservationUseCase;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "fleet.release.command.queue")
    public void handle(Message message) {
        log.info("Received fleet release command");

        JsonNode json = parseMessage(message);
        String vehicleId = json.get("vehicleId").asText();
        String reservationId = json.get("reservationId").asText();

        var command = new ReleaseFleetReservationCommand(vehicleId, reservationId);
        releaseFleetReservationUseCase.execute(command);

        log.info("Fleet release command processed for vehicleId={}, reservationId={}", vehicleId, reservationId);
    }

    private JsonNode parseMessage(Message message) {
        try {
            return objectMapper.readTree(message.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse fleet release command message", e);
        }
    }
}
