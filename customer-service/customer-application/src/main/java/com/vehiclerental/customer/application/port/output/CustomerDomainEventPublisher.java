package com.vehiclerental.customer.application.port.output;

import com.vehiclerental.common.domain.event.DomainEvent;

import java.util.List;

public interface CustomerDomainEventPublisher {

    void publish(List<DomainEvent> events);
}
