package com.vehiclerental.reservation.domain.model.saga;

public enum SagaStatus {

    STARTED,
    PROCESSING,
    COMPENSATING,
    SUCCEEDED,
    FAILED;

    public boolean canTransitionTo(SagaStatus target) {
        return switch (this) {
            case STARTED -> target == PROCESSING;
            case PROCESSING -> target == SUCCEEDED || target == COMPENSATING;
            case COMPENSATING -> target == FAILED;
            case SUCCEEDED, FAILED -> false;
        };
    }
}
