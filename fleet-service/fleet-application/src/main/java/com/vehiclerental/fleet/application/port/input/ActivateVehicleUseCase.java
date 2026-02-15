package com.vehiclerental.fleet.application.port.input;

import com.vehiclerental.fleet.application.dto.command.ActivateVehicleCommand;

public interface ActivateVehicleUseCase {

    void execute(ActivateVehicleCommand command);
}
