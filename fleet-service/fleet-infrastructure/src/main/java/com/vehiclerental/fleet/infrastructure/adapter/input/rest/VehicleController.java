package com.vehiclerental.fleet.infrastructure.adapter.input.rest;

import com.vehiclerental.common.api.ApiResponse;
import com.vehiclerental.fleet.application.dto.command.*;
import com.vehiclerental.fleet.application.dto.response.VehicleResponse;
import com.vehiclerental.fleet.application.port.input.*;
import com.vehiclerental.fleet.infrastructure.adapter.input.rest.dto.RegisterVehicleRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vehicles")
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
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicle(@PathVariable String id) {
        var response = getVehicleUseCase.execute(new GetVehicleCommand(id));
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/{id}/maintenance")
    public ResponseEntity<Void> sendToMaintenance(@PathVariable String id) {
        sendToMaintenanceUseCase.execute(new SendToMaintenanceCommand(id));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activateVehicle(@PathVariable String id) {
        activateVehicleUseCase.execute(new ActivateVehicleCommand(id));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/retire")
    public ResponseEntity<Void> retireVehicle(@PathVariable String id) {
        retireVehicleUseCase.execute(new RetireVehicleCommand(id));
        return ResponseEntity.ok().build();
    }
}
