package com.vehiclerental.payment.infrastructure.adapter.input.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehiclerental.payment.application.dto.command.RefundPaymentCommand;
import com.vehiclerental.payment.application.port.input.RefundPaymentUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentRefundListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentRefundListener.class);

    private final RefundPaymentUseCase refundPaymentUseCase;
    private final ObjectMapper objectMapper;

    public PaymentRefundListener(RefundPaymentUseCase refundPaymentUseCase,
                                 ObjectMapper objectMapper) {
        this.refundPaymentUseCase = refundPaymentUseCase;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "payment.refund.command.queue")
    public void handle(Message message) {
        log.info("Received payment refund command");

        JsonNode json = parseMessage(message);
        String reservationId = json.get("reservationId").asText();

        var command = new RefundPaymentCommand(reservationId);
        refundPaymentUseCase.execute(command);

        log.info("Payment refund command processed for reservationId={}", reservationId);
    }

    private JsonNode parseMessage(Message message) {
        try {
            return objectMapper.readTree(message.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse payment refund command message", e);
        }
    }
}
