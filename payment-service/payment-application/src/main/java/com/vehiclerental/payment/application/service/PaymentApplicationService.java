package com.vehiclerental.payment.application.service;

import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.payment.application.dto.command.GetPaymentCommand;
import com.vehiclerental.payment.application.dto.command.ProcessPaymentCommand;
import com.vehiclerental.payment.application.dto.command.RefundPaymentCommand;
import com.vehiclerental.payment.application.dto.response.PaymentResponse;
import com.vehiclerental.payment.application.exception.PaymentNotFoundException;
import com.vehiclerental.payment.application.mapper.PaymentApplicationMapper;
import com.vehiclerental.payment.application.port.input.GetPaymentUseCase;
import com.vehiclerental.payment.application.port.input.ProcessPaymentUseCase;
import com.vehiclerental.payment.application.port.input.RefundPaymentUseCase;
import com.vehiclerental.payment.application.port.output.PaymentDomainEventPublisher;
import com.vehiclerental.payment.application.port.output.PaymentGateway;
import com.vehiclerental.payment.application.port.output.PaymentGatewayResult;
import com.vehiclerental.payment.domain.model.aggregate.Payment;
import com.vehiclerental.payment.domain.model.vo.CustomerId;
import com.vehiclerental.payment.domain.model.vo.PaymentId;
import com.vehiclerental.payment.domain.model.vo.ReservationId;
import com.vehiclerental.payment.domain.port.output.PaymentRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PaymentApplicationService implements
        ProcessPaymentUseCase,
        RefundPaymentUseCase,
        GetPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentDomainEventPublisher eventPublisher;
    private final PaymentGateway paymentGateway;
    private final PaymentApplicationMapper mapper;

    public PaymentApplicationService(PaymentRepository paymentRepository,
                                     PaymentDomainEventPublisher eventPublisher,
                                     PaymentGateway paymentGateway,
                                     PaymentApplicationMapper mapper) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
        this.paymentGateway = paymentGateway;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public PaymentResponse execute(ProcessPaymentCommand command) {
        ReservationId reservationId = new ReservationId(UUID.fromString(command.reservationId()));

        // Idempotency check: if a payment already exists for this reservation, return it as-is
        Optional<Payment> existingPayment = paymentRepository.findByReservationId(reservationId);
        if (existingPayment.isPresent()) {
            return mapper.toResponse(existingPayment.get());
        }

        CustomerId customerId = new CustomerId(UUID.fromString(command.customerId()));
        Money amount = new Money(command.amount(), Currency.getInstance(command.currency()));

        Payment payment = Payment.create(reservationId, customerId, amount);

        PaymentGatewayResult gatewayResult = paymentGateway.charge(amount);
        if (gatewayResult.success()) {
            payment.complete();
        } else {
            payment.fail(gatewayResult.failureMessages());
        }

        paymentRepository.save(payment);
        eventPublisher.publish(List.copyOf(payment.getDomainEvents()));
        payment.clearDomainEvents();

        return mapper.toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse execute(RefundPaymentCommand command) {
        ReservationId reservationId = new ReservationId(UUID.fromString(command.reservationId()));
        Payment payment = paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new PaymentNotFoundException(command.reservationId()));

        payment.refund();

        paymentRepository.save(payment);
        eventPublisher.publish(List.copyOf(payment.getDomainEvents()));
        payment.clearDomainEvents();

        return mapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse execute(GetPaymentCommand command) {
        PaymentId paymentId = new PaymentId(UUID.fromString(command.paymentId()));
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(command.paymentId()));

        return mapper.toResponse(payment);
    }
}
