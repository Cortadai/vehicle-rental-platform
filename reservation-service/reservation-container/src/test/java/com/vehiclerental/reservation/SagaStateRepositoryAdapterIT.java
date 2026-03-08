package com.vehiclerental.reservation;

import com.vehiclerental.reservation.domain.model.saga.SagaState;
import com.vehiclerental.reservation.domain.model.saga.SagaStatus;
import com.vehiclerental.reservation.domain.port.output.SagaStateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class SagaStateRepositoryAdapterIT {

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Autowired
    private SagaStateRepository sagaStateRepository;

    @Test
    void saveAndFindByIdRoundTrip() {
        UUID sagaId = UUID.randomUUID();
        SagaState sagaState = SagaState.create(sagaId, "RESERVATION_CREATION", 3, "{\"test\":true}");
        sagaState.beginProcessing();

        sagaStateRepository.save(sagaState);

        var found = sagaStateRepository.findById(sagaId);

        assertThat(found).isPresent();
        SagaState loaded = found.get();
        assertThat(loaded.getSagaId()).isEqualTo(sagaId);
        assertThat(loaded.getSagaType()).isEqualTo("RESERVATION_CREATION");
        assertThat(loaded.getStatus()).isEqualTo(SagaStatus.PROCESSING);
        assertThat(loaded.getCurrentStep()).isZero();
        assertThat(loaded.getTotalSteps()).isEqualTo(3);
        assertThat(loaded.getPayload()).isEqualTo("{\"test\":true}");
        assertThat(loaded.getFailureReason()).isNull();
        assertThat(loaded.getVersion()).isZero();
    }

    @Test
    void findByIdReturnsEmptyForNonExisting() {
        var result = sagaStateRepository.findById(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @Test
    void optimisticLockingIncrementsVersion() {
        UUID sagaId = UUID.randomUUID();
        SagaState sagaState = SagaState.create(sagaId, "RESERVATION_CREATION", 3, "{\"test\":true}");
        sagaState.beginProcessing();

        SagaState saved = sagaStateRepository.save(sagaState);
        assertThat(saved.getVersion()).isZero();

        // Reload, modify, and save again
        SagaState loaded = sagaStateRepository.findById(sagaId).orElseThrow();
        loaded.advanceToNextStep();
        SagaState updated = sagaStateRepository.save(loaded);

        assertThat(updated.getVersion()).isEqualTo(1L);
    }
}
