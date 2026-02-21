package com.vehiclerental.payment.application.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentNotFoundExceptionTest {

    @Test
    void messageContainsIdentifier() {
        var identifier = "abc-123";

        var exception = new PaymentNotFoundException(identifier);

        assertThat(exception.getMessage()).contains(identifier);
    }

    @Test
    void extendsRuntimeException() {
        var exception = new PaymentNotFoundException("some-id");

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void doesNotExtendPaymentDomainException() {
        var exception = new PaymentNotFoundException("some-id");

        assertThat(exception)
                .isNotInstanceOf(com.vehiclerental.payment.domain.exception.PaymentDomainException.class);
    }
}
