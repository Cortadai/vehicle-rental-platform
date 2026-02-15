package com.vehiclerental.fleet.infrastructure.adapter.input.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RegisterVehicleRequest(
        @NotBlank String licensePlate,
        @NotBlank String make,
        @NotBlank String model,
        int year,
        @NotBlank String category,
        @NotNull BigDecimal dailyRateAmount,
        @NotBlank String dailyRateCurrency,
        String description) {
}
