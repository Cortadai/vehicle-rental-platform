package com.vehiclerental.reservation.application.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vehiclerental.reservation.application.port.output.SagaCommandPublisher;

public class CustomerValidationStep implements SagaStep<ReservationSagaData> {

    private static final String EXCHANGE = "customer.exchange";
    private static final String ROUTING_KEY = "customer.validate.command";

    private final SagaCommandPublisher sagaCommandPublisher;
    private final ObjectMapper objectMapper;

    public CustomerValidationStep(SagaCommandPublisher sagaCommandPublisher,
                                  ObjectMapper objectMapper) {
        this.sagaCommandPublisher = sagaCommandPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(ReservationSagaData data) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("customerId", data.customerId().toString());
        payload.put("reservationId", data.reservationId().toString());
        sagaCommandPublisher.publish(EXCHANGE, ROUTING_KEY, payload.toString());
    }

    @Override
    public void rollback(ReservationSagaData data) {
        // No-op: customer validation is read-only, nothing to undo
    }

    @Override
    public String getName() {
        return "CUSTOMER_VALIDATION";
    }

    @Override
    public boolean hasCompensation() {
        return false;
    }
}
