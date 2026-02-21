package com.vehiclerental.reservation.infrastructure.adapter.output.persistence.mapper;

import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.reservation.domain.model.aggregate.Reservation;
import com.vehiclerental.reservation.domain.model.entity.ReservationItem;
import com.vehiclerental.reservation.domain.model.vo.CustomerId;
import com.vehiclerental.reservation.domain.model.vo.DateRange;
import com.vehiclerental.reservation.domain.model.vo.PickupLocation;
import com.vehiclerental.reservation.domain.model.vo.ReservationId;
import com.vehiclerental.reservation.domain.model.vo.ReservationStatus;
import com.vehiclerental.reservation.domain.model.vo.TrackingId;
import com.vehiclerental.reservation.domain.model.vo.VehicleId;
import com.vehiclerental.reservation.infrastructure.adapter.output.persistence.entity.ReservationItemJpaEntity;
import com.vehiclerental.reservation.infrastructure.adapter.output.persistence.entity.ReservationJpaEntity;

import java.util.Arrays;
import java.util.Currency;
import java.util.List;

public class ReservationPersistenceMapper {

    public ReservationJpaEntity toJpaEntity(Reservation reservation) {
        var entity = new ReservationJpaEntity();
        entity.setId(reservation.getId().value());
        entity.setTrackingId(reservation.getTrackingId().value());
        entity.setCustomerId(reservation.getCustomerId().value());
        entity.setPickupAddress(reservation.getPickupLocation().address());
        entity.setPickupCity(reservation.getPickupLocation().city());
        entity.setReturnAddress(reservation.getReturnLocation().address());
        entity.setReturnCity(reservation.getReturnLocation().city());
        entity.setPickupDate(reservation.getDateRange().pickupDate());
        entity.setReturnDate(reservation.getDateRange().returnDate());
        entity.setTotalPriceAmount(reservation.getTotalPrice().amount());
        entity.setTotalPriceCurrency(reservation.getTotalPrice().currency().getCurrencyCode());
        entity.setStatus(reservation.getStatus().name());
        entity.setFailureMessages(
                reservation.getFailureMessages() != null && !reservation.getFailureMessages().isEmpty()
                        ? String.join(",", reservation.getFailureMessages())
                        : null);
        entity.setCreatedAt(reservation.getCreatedAt());
        entity.setUpdatedAt(reservation.getUpdatedAt());

        List<ReservationItemJpaEntity> itemEntities = reservation.getItems().stream()
                .map(item -> toItemJpaEntity(item, entity))
                .toList();
        entity.setItems(itemEntities);

        return entity;
    }

    public Reservation toDomainEntity(ReservationJpaEntity entity) {
        List<ReservationItem> items = entity.getItems().stream()
                .map(this::toItemDomainEntity)
                .toList();

        List<String> failureMessages = entity.getFailureMessages() != null && !entity.getFailureMessages().isEmpty()
                ? Arrays.asList(entity.getFailureMessages().split(","))
                : List.of();

        Currency currency = Currency.getInstance(entity.getTotalPriceCurrency());

        return Reservation.reconstruct(
                new ReservationId(entity.getId()),
                new TrackingId(entity.getTrackingId()),
                new CustomerId(entity.getCustomerId()),
                new PickupLocation(entity.getPickupAddress(), entity.getPickupCity()),
                new PickupLocation(entity.getReturnAddress(), entity.getReturnCity()),
                new DateRange(entity.getPickupDate(), entity.getReturnDate()),
                new Money(entity.getTotalPriceAmount(), currency),
                ReservationStatus.valueOf(entity.getStatus()),
                items,
                failureMessages,
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private ReservationItemJpaEntity toItemJpaEntity(ReservationItem item, ReservationJpaEntity parent) {
        var entity = new ReservationItemJpaEntity();
        entity.setId(item.getId());
        entity.setVehicleId(item.getVehicleId().value());
        entity.setDailyRateAmount(item.getDailyRate().amount());
        entity.setDailyRateCurrency(item.getDailyRate().currency().getCurrencyCode());
        entity.setDays(item.getDays());
        entity.setSubtotalAmount(item.getSubtotal().amount());
        entity.setSubtotalCurrency(item.getSubtotal().currency().getCurrencyCode());
        entity.setReservation(parent);
        return entity;
    }

    private ReservationItem toItemDomainEntity(ReservationItemJpaEntity entity) {
        Currency currency = Currency.getInstance(entity.getDailyRateCurrency());
        Currency subtotalCurrency = Currency.getInstance(entity.getSubtotalCurrency());

        return ReservationItem.reconstruct(
                entity.getId(),
                new VehicleId(entity.getVehicleId()),
                new Money(entity.getDailyRateAmount(), currency),
                entity.getDays(),
                new Money(entity.getSubtotalAmount(), subtotalCurrency));
    }
}
