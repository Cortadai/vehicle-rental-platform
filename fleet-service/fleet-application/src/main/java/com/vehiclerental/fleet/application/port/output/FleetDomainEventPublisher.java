package com.vehiclerental.fleet.application.port.output;

import com.vehiclerental.common.domain.event.DomainEvent;

import java.util.List;

public interface FleetDomainEventPublisher {

    void publish(List<DomainEvent> events);
}
