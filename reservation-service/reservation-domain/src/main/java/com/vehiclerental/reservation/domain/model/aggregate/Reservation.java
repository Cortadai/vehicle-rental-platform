package com.vehiclerental.reservation.domain.model.aggregate;

import com.vehiclerental.common.domain.entity.AggregateRoot;
import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.reservation.domain.event.ReservationCancelledEvent;
import com.vehiclerental.reservation.domain.event.ReservationCreatedEvent;
import com.vehiclerental.reservation.domain.event.ReservationItemSnapshot;
import com.vehiclerental.reservation.domain.exception.ReservationDomainException;
import com.vehiclerental.reservation.domain.model.entity.ReservationItem;
import com.vehiclerental.reservation.domain.model.vo.CustomerId;
import com.vehiclerental.reservation.domain.model.vo.DateRange;
import com.vehiclerental.reservation.domain.model.vo.PickupLocation;
import com.vehiclerental.reservation.domain.model.vo.ReservationId;
import com.vehiclerental.reservation.domain.model.vo.ReservationStatus;
import com.vehiclerental.reservation.domain.model.vo.TrackingId;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Reservation extends AggregateRoot<ReservationId> {

    private final TrackingId trackingId;
    private final CustomerId customerId;
    private final PickupLocation pickupLocation;
    private final PickupLocation returnLocation;
    private final DateRange dateRange;
    private final Money totalPrice;
    private final List<ReservationItem> items;
    private final Instant createdAt;
    private ReservationStatus status;
    private List<String> failureMessages;
    private Instant updatedAt;

    private Reservation(ReservationId id, TrackingId trackingId, CustomerId customerId,
                        PickupLocation pickupLocation, PickupLocation returnLocation,
                        DateRange dateRange, Money totalPrice, ReservationStatus status,
                        List<ReservationItem> items, List<String> failureMessages,
                        Instant createdAt, Instant updatedAt) {
        super(id);
        this.trackingId = trackingId;
        this.customerId = customerId;
        this.pickupLocation = pickupLocation;
        this.returnLocation = returnLocation;
        this.dateRange = dateRange;
        this.totalPrice = totalPrice;
        this.status = status;
        this.items = items;
        this.failureMessages = failureMessages;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Reservation create(CustomerId customerId, PickupLocation pickupLocation,
                                     PickupLocation returnLocation, DateRange dateRange,
                                     List<ReservationItem> items) {
        if (customerId == null) {
            throw new ReservationDomainException("customerId must not be null", "RESERVATION_CUSTOMER_ID_REQUIRED");
        }
        if (pickupLocation == null) {
            throw new ReservationDomainException("pickupLocation must not be null", "RESERVATION_PICKUP_LOCATION_REQUIRED");
        }
        if (returnLocation == null) {
            throw new ReservationDomainException("returnLocation must not be null", "RESERVATION_RETURN_LOCATION_REQUIRED");
        }
        if (dateRange == null) {
            throw new ReservationDomainException("dateRange must not be null", "RESERVATION_DATE_RANGE_REQUIRED");
        }
        if (items == null || items.isEmpty()) {
            throw new ReservationDomainException("items must not be null or empty", "RESERVATION_ITEMS_REQUIRED");
        }

        var reservationId = new ReservationId(UUID.randomUUID());
        var trackingId = new TrackingId(UUID.randomUUID());
        var now = Instant.now();

        Money totalPrice = items.stream()
                .map(ReservationItem::getSubtotal)
                .reduce(Money::add)
                .orElseThrow();

        var reservation = new Reservation(reservationId, trackingId, customerId,
                pickupLocation, returnLocation, dateRange, totalPrice,
                ReservationStatus.PENDING, List.copyOf(items), List.of(), now, now);

        List<ReservationItemSnapshot> itemSnapshots = items.stream()
                .map(item -> new ReservationItemSnapshot(
                        item.getVehicleId(), item.getDailyRate(), item.getDays(), item.getSubtotal()))
                .toList();

        reservation.registerDomainEvent(new ReservationCreatedEvent(
                UUID.randomUUID(), now, reservationId, trackingId, customerId,
                totalPrice, dateRange, pickupLocation, returnLocation, itemSnapshots));

        return reservation;
    }

    public static Reservation reconstruct(ReservationId id, TrackingId trackingId, CustomerId customerId,
                                          PickupLocation pickupLocation, PickupLocation returnLocation,
                                          DateRange dateRange, Money totalPrice, ReservationStatus status,
                                          List<ReservationItem> items, List<String> failureMessages,
                                          Instant createdAt, Instant updatedAt) {
        return new Reservation(id, trackingId, customerId, pickupLocation, returnLocation,
                dateRange, totalPrice, status, List.copyOf(items), List.copyOf(failureMessages),
                createdAt, updatedAt);
    }

    public void validateCustomer() {
        if (status != ReservationStatus.PENDING) {
            throw new ReservationDomainException(
                    "Cannot validate customer in state " + status, "RESERVATION_INVALID_STATE");
        }
        status = ReservationStatus.CUSTOMER_VALIDATED;
        updatedAt = Instant.now();
    }

    public void pay() {
        if (status != ReservationStatus.CUSTOMER_VALIDATED) {
            throw new ReservationDomainException(
                    "Cannot pay in state " + status, "RESERVATION_INVALID_STATE");
        }
        status = ReservationStatus.PAID;
        updatedAt = Instant.now();
    }

    public void confirm() {
        if (status != ReservationStatus.PAID) {
            throw new ReservationDomainException(
                    "Cannot confirm in state " + status, "RESERVATION_INVALID_STATE");
        }
        status = ReservationStatus.CONFIRMED;
        updatedAt = Instant.now();
    }

    public void initCancel(List<String> failureMessages) {
        if (failureMessages == null || failureMessages.isEmpty()) {
            throw new ReservationDomainException(
                    "failureMessages must not be null or empty", "RESERVATION_FAILURE_MESSAGES_REQUIRED");
        }
        if (status != ReservationStatus.PAID) {
            throw new ReservationDomainException(
                    "Cannot initCancel in state " + status, "RESERVATION_INVALID_STATE");
        }
        this.failureMessages = List.copyOf(failureMessages);
        status = ReservationStatus.CANCELLING;
        updatedAt = Instant.now();
    }

    public void cancel() {
        Set<ReservationStatus> cancellableStates = Set.of(
                ReservationStatus.PENDING, ReservationStatus.CUSTOMER_VALIDATED, ReservationStatus.CANCELLING);
        if (!cancellableStates.contains(status)) {
            throw new ReservationDomainException(
                    "Cannot cancel in state " + status, "RESERVATION_INVALID_STATE");
        }
        status = ReservationStatus.CANCELLED;
        updatedAt = Instant.now();
        registerDomainEvent(new ReservationCancelledEvent(
                UUID.randomUUID(), Instant.now(), getId(), List.copyOf(failureMessages)));
    }

    public TrackingId getTrackingId() {
        return trackingId;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public PickupLocation getPickupLocation() {
        return pickupLocation;
    }

    public PickupLocation getReturnLocation() {
        return returnLocation;
    }

    public DateRange getDateRange() {
        return dateRange;
    }

    public Money getTotalPrice() {
        return totalPrice;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public List<ReservationItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public List<String> getFailureMessages() {
        return Collections.unmodifiableList(failureMessages);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
