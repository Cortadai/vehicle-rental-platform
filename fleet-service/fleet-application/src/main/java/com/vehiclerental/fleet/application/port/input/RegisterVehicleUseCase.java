package com.vehiclerental.fleet.application.port.input;

import com.vehiclerental.fleet.application.dto.command.RegisterVehicleCommand;
import com.vehiclerental.fleet.application.dto.response.VehicleResponse;

public interface RegisterVehicleUseCase {

    VehicleResponse execute(RegisterVehicleCommand command);
}
