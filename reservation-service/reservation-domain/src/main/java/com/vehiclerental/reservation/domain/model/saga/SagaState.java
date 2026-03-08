package com.vehiclerental.reservation.domain.model.saga;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class SagaState {

    private final UUID sagaId;
    private final String sagaType;
    private SagaStatus status;
    private int currentStep;
    private final int totalSteps;
    private final String payload;
    private String failureReason;
    private final Instant createdAt;
    private Instant updatedAt;
    private Long version;

    private SagaState(UUID sagaId, String sagaType, SagaStatus status, int currentStep,
                      int totalSteps, String payload, String failureReason,
                      Instant createdAt, Instant updatedAt, Long version) {
        this.sagaId = Objects.requireNonNull(sagaId, "sagaId must not be null");
        this.sagaType = Objects.requireNonNull(sagaType, "sagaType must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.currentStep = currentStep;
        this.totalSteps = totalSteps;
        this.payload = Objects.requireNonNull(payload, "payload must not be null");
        this.failureReason = failureReason;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.version = version;
    }

    public static SagaState create(UUID sagaId, String sagaType, int totalSteps, String payload) {
        Instant now = Instant.now();
        return new SagaState(sagaId, sagaType, SagaStatus.STARTED, 0, totalSteps,
                payload, null, now, now, null);
    }

    public static SagaState reconstruct(UUID sagaId, String sagaType, SagaStatus status,
                                        int currentStep, int totalSteps, String payload,
                                        String failureReason, Instant createdAt,
                                        Instant updatedAt, Long version) {
        return new SagaState(sagaId, sagaType, status, currentStep, totalSteps,
                payload, failureReason, createdAt, updatedAt, version);
    }

    public void beginProcessing() {
        validateTransition(SagaStatus.PROCESSING);
        this.status = SagaStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    public void advanceToNextStep() {
        if (this.status != SagaStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Cannot advance step in status " + this.status + "; must be PROCESSING");
        }
        this.currentStep++;
        this.updatedAt = Instant.now();
    }

    public void markAsSucceeded() {
        validateTransition(SagaStatus.SUCCEEDED);
        this.status = SagaStatus.SUCCEEDED;
        this.updatedAt = Instant.now();
    }

    public void startCompensation(String reason) {
        validateTransition(SagaStatus.COMPENSATING);
        this.status = SagaStatus.COMPENSATING;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public void decrementStep() {
        this.currentStep--;
        this.updatedAt = Instant.now();
    }

    public void markAsFailed() {
        validateTransition(SagaStatus.FAILED);
        this.status = SagaStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    private void validateTransition(SagaStatus target) {
        if (!this.status.canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Invalid SAGA transition from " + this.status + " to " + target);
        }
    }

    public UUID getSagaId() {
        return sagaId;
    }

    public String getSagaType() {
        return sagaType;
    }

    public SagaStatus getStatus() {
        return status;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public String getPayload() {
        return payload;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
