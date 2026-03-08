package com.vehiclerental.reservation.domain.model.saga;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SagaStateTest {

    private static final UUID SAGA_ID = UUID.randomUUID();
    private static final String SAGA_TYPE = "RESERVATION_CREATION";
    private static final int TOTAL_STEPS = 3;
    private static final String PAYLOAD = "{\"reservationId\":\"test\"}";

    @Nested
    class Create {

        @Test
        void createsWithCorrectDefaults() {
            SagaState saga = SagaState.create(SAGA_ID, SAGA_TYPE, TOTAL_STEPS, PAYLOAD);

            assertThat(saga.getSagaId()).isEqualTo(SAGA_ID);
            assertThat(saga.getSagaType()).isEqualTo(SAGA_TYPE);
            assertThat(saga.getStatus()).isEqualTo(SagaStatus.STARTED);
            assertThat(saga.getCurrentStep()).isZero();
            assertThat(saga.getTotalSteps()).isEqualTo(TOTAL_STEPS);
            assertThat(saga.getPayload()).isEqualTo(PAYLOAD);
            assertThat(saga.getFailureReason()).isNull();
            assertThat(saga.getCreatedAt()).isNotNull();
            assertThat(saga.getUpdatedAt()).isNotNull();
            assertThat(saga.getVersion()).isNull();
        }

        @Test
        void rejectsNullSagaId() {
            assertThatThrownBy(() -> SagaState.create(null, SAGA_TYPE, TOTAL_STEPS, PAYLOAD))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void rejectsNullSagaType() {
            assertThatThrownBy(() -> SagaState.create(SAGA_ID, null, TOTAL_STEPS, PAYLOAD))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void rejectsNullPayload() {
            assertThatThrownBy(() -> SagaState.create(SAGA_ID, SAGA_TYPE, TOTAL_STEPS, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Reconstruct {

        @Test
        void reconstructsWithAllFieldsExact() {
            Instant created = Instant.parse("2026-01-01T00:00:00Z");
            Instant updated = Instant.parse("2026-01-01T01:00:00Z");

            SagaState saga = SagaState.reconstruct(SAGA_ID, SAGA_TYPE, SagaStatus.PROCESSING,
                    1, TOTAL_STEPS, PAYLOAD, "some reason", created, updated, 5L);

            assertThat(saga.getSagaId()).isEqualTo(SAGA_ID);
            assertThat(saga.getSagaType()).isEqualTo(SAGA_TYPE);
            assertThat(saga.getStatus()).isEqualTo(SagaStatus.PROCESSING);
            assertThat(saga.getCurrentStep()).isEqualTo(1);
            assertThat(saga.getTotalSteps()).isEqualTo(TOTAL_STEPS);
            assertThat(saga.getPayload()).isEqualTo(PAYLOAD);
            assertThat(saga.getFailureReason()).isEqualTo("some reason");
            assertThat(saga.getCreatedAt()).isEqualTo(created);
            assertThat(saga.getUpdatedAt()).isEqualTo(updated);
            assertThat(saga.getVersion()).isEqualTo(5L);
        }
    }

    @Nested
    class BeginProcessing {

        @Test
        void transitionsFromStartedToProcessing() {
            SagaState saga = SagaState.create(SAGA_ID, SAGA_TYPE, TOTAL_STEPS, PAYLOAD);
            Instant beforeTransition = saga.getUpdatedAt();

            saga.beginProcessing();

            assertThat(saga.getStatus()).isEqualTo(SagaStatus.PROCESSING);
            assertThat(saga.getUpdatedAt()).isAfterOrEqualTo(beforeTransition);
        }

        @Test
        void rejectsFromProcessing() {
            SagaState saga = createProcessingSaga();

            assertThatThrownBy(saga::beginProcessing)
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class AdvanceToNextStep {

        @Test
        void incrementsCurrentStep() {
            SagaState saga = createProcessingSaga();

            saga.advanceToNextStep();

            assertThat(saga.getCurrentStep()).isEqualTo(1);
        }

        @Test
        void incrementsMultipleTimes() {
            SagaState saga = createProcessingSaga();

            saga.advanceToNextStep();
            saga.advanceToNextStep();

            assertThat(saga.getCurrentStep()).isEqualTo(2);
        }

        @Test
        void rejectsFromStarted() {
            SagaState saga = SagaState.create(SAGA_ID, SAGA_TYPE, TOTAL_STEPS, PAYLOAD);

            assertThatThrownBy(saga::advanceToNextStep)
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class MarkAsSucceeded {

        @Test
        void transitionsFromProcessingToSucceeded() {
            SagaState saga = createProcessingSaga();

            saga.markAsSucceeded();

            assertThat(saga.getStatus()).isEqualTo(SagaStatus.SUCCEEDED);
        }

        @Test
        void rejectsFromStarted() {
            SagaState saga = SagaState.create(SAGA_ID, SAGA_TYPE, TOTAL_STEPS, PAYLOAD);

            assertThatThrownBy(saga::markAsSucceeded)
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void rejectsFromCompensating() {
            SagaState saga = createCompensatingSaga();

            assertThatThrownBy(saga::markAsSucceeded)
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class StartCompensation {

        @Test
        void transitionsFromProcessingToCompensating() {
            SagaState saga = createProcessingSaga();

            saga.startCompensation("Payment failed");

            assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATING);
            assertThat(saga.getFailureReason()).isEqualTo("Payment failed");
        }

        @Test
        void rejectsFromStarted() {
            SagaState saga = SagaState.create(SAGA_ID, SAGA_TYPE, TOTAL_STEPS, PAYLOAD);

            assertThatThrownBy(() -> saga.startCompensation("reason"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class DecrementStep {

        @Test
        void decrementsCurrentStep() {
            SagaState saga = createProcessingSaga();
            saga.advanceToNextStep();
            saga.advanceToNextStep();

            saga.decrementStep();

            assertThat(saga.getCurrentStep()).isEqualTo(1);
        }
    }

    @Nested
    class MarkAsFailed {

        @Test
        void transitionsFromCompensatingToFailed() {
            SagaState saga = createCompensatingSaga();

            saga.markAsFailed();

            assertThat(saga.getStatus()).isEqualTo(SagaStatus.FAILED);
        }

        @Test
        void rejectsFromProcessing() {
            SagaState saga = createProcessingSaga();

            assertThatThrownBy(saga::markAsFailed)
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void rejectsFromStarted() {
            SagaState saga = SagaState.create(SAGA_ID, SAGA_TYPE, TOTAL_STEPS, PAYLOAD);

            assertThatThrownBy(saga::markAsFailed)
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    private SagaState createProcessingSaga() {
        SagaState saga = SagaState.create(SAGA_ID, SAGA_TYPE, TOTAL_STEPS, PAYLOAD);
        saga.beginProcessing();
        return saga;
    }

    private SagaState createCompensatingSaga() {
        SagaState saga = createProcessingSaga();
        saga.startCompensation("test failure");
        return saga;
    }
}
