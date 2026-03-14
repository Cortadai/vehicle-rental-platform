package com.vehiclerental.reservation.infrastructure.adapter.output.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehiclerental.common.domain.event.DomainEvent;
import com.vehiclerental.common.messaging.outbox.OutboxEvent;
import com.vehiclerental.common.messaging.outbox.OutboxEventRepository;
import com.vehiclerental.common.messaging.outbox.TraceContextHelper;
import com.vehiclerental.reservation.application.port.output.ReservationDomainEventPublisher;
import com.vehiclerental.reservation.domain.event.ReservationCancelledEvent;
import com.vehiclerental.reservation.domain.event.ReservationCreatedEvent;
import io.micrometer.tracing.Tracer;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class OutboxReservationDomainEventPublisher implements ReservationDomainEventPublisher {

    private static final String AGGREGATE_TYPE = "RESERVATION";
    private static final String EXCHANGE = "reservation.exchange";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;

    public OutboxReservationDomainEventPublisher(OutboxEventRepository outboxEventRepository,
                                                 ObjectMapper objectMapper,
                                                 Tracer tracer) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.tracer = tracer;
    }

    @Override
    public void publish(List<DomainEvent> domainEvents) {
        String traceParent = TraceContextHelper.currentTraceparent(tracer);
        for (DomainEvent event : domainEvents) {
            OutboxEvent outboxEvent = OutboxEvent.create(
                    AGGREGATE_TYPE,
                    extractAggregateId(event),
                    event.getClass().getSimpleName(),
                    serializeEvent(event),
                    deriveRoutingKey(event),
                    EXCHANGE,
                    traceParent
            );
            outboxEventRepository.save(outboxEvent);
        }
    }

    private String extractAggregateId(DomainEvent event) {
        if (event instanceof ReservationCreatedEvent created) {
            return created.reservationId().value().toString();
        }
        if (event instanceof ReservationCancelledEvent cancelled) {
            return cancelled.reservationId().value().toString();
        }
        throw new IllegalArgumentException("Unknown domain event type: " + event.getClass().getSimpleName());
    }

    private String deriveRoutingKey(DomainEvent event) {
        String simpleName = event.getClass().getSimpleName();
        String eventName = simpleName
                .replace("Reservation", "")
                .replace("Event", "")
                .toLowerCase();
        return "reservation." + eventName;
    }

    private String serializeEvent(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize domain event: " + event.getClass().getSimpleName(), e);
        }
    }
}
