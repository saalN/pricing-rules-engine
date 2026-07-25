package com.priceforge.promotions.domain.model;

import com.priceforge.promotions.domain.exception.InvalidPromotionDiscountException;
import com.priceforge.sharedkernel.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * A promotion's own discount value object — not shared with Pricing's identically-shaped
 * {@code Discount}, since the two never cross the {@code PromotionApplierPort} boundary (only the
 * already-computed {@code Money} amount does) and each bounded context should stay free to evolve
 * its own effect model independently.
 */
public record Discount(DiscountType type, BigDecimal value) {

    public Discount {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(value, "value must not be null");
        if (value.signum() <= 0) {
            throw new InvalidPromotionDiscountException("Discount value must be positive, but was " + value);
        }
        if (type == DiscountType.PERCENTAGE && value.compareTo(BigDecimal.valueOf(100)) >= 0) {
            throw new InvalidPromotionDiscountException(
                "A percentage discount of 100% or more would always drive the price to zero or below, but was "
                    + value);
        }
    }

    /** The Money amount this discount removes from {@code price} — never clamped here. */
    public Money amountFor(Money price) {
        return switch (type) {
            case PERCENTAGE -> price.multipliedBy(value.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
            case FIXED_AMOUNT -> Money.of(value, price.currency().getCurrencyCode());
        };
    }
}
