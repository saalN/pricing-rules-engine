package com.priceforge.pricing.domain.model;

import com.priceforge.sharedkernel.ConflictExclusionReason;
import com.priceforge.sharedkernel.PricingRuleId;

public record ExcludedRuleDetail(PricingRuleId ruleId, ConflictExclusionReason reason) {
}
