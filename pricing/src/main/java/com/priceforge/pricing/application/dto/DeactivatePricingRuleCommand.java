package com.priceforge.pricing.application.dto;

import com.priceforge.sharedkernel.PricingRuleId;

public record DeactivatePricingRuleCommand(PricingRuleId ruleId, String changedBy) {
}
