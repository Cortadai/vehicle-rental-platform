package com.vehiclerental.reservation.infrastructure.adapter.output.messaging.saga;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehiclerental.common.messaging.outbox.OutboxEvent;
import com.vehiclerental.common.messaging.outbox.OutboxEventRepository;
import com.vehiclerental.common.messaging.outbox.TraceContextHelper;
import com.vehiclerental.reservation.application.port.output.SagaCommandPublisher;
import io.micrometer.tracing.Tracer;
import org.springframework.stereotype.Component;

@Component
public class OutboxSagaCommandPublisher implements SagaCommandPublisher {

    private static final String AGGREGATE_TYPE = "SAGA";
    private static final String EVENT_TYPE = "SAGA_COMMAND";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;

    public OutboxSagaCommandPublisher(OutboxEventRepository outboxEventRepository,
                                      ObjectMapper objectMapper,
                                      Tracer tracer) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.tracer = tracer;
    }

    @Override
    public void publish(String exchange, String routingKey, String payload) {
        String aggregateId = extractReservationId(payload);
        OutboxEvent outboxEvent = OutboxEvent.create(
                AGGREGATE_TYPE,
                aggregateId,
                EVENT_TYPE,
                payload,
                routingKey,
                exchange,
                TraceContextHelper.currentTraceparent(tracer)
        );
        outboxEventRepository.save(outboxEvent);
    }

    private String extractReservationId(String payload) {
        try {
            JsonNode json = objectMapper.readTree(payload);
            return json.get("reservationId").asText();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to extract reservationId from SAGA command payload", e);
        }
    }
}
