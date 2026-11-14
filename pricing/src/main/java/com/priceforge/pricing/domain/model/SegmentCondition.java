package com.priceforge.pricing.domain.model;

import com.priceforge.sharedkernel.CustomerSegment;
import com.priceforge.sharedkernel.PricingContext;
import java.util.Objects;

public record SegmentCondition(CustomerSegment segment) implements RuleCondition {

    public SegmentCondition {
        Objects.requireNonNull(segment, "segment must not be null");
    }

    @Override
    public boolean matches(PricingContext context) {
        return context.userSegment() == segment;
    }

    @Override
    public String describe() {
        return "customer segment " + segment;
    }
}
