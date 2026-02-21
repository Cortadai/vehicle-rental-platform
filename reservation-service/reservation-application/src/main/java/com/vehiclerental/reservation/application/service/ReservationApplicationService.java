package com.vehiclerental.reservation.application.service;

import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.reservation.application.dto.command.CreateReservationCommand;
import com.vehiclerental.reservation.application.dto.command.TrackReservationCommand;
import com.vehiclerental.reservation.application.dto.response.CreateReservationResponse;
import com.vehiclerental.reservation.application.dto.response.TrackReservationResponse;
import com.vehiclerental.reservation.application.exception.ReservationNotFoundException;
import com.vehiclerental.reservation.application.mapper.ReservationApplicationMapper;
import com.vehiclerental.reservation.application.port.input.CreateReservationUseCase;
import com.vehiclerental.reservation.application.port.input.TrackReservationUseCase;
import com.vehiclerental.reservation.application.port.output.ReservationDomainEventPublisher;
import com.vehiclerental.reservation.domain.model.aggregate.Reservation;
import com.vehiclerental.reservation.domain.model.entity.ReservationItem;
import com.vehiclerental.reservation.domain.model.vo.CustomerId;
import com.vehiclerental.reservation.domain.model.vo.DateRange;
import com.vehiclerental.reservation.domain.model.vo.PickupLocation;
import com.vehiclerental.reservation.domain.model.vo.TrackingId;
import com.vehiclerental.reservation.domain.model.vo.VehicleId;
import com.vehiclerental.reservation.domain.port.output.ReservationRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

public class ReservationApplicationService implements CreateReservationUseCase, TrackReservationUseCase {

    private final ReservationRepository reservationRepository;
    private final ReservationDomainEventPublisher eventPublisher;
    private final ReservationApplicationMapper mapper;

    public ReservationApplicationService(ReservationRepository reservationRepository,
                                         ReservationDomainEventPublisher eventPublisher,
                                         ReservationApplicationMapper mapper) {
        this.reservationRepository = reservationRepository;
        this.eventPublisher = eventPublisher;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public CreateReservationResponse execute(CreateReservationCommand command) {
        CustomerId customerId = new CustomerId(UUID.fromString(command.customerId()));
        PickupLocation pickupLocation = new PickupLocation(command.pickupAddress(), command.pickupCity());
        PickupLocation returnLocation = new PickupLocation(command.returnAddress(), command.returnCity());
        DateRange dateRange = new DateRange(LocalDate.parse(command.pickupDate()), LocalDate.parse(command.returnDate()));
        Currency currency = Currency.getInstance(command.currency());

        List<ReservationItem> items = command.items().stream()
                .map(itemCmd -> ReservationItem.create(
                        new VehicleId(UUID.fromString(itemCmd.vehicleId())),
                        new Money(itemCmd.dailyRate(), currency),
                        itemCmd.days()))
                .toList();

        Reservation reservation = Reservation.create(customerId, pickupLocation, returnLocation, dateRange, items);

        Reservation savedReservation = reservationRepository.save(reservation);
        eventPublisher.publish(savedReservation.getDomainEvents());
        savedReservation.clearDomainEvents();

        return mapper.toCreateResponse(savedReservation);
    }

    @Override
    @Transactional(readOnly = true)
    public TrackReservationResponse execute(TrackReservationCommand command) {
        TrackingId trackingId = new TrackingId(UUID.fromString(command.trackingId()));

        Reservation reservation = reservationRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new ReservationNotFoundException(command.trackingId()));

        return mapper.toTrackResponse(reservation);
    }
}
