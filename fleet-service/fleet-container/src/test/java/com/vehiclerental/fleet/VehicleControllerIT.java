package com.vehiclerental.fleet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehiclerental.fleet.infrastructure.adapter.input.rest.dto.RegisterVehicleRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class VehicleControllerIT {

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void postVehicleReturns201() throws Exception {
        var request = new RegisterVehicleRequest(
                "AA-001-BB", "Toyota", "Corolla", 2023,
                "SEDAN", new BigDecimal("49.99"), "USD", "Test vehicle");

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.licensePlate").value("AA-001-BB"))
                .andExpect(jsonPath("$.data.make").value("Toyota"));
    }

    @Test
    void getVehicleReturns200() throws Exception {
        var vehicleId = createVehicle("BB-002-CC", "Honda", "Civic", "SEDAN");

        mockMvc.perform(get("/api/v1/vehicles/{id}", vehicleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.vehicleId").value(vehicleId))
                .andExpect(jsonPath("$.data.make").value("Honda"));
    }

    @Test
    void getNonExistingVehicleReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles/{id}", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void sendToMaintenanceReturns200() throws Exception {
        var vehicleId = createVehicle("CC-003-DD", "Ford", "Focus", "SEDAN");

        mockMvc.perform(post("/api/v1/vehicles/{id}/maintenance", vehicleId))
                .andExpect(status().isOk());
    }

    @Test
    void activateVehicleReturns200() throws Exception {
        var vehicleId = createVehicle("DD-004-EE", "BMW", "X3", "SUV");

        mockMvc.perform(post("/api/v1/vehicles/{id}/maintenance", vehicleId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/vehicles/{id}/activate", vehicleId))
                .andExpect(status().isOk());
    }

    @Test
    void retireVehicleReturns200() throws Exception {
        var vehicleId = createVehicle("EE-005-FF", "Audi", "A4", "SEDAN");

        mockMvc.perform(post("/api/v1/vehicles/{id}/retire", vehicleId))
                .andExpect(status().isOk());
    }

    @Test
    void postWithInvalidBodyReturns400() throws Exception {
        var invalidRequest = new RegisterVehicleRequest(
                "", "", "", 0,
                "", null, "", null);

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void postWithDomainRuleViolationReturns422() throws Exception {
        var vehicleId = createVehicle("FF-006-GG", "Mercedes", "C200", "SEDAN");

        // Retire first
        mockMvc.perform(post("/api/v1/vehicles/{id}/retire", vehicleId))
                .andExpect(status().isOk());

        // Try to send retired vehicle to maintenance — domain rule violation
        mockMvc.perform(post("/api/v1/vehicles/{id}/maintenance", vehicleId))
                .andExpect(status().isUnprocessableEntity());
    }

    private String createVehicle(String licensePlate, String make, String model, String category) throws Exception {
        var request = new RegisterVehicleRequest(
                licensePlate, make, model, 2023,
                category, new BigDecimal("50.00"), "USD", "Test");
        var result = mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("vehicleId").asText();
    }
}
