package com.vehiclerental.reservation.application.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vehiclerental.reservation.application.port.output.SagaCommandPublisher;

public class PaymentStep implements SagaStep<ReservationSagaData> {

    private static final String EXCHANGE = "payment.exchange";
    private static final String PROCESS_ROUTING_KEY = "payment.process.command";
    private static final String REFUND_ROUTING_KEY = "payment.refund.command";

    private final SagaCommandPublisher sagaCommandPublisher;
    private final ObjectMapper objectMapper;

    public PaymentStep(SagaCommandPublisher sagaCommandPublisher,
                       ObjectMapper objectMapper) {
        this.sagaCommandPublisher = sagaCommandPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(ReservationSagaData data) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("reservationId", data.reservationId().toString());
        payload.put("customerId", data.customerId().toString());
        payload.put("amount", data.totalAmount());
        payload.put("currency", data.currency());
        sagaCommandPublisher.publish(EXCHANGE, PROCESS_ROUTING_KEY, payload.toString());
    }

    @Override
    public void rollback(ReservationSagaData data) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("reservationId", data.reservationId().toString());
        sagaCommandPublisher.publish(EXCHANGE, REFUND_ROUTING_KEY, payload.toString());
    }

    @Override
    public String getName() {
        return "PAYMENT";
    }

    @Override
    public boolean hasCompensation() {
        return true;
    }
}
