package com.vehiclerental.fleet.application.port.input;

import com.vehiclerental.fleet.application.dto.command.RetireVehicleCommand;

public interface RetireVehicleUseCase {

    void execute(RetireVehicleCommand command);
}
