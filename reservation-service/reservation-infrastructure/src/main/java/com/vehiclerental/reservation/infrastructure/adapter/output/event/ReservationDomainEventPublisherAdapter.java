package com.vehiclerental.reservation.infrastructure.adapter.output.event;

import com.vehiclerental.common.domain.event.DomainEvent;
import com.vehiclerental.reservation.application.port.output.ReservationDomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReservationDomainEventPublisherAdapter implements ReservationDomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ReservationDomainEventPublisherAdapter.class);

    @Override
    public void publish(List<DomainEvent> domainEvents) {
        domainEvents.forEach(event ->
                log.info("EVENT LOGGED (not published): {} [eventId={}]",
                        event.getClass().getSimpleName(), event.eventId()));
    }
}
