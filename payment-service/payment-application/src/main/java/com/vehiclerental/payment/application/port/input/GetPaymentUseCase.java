package com.vehiclerental.payment.application.port.input;

import com.vehiclerental.payment.application.dto.command.GetPaymentCommand;
import com.vehiclerental.payment.application.dto.response.PaymentResponse;

public interface GetPaymentUseCase {

    PaymentResponse execute(GetPaymentCommand command);
}
