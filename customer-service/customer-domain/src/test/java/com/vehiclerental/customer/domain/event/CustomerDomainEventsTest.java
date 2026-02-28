package com.vehiclerental.customer.domain.event;

import com.vehiclerental.common.domain.event.DomainEvent;
import com.vehiclerental.customer.domain.exception.CustomerDomainException;
import com.vehiclerental.customer.domain.model.vo.CustomerId;
import com.vehiclerental.customer.domain.model.vo.Email;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerDomainEventsTest {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final Instant OCCURRED_ON = Instant.now();
    private static final CustomerId CUSTOMER_ID = new CustomerId(UUID.randomUUID());
    private static final Email EMAIL = new Email("john@example.com");
    private static final UUID RESERVATION_ID = UUID.randomUUID();

    // --- CustomerCreatedEvent ---

    @Test
    void createdEventFieldsAccessible() {
        var event = new CustomerCreatedEvent(EVENT_ID, OCCURRED_ON, CUSTOMER_ID, "John", "Doe", EMAIL);

        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.occurredOn()).isEqualTo(OCCURRED_ON);
        assertThat(event.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(event.firstName()).isEqualTo("John");
        assertThat(event.lastName()).isEqualTo("Doe");
        assertThat(event.email()).isEqualTo(EMAIL);
    }

    @Test
    void createdEventNullEventIdThrows() {
        assertThatThrownBy(() -> new CustomerCreatedEvent(null, OCCURRED_ON, CUSTOMER_ID, "John", "Doe", EMAIL))
                .isInstanceOf(CustomerDomainException.class);
    }

    @Test
    void createdEventNullOccurredOnThrows() {
        assertThatThrownBy(() -> new CustomerCreatedEvent(EVENT_ID, null, CUSTOMER_ID, "John", "Doe", EMAIL))
                .isInstanceOf(CustomerDomainException.class);
    }

    @Test
    void createdEventImplementsDomainEvent() {
        var event = new CustomerCreatedEvent(EVENT_ID, OCCURRED_ON, CUSTOMER_ID, "John", "Doe", EMAIL);

        assertThat(event).isInstanceOf(DomainEvent.class);
    }

    // --- CustomerSuspendedEvent ---

    @Test
    void suspendedEventCarriesCustomerId() {
        var event = new CustomerSuspendedEvent(EVENT_ID, OCCURRED_ON, CUSTOMER_ID);

        assertThat(event.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.occurredOn()).isEqualTo(OCCURRED_ON);
    }

    @Test
    void suspendedEventImplementsDomainEvent() {
        var event = new CustomerSuspendedEvent(EVENT_ID, OCCURRED_ON, CUSTOMER_ID);

        assertThat(event).isInstanceOf(DomainEvent.class);
    }

    @Test
    void suspendedEventNullEventIdThrows() {
        assertThatThrownBy(() -> new CustomerSuspendedEvent(null, OCCURRED_ON, CUSTOMER_ID))
                .isInstanceOf(CustomerDomainException.class);
    }

    @Test
    void suspendedEventNullOccurredOnThrows() {
        assertThatThrownBy(() -> new CustomerSuspendedEvent(EVENT_ID, null, CUSTOMER_ID))
                .isInstanceOf(CustomerDomainException.class);
    }

    // --- CustomerActivatedEvent ---

    @Test
    void activatedEventCarriesCustomerId() {
        var event = new CustomerActivatedEvent(EVENT_ID, OCCURRED_ON, CUSTOMER_ID);

        assertThat(event.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.occurredOn()).isEqualTo(OCCURRED_ON);
    }

    @Test
    void activatedEventImplementsDomainEvent() {
        var event = new CustomerActivatedEvent(EVENT_ID, OCCURRED_ON, CUSTOMER_ID);

        assertThat(event).isInstanceOf(DomainEvent.class);
    }

    @Test
    void activatedEventNullEventIdThrows() {
        assertThatThrownBy(() -> new CustomerActivatedEvent(null, OCCURRED_ON, CUSTOMER_ID))
                .isInstanceOf(CustomerDomainException.class);
    }

    @Test
    void activatedEventNullOccurredOnThrows() {
        assertThatThrownBy(() -> new CustomerActivatedEvent(EVENT_ID, null, CUSTOMER_ID))
                .isInstanceOf(CustomerDomainException.class);
    }

    // --- CustomerDeletedEvent ---

    @Test
    void deletedEventCarriesCustomerId() {
        var event = new CustomerDeletedEvent(EVENT_ID, OCCURRED_ON, CUSTOMER_ID);

        assertThat(event.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.occurredOn()).isEqualTo(OCCURRED_ON);
    }

    @Test
    void deletedEventImplementsDomainEvent() {
        var event = new CustomerDeletedEvent(EVENT_ID, OCCURRED_ON, CUSTOMER_ID);

        assertThat(event).isInstanceOf(DomainEvent.class);
    }

    @Test
    void deletedEventNullEventIdThrows() {
        assertThatThrownBy(() -> new CustomerDeletedEvent(null, OCCURRED_ON, CUSTOMER_ID))
                .isInstanceOf(CustomerDomainException.class);
    }

    @Test
    void deletedEventNullOccurredOnThrows() {
        assertThatThrownBy(() -> new CustomerDeletedEvent(EVENT_ID, null, CUSTOMER_ID))
                .isInstanceOf(CustomerDomainException.class);
    }

    // --- CustomerValidatedEvent ---

    @Test
    void validatedEventFieldsAccessible() {
        var event = new CustomerValidatedEvent(EVENT_ID, OCCURRED_ON, CUSTOMER_ID, RESERVATION_ID);

        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.occurredOn()).isEqualTo(OCCURRED_ON);
        assertThat(event.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(event.reservationId()).isEqualTo(RESERVATION_ID);
    }

    @Test
    void validatedEventImplementsDomainEvent() {
        var event = new CustomerValidatedEvent(EVENT_ID, OCCURRED_ON, CUSTOMER_ID, RESERVATION_ID);

        assertThat(event).isInstanceOf(DomainEvent.class);
    }

    @Test
    void validatedEventNullEventIdThrows() {
        assertThatThrownBy(() -> new CustomerValidatedEvent(null, OCCURRED_ON, CUSTOMER_ID, RESERVATION_ID))
                .isInstanceOf(CustomerDomainException.class);
    }

    @Test
    void validatedEventNullOccurredOnThrows() {
        assertThatThrownBy(() -> new CustomerValidatedEvent(EVENT_ID, null, CUSTOMER_ID, RESERVATION_ID))
                .isInstanceOf(CustomerDomainException.class);
    }

    // --- CustomerRejectedEvent ---

    @Test
    void rejectedEventFieldsAccessible() {
        var failureMessages = java.util.List.of("Customer not found: abc");
        var event = new CustomerRejectedEvent(EVENT_ID, OCCURRED_ON, CUSTOMER_ID, RESERVATION_ID, failureMessages);

        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.occurredOn()).isEqualTo(OCCURRED_ON);
        assertThat(event.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(event.reservationId()).isEqualTo(RESERVATION_ID);
        assertThat(event.failureMessages()).containsExactly("Customer not found: abc");
    }

    @Test
    void rejectedEventImplementsDomainEvent() {
        var failureMessages = java.util.List.of("some failure");
        var event = new CustomerRejectedEvent(EVENT_ID, OCCURRED_ON, CUSTOMER_ID, RESERVATION_ID, failureMessages);

        assertThat(event).isInstanceOf(DomainEvent.class);
    }

    @Test
    void rejectedEventNullEventIdThrows() {
        assertThatThrownBy(() -> new CustomerRejectedEvent(null, OCCURRED_ON, CUSTOMER_ID, RESERVATION_ID, java.util.List.of()))
                .isInstanceOf(CustomerDomainException.class);
    }

    @Test
    void rejectedEventNullOccurredOnThrows() {
        assertThatThrownBy(() -> new CustomerRejectedEvent(EVENT_ID, null, CUSTOMER_ID, RESERVATION_ID, java.util.List.of()))
                .isInstanceOf(CustomerDomainException.class);
    }
}
