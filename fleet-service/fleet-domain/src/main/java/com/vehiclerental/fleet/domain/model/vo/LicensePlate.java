package com.vehiclerental.fleet.domain.model.vo;

import com.vehiclerental.fleet.domain.exception.FleetDomainException;

import java.util.regex.Pattern;

public record LicensePlate(String value) {

    private static final Pattern PLATE_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9 -]*[A-Za-z0-9]$");

    public LicensePlate {
        if (value == null) {
            throw new FleetDomainException("License plate must not be null", "LICENSE_PLATE_NULL");
        }
        if (value.isBlank()) {
            throw new FleetDomainException("License plate must not be blank", "LICENSE_PLATE_BLANK");
        }
        if (value.length() < 2) {
            throw new FleetDomainException("License plate must be at least 2 characters", "LICENSE_PLATE_TOO_SHORT");
        }
        if (value.length() > 15) {
            throw new FleetDomainException("License plate must be at most 15 characters", "LICENSE_PLATE_TOO_LONG");
        }
        if (!PLATE_PATTERN.matcher(value).matches()) {
            throw new FleetDomainException("License plate contains invalid characters: " + value, "LICENSE_PLATE_INVALID_FORMAT");
        }
    }
}
