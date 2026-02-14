package com.vehiclerental.customer.infrastructure.adapter.output.event;

import com.vehiclerental.common.domain.event.DomainEvent;
import com.vehiclerental.customer.application.port.output.CustomerDomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomerDomainEventPublisherAdapter implements CustomerDomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CustomerDomainEventPublisherAdapter.class);

    @Override
    public void publish(List<DomainEvent> events) {
        events.forEach(event ->
                log.info("EVENT LOGGED (not published): {} [eventId={}]",
                        event.getClass().getSimpleName(), event.eventId()));
    }
}
