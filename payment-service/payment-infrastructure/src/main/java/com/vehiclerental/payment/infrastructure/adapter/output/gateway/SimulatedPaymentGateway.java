package com.vehiclerental.payment.infrastructure.adapter.output.gateway;

import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.payment.application.port.output.PaymentGateway;
import com.vehiclerental.payment.application.port.output.PaymentGatewayResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(SimulatedPaymentGateway.class);

    @Override
    public PaymentGatewayResult charge(Money amount) {
        log.info("Simulated payment gateway: charging {} {}",
                amount.amount(), amount.currency().getCurrencyCode());
        return new PaymentGatewayResult(true, List.of());
    }
}
