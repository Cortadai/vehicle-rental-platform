package com.vehiclerental.customer.infrastructure.adapter.output.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehiclerental.common.domain.event.DomainEvent;
import com.vehiclerental.common.messaging.outbox.OutboxEvent;
import com.vehiclerental.common.messaging.outbox.OutboxEventRepository;
import com.vehiclerental.common.messaging.outbox.TraceContextHelper;
import com.vehiclerental.customer.application.port.output.CustomerDomainEventPublisher;
import com.vehiclerental.customer.domain.event.*;
import io.micrometer.tracing.Tracer;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxCustomerDomainEventPublisher implements CustomerDomainEventPublisher {

    private static final String AGGREGATE_TYPE = "CUSTOMER";
    private static final String EXCHANGE = "customer.exchange";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;

    public OutboxCustomerDomainEventPublisher(OutboxEventRepository outboxEventRepository,
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
        if (event instanceof CustomerCreatedEvent e) {
            return e.customerId().value().toString();
        }
        if (event instanceof CustomerSuspendedEvent e) {
            return e.customerId().value().toString();
        }
        if (event instanceof CustomerActivatedEvent e) {
            return e.customerId().value().toString();
        }
        if (event instanceof CustomerDeletedEvent e) {
            return e.customerId().value().toString();
        }
        if (event instanceof CustomerValidatedEvent e) {
            return e.customerId().value().toString();
        }
        if (event instanceof CustomerRejectedEvent e) {
            return e.customerId().value().toString();
        }
        throw new IllegalArgumentException("Unknown domain event type: " + event.getClass().getSimpleName());
    }

    private String deriveRoutingKey(DomainEvent event) {
        String simpleName = event.getClass().getSimpleName();
        String eventName = simpleName
                .replace("Customer", "")
                .replace("Event", "")
                .toLowerCase();
        return "customer." + eventName;
    }

    private String serializeEvent(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize domain event: " + event.getClass().getSimpleName(), e);
        }
    }
}
