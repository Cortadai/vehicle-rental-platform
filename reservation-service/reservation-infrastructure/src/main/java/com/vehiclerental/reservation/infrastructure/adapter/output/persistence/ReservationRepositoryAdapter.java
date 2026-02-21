package com.vehiclerental.reservation.infrastructure.adapter.output.persistence;

import com.vehiclerental.reservation.domain.model.aggregate.Reservation;
import com.vehiclerental.reservation.domain.model.vo.ReservationId;
import com.vehiclerental.reservation.domain.model.vo.TrackingId;
import com.vehiclerental.reservation.domain.port.output.ReservationRepository;
import com.vehiclerental.reservation.infrastructure.adapter.output.persistence.mapper.ReservationPersistenceMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class ReservationRepositoryAdapter implements ReservationRepository {

    private final ReservationJpaRepository jpaRepository;
    private final ReservationPersistenceMapper mapper;

    public ReservationRepositoryAdapter(ReservationJpaRepository jpaRepository,
                                        ReservationPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Reservation save(Reservation reservation) {
        var jpaEntity = mapper.toJpaEntity(reservation);
        var savedEntity = jpaRepository.save(jpaEntity);
        return mapper.toDomainEntity(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reservation> findById(ReservationId reservationId) {
        return jpaRepository.findById(reservationId.value())
                .map(mapper::toDomainEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reservation> findByTrackingId(TrackingId trackingId) {
        return jpaRepository.findByTrackingId(trackingId.value())
                .map(mapper::toDomainEntity);
    }
}
