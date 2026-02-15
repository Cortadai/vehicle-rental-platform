package com.vehiclerental.fleet.application.port.input;

import com.vehiclerental.fleet.application.dto.command.GetVehicleCommand;
import com.vehiclerental.fleet.application.dto.response.VehicleResponse;

public interface GetVehicleUseCase {

    VehicleResponse execute(GetVehicleCommand command);
}
