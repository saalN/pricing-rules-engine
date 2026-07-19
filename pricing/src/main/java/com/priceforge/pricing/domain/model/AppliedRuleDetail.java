package com.priceforge.pricing.domain.model;

import com.priceforge.sharedkernel.Money;
import com.priceforge.sharedkernel.PricingRuleId;

public record AppliedRuleDetail(
    PricingRuleId ruleId,
    PricingRuleType ruleType,
    String description,
    Money discountApplied,
    String reason
) {
}
