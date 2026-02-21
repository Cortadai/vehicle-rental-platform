package com.vehiclerental.reservation;

import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.reservation.domain.model.aggregate.Reservation;
import com.vehiclerental.reservation.domain.model.entity.ReservationItem;
import com.vehiclerental.reservation.domain.model.vo.CustomerId;
import com.vehiclerental.reservation.domain.model.vo.DateRange;
import com.vehiclerental.reservation.domain.model.vo.PickupLocation;
import com.vehiclerental.reservation.domain.model.vo.ReservationId;
import com.vehiclerental.reservation.domain.model.vo.TrackingId;
import com.vehiclerental.reservation.domain.model.vo.VehicleId;
import com.vehiclerental.reservation.domain.port.output.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ReservationRepositoryAdapterIT {

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    void saveAndFindByIdRoundTripWithItems() {
        var currency = Currency.getInstance("USD");
        var item1 = ReservationItem.create(
                new VehicleId(UUID.randomUUID()),
                new Money(new BigDecimal("50.00"), currency),
                3);
        var item2 = ReservationItem.create(
                new VehicleId(UUID.randomUUID()),
                new Money(new BigDecimal("75.00"), currency),
                2);

        var reservation = Reservation.create(
                new CustomerId(UUID.randomUUID()),
                new PickupLocation("123 Main St", "New York"),
                new PickupLocation("456 Oak Ave", "Boston"),
                new DateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 4)),
                List.of(item1, item2));
        reservation.clearDomainEvents();

        var saved = reservationRepository.save(reservation);

        var found = reservationRepository.findById(saved.getId());

        assertThat(found).isPresent();
        var loaded = found.get();
        assertThat(loaded.getId()).isEqualTo(saved.getId());
        assertThat(loaded.getTrackingId()).isEqualTo(saved.getTrackingId());
        assertThat(loaded.getCustomerId()).isEqualTo(saved.getCustomerId());
        assertThat(loaded.getPickupLocation().address()).isEqualTo("123 Main St");
        assertThat(loaded.getPickupLocation().city()).isEqualTo("New York");
        assertThat(loaded.getReturnLocation().address()).isEqualTo("456 Oak Ave");
        assertThat(loaded.getReturnLocation().city()).isEqualTo("Boston");
        assertThat(loaded.getDateRange().pickupDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(loaded.getDateRange().returnDate()).isEqualTo(LocalDate.of(2026, 3, 4));
        assertThat(loaded.getTotalPrice().amount()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(loaded.getTotalPrice().currency()).isEqualTo(currency);
        assertThat(loaded.getStatus().name()).isEqualTo("PENDING");
        assertThat(loaded.getItems()).hasSize(2);
        assertThat(loaded.getCreatedAt()).isCloseTo(saved.getCreatedAt(), within(1, ChronoUnit.MICROS));
    }

    @Test
    void findByTrackingIdRoundTrip() {
        var currency = Currency.getInstance("EUR");
        var item = ReservationItem.create(
                new VehicleId(UUID.randomUUID()),
                new Money(new BigDecimal("100.00"), currency),
                5);

        var reservation = Reservation.create(
                new CustomerId(UUID.randomUUID()),
                new PickupLocation("789 Elm St", "Madrid"),
                new PickupLocation("101 Pine Rd", "Barcelona"),
                new DateRange(LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 15)),
                List.of(item));
        reservation.clearDomainEvents();

        var saved = reservationRepository.save(reservation);

        var found = reservationRepository.findByTrackingId(saved.getTrackingId());

        assertThat(found).isPresent();
        var loaded = found.get();
        assertThat(loaded.getTrackingId()).isEqualTo(saved.getTrackingId());
        assertThat(loaded.getTotalPrice().amount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(loaded.getTotalPrice().currency()).isEqualTo(currency);
        assertThat(loaded.getItems()).hasSize(1);

        var loadedItem = loaded.getItems().get(0);
        assertThat(loadedItem.getVehicleId()).isEqualTo(item.getVehicleId());
        assertThat(loadedItem.getDailyRate().amount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(loadedItem.getDays()).isEqualTo(5);
        assertThat(loadedItem.getSubtotal().amount()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void findByIdReturnsEmptyForNonExisting() {
        var nonExistingId = new ReservationId(UUID.randomUUID());

        var result = reservationRepository.findById(nonExistingId);

        assertThat(result).isEmpty();
    }

    @Test
    void findByTrackingIdReturnsEmptyForNonExisting() {
        var nonExistingTrackingId = new TrackingId(UUID.randomUUID());

        var result = reservationRepository.findByTrackingId(nonExistingTrackingId);

        assertThat(result).isEmpty();
    }
}
