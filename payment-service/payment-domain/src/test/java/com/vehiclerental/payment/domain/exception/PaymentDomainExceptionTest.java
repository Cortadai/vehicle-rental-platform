package com.vehiclerental.payment.domain.exception;

import com.vehiclerental.common.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentDomainExceptionTest {

    @Test
    void errorCodeAccessible() {
        var exception = new PaymentDomainException("something went wrong", "PAYMENT_ERROR");

        assertThat(exception.getErrorCode()).isEqualTo("PAYMENT_ERROR");
    }

    @Test
    void messageAccessible() {
        var exception = new PaymentDomainException("something went wrong", "PAYMENT_ERROR");

        assertThat(exception.getMessage()).isEqualTo("something went wrong");
    }

    @Test
    void constructorWithCause() {
        var cause = new RuntimeException("root cause");
        var exception = new PaymentDomainException("wrapped", "PAYMENT_ERROR", cause);

        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getErrorCode()).isEqualTo("PAYMENT_ERROR");
        assertThat(exception.getMessage()).isEqualTo("wrapped");
    }

    @Test
    void extendsDomainException() {
        var exception = new PaymentDomainException("test", "CODE");

        assertThat(exception).isInstanceOf(DomainException.class);
    }
}
