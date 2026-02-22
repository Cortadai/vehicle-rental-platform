package com.vehiclerental.payment.infrastructure.adapter.output.persistence;

import com.vehiclerental.payment.domain.model.aggregate.Payment;
import com.vehiclerental.payment.domain.model.vo.PaymentId;
import com.vehiclerental.payment.domain.model.vo.ReservationId;
import com.vehiclerental.payment.domain.port.output.PaymentRepository;
import com.vehiclerental.payment.infrastructure.adapter.output.persistence.mapper.PaymentPersistenceMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;
    private final PaymentPersistenceMapper mapper;

    public PaymentRepositoryAdapter(PaymentJpaRepository jpaRepository,
                                    PaymentPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Payment save(Payment payment) {
        var jpaEntity = mapper.toJpaEntity(payment);
        var savedEntity = jpaRepository.save(jpaEntity);
        return mapper.toDomainEntity(savedEntity);
    }

    @Override
    public Optional<Payment> findById(PaymentId paymentId) {
        return jpaRepository.findById(paymentId.value())
                .map(mapper::toDomainEntity);
    }

    @Override
    public Optional<Payment> findByReservationId(ReservationId reservationId) {
        return jpaRepository.findByReservationId(reservationId.value())
                .map(mapper::toDomainEntity);
    }
}
