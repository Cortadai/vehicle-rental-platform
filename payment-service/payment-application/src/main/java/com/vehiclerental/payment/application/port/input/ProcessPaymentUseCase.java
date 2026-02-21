package com.vehiclerental.payment.application.port.input;

import com.vehiclerental.payment.application.dto.command.ProcessPaymentCommand;
import com.vehiclerental.payment.application.dto.response.PaymentResponse;

public interface ProcessPaymentUseCase {

    PaymentResponse execute(ProcessPaymentCommand command);
}
