package com.priceforge.pricing.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.priceforge.pricing.domain.exception.InvalidDiscountException;
import com.priceforge.sharedkernel.Money;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DiscountTest {

    @Test
    void computesAPercentageDiscount() {
        Discount discount = new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(20));

        Money amount = discount.amountFor(Money.of(BigDecimal.valueOf(100), "EUR"));

        assertThat(amount.amount()).isEqualByComparingTo(BigDecimal.valueOf(20));
        assertThat(amount.currency().getCurrencyCode()).isEqualTo("EUR");
    }

    @Test
    void computesAFixedAmountDiscount() {
        Discount discount = new Discount(DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(5));

        Money amount = discount.amountFor(Money.of(BigDecimal.valueOf(100), "EUR"));

        assertThat(amount.amount()).isEqualByComparingTo(BigDecimal.valueOf(5));
    }

    @Test
    void rejectsAPercentageOf100OrMore() {
        assertThatThrownBy(() -> new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(100)))
            .isInstanceOf(InvalidDiscountException.class);
        assertThatThrownBy(() -> new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(150)))
            .isInstanceOf(InvalidDiscountException.class);
    }

    @Test
    void rejectsANonPositiveValue() {
        assertThatThrownBy(() -> new Discount(DiscountType.FIXED_AMOUNT, BigDecimal.ZERO))
            .isInstanceOf(InvalidDiscountException.class);
        assertThatThrownBy(() -> new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(-10)))
            .isInstanceOf(InvalidDiscountException.class);
    }
}
