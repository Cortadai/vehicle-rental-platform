package com.vehiclerental.reservation.application.service;

import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.reservation.application.dto.command.TrackReservationCommand;
import com.vehiclerental.reservation.application.dto.response.TrackReservationResponse;
import com.vehiclerental.reservation.application.exception.ReservationNotFoundException;
import com.vehiclerental.reservation.application.mapper.ReservationApplicationMapper;
import com.vehiclerental.reservation.application.port.output.ReservationDomainEventPublisher;
import com.vehiclerental.reservation.application.saga.ReservationSagaOrchestrator;
import com.vehiclerental.reservation.domain.model.aggregate.Reservation;
import com.vehiclerental.reservation.domain.model.entity.ReservationItem;
import com.vehiclerental.reservation.domain.model.vo.CustomerId;
import com.vehiclerental.reservation.domain.model.vo.DateRange;
import com.vehiclerental.reservation.domain.model.vo.PickupLocation;
import com.vehiclerental.reservation.domain.model.vo.ReservationId;
import com.vehiclerental.reservation.domain.model.vo.ReservationStatus;
import com.vehiclerental.reservation.domain.model.vo.TrackingId;
import com.vehiclerental.reservation.domain.model.vo.VehicleId;
import com.vehiclerental.reservation.domain.port.output.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationApplicationServiceTrackTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationDomainEventPublisher eventPublisher;

    @Mock
    private ReservationSagaOrchestrator sagaOrchestrator;

    private final ReservationApplicationMapper mapper = new ReservationApplicationMapper();

    private ReservationApplicationService service;

    private static final UUID TRACKING_UUID = UUID.randomUUID();
    private static final String TRACKING_ID_STR = TRACKING_UUID.toString();
    private static final UUID CUSTOMER_UUID = UUID.randomUUID();
    private static final UUID VEHICLE_UUID = UUID.randomUUID();
    private static final Currency EUR = Currency.getInstance("EUR");

    @BeforeEach
    void setUp() {
        service = new ReservationApplicationService(reservationRepository, eventPublisher, mapper, sagaOrchestrator);
    }

    @Nested
    class TrackReservation {

        @Test
        void returnsFullSnapshotWhenReservationFound() {
            Reservation reservation = buildReservation();
            when(reservationRepository.findByTrackingId(new TrackingId(TRACKING_UUID)))
                    .thenReturn(Optional.of(reservation));

            var command = new TrackReservationCommand(TRACKING_ID_STR);
            TrackReservationResponse response = service.execute(command);

            assertThat(response.trackingId()).isEqualTo(TRACKING_ID_STR);
            assertThat(response.customerId()).isEqualTo(CUSTOMER_UUID.toString());
            assertThat(response.pickupAddress()).isEqualTo("123 Pickup St");
            assertThat(response.pickupCity()).isEqualTo("Madrid");
            assertThat(response.returnAddress()).isEqualTo("456 Return Ave");
            assertThat(response.returnCity()).isEqualTo("Barcelona");
            assertThat(response.pickupDate()).isEqualTo("2025-06-01");
            assertThat(response.returnDate()).isEqualTo("2025-06-04");
            assertThat(response.status()).isEqualTo("PENDING");
            assertThat(response.totalPrice()).isEqualByComparingTo(new BigDecimal("300.00"));
            assertThat(response.currency()).isEqualTo("EUR");
            assertThat(response.items()).hasSize(1);
            assertThat(response.items().get(0).vehicleId()).isEqualTo(VEHICLE_UUID.toString());
        }

        @Test
        void throwsReservationNotFoundExceptionWhenNotFound() {
            String nonExistentId = UUID.randomUUID().toString();
            when(reservationRepository.findByTrackingId(new TrackingId(UUID.fromString(nonExistentId))))
                    .thenReturn(Optional.empty());

            var command = new TrackReservationCommand(nonExistentId);

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(ReservationNotFoundException.class)
                    .hasMessageContaining(nonExistentId);
        }

        @Test
        void throwsExceptionForInvalidTrackingIdFormat() {
            var command = new TrackReservationCommand("not-a-valid-uuid");

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class AnnotationChecks {

        @Test
        void trackMethodIsReadOnlyTransactional() throws NoSuchMethodException {
            var method = ReservationApplicationService.class.getMethod("execute", TrackReservationCommand.class);
            var transactional = method.getAnnotation(Transactional.class);

            assertThat(transactional).isNotNull();
            assertThat(transactional.readOnly()).isTrue();
        }
    }

    private Reservation buildReservation() {
        var item = ReservationItem.create(
                new VehicleId(VEHICLE_UUID),
                new Money(new BigDecimal("100.00"), EUR),
                3);

        return Reservation.reconstruct(
                new ReservationId(UUID.randomUUID()),
                new TrackingId(TRACKING_UUID),
                new CustomerId(CUSTOMER_UUID),
                new PickupLocation("123 Pickup St", "Madrid"),
                new PickupLocation("456 Return Ave", "Barcelona"),
                new DateRange(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 4)),
                new Money(new BigDecimal("300.00"), EUR),
                ReservationStatus.PENDING,
                List.of(item),
                List.of(),
                Instant.now(),
                Instant.now());
    }
}
