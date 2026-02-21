package com.vehiclerental.reservation.application.mapper;

import com.vehiclerental.reservation.application.dto.response.CreateReservationResponse;
import com.vehiclerental.reservation.application.dto.response.TrackReservationResponse;
import com.vehiclerental.reservation.domain.model.aggregate.Reservation;
import com.vehiclerental.reservation.domain.model.entity.ReservationItem;

import java.util.List;

public class ReservationApplicationMapper {

    public CreateReservationResponse toCreateResponse(Reservation reservation) {
        return new CreateReservationResponse(
                reservation.getTrackingId().value().toString(),
                reservation.getStatus().name());
    }

    public TrackReservationResponse toTrackResponse(Reservation reservation) {
        List<TrackReservationResponse.TrackReservationItemResponse> itemResponses = reservation.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        return new TrackReservationResponse(
                reservation.getTrackingId().value().toString(),
                reservation.getCustomerId().value().toString(),
                reservation.getPickupLocation().address(),
                reservation.getPickupLocation().city(),
                reservation.getReturnLocation().address(),
                reservation.getReturnLocation().city(),
                reservation.getDateRange().pickupDate().toString(),
                reservation.getDateRange().returnDate().toString(),
                reservation.getStatus().name(),
                reservation.getTotalPrice().amount(),
                reservation.getTotalPrice().currency().getCurrencyCode(),
                itemResponses,
                reservation.getFailureMessages(),
                reservation.getCreatedAt());
    }

    private TrackReservationResponse.TrackReservationItemResponse toItemResponse(ReservationItem item) {
        return new TrackReservationResponse.TrackReservationItemResponse(
                item.getVehicleId().value().toString(),
                item.getDailyRate().amount(),
                item.getDays(),
                item.getSubtotal().amount());
    }
}
