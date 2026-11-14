package com.priceforge.promotions.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.priceforge.promotions.domain.exception.InvalidPromotionDiscountException;
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
            .isInstanceOf(InvalidPromotionDiscountException.class);
        assertThatThrownBy(() -> new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(150)))
            .isInstanceOf(InvalidPromotionDiscountException.class);
    }

    @Test
    void rejectsANonPositiveValue() {
        assertThatThrownBy(() -> new Discount(DiscountType.FIXED_AMOUNT, BigDecimal.ZERO))
            .isInstanceOf(InvalidPromotionDiscountException.class);
        assertThatThrownBy(() -> new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(-10)))
            .isInstanceOf(InvalidPromotionDiscountException.class);
    }

    @Test
    void roundsAPercentageDiscountToTheCurrencysDecimals() {
        Discount discount = new Discount(DiscountType.PERCENTAGE, BigDecimal.valueOf(15));

        // 15% of 33.33 is 4.9995, which is not payable: it becomes 5.00.
        Money amount = discount.amountFor(Money.of(new BigDecimal("33.33"), "EUR"));

        assertThat(amount.amount()).isEqualTo(new BigDecimal("5.00"));
    }

    @Test
    void neverReturnsMoreDecimalsThanTheCurrencyHas() {
        Discount discount = new Discount(DiscountType.PERCENTAGE, new BigDecimal("12.3456"));

        Money amount = discount.amountFor(Money.of(new BigDecimal("100.0000"), "EUR"));

        assertThat(amount.amount().scale()).isEqualTo(2);
        assertThat(amount.amount()).isEqualTo(new BigDecimal("12.35"));
    }

    @Test
    void aFixedAmountIsAlsoExpressedInTheCurrencysDecimals() {
        Discount discount = new Discount(DiscountType.FIXED_AMOUNT, new BigDecimal("5.0000"));

        assertThat(discount.amountFor(Money.of(BigDecimal.TEN, "EUR")).amount()).isEqualTo(new BigDecimal("5.00"));
    }
}
