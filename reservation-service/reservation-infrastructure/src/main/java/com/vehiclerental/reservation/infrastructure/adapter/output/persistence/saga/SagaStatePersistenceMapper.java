package com.vehiclerental.reservation.infrastructure.adapter.output.persistence.saga;

import com.vehiclerental.reservation.domain.model.saga.SagaState;
import com.vehiclerental.reservation.domain.model.saga.SagaStatus;

public class SagaStatePersistenceMapper {

    public SagaStateJpaEntity toJpaEntity(SagaState sagaState) {
        SagaStateJpaEntity entity = new SagaStateJpaEntity();
        entity.setSagaId(sagaState.getSagaId());
        entity.setSagaType(sagaState.getSagaType());
        entity.setStatus(sagaState.getStatus().name());
        entity.setCurrentStep(sagaState.getCurrentStep());
        entity.setTotalSteps(sagaState.getTotalSteps());
        entity.setPayload(sagaState.getPayload());
        entity.setFailureReason(sagaState.getFailureReason());
        entity.setCreatedAt(sagaState.getCreatedAt());
        entity.setUpdatedAt(sagaState.getUpdatedAt());
        entity.setVersion(sagaState.getVersion());
        return entity;
    }

    public SagaState toDomainEntity(SagaStateJpaEntity entity) {
        return SagaState.reconstruct(
                entity.getSagaId(),
                entity.getSagaType(),
                SagaStatus.valueOf(entity.getStatus()),
                entity.getCurrentStep(),
                entity.getTotalSteps(),
                entity.getPayload(),
                entity.getFailureReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }
}
