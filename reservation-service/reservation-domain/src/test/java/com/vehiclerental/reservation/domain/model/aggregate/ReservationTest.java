package com.vehiclerental.reservation.domain.model.aggregate;

import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.reservation.domain.event.ReservationCreatedEvent;
import com.vehiclerental.reservation.domain.exception.ReservationDomainException;
import com.vehiclerental.reservation.domain.model.entity.ReservationItem;
import com.vehiclerental.reservation.domain.model.vo.CustomerId;
import com.vehiclerental.reservation.domain.model.vo.DateRange;
import com.vehiclerental.reservation.domain.model.vo.PickupLocation;
import com.vehiclerental.reservation.domain.model.vo.ReservationId;
import com.vehiclerental.reservation.domain.model.vo.ReservationStatus;
import com.vehiclerental.reservation.domain.model.vo.TrackingId;
import com.vehiclerental.reservation.domain.model.vo.VehicleId;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final CustomerId CUSTOMER_ID = new CustomerId(UUID.randomUUID());
    private static final PickupLocation PICKUP_LOCATION = new PickupLocation("123 Main St", "Madrid");
    private static final PickupLocation RETURN_LOCATION = new PickupLocation("456 Oak Ave", "Barcelona");
    private static final DateRange DATE_RANGE = new DateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 4));
    private static final Money DAILY_RATE_1 = new Money(new BigDecimal("100.00"), EUR);
    private static final Money DAILY_RATE_2 = new Money(new BigDecimal("200.00"), EUR);

    private List<ReservationItem> createItems() {
        return List.of(
                ReservationItem.create(new VehicleId(UUID.randomUUID()), DAILY_RATE_1, 3),
                ReservationItem.create(new VehicleId(UUID.randomUUID()), DAILY_RATE_2, 3)
        );
    }

    @Test
    void successfulCreation() {
        var items = createItems();
        var reservation = Reservation.create(CUSTOMER_ID, PICKUP_LOCATION, RETURN_LOCATION, DATE_RANGE, items);

        assertThat(reservation.getId()).isNotNull();
        assertThat(reservation.getTrackingId()).isNotNull();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(reservation.getDomainEvents()).hasSize(1);
        assertThat(reservation.getDomainEvents().get(0)).isInstanceOf(ReservationCreatedEvent.class);
        assertThat(reservation.getFailureMessages()).isEmpty();
    }

    @Test
    void totalPriceCalculatedFromItems() {
        var items = createItems();
        var reservation = Reservation.create(CUSTOMER_ID, PICKUP_LOCATION, RETURN_LOCATION, DATE_RANGE, items);

        // 100*3 + 200*3 = 300 + 600 = 900
        assertThat(reservation.getTotalPrice()).isEqualTo(new Money(new BigDecimal("900.00"), EUR));
    }

    @Test
    void nullCustomerIdRejected() {
        var items = createItems();
        assertThatThrownBy(() -> Reservation.create(null, PICKUP_LOCATION, RETURN_LOCATION, DATE_RANGE, items))
                .isInstanceOf(ReservationDomainException.class);
    }

    @Test
    void nullPickupLocationRejected() {
        var items = createItems();
        assertThatThrownBy(() -> Reservation.create(CUSTOMER_ID, null, RETURN_LOCATION, DATE_RANGE, items))
                .isInstanceOf(ReservationDomainException.class);
    }

    @Test
    void nullReturnLocationRejected() {
        var items = createItems();
        assertThatThrownBy(() -> Reservation.create(CUSTOMER_ID, PICKUP_LOCATION, null, DATE_RANGE, items))
                .isInstanceOf(ReservationDomainException.class);
    }

    @Test
    void nullDateRangeRejected() {
        var items = createItems();
        assertThatThrownBy(() -> Reservation.create(CUSTOMER_ID, PICKUP_LOCATION, RETURN_LOCATION, null, items))
                .isInstanceOf(ReservationDomainException.class);
    }

    @Test
    void nullItemsRejected() {
        assertThatThrownBy(() -> Reservation.create(CUSTOMER_ID, PICKUP_LOCATION, RETURN_LOCATION, DATE_RANGE, null))
                .isInstanceOf(ReservationDomainException.class);
    }

    @Test
    void emptyItemsRejected() {
        assertThatThrownBy(() -> Reservation.create(CUSTOMER_ID, PICKUP_LOCATION, RETURN_LOCATION, DATE_RANGE, List.of()))
                .isInstanceOf(ReservationDomainException.class);
    }

    @Test
    void fieldsAccessibleAfterCreation() {
        var items = createItems();
        var reservation = Reservation.create(CUSTOMER_ID, PICKUP_LOCATION, RETURN_LOCATION, DATE_RANGE, items);

        assertThat(reservation.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(reservation.getPickupLocation()).isEqualTo(PICKUP_LOCATION);
        assertThat(reservation.getReturnLocation()).isEqualTo(RETURN_LOCATION);
        assertThat(reservation.getDateRange()).isEqualTo(DATE_RANGE);
        assertThat(reservation.getItems()).hasSize(2);
        assertThat(reservation.getCreatedAt()).isNotNull();
        assertThat(reservation.getUpdatedAt()).isNotNull();
    }

    @Test
    void reconstructDoesNotEmitEvents() {
        var reservationId = new ReservationId(UUID.randomUUID());
        var trackingId = new TrackingId(UUID.randomUUID());
        var now = Instant.now();
        var totalPrice = new Money(new BigDecimal("300.00"), EUR);
        var items = List.of(ReservationItem.create(new VehicleId(UUID.randomUUID()), DAILY_RATE_1, 3));
        List<String> failureMessages = List.of();

        var reservation = Reservation.reconstruct(reservationId, trackingId, CUSTOMER_ID,
                PICKUP_LOCATION, RETURN_LOCATION, DATE_RANGE, totalPrice,
                ReservationStatus.PENDING, items, failureMessages, now, now);

        assertThat(reservation.getDomainEvents()).isEmpty();
        assertThat(reservation.getId()).isEqualTo(reservationId);
        assertThat(reservation.getTrackingId()).isEqualTo(trackingId);
        assertThat(reservation.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    void reconstructPreservesItemsAndFailureMessages() {
        var reservationId = new ReservationId(UUID.randomUUID());
        var trackingId = new TrackingId(UUID.randomUUID());
        var now = Instant.now();
        var totalPrice = new Money(new BigDecimal("900.00"), EUR);
        var items = createItems();
        var failureMessages = List.of("Fleet unavailable");

        var reservation = Reservation.reconstruct(reservationId, trackingId, CUSTOMER_ID,
                PICKUP_LOCATION, RETURN_LOCATION, DATE_RANGE, totalPrice,
                ReservationStatus.CANCELLING, items, failureMessages, now, now);

        assertThat(reservation.getItems()).hasSize(2);
        assertThat(reservation.getFailureMessages()).hasSize(1);
        assertThat(reservation.getFailureMessages().get(0)).isEqualTo("Fleet unavailable");
    }

    @Test
    void noPublicConstructors() {
        var constructors = Reservation.class.getDeclaredConstructors();

        assertThat(Arrays.stream(constructors).noneMatch(c -> Modifier.isPublic(c.getModifiers()))).isTrue();
    }
}
