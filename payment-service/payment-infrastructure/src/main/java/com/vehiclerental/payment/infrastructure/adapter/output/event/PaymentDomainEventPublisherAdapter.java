package com.vehiclerental.payment.infrastructure.adapter.output.event;

import com.vehiclerental.common.domain.event.DomainEvent;
import com.vehiclerental.payment.application.port.output.PaymentDomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentDomainEventPublisherAdapter implements PaymentDomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentDomainEventPublisherAdapter.class);

    @Override
    public void publish(List<DomainEvent> events) {
        events.forEach(event ->
                log.info("EVENT LOGGED (not published): {} [eventId={}]",
                        event.getClass().getSimpleName(), event.eventId()));
    }
}
