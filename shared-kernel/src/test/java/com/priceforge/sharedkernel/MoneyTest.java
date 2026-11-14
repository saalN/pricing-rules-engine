package com.priceforge.sharedkernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void addsAmountsWithTheSameCurrency() {
        Money ten = Money.of(BigDecimal.TEN, "EUR");
        Money five = Money.of(BigDecimal.valueOf(5), "EUR");

        assertThat(ten.plus(five)).isEqualTo(Money.of(BigDecimal.valueOf(15), "EUR"));
    }

    @Test
    void subtractsAmountsWithTheSameCurrency() {
        Money ten = Money.of(BigDecimal.TEN, "EUR");
        Money five = Money.of(BigDecimal.valueOf(5), "EUR");

        assertThat(ten.minus(five)).isEqualTo(Money.of(BigDecimal.valueOf(5), "EUR"));
    }

    @Test
    void rejectsOperationsAcrossDifferentCurrencies() {
        Money euros = Money.of(BigDecimal.TEN, "EUR");
        Money dollars = Money.of(BigDecimal.TEN, "USD");

        assertThatThrownBy(() -> euros.plus(dollars)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void multipliesByAFactor() {
        Money ten = Money.of(BigDecimal.TEN, "EUR");

        assertThat(ten.multipliedBy(BigDecimal.valueOf(0.2)))
            .isEqualTo(Money.of(BigDecimal.valueOf(2.0), "EUR"));
    }

    @Test
    void reportsNegativeAndZeroAmounts() {
        assertThat(Money.of(BigDecimal.valueOf(-1), "EUR").isNegative()).isTrue();
        assertThat(Money.of(BigDecimal.ZERO, "EUR").isZero()).isTrue();
        assertThat(Money.of(BigDecimal.TEN, "EUR").isNegative()).isFalse();
    }

    @Test
    void roundsHalfUpToTheCurrencysDecimals() {
        assertThat(Money.of(new BigDecimal("2.345"), "EUR").roundedToCurrency().amount()).isEqualTo(new BigDecimal("2.35"));
        assertThat(Money.of(new BigDecimal("2.344"), "EUR").roundedToCurrency().amount()).isEqualTo(new BigDecimal("2.34"));
        assertThat(Money.of(new BigDecimal("137.000000000000000000000000"), "EUR").roundedToCurrency().amount())
            .isEqualTo(new BigDecimal("137.00"));
    }

    @Test
    void padsAmountsThatHaveFewerDecimalsThanTheCurrency() {
        assertThat(Money.of(BigDecimal.TEN, "EUR").roundedToCurrency().amount()).isEqualTo(new BigDecimal("10.00"));
    }

    @Test
    void usesTheCurrencysOwnNumberOfDecimals() {
        assertThat(Money.of(new BigDecimal("1234.5"), "JPY").roundedToCurrency().amount()).isEqualTo(new BigDecimal("1235"));
    }
}
