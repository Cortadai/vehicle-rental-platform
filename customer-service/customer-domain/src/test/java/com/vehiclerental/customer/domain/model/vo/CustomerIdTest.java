package com.vehiclerental.customer.domain.model.vo;

import com.vehiclerental.customer.domain.exception.CustomerDomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerIdTest {

    @Test
    void validConstruction() {
        UUID uuid = UUID.randomUUID();
        var customerId = new CustomerId(uuid);

        assertThat(customerId.value()).isEqualTo(uuid);
    }

    @Test
    void nullUuidRejected() {
        assertThatThrownBy(() -> new CustomerId(null))
                .isInstanceOf(CustomerDomainException.class);
    }

    @Test
    void equalityByValue() {
        UUID uuid = UUID.randomUUID();
        var id1 = new CustomerId(uuid);
        var id2 = new CustomerId(uuid);

        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }
}
