package com.vehiclerental.reservation.domain.model.vo;

import com.vehiclerental.reservation.domain.exception.ReservationDomainException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record DateRange(LocalDate pickupDate, LocalDate returnDate) {

    public DateRange {
        if (pickupDate == null) {
            throw new ReservationDomainException("pickupDate must not be null", "RESERVATION_PICKUP_DATE_NULL");
        }
        if (returnDate == null) {
            throw new ReservationDomainException("returnDate must not be null", "RESERVATION_RETURN_DATE_NULL");
        }
        if (!returnDate.isAfter(pickupDate)) {
            throw new ReservationDomainException(
                    "returnDate must be after pickupDate", "RESERVATION_INVALID_DATE_RANGE");
        }
    }

    public int getDays() {
        return (int) ChronoUnit.DAYS.between(pickupDate, returnDate);
    }
}
