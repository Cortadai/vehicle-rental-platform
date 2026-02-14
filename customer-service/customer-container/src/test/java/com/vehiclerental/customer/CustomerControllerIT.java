package com.vehiclerental.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehiclerental.customer.infrastructure.adapter.input.rest.dto.CreateCustomerRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class CustomerControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void postCustomerReturns201() throws Exception {
        var request = new CreateCustomerRequest("John", "Doe", "john.ctrl@example.com", "+1234567890");

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.email").value("john.ctrl@example.com"));
    }

    @Test
    void getCustomerReturns200() throws Exception {
        var request = new CreateCustomerRequest("Jane", "Get", "jane.get@example.com", null);
        var createResult = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        var customerId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("customerId").asText();

        mockMvc.perform(get("/api/v1/customers/{id}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerId").value(customerId))
                .andExpect(jsonPath("$.data.firstName").value("Jane"));
    }

    @Test
    void getNonExistingCustomerReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/customers/{id}", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void suspendCustomerReturns200() throws Exception {
        var customerId = createCustomer("Suspend", "Test", "suspend.test@example.com");

        mockMvc.perform(post("/api/v1/customers/{id}/suspend", customerId))
                .andExpect(status().isOk());
    }

    @Test
    void activateCustomerReturns200() throws Exception {
        var customerId = createCustomer("Activate", "Test", "activate.test@example.com");

        mockMvc.perform(post("/api/v1/customers/{id}/suspend", customerId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/customers/{id}/activate", customerId))
                .andExpect(status().isOk());
    }

    @Test
    void deleteCustomerReturns204() throws Exception {
        var customerId = createCustomer("Delete", "Test", "delete.test@example.com");

        mockMvc.perform(delete("/api/v1/customers/{id}", customerId))
                .andExpect(status().isNoContent());
    }

    @Test
    void postWithInvalidBodyReturns400() throws Exception {
        var invalidRequest = new CreateCustomerRequest("", "", "not-an-email", null);

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    private String createCustomer(String firstName, String lastName, String email) throws Exception {
        var request = new CreateCustomerRequest(firstName, lastName, email, "+1234567890");
        var result = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("customerId").asText();
    }
}
