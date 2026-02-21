package com.vehiclerental.payment.application.port.input;

import com.vehiclerental.payment.application.dto.command.RefundPaymentCommand;
import com.vehiclerental.payment.application.dto.response.PaymentResponse;

public interface RefundPaymentUseCase {

    PaymentResponse execute(RefundPaymentCommand command);
}
