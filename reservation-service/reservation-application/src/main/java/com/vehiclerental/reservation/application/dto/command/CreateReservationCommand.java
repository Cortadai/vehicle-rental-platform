package com.vehiclerental.reservation.application.dto.command;

import java.math.BigDecimal;
import java.util.List;

public record CreateReservationCommand(
        String customerId,
        String pickupAddress,
        String pickupCity,
        String returnAddress,
        String returnCity,
        String pickupDate,
        String returnDate,
        String currency,
        List<CreateReservationItemCommand> items) {

    public record CreateReservationItemCommand(
            String vehicleId,
            BigDecimal dailyRate,
            int days) {
    }
}
