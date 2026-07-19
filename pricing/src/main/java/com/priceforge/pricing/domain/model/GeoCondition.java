package com.priceforge.pricing.domain.model;

import java.util.Objects;

/**
 * {@code zone} is a free-form code (URBAN, RURAL, ZONE_A, ZONE_B, ...), not a closed enum — the
 * functional spec leaves the set of geographic zones open-ended.
 */
public record GeoCondition(String zone) implements RuleCondition {

    public GeoCondition {
        Objects.requireNonNull(zone, "zone must not be null");
    }

    @Override
    public boolean matches(PricingContext context) {
        return zone.equals(context.location());
    }

    @Override
    public String describe() {
        return "zone " + zone;
    }
}
