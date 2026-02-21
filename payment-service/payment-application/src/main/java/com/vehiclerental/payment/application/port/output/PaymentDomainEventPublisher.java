package com.vehiclerental.payment.application.port.output;

import com.vehiclerental.common.domain.event.DomainEvent;

import java.util.List;

public interface PaymentDomainEventPublisher {

    void publish(List<DomainEvent> events);
}
