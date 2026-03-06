package com.vehiclerental.fleet.application.port.input;

import com.vehiclerental.fleet.application.dto.command.ConfirmFleetAvailabilityCommand;

public interface ConfirmFleetAvailabilityUseCase {
    void execute(ConfirmFleetAvailabilityCommand command);
}
