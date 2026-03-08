package com.vehiclerental.reservation.application.port.output;

public interface SagaCommandPublisher {

    void publish(String exchange, String routingKey, String payload);
}
