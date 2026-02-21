package com.vehiclerental.payment.application.port.output;

import com.vehiclerental.common.domain.vo.Money;

public interface PaymentGateway {

    PaymentGatewayResult charge(Money amount);
}
