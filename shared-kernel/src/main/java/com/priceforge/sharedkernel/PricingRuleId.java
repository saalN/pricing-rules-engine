package com.priceforge.sharedkernel;

import java.util.Objects;
import java.util.UUID;

public record PricingRuleId(UUID value) {

    public PricingRuleId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static PricingRuleId newId() {
        return new PricingRuleId(UUID.randomUUID());
    }

    public static PricingRuleId of(String value) {
        return new PricingRuleId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
