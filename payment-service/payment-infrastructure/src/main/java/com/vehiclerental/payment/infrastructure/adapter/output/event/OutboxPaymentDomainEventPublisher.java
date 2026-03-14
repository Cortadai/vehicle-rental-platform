package com.vehiclerental.payment.infrastructure.adapter.output.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehiclerental.common.domain.event.DomainEvent;
import com.vehiclerental.common.messaging.outbox.OutboxEvent;
import com.vehiclerental.common.messaging.outbox.OutboxEventRepository;
import com.vehiclerental.common.messaging.outbox.TraceContextHelper;
import com.vehiclerental.payment.application.port.output.PaymentDomainEventPublisher;
import com.vehiclerental.payment.domain.event.PaymentCompletedEvent;
import com.vehiclerental.payment.domain.event.PaymentFailedEvent;
import com.vehiclerental.payment.domain.event.PaymentRefundedEvent;
import io.micrometer.tracing.Tracer;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxPaymentDomainEventPublisher implements PaymentDomainEventPublisher {

    private static final String AGGREGATE_TYPE = "PAYMENT";
    private static final String EXCHANGE = "payment.exchange";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;

    public OutboxPaymentDomainEventPublisher(OutboxEventRepository outboxEventRepository,
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
        if (event instanceof PaymentCompletedEvent e) {
            return e.paymentId().value().toString();
        }
        if (event instanceof PaymentFailedEvent e) {
            return e.paymentId().value().toString();
        }
        if (event instanceof PaymentRefundedEvent e) {
            return e.paymentId().value().toString();
        }
        throw new IllegalArgumentException("Unknown domain event type: " + event.getClass().getSimpleName());
    }

    private String deriveRoutingKey(DomainEvent event) {
        String simpleName = event.getClass().getSimpleName();
        // PaymentCompletedEvent → payment.completed
        // PaymentFailedEvent → payment.failed
        // PaymentRefundedEvent → payment.refunded
        String eventName = simpleName
                .replace("Payment", "")
                .replace("Event", "")
                .toLowerCase();
        return "payment." + eventName;
    }

    private String serializeEvent(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize domain event: " + event.getClass().getSimpleName(), e);
        }
    }
}
