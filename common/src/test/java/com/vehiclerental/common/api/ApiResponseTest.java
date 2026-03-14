package com.vehiclerental.common.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void factoryMethod_shouldWrapDataWithMetadata() {
        var response = ApiResponse.of("hello");

        assertThat(response.data()).isEqualTo("hello");
        assertThat(response.meta()).isNotNull();
        assertThat(response.meta().timestamp()).isNotNull();
        assertThat(response.meta().requestId()).isNotBlank();
    }

    @Test
    void factoryMethod_shouldAcceptNullData() {
        var response = ApiResponse.of(null);

        assertThat(response.data()).isNull();
        assertThat(response.meta()).isNotNull();
    }
}
