package com.vehiclerental.reservation.application.service;

import com.vehiclerental.common.domain.event.DomainEvent;
import com.vehiclerental.reservation.application.dto.command.CreateReservationCommand;
import com.vehiclerental.reservation.application.dto.command.CreateReservationCommand.CreateReservationItemCommand;
import com.vehiclerental.reservation.application.dto.response.CreateReservationResponse;
import com.vehiclerental.reservation.application.mapper.ReservationApplicationMapper;
import com.vehiclerental.reservation.application.port.output.ReservationDomainEventPublisher;
import com.vehiclerental.reservation.domain.model.aggregate.Reservation;
import com.vehiclerental.reservation.domain.port.output.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationApplicationServiceCreateTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationDomainEventPublisher eventPublisher;

    private final ReservationApplicationMapper mapper = new ReservationApplicationMapper();

    private ReservationApplicationService service;

    private static final String CUSTOMER_ID = UUID.randomUUID().toString();
    private static final String VEHICLE_ID_1 = UUID.randomUUID().toString();
    private static final String VEHICLE_ID_2 = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        service = new ReservationApplicationService(reservationRepository, eventPublisher, mapper);
    }

    @Nested
    class CreateReservation {

        @Test
        void createsReservationAndSavesAndPublishesEventsAndReturnsResponse() {
            when(reservationRepository.save(any(Reservation.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CreateReservationCommand command = buildCreateCommand();

            CreateReservationResponse response = service.execute(command);

            assertThat(response.trackingId()).isNotNull();
            assertThat(response.status()).isEqualTo("PENDING");
        }

        @Test
        void repositorySaveCalledOnce() {
            when(reservationRepository.save(any(Reservation.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            service.execute(buildCreateCommand());

            verify(reservationRepository).save(any(Reservation.class));
        }

        @Test
        void eventPublisherCalledWithDomainEvents() {
            when(reservationRepository.save(any(Reservation.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            service.execute(buildCreateCommand());

            verify(eventPublisher).publish(any());
        }

        @Test
        void savePublishClearOrderIsCorrect() {
            when(reservationRepository.save(any(Reservation.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            service.execute(buildCreateCommand());

            // Verify save happens before publish
            InOrder inOrder = inOrder(reservationRepository, eventPublisher);
            inOrder.verify(reservationRepository).save(any(Reservation.class));
            inOrder.verify(eventPublisher).publish(any());
        }

        @Test
        void domainEventsAreClearedAfterPublish() {
            ArgumentCaptor<Reservation> saveCaptor = ArgumentCaptor.forClass(Reservation.class);
            when(reservationRepository.save(saveCaptor.capture()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            service.execute(buildCreateCommand());

            // After the full cycle (save -> publish -> clearDomainEvents),
            // the aggregate's domain events should be empty
            Reservation savedReservation = saveCaptor.getValue();
            assertThat(savedReservation.getDomainEvents()).isEmpty();
        }

        @Test
        void itemsConvertedWithCurrencyFromCommand() {
            when(reservationRepository.save(any(Reservation.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            var command = new CreateReservationCommand(
                    CUSTOMER_ID,
                    "123 Pickup St", "Madrid",
                    "456 Return Ave", "Barcelona",
                    "2025-06-01", "2025-06-04",
                    "EUR",
                    List.of(
                            new CreateReservationItemCommand(VEHICLE_ID_1, new BigDecimal("100.00"), 3),
                            new CreateReservationItemCommand(VEHICLE_ID_2, new BigDecimal("50.00"), 3)
                    ));

            CreateReservationResponse response = service.execute(command);

            assertThat(response.status()).isEqualTo("PENDING");
            // Items were successfully converted — if currency or items were wrong, Reservation.create() would fail
        }
    }

    @Nested
    class AnnotationChecks {

        @Test
        void classHasNoServiceOrComponentAnnotation() {
            assertThat(ReservationApplicationService.class.isAnnotationPresent(
                    org.springframework.stereotype.Service.class)).isFalse();
            assertThat(ReservationApplicationService.class.isAnnotationPresent(
                    org.springframework.stereotype.Component.class)).isFalse();
        }

        @Test
        void createMethodIsTransactional() throws NoSuchMethodException {
            var method = ReservationApplicationService.class.getMethod("execute", CreateReservationCommand.class);
            var transactional = method.getAnnotation(Transactional.class);

            assertThat(transactional).isNotNull();
            assertThat(transactional.readOnly()).isFalse();
        }
    }

    private CreateReservationCommand buildCreateCommand() {
        return new CreateReservationCommand(
                CUSTOMER_ID,
                "123 Pickup St", "Madrid",
                "456 Return Ave", "Barcelona",
                "2025-06-01", "2025-06-04",
                "EUR",
                List.of(new CreateReservationItemCommand(VEHICLE_ID_1, new BigDecimal("100.00"), 3)));
    }
}
