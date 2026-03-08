package com.vehiclerental.payment.config;

import com.vehiclerental.payment.application.mapper.PaymentApplicationMapper;
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
}
