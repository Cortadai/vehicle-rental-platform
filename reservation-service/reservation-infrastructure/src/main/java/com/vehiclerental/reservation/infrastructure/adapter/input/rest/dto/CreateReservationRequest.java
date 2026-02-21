package com.vehiclerental.reservation.infrastructure.adapter.input.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record CreateReservationRequest(
        @NotBlank String customerId,
        @NotBlank String pickupAddress,
        @NotBlank String pickupCity,
        @NotBlank String returnAddress,
        @NotBlank String returnCity,
        @NotBlank String pickupDate,
        @NotBlank String returnDate,
        @NotBlank String currency,
        @NotEmpty @Valid List<CreateReservationItemRequest> items) {

    public record CreateReservationItemRequest(
            @NotBlank String vehicleId,
            @NotNull @Positive BigDecimal dailyRate,
            @Positive int days) {
    }
}
