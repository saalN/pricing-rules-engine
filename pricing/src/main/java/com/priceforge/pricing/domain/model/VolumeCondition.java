package com.priceforge.pricing.domain.model;

import com.priceforge.sharedkernel.PricingContext;
public record VolumeCondition(int minimumQuantity) implements RuleCondition {

    public VolumeCondition {
        if (minimumQuantity <= 0) {
            throw new IllegalArgumentException("minimumQuantity must be positive, but was " + minimumQuantity);
        }
    }

    @Override
    public boolean matches(PricingContext context) {
        return context.quantity() >= minimumQuantity;
    }

    @Override
    public String describe() {
        return "quantity >= " + minimumQuantity;
    }
}
