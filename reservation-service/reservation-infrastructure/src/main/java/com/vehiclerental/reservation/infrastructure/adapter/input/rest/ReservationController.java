package com.vehiclerental.reservation.infrastructure.adapter.input.rest;

import com.vehiclerental.common.api.ApiResponse;
import com.vehiclerental.reservation.application.dto.command.CreateReservationCommand;
import com.vehiclerental.reservation.application.dto.command.TrackReservationCommand;
import com.vehiclerental.reservation.application.dto.response.CreateReservationResponse;
import com.vehiclerental.reservation.application.dto.response.TrackReservationResponse;
import com.vehiclerental.reservation.application.port.input.CreateReservationUseCase;
import com.vehiclerental.reservation.application.port.input.TrackReservationUseCase;
import com.vehiclerental.reservation.infrastructure.adapter.input.rest.dto.CreateReservationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reservations")
@Tag(name = "Reservation Service", description = "Reservation creation and tracking")
public class ReservationController {

    private final CreateReservationUseCase createReservationUseCase;
    private final TrackReservationUseCase trackReservationUseCase;

    public ReservationController(CreateReservationUseCase createReservationUseCase,
                                  TrackReservationUseCase trackReservationUseCase) {
        this.createReservationUseCase = createReservationUseCase;
        this.trackReservationUseCase = trackReservationUseCase;
    }

    @PostMapping
    @Operation(summary = "Create a new reservation (starts SAGA)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Reservation created in PENDING state")
    public ResponseEntity<ApiResponse<CreateReservationResponse>> createReservation(
            @Valid @RequestBody CreateReservationRequest request) {

        List<CreateReservationCommand.CreateReservationItemCommand> itemCommands = request.items().stream()
                .map(item -> new CreateReservationCommand.CreateReservationItemCommand(
                        item.vehicleId(), item.dailyRate(), item.days()))
                .toList();

        var command = new CreateReservationCommand(
                request.customerId(),
                request.pickupAddress(),
                request.pickupCity(),
                request.returnAddress(),
                request.returnCity(),
                request.pickupDate(),
                request.returnDate(),
                request.currency(),
                itemCommands);

        var response = createReservationUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping("/{trackingId}")
    @Operation(summary = "Track reservation status by tracking ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reservation found")
    public ResponseEntity<ApiResponse<TrackReservationResponse>> trackReservation(
            @PathVariable String trackingId) {
        var response = trackReservationUseCase.execute(new TrackReservationCommand(trackingId));
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
