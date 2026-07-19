package com.priceforge.pricing.domain.exception;

import com.priceforge.sharedkernel.PricingRuleId;

public class PricingRuleNotFoundException extends RuntimeException {

    public PricingRuleNotFoundException(PricingRuleId id) {
        super("Pricing rule '" + id + "' does not exist");
    }
}
