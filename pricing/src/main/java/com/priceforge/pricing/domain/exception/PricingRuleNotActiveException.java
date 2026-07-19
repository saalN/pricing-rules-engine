package com.priceforge.pricing.domain.exception;

import com.priceforge.sharedkernel.PricingRuleId;

public class PricingRuleNotActiveException extends RuntimeException {

    public PricingRuleNotActiveException(PricingRuleId id) {
        super("Pricing rule '" + id + "' is not ACTIVE");
    }
}
