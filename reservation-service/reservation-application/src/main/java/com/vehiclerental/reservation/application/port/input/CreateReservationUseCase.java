package com.vehiclerental.reservation.application.port.input;

import com.vehiclerental.reservation.application.dto.command.CreateReservationCommand;
import com.vehiclerental.reservation.application.dto.response.CreateReservationResponse;

public interface CreateReservationUseCase {
    CreateReservationResponse execute(CreateReservationCommand command);
}
