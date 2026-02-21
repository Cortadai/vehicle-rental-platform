package com.vehiclerental.reservation.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "reservation_items")
public class ReservationItemJpaEntity {

    @Id
    private UUID id;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "daily_rate_amount", nullable = false)
    private BigDecimal dailyRateAmount;

    @Column(name = "daily_rate_currency", nullable = false)
    private String dailyRateCurrency;

    @Column(name = "days", nullable = false)
    private int days;

    @Column(name = "subtotal_amount", nullable = false)
    private BigDecimal subtotalAmount;

    @Column(name = "subtotal_currency", nullable = false)
    private String subtotalCurrency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private ReservationJpaEntity reservation;

    public ReservationItemJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(UUID vehicleId) {
        this.vehicleId = vehicleId;
    }

    public BigDecimal getDailyRateAmount() {
        return dailyRateAmount;
    }

    public void setDailyRateAmount(BigDecimal dailyRateAmount) {
        this.dailyRateAmount = dailyRateAmount;
    }

    public String getDailyRateCurrency() {
        return dailyRateCurrency;
    }

    public void setDailyRateCurrency(String dailyRateCurrency) {
        this.dailyRateCurrency = dailyRateCurrency;
    }

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public void setSubtotalAmount(BigDecimal subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
    }

    public String getSubtotalCurrency() {
        return subtotalCurrency;
    }

    public void setSubtotalCurrency(String subtotalCurrency) {
        this.subtotalCurrency = subtotalCurrency;
    }

    public ReservationJpaEntity getReservation() {
        return reservation;
    }

    public void setReservation(ReservationJpaEntity reservation) {
        this.reservation = reservation;
    }
}
