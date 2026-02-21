package com.vehiclerental.reservation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehiclerental.reservation.infrastructure.adapter.input.rest.dto.CreateReservationRequest;
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
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class ReservationControllerIT {

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void postReservationReturns201WithTrackingIdAndStatus() throws Exception {
        var request = createValidRequest();

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.trackingId").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void getReservationByTrackingIdReturns200WithFullSnapshot() throws Exception {
        var request = createValidRequest();

        var createResult = mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        var trackingId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("trackingId").asText();

        mockMvc.perform(get("/api/v1/reservations/{trackingId}", trackingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trackingId").value(trackingId))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.pickupAddress").value("123 Main St"))
                .andExpect(jsonPath("$.data.pickupCity").value("New York"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(1));
    }

    @Test
    void getNonExistingReservationReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/{trackingId}", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void postWithInvalidBodyReturns400() throws Exception {
        var invalidRequest = new CreateReservationRequest(
                "", "", "", "", "", "", "", "",
                List.of());

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void postWithEmptyItemsReturns400() throws Exception {
        var requestWithNoItems = new CreateReservationRequest(
                UUID.randomUUID().toString(),
                "123 Main St", "New York",
                "456 Oak Ave", "Boston",
                "2026-03-01", "2026-03-04",
                "USD",
                List.of());

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithNoItems)))
                .andExpect(status().isBadRequest());
    }

    private CreateReservationRequest createValidRequest() {
        var item = new CreateReservationRequest.CreateReservationItemRequest(
                UUID.randomUUID().toString(),
                new BigDecimal("50.00"),
                3);

        return new CreateReservationRequest(
                UUID.randomUUID().toString(),
                "123 Main St", "New York",
                "456 Oak Ave", "Boston",
                "2026-03-01", "2026-03-04",
                "USD",
                List.of(item));
    }
}
