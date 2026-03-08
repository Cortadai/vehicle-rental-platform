package com.vehiclerental.reservation.application.saga;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehiclerental.reservation.application.port.output.SagaCommandPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SagaStepsTest {

    @Mock
    private SagaCommandPublisher sagaCommandPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final UUID RESERVATION_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID VEHICLE_ID = UUID.randomUUID();

    private final ReservationSagaData sagaData = new ReservationSagaData(
            RESERVATION_ID, CUSTOMER_ID, VEHICLE_ID,
            new BigDecimal("150.00"), "EUR",
            LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 5)
    );

    @Nested
    class CustomerValidationStepTest {

        private CustomerValidationStep step;

        @BeforeEach
        void setUp() {
            step = new CustomerValidationStep(sagaCommandPublisher, objectMapper);
        }

        @Test
        void processPublishesToCorrectExchangeAndRoutingKey() throws Exception {
            step.process(sagaData);

            ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
            verify(sagaCommandPublisher).publish(exchangeCaptor.capture(), routingKeyCaptor.capture(), payloadCaptor.capture());

            assertThat(exchangeCaptor.getValue()).isEqualTo("customer.exchange");
            assertThat(routingKeyCaptor.getValue()).isEqualTo("customer.validate.command");

            JsonNode json = objectMapper.readTree(payloadCaptor.getValue());
            assertThat(json.get("customerId").asText()).isEqualTo(CUSTOMER_ID.toString());
            assertThat(json.get("reservationId").asText()).isEqualTo(RESERVATION_ID.toString());
        }

        @Test
        void hasCompensationReturnsFalse() {
            assertThat(step.hasCompensation()).isFalse();
        }

        @Test
        void getNameReturnsCustomerValidation() {
            assertThat(step.getName()).isEqualTo("CUSTOMER_VALIDATION");
        }

        @Test
        void rollbackIsNoOp() {
            step.rollback(sagaData);

            verifyNoInteractions(sagaCommandPublisher);
        }
    }

    @Nested
    class PaymentStepTest {

        private PaymentStep step;

        @BeforeEach
        void setUp() {
            step = new PaymentStep(sagaCommandPublisher, objectMapper);
        }

        @Test
        void processPublishesPaymentCommand() throws Exception {
            step.process(sagaData);

            ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
            verify(sagaCommandPublisher).publish(exchangeCaptor.capture(), routingKeyCaptor.capture(), payloadCaptor.capture());

            assertThat(exchangeCaptor.getValue()).isEqualTo("payment.exchange");
            assertThat(routingKeyCaptor.getValue()).isEqualTo("payment.process.command");

            JsonNode json = objectMapper.readTree(payloadCaptor.getValue());
            assertThat(json.get("reservationId").asText()).isEqualTo(RESERVATION_ID.toString());
            assertThat(json.get("customerId").asText()).isEqualTo(CUSTOMER_ID.toString());
            assertThat(json.get("amount").decimalValue()).isEqualByComparingTo(new BigDecimal("150.00"));
            assertThat(json.get("currency").asText()).isEqualTo("EUR");
        }

        @Test
        void rollbackPublishesRefundCommand() throws Exception {
            step.rollback(sagaData);

            ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
            verify(sagaCommandPublisher).publish(exchangeCaptor.capture(), routingKeyCaptor.capture(), payloadCaptor.capture());

            assertThat(exchangeCaptor.getValue()).isEqualTo("payment.exchange");
            assertThat(routingKeyCaptor.getValue()).isEqualTo("payment.refund.command");

            JsonNode json = objectMapper.readTree(payloadCaptor.getValue());
            assertThat(json.get("reservationId").asText()).isEqualTo(RESERVATION_ID.toString());
        }

        @Test
        void hasCompensationReturnsTrue() {
            assertThat(step.hasCompensation()).isTrue();
        }

        @Test
        void getNameReturnsPayment() {
            assertThat(step.getName()).isEqualTo("PAYMENT");
        }
    }

    @Nested
    class FleetConfirmationStepTest {

        private FleetConfirmationStep step;

        @BeforeEach
        void setUp() {
            step = new FleetConfirmationStep(sagaCommandPublisher, objectMapper);
        }

        @Test
        void processPublishesFleetConfirmCommand() throws Exception {
            step.process(sagaData);

            ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
            verify(sagaCommandPublisher).publish(exchangeCaptor.capture(), routingKeyCaptor.capture(), payloadCaptor.capture());

            assertThat(exchangeCaptor.getValue()).isEqualTo("fleet.exchange");
            assertThat(routingKeyCaptor.getValue()).isEqualTo("fleet.confirm.command");

            JsonNode json = objectMapper.readTree(payloadCaptor.getValue());
            assertThat(json.get("vehicleId").asText()).isEqualTo(VEHICLE_ID.toString());
            assertThat(json.get("reservationId").asText()).isEqualTo(RESERVATION_ID.toString());
            assertThat(json.get("pickupDate").asText()).isEqualTo("2026-04-01");
            assertThat(json.get("returnDate").asText()).isEqualTo("2026-04-05");
        }

        @Test
        void hasCompensationReturnsFalse() {
            assertThat(step.hasCompensation()).isFalse();
        }

        @Test
        void getNameReturnsFleetConfirmation() {
            assertThat(step.getName()).isEqualTo("FLEET_CONFIRMATION");
        }

        @Test
        void rollbackIsNoOp() {
            step.rollback(sagaData);

            verifyNoInteractions(sagaCommandPublisher);
        }
    }
}
