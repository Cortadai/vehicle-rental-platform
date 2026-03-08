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
class ReservationSagaFleetRejectionIT {

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
    void fleetRejectionTriggersCompensationAndCancellation() throws Exception {
        UUID reservationId = createReservationAndGetId();

        // Step 1: Customer validated
        rabbitTemplate.convertAndSend("customer.exchange", "customer.validated",
                Map.of("customerId", UUID.randomUUID().toString(), "reservationId", reservationId.toString()));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(getStatus(reservationId)).isEqualTo(ReservationStatus.CUSTOMER_VALIDATED));

        // Step 2: Payment completed
        rabbitTemplate.convertAndSend("payment.exchange", "payment.completed",
                Map.of("paymentId", UUID.randomUUID().toString(),
                        "reservationId", reservationId.toString(),
                        "customerId", UUID.randomUUID().toString(),
                        "amount", 150.00));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(getStatus(reservationId)).isEqualTo(ReservationStatus.PAID));

        // Step 3: Fleet rejected → triggers compensation
        rabbitTemplate.convertAndSend("fleet.exchange", "fleet.rejected",
                Map.of("vehicleId", UUID.randomUUID().toString(),
                        "reservationId", reservationId.toString(),
                        "failureMessages", List.of("Vehicle unavailable")));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(getStatus(reservationId)).isEqualTo(ReservationStatus.CANCELLING));

        // Step 4: Payment refunded (compensation complete)
        rabbitTemplate.convertAndSend("payment.exchange", "payment.refunded",
                Map.of("paymentId", UUID.randomUUID().toString(),
                        "reservationId", reservationId.toString(),
                        "amount", 150.00));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(getStatus(reservationId)).isEqualTo(ReservationStatus.CANCELLED));

        // Verify SAGA state is FAILED
        var sagaState = sagaStateRepository.findById(reservationId).orElseThrow();
        assertThat(sagaState.getStatus()).isEqualTo(SagaStatus.FAILED);
        assertThat(sagaState.getFailureReason()).contains("Vehicle unavailable");
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

    private ReservationStatus getStatus(UUID reservationId) {
        return reservationRepository.findById(new ReservationId(reservationId))
                .orElseThrow().getStatus();
    }
}
