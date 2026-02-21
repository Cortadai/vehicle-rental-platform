package com.vehiclerental.payment.domain.port.output;

import com.vehiclerental.payment.domain.model.aggregate.Payment;
import com.vehiclerental.payment.domain.model.vo.PaymentId;
import com.vehiclerental.payment.domain.model.vo.ReservationId;

import java.util.Optional;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(PaymentId paymentId);

    Optional<Payment> findByReservationId(ReservationId reservationId);
}
