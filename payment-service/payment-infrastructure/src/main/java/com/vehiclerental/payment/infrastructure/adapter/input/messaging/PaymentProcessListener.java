package com.vehiclerental.payment.infrastructure.adapter.input.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehiclerental.payment.application.dto.command.ProcessPaymentCommand;
import com.vehiclerental.payment.application.port.input.ProcessPaymentUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PaymentProcessListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessListener.class);

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final ObjectMapper objectMapper;

    public PaymentProcessListener(ProcessPaymentUseCase processPaymentUseCase,
                                  ObjectMapper objectMapper) {
        this.processPaymentUseCase = processPaymentUseCase;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "payment.process.command.queue")
    public void handle(Message message) {
        log.info("Received payment process command");

        JsonNode json = parseMessage(message);
        String reservationId = json.get("reservationId").asText();
        String customerId = json.get("customerId").asText();
        BigDecimal amount = json.get("amount").decimalValue();
        String currency = json.get("currency").asText();

        var command = new ProcessPaymentCommand(reservationId, customerId, amount, currency);
        processPaymentUseCase.execute(command);

        log.info("Payment process command processed for reservationId={}, customerId={}", reservationId, customerId);
    }

    private JsonNode parseMessage(Message message) {
        try {
            return objectMapper.readTree(message.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse payment process command message", e);
        }
    }
}
