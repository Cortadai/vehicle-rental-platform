package com.vehiclerental.fleet.infrastructure.adapter.input.rest;

import com.vehiclerental.common.api.ApiResponse;
import com.vehiclerental.fleet.application.dto.command.*;
import com.vehiclerental.fleet.application.dto.response.VehicleResponse;
import com.vehiclerental.fleet.application.port.input.*;
import com.vehiclerental.fleet.infrastructure.adapter.input.rest.dto.RegisterVehicleRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vehicles")
@Tag(name = "Fleet Service", description = "Vehicle fleet management")
public class VehicleController {

    private final RegisterVehicleUseCase registerVehicleUseCase;
    private final GetVehicleUseCase getVehicleUseCase;
    private final SendToMaintenanceUseCase sendToMaintenanceUseCase;
    private final ActivateVehicleUseCase activateVehicleUseCase;
    private final RetireVehicleUseCase retireVehicleUseCase;

    public VehicleController(RegisterVehicleUseCase registerVehicleUseCase,
                             GetVehicleUseCase getVehicleUseCase,
                             SendToMaintenanceUseCase sendToMaintenanceUseCase,
                             ActivateVehicleUseCase activateVehicleUseCase,
                             RetireVehicleUseCase retireVehicleUseCase) {
        this.registerVehicleUseCase = registerVehicleUseCase;
        this.getVehicleUseCase = getVehicleUseCase;
        this.sendToMaintenanceUseCase = sendToMaintenanceUseCase;
        this.activateVehicleUseCase = activateVehicleUseCase;
        this.retireVehicleUseCase = retireVehicleUseCase;
    }

    @PostMapping
    @Operation(summary = "Register a new vehicle")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Vehicle registered")
    public ResponseEntity<ApiResponse<VehicleResponse>> registerVehicle(
            @Valid @RequestBody RegisterVehicleRequest request) {
        var command = new RegisterVehicleCommand(
                request.licensePlate(), request.make(), request.model(),
                request.year(), request.category(),
                request.dailyRateAmount(), request.dailyRateCurrency(),
                request.description());
        var response = registerVehicleUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get vehicle by ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Vehicle found")
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicle(@PathVariable String id) {
        var response = getVehicleUseCase.execute(new GetVehicleCommand(id));
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/{id}/maintenance")
    @Operation(summary = "Send vehicle to maintenance")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Vehicle sent to maintenance")
    public ResponseEntity<Void> sendToMaintenance(@PathVariable String id) {
        sendToMaintenanceUseCase.execute(new SendToMaintenanceCommand(id));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate a vehicle")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Vehicle activated")
    public ResponseEntity<Void> activateVehicle(@PathVariable String id) {
        activateVehicleUseCase.execute(new ActivateVehicleCommand(id));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/retire")
    @Operation(summary = "Retire a vehicle")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Vehicle retired")
    public ResponseEntity<Void> retireVehicle(@PathVariable String id) {
        retireVehicleUseCase.execute(new RetireVehicleCommand(id));
        return ResponseEntity.ok().build();
    }
}
