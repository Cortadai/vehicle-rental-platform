package com.vehiclerental.customer.domain.model.aggregate;

import com.vehiclerental.customer.domain.event.CustomerCreatedEvent;
import com.vehiclerental.customer.domain.exception.CustomerDomainException;
import com.vehiclerental.customer.domain.model.vo.CustomerId;
import com.vehiclerental.customer.domain.model.vo.CustomerStatus;
import com.vehiclerental.customer.domain.model.vo.Email;
import com.vehiclerental.customer.domain.model.vo.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerTest {

    private static final Email VALID_EMAIL = new Email("john@example.com");
    private static final PhoneNumber VALID_PHONE = new PhoneNumber("+1-555-123-4567");

    @Test
    void successfulCreation() {
        var customer = Customer.create("John", "Doe", VALID_EMAIL, VALID_PHONE);

        assertThat(customer.getId()).isNotNull();
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(customer.getDomainEvents()).hasSize(1);
        assertThat(customer.getDomainEvents().get(0)).isInstanceOf(CustomerCreatedEvent.class);
    }

    @Test
    void nullFirstNameRejected() {
        assertThatThrownBy(() -> Customer.create(null, "Doe", VALID_EMAIL, VALID_PHONE))
                .isInstanceOf(CustomerDomainException.class);
    }

    @Test
    void blankFirstNameRejected() {
        assertThatThrownBy(() -> Customer.create("", "Doe", VALID_EMAIL, VALID_PHONE))
                .isInstanceOf(CustomerDomainException.class);
        assertThatThrownBy(() -> Customer.create("   ", "Doe", VALID_EMAIL, VALID_PHONE))
                .isInstanceOf(CustomerDomainException.class);
    }

    @Test
    void nullLastNameRejected() {
        assertThatThrownBy(() -> Customer.create("John", null, VALID_EMAIL, VALID_PHONE))
                .isInstanceOf(CustomerDomainException.class);
    }

    @Test
    void blankLastNameRejected() {
        assertThatThrownBy(() -> Customer.create("John", "", VALID_EMAIL, VALID_PHONE))
                .isInstanceOf(CustomerDomainException.class);
        assertThatThrownBy(() -> Customer.create("John", "   ", VALID_EMAIL, VALID_PHONE))
                .isInstanceOf(CustomerDomainException.class);
    }

    @Test
    void nullEmailRejected() {
        assertThatThrownBy(() -> Customer.create("John", "Doe", null, VALID_PHONE))
                .isInstanceOf(CustomerDomainException.class);
    }

    @Test
    void nullPhoneAccepted() {
        var customer = Customer.create("John", "Doe", VALID_EMAIL, null);

        assertThat(customer.getPhone()).isNull();
    }

    @Test
    void fieldsAccessibleAfterCreation() {
        var customer = Customer.create("John", "Doe", VALID_EMAIL, VALID_PHONE);

        assertThat(customer.getFirstName()).isEqualTo("John");
        assertThat(customer.getLastName()).isEqualTo("Doe");
        assertThat(customer.getEmail()).isEqualTo(VALID_EMAIL);
        assertThat(customer.getPhone()).isEqualTo(VALID_PHONE);
        assertThat(customer.getCreatedAt()).isNotNull();
    }

    @Test
    void reconstructDoesNotEmitEvents() {
        var customerId = new CustomerId(UUID.randomUUID());
        var createdAt = Instant.now();

        var customer = Customer.reconstruct(customerId, "John", "Doe", VALID_EMAIL, VALID_PHONE,
                CustomerStatus.ACTIVE, createdAt);

        assertThat(customer.getDomainEvents()).isEmpty();
        assertThat(customer.getId()).isEqualTo(customerId);
        assertThat(customer.getFirstName()).isEqualTo("John");
        assertThat(customer.getLastName()).isEqualTo("Doe");
        assertThat(customer.getEmail()).isEqualTo(VALID_EMAIL);
        assertThat(customer.getPhone()).isEqualTo(VALID_PHONE);
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(customer.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void noPublicConstructors() {
        var constructors = Customer.class.getDeclaredConstructors();

        assertThat(Arrays.stream(constructors).noneMatch(c -> Modifier.isPublic(c.getModifiers()))).isTrue();
    }
}
