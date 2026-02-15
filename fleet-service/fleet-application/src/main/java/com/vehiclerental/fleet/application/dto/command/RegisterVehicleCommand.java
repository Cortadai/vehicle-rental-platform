package com.vehiclerental.fleet.application.dto.command;

import java.math.BigDecimal;

public record RegisterVehicleCommand(
        String licensePlate,
        String make,
        String model,
        int year,
        String category,
        BigDecimal dailyRateAmount,
        String dailyRateCurrency,
        String description) {
}
