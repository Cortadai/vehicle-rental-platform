package com.vehiclerental.common.domain.vo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency USD = Currency.getInstance("USD");

    @Test
    void validConstruction_shouldSucceed() {
        var money = new Money(new BigDecimal("10.00"), EUR);

        assertThat(money.amount()).isEqualTo(new BigDecimal("10.00"));
        assertThat(money.currency()).isEqualTo(EUR);
    }

    @Test
    void nullAmount_shouldThrow() {
        assertThatThrownBy(() -> new Money(null, EUR))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullCurrency_shouldThrow() {
        assertThatThrownBy(() -> new Money(BigDecimal.TEN, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void negativeAmount_shouldThrow() {
        assertThatThrownBy(() -> new Money(new BigDecimal("-1.00"), EUR))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void scaleNormalization_shouldNormalizeTo2Decimals() {
        var money = new Money(new BigDecimal("10.5"), EUR);

        assertThat(money.amount()).isEqualTo(new BigDecimal("10.50"));
    }

    @Test
    void scaleNormalization_equalityAfterNormalization() {
        var money1 = new Money(new BigDecimal("10.5"), EUR);
        var money2 = new Money(new BigDecimal("10.50"), EUR);

        assertThat(money1).isEqualTo(money2);
    }

    @Test
    void add_sameCurrency_shouldReturnSum() {
        var a = new Money(new BigDecimal("10.00"), EUR);
        var b = new Money(new BigDecimal("5.50"), EUR);

        var result = a.add(b);

        assertThat(result.amount()).isEqualTo(new BigDecimal("15.50"));
        assertThat(result.currency()).isEqualTo(EUR);
    }

    @Test
    void subtract_sameCurrency_nonNegativeResult_shouldReturnDifference() {
        var a = new Money(new BigDecimal("10.00"), EUR);
        var b = new Money(new BigDecimal("3.50"), EUR);

        var result = a.subtract(b);

        assertThat(result.amount()).isEqualTo(new BigDecimal("6.50"));
        assertThat(result.currency()).isEqualTo(EUR);
    }

    @Test
    void subtract_resultingInNegative_shouldThrow() {
        var a = new Money(new BigDecimal("3.00"), EUR);
        var b = new Money(new BigDecimal("5.00"), EUR);

        assertThatThrownBy(() -> a.subtract(b))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void multiply_nonNegativeFactor_shouldReturnProduct() {
        var money = new Money(new BigDecimal("10.00"), EUR);

        var result = money.multiply(3);

        assertThat(result.amount()).isEqualTo(new BigDecimal("30.00"));
        assertThat(result.currency()).isEqualTo(EUR);
    }

    @Test
    void multiply_byZero_shouldReturnZero() {
        var money = new Money(new BigDecimal("10.00"), EUR);

        var result = money.multiply(0);

        assertThat(result.amount()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void multiply_byNegativeFactor_shouldThrow() {
        var money = new Money(new BigDecimal("10.00"), EUR);

        assertThatThrownBy(() -> money.multiply(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void add_currencyMismatch_shouldThrow() {
        var eur = new Money(new BigDecimal("10.00"), EUR);
        var usd = new Money(new BigDecimal("10.00"), USD);

        assertThatThrownBy(() -> eur.add(usd))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void subtract_currencyMismatch_shouldThrow() {
        var eur = new Money(new BigDecimal("10.00"), EUR);
        var usd = new Money(new BigDecimal("5.00"), USD);

        assertThatThrownBy(() -> eur.subtract(usd))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void arithmeticOperations_shouldNotModifyOriginal() {
        var original = new Money(new BigDecimal("10.00"), EUR);
        var other = new Money(new BigDecimal("5.00"), EUR);

        original.add(other);
        original.subtract(other);
        original.multiply(3);

        assertThat(original.amount()).isEqualTo(new BigDecimal("10.00"));
    }
}
