package com.vehiclerental.customer.domain.exception;

import com.vehiclerental.common.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerDomainExceptionTest {

    @Test
    void errorCodeAccessible() {
        var exception = new CustomerDomainException("something went wrong", "CUSTOMER_ERROR");

        assertThat(exception.getErrorCode()).isEqualTo("CUSTOMER_ERROR");
    }

    @Test
    void messageAccessible() {
        var exception = new CustomerDomainException("something went wrong", "CUSTOMER_ERROR");

        assertThat(exception.getMessage()).isEqualTo("something went wrong");
    }

    @Test
    void constructorWithCause() {
        var cause = new RuntimeException("root cause");
        var exception = new CustomerDomainException("wrapped", "CUSTOMER_ERROR", cause);

        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getErrorCode()).isEqualTo("CUSTOMER_ERROR");
        assertThat(exception.getMessage()).isEqualTo("wrapped");
    }

    @Test
    void extendsDomainException() {
        var exception = new CustomerDomainException("test", "CODE");

        assertThat(exception).isInstanceOf(DomainException.class);
    }
}
