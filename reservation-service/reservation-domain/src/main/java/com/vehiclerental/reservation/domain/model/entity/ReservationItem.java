package com.vehiclerental.reservation.domain.model.entity;

import com.vehiclerental.common.domain.entity.BaseEntity;
import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.reservation.domain.exception.ReservationDomainException;
import com.vehiclerental.reservation.domain.model.vo.VehicleId;

import java.math.BigDecimal;
import java.util.UUID;

public class ReservationItem extends BaseEntity<UUID> {

    private final VehicleId vehicleId;
    private final Money dailyRate;
    private final int days;
    private final Money subtotal;

    private ReservationItem(UUID id, VehicleId vehicleId, Money dailyRate, int days, Money subtotal) {
        super(id);
        this.vehicleId = vehicleId;
        this.dailyRate = dailyRate;
        this.days = days;
        this.subtotal = subtotal;
    }

    public static ReservationItem create(VehicleId vehicleId, Money dailyRate, int days) {
        if (vehicleId == null) {
            throw new ReservationDomainException("vehicleId must not be null", "RESERVATION_ITEM_VEHICLE_ID_REQUIRED");
        }
        if (dailyRate == null) {
            throw new ReservationDomainException("dailyRate must not be null", "RESERVATION_ITEM_DAILY_RATE_REQUIRED");
        }
        if (dailyRate.amount().compareTo(BigDecimal.ZERO) == 0) {
            throw new ReservationDomainException("dailyRate amount must be greater than zero", "RESERVATION_ITEM_DAILY_RATE_ZERO");
        }
        if (days <= 0) {
            throw new ReservationDomainException("days must be greater than zero", "RESERVATION_ITEM_DAYS_INVALID");
        }

        Money subtotal = dailyRate.multiply(days);
        return new ReservationItem(UUID.randomUUID(), vehicleId, dailyRate, days, subtotal);
    }

    public static ReservationItem reconstruct(UUID id, VehicleId vehicleId, Money dailyRate, int days, Money subtotal) {
        return new ReservationItem(id, vehicleId, dailyRate, days, subtotal);
    }

    public VehicleId getVehicleId() {
        return vehicleId;
    }

    public Money getDailyRate() {
        return dailyRate;
    }

    public int getDays() {
        return days;
    }

    public Money getSubtotal() {
        return subtotal;
    }
}
