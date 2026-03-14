package com.vehiclerental.fleet.infrastructure.adapter.output.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehiclerental.common.domain.event.DomainEvent;
import com.vehiclerental.common.messaging.outbox.OutboxEvent;
import com.vehiclerental.common.messaging.outbox.OutboxEventRepository;
import com.vehiclerental.common.messaging.outbox.TraceContextHelper;
import com.vehiclerental.fleet.application.port.output.FleetDomainEventPublisher;
import com.vehiclerental.fleet.domain.event.*;
import io.micrometer.tracing.Tracer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class OutboxFleetDomainEventPublisher implements FleetDomainEventPublisher {

    private static final String AGGREGATE_TYPE = "FLEET";
    private static final String EXCHANGE = "fleet.exchange";

    private static final Map<Class<? extends DomainEvent>, String> ROUTING_KEYS = Map.of(
            VehicleRegisteredEvent.class, "fleet.registered",
            VehicleSentToMaintenanceEvent.class, "fleet.senttomaintenance",
            VehicleActivatedEvent.class, "fleet.activated",
            VehicleRetiredEvent.class, "fleet.retired",
            FleetConfirmedEvent.class, "fleet.confirmed",
            FleetRejectedEvent.class, "fleet.rejected",
            FleetReleasedEvent.class, "fleet.released"
    );

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;

    public OutboxFleetDomainEventPublisher(OutboxEventRepository outboxEventRepository,
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
        if (event instanceof VehicleRegisteredEvent e) return e.vehicleId().value().toString();
        if (event instanceof VehicleSentToMaintenanceEvent e) return e.vehicleId().value().toString();
        if (event instanceof VehicleActivatedEvent e) return e.vehicleId().value().toString();
        if (event instanceof VehicleRetiredEvent e) return e.vehicleId().value().toString();
        if (event instanceof FleetConfirmedEvent e) return e.vehicleId().value().toString();
        if (event instanceof FleetRejectedEvent e) return e.vehicleId().value().toString();
        if (event instanceof FleetReleasedEvent e) return e.vehicleId().value().toString();
        throw new IllegalArgumentException("Unknown domain event type: " + event.getClass().getSimpleName());
    }

    private String deriveRoutingKey(DomainEvent event) {
        String routingKey = ROUTING_KEYS.get(event.getClass());
        if (routingKey == null) {
            throw new IllegalArgumentException("No routing key for event type: " + event.getClass().getSimpleName());
        }
        return routingKey;
    }

    private String serializeEvent(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize domain event: " + event.getClass().getSimpleName(), e);
        }
    }
}
