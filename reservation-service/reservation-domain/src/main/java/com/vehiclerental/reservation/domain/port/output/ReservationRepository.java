package com.vehiclerental.reservation.domain.port.output;

import com.vehiclerental.reservation.domain.model.aggregate.Reservation;
import com.vehiclerental.reservation.domain.model.vo.ReservationId;
import com.vehiclerental.reservation.domain.model.vo.TrackingId;

import java.util.Optional;

public interface ReservationRepository {

    Reservation save(Reservation reservation);

    Optional<Reservation> findById(ReservationId reservationId);

    Optional<Reservation> findByTrackingId(TrackingId trackingId);
}
