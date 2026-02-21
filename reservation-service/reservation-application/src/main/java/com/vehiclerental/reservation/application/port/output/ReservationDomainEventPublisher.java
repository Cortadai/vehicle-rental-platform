package com.vehiclerental.reservation.application.port.output;

import com.vehiclerental.common.domain.event.DomainEvent;

import java.util.List;

public interface ReservationDomainEventPublisher {
    void publish(List<DomainEvent> domainEvents);
}
