package com.vehiclerental.reservation.domain.model.vo;

import com.vehiclerental.reservation.domain.exception.ReservationDomainException;

public record PickupLocation(String address, String city) {

    public PickupLocation {
        if (address == null || address.isBlank()) {
            throw new ReservationDomainException("address must not be null or blank", "RESERVATION_ADDRESS_REQUIRED");
        }
        if (city == null || city.isBlank()) {
            throw new ReservationDomainException("city must not be null or blank", "RESERVATION_CITY_REQUIRED");
        }
    }
}
