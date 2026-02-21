package com.vehiclerental.reservation.domain.event;

import com.vehiclerental.common.domain.event.DomainEvent;
import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.reservation.domain.exception.ReservationDomainException;
import com.vehiclerental.reservation.domain.model.vo.CustomerId;
import com.vehiclerental.reservation.domain.model.vo.DateRange;
import com.vehiclerental.reservation.domain.model.vo.PickupLocation;
import com.vehiclerental.reservation.domain.model.vo.ReservationId;
import com.vehiclerental.reservation.domain.model.vo.TrackingId;
import com.vehiclerental.reservation.domain.model.vo.VehicleId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationDomainEventsTest {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final Instant OCCURRED_ON = Instant.now();
    private static final ReservationId RESERVATION_ID = new ReservationId(UUID.randomUUID());
    private static final TrackingId TRACKING_ID = new TrackingId(UUID.randomUUID());
    private static final CustomerId CUSTOMER_ID = new CustomerId(UUID.randomUUID());
    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Money TOTAL_PRICE = new Money(new BigDecimal("500.00"), EUR);
    private static final DateRange DATE_RANGE = new DateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5));
    private static final PickupLocation PICKUP_LOCATION = new PickupLocation("123 Main St", "Madrid");
    private static final PickupLocation RETURN_LOCATION = new PickupLocation("456 Oak Ave", "Barcelona");
    private static final VehicleId VEHICLE_ID = new VehicleId(UUID.randomUUID());
    private static final Money DAILY_RATE = new Money(new BigDecimal("100.00"), EUR);
    private static final Money SUBTOTAL = new Money(new BigDecimal("400.00"), EUR);
    private static final List<ReservationItemSnapshot> ITEMS = List.of(
            new ReservationItemSnapshot(VEHICLE_ID, DAILY_RATE, 4, SUBTOTAL));

    // --- ReservationCreatedEvent ---

    @Test
    void createdEventFieldsAccessible() {
        var event = new ReservationCreatedEvent(EVENT_ID, OCCURRED_ON, RESERVATION_ID, TRACKING_ID,
                CUSTOMER_ID, TOTAL_PRICE, DATE_RANGE, PICKUP_LOCATION, RETURN_LOCATION, ITEMS);

        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.occurredOn()).isEqualTo(OCCURRED_ON);
        assertThat(event.reservationId()).isEqualTo(RESERVATION_ID);
        assertThat(event.trackingId()).isEqualTo(TRACKING_ID);
        assertThat(event.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(event.totalPrice()).isEqualTo(TOTAL_PRICE);
        assertThat(event.dateRange()).isEqualTo(DATE_RANGE);
        assertThat(event.pickupLocation()).isEqualTo(PICKUP_LOCATION);
        assertThat(event.returnLocation()).isEqualTo(RETURN_LOCATION);
        assertThat(event.items()).isEqualTo(ITEMS);
    }

    @Test
    void createdEventNullEventIdThrows() {
        assertThatThrownBy(() -> new ReservationCreatedEvent(null, OCCURRED_ON, RESERVATION_ID, TRACKING_ID,
                CUSTOMER_ID, TOTAL_PRICE, DATE_RANGE, PICKUP_LOCATION, RETURN_LOCATION, ITEMS))
                .isInstanceOf(ReservationDomainException.class);
    }

    @Test
    void createdEventNullOccurredOnThrows() {
        assertThatThrownBy(() -> new ReservationCreatedEvent(EVENT_ID, null, RESERVATION_ID, TRACKING_ID,
                CUSTOMER_ID, TOTAL_PRICE, DATE_RANGE, PICKUP_LOCATION, RETURN_LOCATION, ITEMS))
                .isInstanceOf(ReservationDomainException.class);
    }

    @Test
    void createdEventImplementsDomainEvent() {
        var event = new ReservationCreatedEvent(EVENT_ID, OCCURRED_ON, RESERVATION_ID, TRACKING_ID,
                CUSTOMER_ID, TOTAL_PRICE, DATE_RANGE, PICKUP_LOCATION, RETURN_LOCATION, ITEMS);

        assertThat(event).isInstanceOf(DomainEvent.class);
    }

    @Test
    void createdEventIsRecord() {
        assertThat(ReservationCreatedEvent.class).isRecord();
    }

    // --- ReservationCancelledEvent ---

    @Test
    void cancelledEventFieldsAccessible() {
        var failureMessages = List.of("Fleet unavailable");
        var event = new ReservationCancelledEvent(EVENT_ID, OCCURRED_ON, RESERVATION_ID, failureMessages);

        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.occurredOn()).isEqualTo(OCCURRED_ON);
        assertThat(event.reservationId()).isEqualTo(RESERVATION_ID);
        assertThat(event.failureMessages()).isEqualTo(failureMessages);
    }

    @Test
    void cancelledEventNullEventIdThrows() {
        assertThatThrownBy(() -> new ReservationCancelledEvent(null, OCCURRED_ON, RESERVATION_ID, List.of()))
                .isInstanceOf(ReservationDomainException.class);
    }

    @Test
    void cancelledEventNullOccurredOnThrows() {
        assertThatThrownBy(() -> new ReservationCancelledEvent(EVENT_ID, null, RESERVATION_ID, List.of()))
                .isInstanceOf(ReservationDomainException.class);
    }

    @Test
    void cancelledEventImplementsDomainEvent() {
        var event = new ReservationCancelledEvent(EVENT_ID, OCCURRED_ON, RESERVATION_ID, List.of());

        assertThat(event).isInstanceOf(DomainEvent.class);
    }

    @Test
    void cancelledEventIsRecord() {
        assertThat(ReservationCancelledEvent.class).isRecord();
    }
}
