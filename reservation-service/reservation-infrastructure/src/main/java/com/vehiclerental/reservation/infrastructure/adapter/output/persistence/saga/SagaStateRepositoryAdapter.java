package com.vehiclerental.reservation.infrastructure.adapter.output.persistence.saga;

import com.vehiclerental.reservation.domain.model.saga.SagaState;
import com.vehiclerental.reservation.domain.port.output.SagaStateRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class SagaStateRepositoryAdapter implements SagaStateRepository {

    private final SagaStateJpaRepository jpaRepository;
    private final SagaStatePersistenceMapper mapper;

    public SagaStateRepositoryAdapter(SagaStateJpaRepository jpaRepository,
                                      SagaStatePersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public SagaState save(SagaState sagaState) {
        var jpaEntity = mapper.toJpaEntity(sagaState);
        var savedEntity = jpaRepository.saveAndFlush(jpaEntity);
        return mapper.toDomainEntity(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SagaState> findById(UUID sagaId) {
        return jpaRepository.findById(sagaId)
                .map(mapper::toDomainEntity);
    }
}
