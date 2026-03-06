package com.vehiclerental.fleet.application.dto.command;

public record ConfirmFleetAvailabilityCommand(
        String vehicleId,
        String reservationId,
        String pickupDate,
        String returnDate
) {
}
