package com.vehiclerental.customer.infrastructure.adapter.input.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehiclerental.customer.application.dto.command.ValidateCustomerCommand;
import com.vehiclerental.customer.application.port.input.ValidateCustomerForReservationUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class CustomerValidationListener {

    private static final Logger log = LoggerFactory.getLogger(CustomerValidationListener.class);

    private final ValidateCustomerForReservationUseCase validateCustomerForReservationUseCase;
    private final ObjectMapper objectMapper;

    public CustomerValidationListener(ValidateCustomerForReservationUseCase validateCustomerForReservationUseCase,
                                      ObjectMapper objectMapper) {
        this.validateCustomerForReservationUseCase = validateCustomerForReservationUseCase;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "customer.validate.command.queue")
    public void handle(Message message) {
        log.info("Received customer validation command");

        JsonNode json = parseMessage(message);
        String customerId = json.get("customerId").asText();
        String reservationId = json.get("reservationId").asText();

        var command = new ValidateCustomerCommand(customerId, reservationId);
        validateCustomerForReservationUseCase.execute(command);

        log.info("Customer validation command processed for customerId={}, reservationId={}", customerId, reservationId);
    }

    private JsonNode parseMessage(Message message) {
        try {
            return objectMapper.readTree(message.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse customer validation command message", e);
        }
    }
}
