package com.vehiclerental.customer.domain.model.aggregate;

import com.vehiclerental.customer.domain.event.CustomerActivatedEvent;
import com.vehiclerental.customer.domain.event.CustomerDeletedEvent;
import com.vehiclerental.customer.domain.event.CustomerSuspendedEvent;
import com.vehiclerental.customer.domain.exception.CustomerDomainException;
import com.vehiclerental.customer.domain.model.vo.CustomerId;
import com.vehiclerental.customer.domain.model.vo.CustomerStatus;
import com.vehiclerental.customer.domain.model.vo.Email;
import com.vehiclerental.customer.domain.model.vo.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerLifecycleTest {

    private static final Email VALID_EMAIL = new Email("john@example.com");
    private static final PhoneNumber VALID_PHONE = new PhoneNumber("+1-555-123-4567");

    private Customer createActiveCustomer() {
        var customer = Customer.create("John", "Doe", VALID_EMAIL, VALID_PHONE);
        customer.clearDomainEvents();
        return customer;
    }

    private Customer createSuspendedCustomer() {
        var customer = createActiveCustomer();
        customer.suspend();
        customer.clearDomainEvents();
        return customer;
    }

    private Customer createDeletedCustomer() {
        var customer = createActiveCustomer();
        customer.delete();
        customer.clearDomainEvents();
        return customer;
    }

    // --- Suspend ---

    @Test
    void suspendActiveCustomer() {
        var customer = createActiveCustomer();

        customer.suspend();

        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.SUSPENDED);
        assertThat(customer.getDomainEvents()).hasSize(1);
        assertThat(customer.getDomainEvents().get(0)).isInstanceOf(CustomerSuspendedEvent.class);
    }

    @Test
    void suspendNonActiveThrows() {
        var suspended = createSuspendedCustomer();
        assertThatThrownBy(suspended::suspend)
                .isInstanceOf(CustomerDomainException.class);

        var deleted = createDeletedCustomer();
        assertThatThrownBy(deleted::suspend)
                .isInstanceOf(CustomerDomainException.class);
    }

    // --- Activate ---

    @Test
    void activateSuspendedCustomer() {
        var customer = createSuspendedCustomer();

        customer.activate();

        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(customer.getDomainEvents()).hasSize(1);
        assertThat(customer.getDomainEvents().get(0)).isInstanceOf(CustomerActivatedEvent.class);
    }

    @Test
    void activateNonSuspendedThrows() {
        var active = createActiveCustomer();
        assertThatThrownBy(active::activate)
                .isInstanceOf(CustomerDomainException.class);

        var deleted = createDeletedCustomer();
        assertThatThrownBy(deleted::activate)
                .isInstanceOf(CustomerDomainException.class);
    }

    // --- Delete ---

    @Test
    void deleteActiveCustomer() {
        var customer = createActiveCustomer();

        customer.delete();

        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.DELETED);
        assertThat(customer.getDomainEvents()).hasSize(1);
        assertThat(customer.getDomainEvents().get(0)).isInstanceOf(CustomerDeletedEvent.class);
    }

    @Test
    void deleteSuspendedCustomer() {
        var customer = createSuspendedCustomer();

        customer.delete();

        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.DELETED);
        assertThat(customer.getDomainEvents()).hasSize(1);
        assertThat(customer.getDomainEvents().get(0)).isInstanceOf(CustomerDeletedEvent.class);
    }

    @Test
    void deleteAlreadyDeletedThrows() {
        var customer = createDeletedCustomer();

        assertThatThrownBy(customer::delete)
                .isInstanceOf(CustomerDomainException.class);
    }

    // --- isActive ---

    @Test
    void isActiveTrueForActiveCustomer() {
        var customer = createActiveCustomer();

        assertThat(customer.isActive()).isTrue();
    }

    @Test
    void isActiveFalseForNonActiveCustomer() {
        assertThat(createSuspendedCustomer().isActive()).isFalse();
        assertThat(createDeletedCustomer().isActive()).isFalse();
    }

    // --- Exception details ---

    @Test
    void exceptionCarriesErrorCode() {
        var deleted = createDeletedCustomer();

        assertThatThrownBy(deleted::suspend)
                .isInstanceOf(CustomerDomainException.class)
                .satisfies(ex -> assertThat(((CustomerDomainException) ex).getErrorCode()).isNotBlank());
    }

    @Test
    void exceptionMessageIncludesCurrentState() {
        var deleted = createDeletedCustomer();

        assertThatThrownBy(deleted::suspend)
                .isInstanceOf(CustomerDomainException.class)
                .hasMessageContaining("DELETED");
    }
}
