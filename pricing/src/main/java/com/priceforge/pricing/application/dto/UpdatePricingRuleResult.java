package com.priceforge.pricing.application.dto;

import com.priceforge.sharedkernel.PricingRuleId;

public record UpdatePricingRuleResult(PricingRuleId ruleId, String previousState, String newState) {
}
