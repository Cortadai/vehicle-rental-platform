package com.vehiclerental.customer.application.port.input;

import com.vehiclerental.customer.application.dto.command.GetCustomerCommand;
import com.vehiclerental.customer.application.dto.response.CustomerResponse;

public interface GetCustomerUseCase {

    CustomerResponse execute(GetCustomerCommand command);
}
