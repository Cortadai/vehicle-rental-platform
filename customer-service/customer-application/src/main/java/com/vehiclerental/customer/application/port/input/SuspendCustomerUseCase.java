package com.vehiclerental.customer.application.port.input;

import com.vehiclerental.customer.application.dto.command.SuspendCustomerCommand;

public interface SuspendCustomerUseCase {

    void execute(SuspendCustomerCommand command);
}
