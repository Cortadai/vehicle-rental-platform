package com.vehiclerental.fleet.application.port.input;

import com.vehiclerental.fleet.application.dto.command.ReleaseFleetReservationCommand;

public interface ReleaseFleetReservationUseCase {
    void execute(ReleaseFleetReservationCommand command);
}
