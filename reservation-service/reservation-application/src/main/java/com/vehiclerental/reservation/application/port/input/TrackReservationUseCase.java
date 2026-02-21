package com.vehiclerental.reservation.application.port.input;

import com.vehiclerental.reservation.application.dto.command.TrackReservationCommand;
import com.vehiclerental.reservation.application.dto.response.TrackReservationResponse;

public interface TrackReservationUseCase {
    TrackReservationResponse execute(TrackReservationCommand command);
}
