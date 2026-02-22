package com.vehiclerental.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehiclerental.payment.infrastructure.adapter.input.rest.dto.ProcessPaymentRequest;
import com.vehiclerental.payment.infrastructure.adapter.input.rest.dto.RefundPaymentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class PaymentControllerIT {

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void postPaymentReturns201() throws Exception {
        var request = new ProcessPaymentRequest(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                new BigDecimal("150.00"),
                "USD");

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reservationId").value(request.reservationId()))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void getPaymentReturns200() throws Exception {
        var request = new ProcessPaymentRequest(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                new BigDecimal("200.00"),
                "EUR");

        var createResult = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        var paymentId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("paymentId").asText();

        mockMvc.perform(get("/api/v1/payments/{id}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentId").value(paymentId))
                .andExpect(jsonPath("$.data.amount").value(200.00));
    }

    @Test
    void getNonExistingPaymentReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/payments/{id}", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void postRefundReturns200() throws Exception {
        var reservationId = UUID.randomUUID().toString();
        var processRequest = new ProcessPaymentRequest(
                reservationId,
                UUID.randomUUID().toString(),
                new BigDecimal("300.00"),
                "USD");

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(processRequest)))
                .andExpect(status().isCreated());

        var refundRequest = new RefundPaymentRequest(reservationId);

        mockMvc.perform(post("/api/v1/payments/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REFUNDED"));
    }

    @Test
    void postWithInvalidBodyReturns400() throws Exception {
        var invalidRequest = new ProcessPaymentRequest("", "", null, "");

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void postWithDomainRuleViolationReturns422() throws Exception {
        var request = new ProcessPaymentRequest(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                new BigDecimal("0.00"),
                "USD");

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").exists());
    }
}
