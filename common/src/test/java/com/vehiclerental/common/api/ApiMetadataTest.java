package com.vehiclerental.common.api;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiMetadataTest {

    @Test
    void factoryMethod_shouldGenerateTimestampAndRequestId() {
        var before = Instant.now();
        var meta = ApiMetadata.of();
        var after = Instant.now();

        assertThat(meta.timestamp()).isBetween(before, after);
        assertThat(UUID.fromString(meta.requestId())).isNotNull();
    }

    @Test
    void nullTimestamp_shouldThrow() {
        assertThatThrownBy(() -> new ApiMetadata(null, UUID.randomUUID().toString()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("timestamp");
    }

    @Test
    void nullRequestId_shouldThrow() {
        assertThatThrownBy(() -> new ApiMetadata(Instant.now(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("requestId");
    }

    @Test
    void blankRequestId_shouldThrow() {
        assertThatThrownBy(() -> new ApiMetadata(Instant.now(), "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requestId");
    }

    @Test
    void validConstruction_shouldRetainFields() {
        var timestamp = Instant.now();
        var requestId = "test-request-id";

        var meta = new ApiMetadata(timestamp, requestId);

        assertThat(meta.timestamp()).isEqualTo(timestamp);
        assertThat(meta.requestId()).isEqualTo(requestId);
    }
}
