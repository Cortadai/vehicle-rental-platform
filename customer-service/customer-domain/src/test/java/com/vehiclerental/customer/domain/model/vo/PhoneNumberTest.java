package com.vehiclerental.customer.domain.model.vo;

import com.vehiclerental.customer.domain.exception.CustomerDomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumberTest {

    @Test
    void validPhoneNumber() {
        var phone = new PhoneNumber("+1-555-123-4567");

        assertThat(phone.value()).isEqualTo("+1-555-123-4567");
    }

    @Test
    void phoneWithParentheses() {
        var phone = new PhoneNumber("(555) 123-4567");

        assertThat(phone.value()).isEqualTo("(555) 123-4567");
    }

    @Test
    void nullRejected() {
        assertThatThrownBy(() -> new PhoneNumber(null))
                .isInstanceOf(CustomerDomainException.class);
    }

    @Test
    void blankRejected() {
        assertThatThrownBy(() -> new PhoneNumber(""))
                .isInstanceOf(CustomerDomainException.class);
        assertThatThrownBy(() -> new PhoneNumber("   "))
                .isInstanceOf(CustomerDomainException.class);
    }

    @Test
    void tooShortRejected() {
        assertThatThrownBy(() -> new PhoneNumber("12"))
                .isInstanceOf(CustomerDomainException.class);
    }

    @Test
    void tooLongRejected() {
        assertThatThrownBy(() -> new PhoneNumber("123456789012345678901"))
                .isInstanceOf(CustomerDomainException.class);
    }

    @Test
    void lettersRejected() {
        assertThatThrownBy(() -> new PhoneNumber("555-ABC-1234"))
                .isInstanceOf(CustomerDomainException.class);
    }
}
