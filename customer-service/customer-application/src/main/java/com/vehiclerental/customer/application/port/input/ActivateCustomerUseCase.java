package com.vehiclerental.customer.application.port.input;

import com.vehiclerental.customer.application.dto.command.ActivateCustomerCommand;

public interface ActivateCustomerUseCase {

    void execute(ActivateCustomerCommand command);
}
