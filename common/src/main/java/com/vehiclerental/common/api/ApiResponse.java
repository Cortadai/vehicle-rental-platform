package com.vehiclerental.common.api;

public record ApiResponse<T>(T data, ApiMetadata meta) {

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, ApiMetadata.of());
    }
}
