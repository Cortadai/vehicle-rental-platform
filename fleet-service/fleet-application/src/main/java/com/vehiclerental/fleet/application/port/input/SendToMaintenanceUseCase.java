package com.vehiclerental.fleet.application.port.input;

import com.vehiclerental.fleet.application.dto.command.SendToMaintenanceCommand;

public interface SendToMaintenanceUseCase {

    void execute(SendToMaintenanceCommand command);
}
