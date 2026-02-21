package com.vehiclerental.reservation.application.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TrackReservationResponse(
        String trackingId,
        String customerId,
        String pickupAddress,
        String pickupCity,
        String returnAddress,
        String returnCity,
        String pickupDate,
        String returnDate,
        String status,
        BigDecimal totalPrice,
        String currency,
        List<TrackReservationItemResponse> items,
        List<String> failureMessages,
        Instant createdAt) {

    public record TrackReservationItemResponse(
            String vehicleId,
            BigDecimal dailyRate,
            int days,
            BigDecimal subtotal) {
    }
}
