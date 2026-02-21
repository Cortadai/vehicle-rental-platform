package com.vehiclerental.reservation.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reservations")
public class ReservationJpaEntity {

    @Id
    private UUID id;

    @Column(name = "tracking_id", nullable = false, unique = true)
    private UUID trackingId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "pickup_address", nullable = false)
    private String pickupAddress;

    @Column(name = "pickup_city", nullable = false)
    private String pickupCity;

    @Column(name = "return_address", nullable = false)
    private String returnAddress;

    @Column(name = "return_city", nullable = false)
    private String returnCity;

    @Column(name = "pickup_date", nullable = false)
    private LocalDate pickupDate;

    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate;

    @Column(name = "total_price_amount", nullable = false)
    private BigDecimal totalPriceAmount;

    @Column(name = "total_price_currency", nullable = false)
    private String totalPriceCurrency;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "failure_messages")
    private String failureMessages;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservationItemJpaEntity> items = new ArrayList<>();

    public ReservationJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(UUID trackingId) {
        this.trackingId = trackingId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public String getPickupCity() {
        return pickupCity;
    }

    public void setPickupCity(String pickupCity) {
        this.pickupCity = pickupCity;
    }

    public String getReturnAddress() {
        return returnAddress;
    }

    public void setReturnAddress(String returnAddress) {
        this.returnAddress = returnAddress;
    }

    public String getReturnCity() {
        return returnCity;
    }

    public void setReturnCity(String returnCity) {
        this.returnCity = returnCity;
    }

    public LocalDate getPickupDate() {
        return pickupDate;
    }

    public void setPickupDate(LocalDate pickupDate) {
        this.pickupDate = pickupDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public BigDecimal getTotalPriceAmount() {
        return totalPriceAmount;
    }

    public void setTotalPriceAmount(BigDecimal totalPriceAmount) {
        this.totalPriceAmount = totalPriceAmount;
    }

    public String getTotalPriceCurrency() {
        return totalPriceCurrency;
    }

    public void setTotalPriceCurrency(String totalPriceCurrency) {
        this.totalPriceCurrency = totalPriceCurrency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFailureMessages() {
        return failureMessages;
    }

    public void setFailureMessages(String failureMessages) {
        this.failureMessages = failureMessages;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<ReservationItemJpaEntity> getItems() {
        return items;
    }

    public void setItems(List<ReservationItemJpaEntity> items) {
        this.items = items;
    }
}
