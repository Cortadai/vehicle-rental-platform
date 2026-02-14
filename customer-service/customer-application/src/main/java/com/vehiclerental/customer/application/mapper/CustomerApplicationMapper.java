package com.vehiclerental.customer.application.mapper;

import com.vehiclerental.customer.application.dto.response.CustomerResponse;
import com.vehiclerental.customer.domain.model.aggregate.Customer;

public class CustomerApplicationMapper {

    public CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId().value().toString(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail().value(),
                customer.getPhone() != null ? customer.getPhone().value() : null,
                customer.getStatus().name(),
                customer.getCreatedAt());
    }
}
