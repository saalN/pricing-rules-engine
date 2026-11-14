package com.priceforge.pricing.application.dto;

import com.priceforge.sharedkernel.CombinationMode;
import com.priceforge.sharedkernel.CustomerSegment;
import com.priceforge.pricing.domain.model.DiscountType;
import com.priceforge.pricing.domain.model.PricingRuleType;
import com.priceforge.sharedkernel.ProductId;
import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * UC-08: "the same fields as UC-05, but never persisted." Same shape as
 * {@link CreatePricingRuleCommand}'s rule-definition fields.
 */
public record HypotheticalPricingRule(
    PricingRuleType type,
    LocalTime timeWindowStart,
    LocalTime timeWindowEnd,
    CustomerSegment segment,
    Integer minimumQuantity,
    String zone,
    DiscountType discountType,
    BigDecimal discountValue,
    int priority,
    CombinationMode combinationMode,
    ProductId scopeProductId,
    String scopeCategory
) {
}
