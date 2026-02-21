package com.vehiclerental.payment.application.port.output;

import java.util.List;

public record PaymentGatewayResult(boolean success, List<String> failureMessages) {
}
