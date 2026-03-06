package com.vehiclerental.fleet.application.dto.command;

public record ReleaseFleetReservationCommand(
        String vehicleId,
        String reservationId
) {
}
