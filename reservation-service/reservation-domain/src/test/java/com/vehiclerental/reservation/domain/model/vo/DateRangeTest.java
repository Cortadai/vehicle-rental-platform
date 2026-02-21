package com.vehiclerental.reservation.domain.model.vo;

import com.vehiclerental.reservation.domain.exception.ReservationDomainException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateRangeTest {

    @Test
    void validConstruction() {
        var pickup = LocalDate.of(2026, 3, 1);
        var returnDate = LocalDate.of(2026, 3, 5);
        var dateRange = new DateRange(pickup, returnDate);

        assertThat(dateRange.pickupDate()).isEqualTo(pickup);
        assertThat(dateRange.returnDate()).isEqualTo(returnDate);
    }

    @Test
    void getDaysReturnsCorrectCount() {
        var dateRange = new DateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5));

        assertThat(dateRange.getDays()).isEqualTo(4);
    }

    @Test
    void singleDayRange() {
        var dateRange = new DateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 2));

        assertThat(dateRange.getDays()).isEqualTo(1);
    }

    @Test
    void nullPickupDateRejected() {
        assertThatThrownBy(() -> new DateRange(null, LocalDate.of(2026, 3, 5)))
                .isInstanceOf(ReservationDomainException.class);
    }

    @Test
    void nullReturnDateRejected() {
        assertThatThrownBy(() -> new DateRange(LocalDate.of(2026, 3, 1), null))
                .isInstanceOf(ReservationDomainException.class);
    }

    @Test
    void returnDateEqualsPickupDateRejected() {
        var date = LocalDate.of(2026, 3, 1);
        assertThatThrownBy(() -> new DateRange(date, date))
                .isInstanceOf(ReservationDomainException.class);
    }

    @Test
    void returnDateBeforePickupDateRejected() {
        assertThatThrownBy(() -> new DateRange(LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 1)))
                .isInstanceOf(ReservationDomainException.class);
    }

    @Test
    void pastDatesAccepted() {
        var dateRange = new DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 5));

        assertThat(dateRange.pickupDate()).isEqualTo(LocalDate.of(2020, 1, 1));
        assertThat(dateRange.getDays()).isEqualTo(4);
    }
}
