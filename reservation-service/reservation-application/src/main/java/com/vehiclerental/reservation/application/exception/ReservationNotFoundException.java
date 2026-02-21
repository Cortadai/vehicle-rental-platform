package com.vehiclerental.reservation.application.exception;

public class ReservationNotFoundException extends RuntimeException {

    private final String trackingId;

    public ReservationNotFoundException(String trackingId) {
        super("Reservation not found with tracking id: " + trackingId);
        this.trackingId = trackingId;
    }

    public String getTrackingId() {
        return trackingId;
    }
}
