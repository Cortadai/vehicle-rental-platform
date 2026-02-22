package com.vehiclerental.payment.infrastructure.adapter.input.rest;

import com.vehiclerental.common.api.ApiResponse;
import com.vehiclerental.payment.application.dto.command.GetPaymentCommand;
import com.vehiclerental.payment.application.dto.command.ProcessPaymentCommand;
import com.vehiclerental.payment.application.dto.command.RefundPaymentCommand;
import com.vehiclerental.payment.application.dto.response.PaymentResponse;
import com.vehiclerental.payment.application.port.input.GetPaymentUseCase;
import com.vehiclerental.payment.application.port.input.ProcessPaymentUseCase;
import com.vehiclerental.payment.application.port.input.RefundPaymentUseCase;
import com.vehiclerental.payment.infrastructure.adapter.input.rest.dto.ProcessPaymentRequest;
import com.vehiclerental.payment.infrastructure.adapter.input.rest.dto.RefundPaymentRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final RefundPaymentUseCase refundPaymentUseCase;
    private final GetPaymentUseCase getPaymentUseCase;

    public PaymentController(ProcessPaymentUseCase processPaymentUseCase,
                             RefundPaymentUseCase refundPaymentUseCase,
                             GetPaymentUseCase getPaymentUseCase) {
        this.processPaymentUseCase = processPaymentUseCase;
        this.refundPaymentUseCase = refundPaymentUseCase;
        this.getPaymentUseCase = getPaymentUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody ProcessPaymentRequest request) {
        var command = new ProcessPaymentCommand(
                request.reservationId(), request.customerId(), request.amount(), request.currency());
        var response = processPaymentUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @PostMapping("/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @Valid @RequestBody RefundPaymentRequest request) {
        var command = new RefundPaymentCommand(request.reservationId());
        var response = refundPaymentUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(@PathVariable String id) {
        var response = getPaymentUseCase.execute(new GetPaymentCommand(id));
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
