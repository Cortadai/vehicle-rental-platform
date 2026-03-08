package com.vehiclerental.reservation.application.saga;

public interface SagaStep<T> {

    void process(T data);

    void rollback(T data);

    String getName();

    boolean hasCompensation();
}
