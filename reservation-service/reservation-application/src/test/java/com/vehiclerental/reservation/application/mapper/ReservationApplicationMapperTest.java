package com.vehiclerental.reservation.application.mapper;

import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.reservation.application.dto.response.CreateReservationResponse;
import com.vehiclerental.reservation.application.dto.response.TrackReservationResponse;
import com.vehiclerental.reservation.domain.model.aggregate.Reservation;
import com.vehiclerental.reservation.domain.model.entity.ReservationItem;
import com.vehiclerental.reservation.domain.model.vo.CustomerId;
import com.vehiclerental.reservation.domain.model.vo.DateRange;
import com.vehiclerental.reservation.domain.model.vo.PickupLocation;
import com.vehiclerental.reservation.domain.model.vo.ReservationId;
import com.vehiclerental.reservation.domain.model.vo.ReservationStatus;
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

class ReservationApplicationMapperTest {

    private final ReservationApplicationMapper mapper = new ReservationApplicationMapper();

    private static final UUID TRACKING_UUID = UUID.randomUUID();
    private static final UUID CUSTOMER_UUID = UUID.randomUUID();
    private static final UUID VEHICLE_UUID_1 = UUID.randomUUID();
    private static final UUID VEHICLE_UUID_2 = UUID.randomUUID();
    private static final Currency EUR = Currency.getInstance("EUR");

    @Test
    void toCreateResponseMapsTrackingIdAndStatus() {
        Reservation reservation = buildReservation();

        CreateReservationResponse response = mapper.toCreateResponse(reservation);

        assertThat(response.trackingId()).isEqualTo(TRACKING_UUID.toString());
        assertThat(response.status()).isEqualTo("PENDING");
    }

    @Test
    void toTrackResponseMapsAllFieldsIncludingItems() {
        Reservation reservation = buildReservation();

        TrackReservationResponse response = mapper.toTrackResponse(reservation);

        assertThat(response.trackingId()).isEqualTo(TRACKING_UUID.toString());
        assertThat(response.customerId()).isEqualTo(CUSTOMER_UUID.toString());
        assertThat(response.pickupAddress()).isEqualTo("123 Pickup St");
        assertThat(response.pickupCity()).isEqualTo("Madrid");
        assertThat(response.returnAddress()).isEqualTo("456 Return Ave");
        assertThat(response.returnCity()).isEqualTo("Barcelona");
        assertThat(response.pickupDate()).isEqualTo("2025-06-01");
        assertThat(response.returnDate()).isEqualTo("2025-06-04");
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.totalPrice()).isEqualByComparingTo(new BigDecimal("450.00"));
        assertThat(response.currency()).isEqualTo("EUR");
        assertThat(response.failureMessages()).isEmpty();
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void toTrackResponseMapsItemsWithVehicleIdDailyRateDaysAndSubtotal() {
        Reservation reservation = buildReservation();

        TrackReservationResponse response = mapper.toTrackResponse(reservation);

        assertThat(response.items()).hasSize(2);

        var item1 = response.items().get(0);
        assertThat(item1.vehicleId()).isEqualTo(VEHICLE_UUID_1.toString());
        assertThat(item1.dailyRate()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(item1.days()).isEqualTo(3);
        assertThat(item1.subtotal()).isEqualByComparingTo(new BigDecimal("300.00"));

        var item2 = response.items().get(1);
        assertThat(item2.vehicleId()).isEqualTo(VEHICLE_UUID_2.toString());
        assertThat(item2.dailyRate()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(item2.days()).isEqualTo(3);
        assertThat(item2.subtotal()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void toTrackResponsePreservesFailureMessages() {
        Reservation reservation = buildReservationWithFailureMessages();

        TrackReservationResponse response = mapper.toTrackResponse(reservation);

        assertThat(response.failureMessages()).containsExactly("Customer validation failed", "Payment declined");
    }

    private Reservation buildReservation() {
        var item1 = ReservationItem.create(
                new VehicleId(VEHICLE_UUID_1),
                new Money(new BigDecimal("100.00"), EUR),
                3);
        var item2 = ReservationItem.create(
                new VehicleId(VEHICLE_UUID_2),
                new Money(new BigDecimal("50.00"), EUR),
                3);

        return Reservation.reconstruct(
                new ReservationId(UUID.randomUUID()),
                new TrackingId(TRACKING_UUID),
                new CustomerId(CUSTOMER_UUID),
                new PickupLocation("123 Pickup St", "Madrid"),
                new PickupLocation("456 Return Ave", "Barcelona"),
                new DateRange(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 4)),
                new Money(new BigDecimal("450.00"), EUR),
                ReservationStatus.PENDING,
                List.of(item1, item2),
                List.of(),
                Instant.now(),
                Instant.now());
    }

    private Reservation buildReservationWithFailureMessages() {
        var item1 = ReservationItem.create(
                new VehicleId(VEHICLE_UUID_1),
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
                ReservationStatus.CANCELLING,
                List.of(item1),
                List.of("Customer validation failed", "Payment declined"),
                Instant.now(),
                Instant.now());
    }
}
