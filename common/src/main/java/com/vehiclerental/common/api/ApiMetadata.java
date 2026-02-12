package com.vehiclerental.common.api;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ApiMetadata(Instant timestamp, String requestId) {

    public ApiMetadata {
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        Objects.requireNonNull(requestId, "requestId must not be null");
        if (requestId.isBlank()) {
            throw new IllegalArgumentException("requestId must not be blank");
        }
    }

    public static ApiMetadata of() {
        return new ApiMetadata(Instant.now(), UUID.randomUUID().toString());
    }
}
