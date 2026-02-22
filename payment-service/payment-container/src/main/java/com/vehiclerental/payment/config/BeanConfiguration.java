package com.vehiclerental.payment.config;

import com.vehiclerental.payment.application.mapper.PaymentApplicationMapper;
import com.vehiclerental.payment.application.port.input.GetPaymentUseCase;
import com.vehiclerental.payment.application.port.input.ProcessPaymentUseCase;
import com.vehiclerental.payment.application.port.input.RefundPaymentUseCase;
import com.vehiclerental.payment.application.port.output.PaymentDomainEventPublisher;
import com.vehiclerental.payment.application.port.output.PaymentGateway;
import com.vehiclerental.payment.application.service.PaymentApplicationService;
import com.vehiclerental.payment.domain.port.output.PaymentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public PaymentApplicationMapper paymentApplicationMapper() {
        return new PaymentApplicationMapper();
    }

    @Bean
    public PaymentApplicationService paymentApplicationService(
            PaymentRepository paymentRepository,
            PaymentDomainEventPublisher eventPublisher,
            PaymentGateway paymentGateway,
            PaymentApplicationMapper mapper) {
        return new PaymentApplicationService(paymentRepository, eventPublisher, paymentGateway, mapper);
    }

    @Bean
    public ProcessPaymentUseCase processPaymentUseCase(PaymentApplicationService service) {
        return service;
    }

    @Bean
    public RefundPaymentUseCase refundPaymentUseCase(PaymentApplicationService service) {
        return service;
    }

    @Bean
    public GetPaymentUseCase getPaymentUseCase(PaymentApplicationService service) {
        return service;
    }
}
