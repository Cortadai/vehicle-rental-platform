package com.vehiclerental.common.domain.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainExceptionTest {

    // Concrete subclass for testing the abstract DomainException
    private static class TestDomainException extends DomainException {
        TestDomainException(String message, String errorCode) {
            super(message, errorCode);
        }

        TestDomainException(String message, String errorCode, Throwable cause) {
            super(message, errorCode, cause);
        }
    }

    @Test
    void errorCode_shouldBeAccessible() {
        var exception = new TestDomainException("Customer not found", "CUSTOMER_NOT_FOUND");

        assertThat(exception.getErrorCode()).isEqualTo("CUSTOMER_NOT_FOUND");
        assertThat(exception.getMessage()).isEqualTo("Customer not found");
    }

    @Test
    void nullErrorCode_shouldThrow() {
        assertThatThrownBy(() -> new TestDomainException("message", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void blankErrorCode_shouldThrow() {
        assertThatThrownBy(() -> new TestDomainException("message", ""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new TestDomainException("message", "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructorWithCause_shouldPreserveCause() {
        var cause = new RuntimeException("underlying error");
        var exception = new TestDomainException("Wrapped error", "INTERNAL_ERROR", cause);

        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getErrorCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(exception.getMessage()).isEqualTo("Wrapped error");
    }
}
