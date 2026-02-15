package com.vehiclerental.fleet.application.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record VehicleResponse(
        String vehicleId,
        String licensePlate,
        String make,
        String model,
        int year,
        String category,
        BigDecimal dailyRateAmount,
        String dailyRateCurrency,
        String description,
        String status,
        Instant createdAt) {
}
