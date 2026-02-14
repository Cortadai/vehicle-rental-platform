package com.vehiclerental.customer.infrastructure.adapter.input.rest;

import com.vehiclerental.common.api.ApiResponse;
import com.vehiclerental.customer.application.dto.command.*;
import com.vehiclerental.customer.application.dto.response.CustomerResponse;
import com.vehiclerental.customer.application.port.input.*;
import com.vehiclerental.customer.infrastructure.adapter.input.rest.dto.CreateCustomerRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CreateCustomerUseCase createCustomerUseCase;
    private final GetCustomerUseCase getCustomerUseCase;
    private final SuspendCustomerUseCase suspendCustomerUseCase;
    private final ActivateCustomerUseCase activateCustomerUseCase;
    private final DeleteCustomerUseCase deleteCustomerUseCase;

    public CustomerController(CreateCustomerUseCase createCustomerUseCase,
                              GetCustomerUseCase getCustomerUseCase,
                              SuspendCustomerUseCase suspendCustomerUseCase,
                              ActivateCustomerUseCase activateCustomerUseCase,
                              DeleteCustomerUseCase deleteCustomerUseCase) {
        this.createCustomerUseCase = createCustomerUseCase;
        this.getCustomerUseCase = getCustomerUseCase;
        this.suspendCustomerUseCase = suspendCustomerUseCase;
        this.activateCustomerUseCase = activateCustomerUseCase;
        this.deleteCustomerUseCase = deleteCustomerUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        var command = new CreateCustomerCommand(
                request.firstName(), request.lastName(), request.email(), request.phone());
        var response = createCustomerUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomer(@PathVariable String id) {
        var response = getCustomerUseCase.execute(new GetCustomerCommand(id));
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<Void> suspendCustomer(@PathVariable String id) {
        suspendCustomerUseCase.execute(new SuspendCustomerCommand(id));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activateCustomer(@PathVariable String id) {
        activateCustomerUseCase.execute(new ActivateCustomerCommand(id));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable String id) {
        deleteCustomerUseCase.execute(new DeleteCustomerCommand(id));
        return ResponseEntity.noContent().build();
    }
}
