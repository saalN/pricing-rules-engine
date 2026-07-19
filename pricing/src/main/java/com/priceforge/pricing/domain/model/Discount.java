package com.priceforge.pricing.domain.model;

import com.priceforge.pricing.domain.exception.InvalidDiscountException;
import com.priceforge.sharedkernel.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * {@code value} is the raw discount magnitude: a percentage point count (20 means 20%) for
 * {@code PERCENTAGE}, or a plain amount in the priced product's currency for {@code FIXED_AMOUNT}
 * — a discount never carries its own currency, it always applies in whatever currency the
 * product being priced uses (the platform never converts currencies).
 */
public record Discount(DiscountType type, BigDecimal value) {

    public Discount {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(value, "value must not be null");
        if (value.signum() <= 0) {
            throw new InvalidDiscountException("Discount value must be positive, but was " + value);
        }
        if (type == DiscountType.PERCENTAGE && value.compareTo(BigDecimal.valueOf(100)) >= 0) {
            throw new InvalidDiscountException(
                "A percentage discount of 100% or more would always drive the price to zero or below, but was " + value);
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
