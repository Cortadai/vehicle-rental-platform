package com.vehiclerental.fleet.domain.model.vo;

import com.vehiclerental.common.domain.vo.Money;
import com.vehiclerental.fleet.domain.exception.FleetDomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DailyRateTest {

    private static final Currency EUR = Currency.getInstance("EUR");

    @Test
    void validConstruction() {
        var money = new Money(new BigDecimal("50.00"), EUR);

        var dailyRate = new DailyRate(money);

        assertThat(dailyRate.money()).isEqualTo(money);
    }

    @Test
    void nullMoneyRejected() {
        assertThatThrownBy(() -> new DailyRate(null))
                .isInstanceOf(FleetDomainException.class);
    }

    @Test
    void zeroAmountRejected() {
        var zeroMoney = new Money(BigDecimal.ZERO, EUR);

        assertThatThrownBy(() -> new DailyRate(zeroMoney))
                .isInstanceOf(FleetDomainException.class);
    }

    @Test
    void equalityByValue() {
        var money = new Money(new BigDecimal("50.00"), EUR);

        var rate1 = new DailyRate(money);
        var rate2 = new DailyRate(money);

        assertThat(rate1).isEqualTo(rate2);
    }
}
