package com.vehiclerental.customer.application.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerNotFoundExceptionTest {

    @Test
    void messageContainsCustomerId() {
        var customerId = "abc-123";

        var exception = new CustomerNotFoundException(customerId);

        assertThat(exception.getMessage()).contains(customerId);
    }

    @Test
    void extendsRuntimeException() {
        var exception = new CustomerNotFoundException("some-id");

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void doesNotExtendCustomerDomainException() {
        var exception = new CustomerNotFoundException("some-id");

        assertThat(exception)
                .isNotInstanceOf(com.vehiclerental.customer.domain.exception.CustomerDomainException.class);
    }
}
