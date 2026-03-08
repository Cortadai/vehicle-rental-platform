package com.vehiclerental.reservation.application.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vehiclerental.reservation.application.port.output.SagaCommandPublisher;

public class FleetConfirmationStep implements SagaStep<ReservationSagaData> {

    private static final String EXCHANGE = "fleet.exchange";
    private static final String ROUTING_KEY = "fleet.confirm.command";

    private final SagaCommandPublisher sagaCommandPublisher;
    private final ObjectMapper objectMapper;

    public FleetConfirmationStep(SagaCommandPublisher sagaCommandPublisher,
                                 ObjectMapper objectMapper) {
        this.sagaCommandPublisher = sagaCommandPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(ReservationSagaData data) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("vehicleId", data.vehicleId().toString());
        payload.put("reservationId", data.reservationId().toString());
        payload.put("pickupDate", data.pickupDate().toString());
        payload.put("returnDate", data.returnDate().toString());
        sagaCommandPublisher.publish(EXCHANGE, ROUTING_KEY, payload.toString());
    }

    @Override
    public void rollback(ReservationSagaData data) {
        // No-op: Fleet is the last step — if it rejects, nothing was confirmed
    }

    @Override
    public String getName() {
        return "FLEET_CONFIRMATION";
    }

    @Override
    public boolean hasCompensation() {
        return false;
    }
}
