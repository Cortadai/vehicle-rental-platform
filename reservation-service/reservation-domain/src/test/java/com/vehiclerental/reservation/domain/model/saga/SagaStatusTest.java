package com.vehiclerental.reservation.domain.model.saga;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SagaStatusTest {

    @Test
    void startedCanTransitionToProcessing() {
        assertThat(SagaStatus.STARTED.canTransitionTo(SagaStatus.PROCESSING)).isTrue();
    }

    @Test
    void processingCanTransitionToSucceeded() {
        assertThat(SagaStatus.PROCESSING.canTransitionTo(SagaStatus.SUCCEEDED)).isTrue();
    }

    @Test
    void processingCanTransitionToCompensating() {
        assertThat(SagaStatus.PROCESSING.canTransitionTo(SagaStatus.COMPENSATING)).isTrue();
    }

    @Test
    void compensatingCanTransitionToFailed() {
        assertThat(SagaStatus.COMPENSATING.canTransitionTo(SagaStatus.FAILED)).isTrue();
    }

    @Test
    void startedCannotTransitionToSucceeded() {
        assertThat(SagaStatus.STARTED.canTransitionTo(SagaStatus.SUCCEEDED)).isFalse();
    }

    @Test
    void startedCannotTransitionToCompensating() {
        assertThat(SagaStatus.STARTED.canTransitionTo(SagaStatus.COMPENSATING)).isFalse();
    }

    @Test
    void startedCannotTransitionToFailed() {
        assertThat(SagaStatus.STARTED.canTransitionTo(SagaStatus.FAILED)).isFalse();
    }

    @Test
    void compensatingCannotTransitionToSucceeded() {
        assertThat(SagaStatus.COMPENSATING.canTransitionTo(SagaStatus.SUCCEEDED)).isFalse();
    }

    @Test
    void compensatingCannotTransitionToProcessing() {
        assertThat(SagaStatus.COMPENSATING.canTransitionTo(SagaStatus.PROCESSING)).isFalse();
    }

    @Test
    void succeededRejectsAllTransitions() {
        for (SagaStatus target : SagaStatus.values()) {
            assertThat(SagaStatus.SUCCEEDED.canTransitionTo(target)).isFalse();
        }
    }

    @Test
    void failedRejectsAllTransitions() {
        for (SagaStatus target : SagaStatus.values()) {
            assertThat(SagaStatus.FAILED.canTransitionTo(target)).isFalse();
        }
    }
}
