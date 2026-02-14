package com.vehiclerental.customer.domain.model.vo;

import com.vehiclerental.customer.domain.exception.CustomerDomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void validEmail() {
        var email = new Email("john@example.com");

        assertThat(email.value()).isEqualTo("john@example.com");
    }

    @Test
    void emailWithSubdomains() {
        var email = new Email("user@mail.example.com");

        assertThat(email.value()).isEqualTo("user@mail.example.com");
    }

    @Test
    void nullRejected() {
        assertThatThrownBy(() -> new Email(null))
                .isInstanceOf(CustomerDomainException.class);
    }

    @Test
    void blankRejected() {
        assertThatThrownBy(() -> new Email(""))
                .isInstanceOf(CustomerDomainException.class);
        assertThatThrownBy(() -> new Email("   "))
                .isInstanceOf(CustomerDomainException.class);
    }

    @Test
    void missingAtRejected() {
        assertThatThrownBy(() -> new Email("johnexample.com"))
                .isInstanceOf(CustomerDomainException.class);
    }

    @Test
    void missingDomainRejected() {
        assertThatThrownBy(() -> new Email("john@"))
                .isInstanceOf(CustomerDomainException.class);
    }

    @Test
    void missingLocalPartRejected() {
        assertThatThrownBy(() -> new Email("@example.com"))
                .isInstanceOf(CustomerDomainException.class);
    }
}
