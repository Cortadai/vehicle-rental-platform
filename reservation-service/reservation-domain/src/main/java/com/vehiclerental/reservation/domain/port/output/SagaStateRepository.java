package com.vehiclerental.reservation.domain.port.output;

import com.vehiclerental.reservation.domain.model.saga.SagaState;

import java.util.Optional;
import java.util.UUID;

public interface SagaStateRepository {

    SagaState save(SagaState sagaState);

    Optional<SagaState> findById(UUID sagaId);
}
