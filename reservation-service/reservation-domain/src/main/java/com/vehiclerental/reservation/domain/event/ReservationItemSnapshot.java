package com.vehiclerental.reservation.domain.event;

import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.reservation.domain.model.vo.VehicleId;

public record ReservationItemSnapshot(VehicleId vehicleId, Money dailyRate, int days, Money subtotal) {
}
