package com.priceforge.pricing.application.dto;

import com.priceforge.pricing.domain.model.PricingRuleStatus;
import com.priceforge.sharedkernel.PricingRuleId;

public record DeactivatePricingRuleResult(PricingRuleId ruleId, PricingRuleStatus status) {
}
