package com.vehiclerental.customer.application.port.input;

import com.vehiclerental.customer.application.dto.command.ValidateCustomerCommand;

public interface ValidateCustomerForReservationUseCase {
    void execute(ValidateCustomerCommand command);
}
