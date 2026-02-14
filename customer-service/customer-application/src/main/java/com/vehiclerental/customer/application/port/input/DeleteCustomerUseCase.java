package com.vehiclerental.customer.application.port.input;

import com.vehiclerental.customer.application.dto.command.DeleteCustomerCommand;

public interface DeleteCustomerUseCase {

    void execute(DeleteCustomerCommand command);
}
