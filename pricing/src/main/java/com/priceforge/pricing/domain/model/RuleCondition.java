package com.priceforge.pricing.domain.model;

import com.priceforge.sharedkernel.PricingContext;
public sealed interface RuleCondition
    permits TimeWindowCondition, SegmentCondition, VolumeCondition, GeoCondition {

    boolean matches(PricingContext context);

    /** Human-readable summary used in {@code PriceQuote}'s applied/excluded rule breakdown. */
    String describe();
}
