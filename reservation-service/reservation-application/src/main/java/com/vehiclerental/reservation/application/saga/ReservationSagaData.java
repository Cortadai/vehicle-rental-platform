package com.vehiclerental.reservation.application.saga;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReservationSagaData(
        UUID reservationId,
        UUID customerId,
        UUID vehicleId,
        BigDecimal totalAmount,
        String currency,
        LocalDate pickupDate,
        LocalDate returnDate
) {
}
