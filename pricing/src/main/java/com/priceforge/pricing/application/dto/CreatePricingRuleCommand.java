package com.priceforge.pricing.application.dto;

import com.priceforge.sharedkernel.CombinationMode;
import com.priceforge.pricing.domain.model.CustomerSegment;
import com.priceforge.pricing.domain.model.DiscountType;
import com.priceforge.pricing.domain.model.PricingRuleType;
import com.priceforge.sharedkernel.ProductId;
import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Only the fields matching {@code type} are populated (e.g. only timeWindowStart/End for
 * TIME_BASED) — a flat shape mirroring how the same rule definition is reused by
 * {@code HypotheticalPricingRule} (UC-08). {@code scopeProductId}/{@code scopeCategory} both null
 * means global scope.
 */
public record CreatePricingRuleCommand(
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
