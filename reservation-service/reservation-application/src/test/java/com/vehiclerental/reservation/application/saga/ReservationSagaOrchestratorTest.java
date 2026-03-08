package com.vehiclerental.reservation.application.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.reservation.domain.model.aggregate.Reservation;
import com.vehiclerental.reservation.domain.model.entity.ReservationItem;
import com.vehiclerental.reservation.domain.model.saga.SagaState;
import com.vehiclerental.reservation.domain.model.saga.SagaStatus;
import com.vehiclerental.reservation.domain.model.vo.CustomerId;
import com.vehiclerental.reservation.domain.model.vo.DateRange;
import com.vehiclerental.reservation.domain.model.vo.PickupLocation;
import com.vehiclerental.reservation.domain.model.vo.ReservationId;
import com.vehiclerental.reservation.domain.model.vo.ReservationStatus;
import com.vehiclerental.reservation.domain.model.vo.VehicleId;
import com.vehiclerental.reservation.domain.port.output.ReservationRepository;
import com.vehiclerental.reservation.domain.port.output.SagaStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservationSagaOrchestratorTest {

    @Mock
    private SagaStateRepository sagaStateRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private SagaStep<ReservationSagaData> customerValidationStep;
    @Mock
    private SagaStep<ReservationSagaData> paymentStep;
    @Mock
    private SagaStep<ReservationSagaData> fleetConfirmationStep;

    private ReservationSagaOrchestrator orchestrator;
    private ObjectMapper objectMapper;

    private static final UUID RESERVATION_UUID = UUID.randomUUID();
    private static final UUID CUSTOMER_UUID = UUID.randomUUID();
    private static final UUID VEHICLE_UUID = UUID.randomUUID();

    private ReservationSagaData sagaData;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        when(customerValidationStep.getName()).thenReturn("CUSTOMER_VALIDATION");
        when(paymentStep.getName()).thenReturn("PAYMENT");
        when(fleetConfirmationStep.getName()).thenReturn("FLEET_CONFIRMATION");
        when(customerValidationStep.hasCompensation()).thenReturn(false);
        when(paymentStep.hasCompensation()).thenReturn(true);
        when(fleetConfirmationStep.hasCompensation()).thenReturn(false);
        when(sagaStateRepository.save(any(SagaState.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        orchestrator = new ReservationSagaOrchestrator(
                List.of(customerValidationStep, paymentStep, fleetConfirmationStep),
                sagaStateRepository,
                reservationRepository,
                objectMapper
        );

        sagaData = new ReservationSagaData(
                RESERVATION_UUID, CUSTOMER_UUID, VEHICLE_UUID,
                new BigDecimal("300.00"), "EUR",
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 5)
        );
    }

    @Nested
    class Start {

        @Test
        void createsSagaStateAndExecutesFirstStep() {
            orchestrator.start(sagaData);

            ArgumentCaptor<SagaState> sagaCaptor = ArgumentCaptor.forClass(SagaState.class);
            verify(sagaStateRepository).save(sagaCaptor.capture());
            SagaState saved = sagaCaptor.getValue();
            assertThat(saved.getSagaId()).isEqualTo(RESERVATION_UUID);
            assertThat(saved.getStatus()).isEqualTo(SagaStatus.PROCESSING);
            assertThat(saved.getCurrentStep()).isZero();
            assertThat(saved.getTotalSteps()).isEqualTo(3);

            verify(customerValidationStep).process(any(ReservationSagaData.class));
        }
    }

    @Nested
    class HandleStepSuccess {

        @Test
        void customerValidationSuccessAdvancesToPayment() {
            SagaState sagaState = createProcessingSagaState(0);
            Reservation reservation = createPendingReservation();
            when(sagaStateRepository.findById(RESERVATION_UUID)).thenReturn(Optional.of(sagaState));
            when(reservationRepository.findById(new ReservationId(RESERVATION_UUID))).thenReturn(Optional.of(reservation));

            orchestrator.handleStepSuccess(RESERVATION_UUID, "CUSTOMER_VALIDATION");

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CUSTOMER_VALIDATED);
            verify(paymentStep).process(any(ReservationSagaData.class));
        }

        @Test
        void paymentSuccessAdvancesToFleet() {
            SagaState sagaState = createProcessingSagaState(1);
            Reservation reservation = createReservationInState(ReservationStatus.CUSTOMER_VALIDATED);
            when(sagaStateRepository.findById(RESERVATION_UUID)).thenReturn(Optional.of(sagaState));
            when(reservationRepository.findById(new ReservationId(RESERVATION_UUID))).thenReturn(Optional.of(reservation));

            orchestrator.handleStepSuccess(RESERVATION_UUID, "PAYMENT");

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PAID);
            verify(fleetConfirmationStep).process(any(ReservationSagaData.class));
        }

        @Test
        void fleetConfirmationSuccessCompletesSaga() {
            SagaState sagaState = createProcessingSagaState(2);
            Reservation reservation = createReservationInState(ReservationStatus.PAID);
            when(sagaStateRepository.findById(RESERVATION_UUID)).thenReturn(Optional.of(sagaState));
            when(reservationRepository.findById(new ReservationId(RESERVATION_UUID))).thenReturn(Optional.of(reservation));

            orchestrator.handleStepSuccess(RESERVATION_UUID, "FLEET_CONFIRMATION");

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
            assertThat(sagaState.getStatus()).isEqualTo(SagaStatus.SUCCEEDED);
        }
    }

    @Nested
    class CustomerRejection {

        @Test
        void cancelledImmediatelyWithNoCompensations() {
            SagaState sagaState = createProcessingSagaState(0);
            Reservation reservation = createPendingReservation();
            when(sagaStateRepository.findById(RESERVATION_UUID)).thenReturn(Optional.of(sagaState));
            when(reservationRepository.findById(new ReservationId(RESERVATION_UUID))).thenReturn(Optional.of(reservation));

            orchestrator.handleStepFailure(RESERVATION_UUID, "CUSTOMER_VALIDATION", List.of("Customer not found"));

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
            assertThat(sagaState.getStatus()).isEqualTo(SagaStatus.FAILED);
            assertThat(sagaState.getFailureReason()).isEqualTo("Customer not found");
            verify(paymentStep, never()).rollback(any());
            verify(fleetConfirmationStep, never()).rollback(any());
        }
    }

    @Nested
    class PaymentFailure {

        @Test
        void cancelledImmediatelySkippingCustomerCompensation() {
            SagaState sagaState = createProcessingSagaState(1);
            Reservation reservation = createReservationInState(ReservationStatus.CUSTOMER_VALIDATED);
            when(sagaStateRepository.findById(RESERVATION_UUID)).thenReturn(Optional.of(sagaState));
            when(reservationRepository.findById(new ReservationId(RESERVATION_UUID))).thenReturn(Optional.of(reservation));

            orchestrator.handleStepFailure(RESERVATION_UUID, "PAYMENT", List.of("Insufficient funds"));

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
            assertThat(sagaState.getStatus()).isEqualTo(SagaStatus.FAILED);
            verify(customerValidationStep, never()).rollback(any());
            verify(paymentStep, never()).rollback(any());
        }
    }

    @Nested
    class FleetRejection {

        @Test
        void initiatesCancellationAndCompensatesPayment() {
            SagaState sagaState = createProcessingSagaState(2);
            Reservation reservation = createReservationInState(ReservationStatus.PAID);
            when(sagaStateRepository.findById(RESERVATION_UUID)).thenReturn(Optional.of(sagaState));
            when(reservationRepository.findById(new ReservationId(RESERVATION_UUID))).thenReturn(Optional.of(reservation));

            orchestrator.handleStepFailure(RESERVATION_UUID, "FLEET_CONFIRMATION", List.of("Vehicle unavailable"));

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLING);
            assertThat(sagaState.getStatus()).isEqualTo(SagaStatus.COMPENSATING);
            verify(paymentStep).rollback(any(ReservationSagaData.class));
            verify(fleetConfirmationStep, never()).rollback(any());
        }

        @Test
        void compensationCompleteLeadsToCancelledAndFailed() {
            SagaState sagaState = SagaState.reconstruct(RESERVATION_UUID, "RESERVATION_CREATION",
                    SagaStatus.COMPENSATING, 2, 3,
                    serialize(sagaData), "Vehicle unavailable",
                    Instant.now(), Instant.now(), 1L);
            Reservation reservation = createReservationInState(ReservationStatus.CANCELLING);
            when(sagaStateRepository.findById(RESERVATION_UUID)).thenReturn(Optional.of(sagaState));
            when(reservationRepository.findById(new ReservationId(RESERVATION_UUID))).thenReturn(Optional.of(reservation));

            orchestrator.handleCompensationComplete(RESERVATION_UUID, "PAYMENT");

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
            assertThat(sagaState.getStatus()).isEqualTo(SagaStatus.FAILED);
        }
    }

    // --- Helper methods ---

    private Reservation createPendingReservation() {
        return createReservationInState(ReservationStatus.PENDING);
    }

    private Reservation createReservationInState(ReservationStatus status) {
        VehicleId vehicleId = new VehicleId(VEHICLE_UUID);
        Money dailyRate = new Money(new BigDecimal("75.00"), Currency.getInstance("EUR"));
        ReservationItem item = ReservationItem.create(vehicleId, dailyRate, 4);

        Reservation reservation = Reservation.create(
                new CustomerId(CUSTOMER_UUID),
                new PickupLocation("Street 1", "Madrid"),
                new PickupLocation("Street 2", "Madrid"),
                new DateRange(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 5)),
                List.of(item)
        );

        if (status == ReservationStatus.CUSTOMER_VALIDATED || status == ReservationStatus.PAID
                || status == ReservationStatus.CONFIRMED || status == ReservationStatus.CANCELLING) {
            reservation.validateCustomer();
        }
        if (status == ReservationStatus.PAID || status == ReservationStatus.CONFIRMED
                || status == ReservationStatus.CANCELLING) {
            reservation.pay();
        }
        if (status == ReservationStatus.CONFIRMED) {
            reservation.confirm();
        }
        if (status == ReservationStatus.CANCELLING) {
            reservation.initCancel(List.of("Test failure"));
        }

        return reservation;
    }

    private SagaState createProcessingSagaState(int currentStep) {
        return SagaState.reconstruct(RESERVATION_UUID, "RESERVATION_CREATION",
                SagaStatus.PROCESSING, currentStep, 3,
                serialize(sagaData), null,
                Instant.now(), Instant.now(), 0L);
    }

    private String serialize(ReservationSagaData data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
