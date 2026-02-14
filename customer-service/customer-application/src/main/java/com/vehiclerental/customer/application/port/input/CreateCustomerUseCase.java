package com.vehiclerental.customer.application.port.input;

import com.vehiclerental.customer.application.dto.command.CreateCustomerCommand;
import com.vehiclerental.customer.application.dto.response.CustomerResponse;

public interface CreateCustomerUseCase {

    CustomerResponse execute(CreateCustomerCommand command);
}
