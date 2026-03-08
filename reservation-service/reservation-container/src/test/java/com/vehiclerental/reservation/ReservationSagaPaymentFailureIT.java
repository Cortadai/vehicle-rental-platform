package com.vehiclerental.reservation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehiclerental.reservation.domain.model.saga.SagaStatus;
import com.vehiclerental.reservation.domain.model.vo.ReservationId;
import com.vehiclerental.reservation.domain.model.vo.ReservationStatus;
import com.vehiclerental.reservation.domain.model.vo.TrackingId;
import com.vehiclerental.reservation.domain.port.output.ReservationRepository;
import com.vehiclerental.reservation.domain.port.output.SagaStateRepository;
import com.vehiclerental.reservation.infrastructure.adapter.input.rest.dto.CreateReservationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class ReservationSagaPaymentFailureIT {

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private SagaStateRepository sagaStateRepository;

    @Test
    void paymentFailureAfterCustomerValidationLeadsToCancelled() throws Exception {
        UUID reservationId = createReservationAndGetId();

        // Step 1: Customer validated
        rabbitTemplate.convertAndSend("customer.exchange", "customer.validated",
                Map.of("customerId", UUID.randomUUID().toString(), "reservationId", reservationId.toString()));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(reservationRepository.findById(new ReservationId(reservationId)).orElseThrow().getStatus())
                        .isEqualTo(ReservationStatus.CUSTOMER_VALIDATED));

        // Step 2: Payment failed
        rabbitTemplate.convertAndSend("payment.exchange", "payment.failed",
                Map.of("paymentId", UUID.randomUUID().toString(),
                        "reservationId", reservationId.toString(),
                        "failureMessages", List.of("Insufficient funds")));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var reservation = reservationRepository.findById(new ReservationId(reservationId)).orElseThrow();
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        });

        var sagaState = sagaStateRepository.findById(reservationId).orElseThrow();
        assertThat(sagaState.getStatus()).isEqualTo(SagaStatus.FAILED);
    }

    private UUID createReservationAndGetId() throws Exception {
        var request = new CreateReservationRequest(
                UUID.randomUUID().toString(),
                "123 Main St", "Madrid",
                "456 Oak Ave", "Barcelona",
                "2026-06-01", "2026-06-05",
                "EUR",
                List.of(new CreateReservationRequest.CreateReservationItemRequest(
                        UUID.randomUUID().toString(), new BigDecimal("37.50"), 4)));

        String responseJson = mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode responseNode = objectMapper.readTree(responseJson);
        String trackingId = responseNode.get("data").get("trackingId").asText();
        return reservationRepository.findByTrackingId(new TrackingId(UUID.fromString(trackingId)))
                .orElseThrow().getId().value();
    }
}
