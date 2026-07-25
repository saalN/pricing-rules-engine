package com.priceforge.pricing.application.dto;

import com.priceforge.sharedkernel.CombinationMode;
import com.priceforge.pricing.domain.model.CustomerSegment;
import com.priceforge.pricing.domain.model.DiscountType;
import com.priceforge.sharedkernel.PricingRuleId;
import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Type and scope are not modifiable (see {@code PricingRule.update}) — only the condition's
 * parameters, the discount, priority and combination mode can change.
 */
public record UpdatePricingRuleCommand(
    PricingRuleId ruleId,
    LocalTime timeWindowStart,
    LocalTime timeWindowEnd,
    CustomerSegment segment,
    Integer minimumQuantity,
    String zone,
    DiscountType discountType,
    BigDecimal discountValue,
    int priority,
    CombinationMode combinationMode,
    String changedBy
) {
}
