package com.priceforge.pricing.infrastructure.in.http.dto;

import com.priceforge.sharedkernel.CombinationMode;
import com.priceforge.pricing.domain.model.CustomerSegment;
import com.priceforge.pricing.domain.model.DiscountType;
import com.priceforge.pricing.domain.model.PricingRuleType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Shared shape for UC-05's rule fields, UC-08's hypothetical rule and UC-09's update payload —
 * only the fields matching {@code type} are populated. {@code scopeProductId}/{@code scopeCategory}
 * both absent means global scope.
 */
public record PricingRuleDefinitionRequest(
    @NotNull PricingRuleType type,
    LocalTime timeWindowStart,
    LocalTime timeWindowEnd,
    CustomerSegment segment,
    Integer minimumQuantity,
    String zone,
    @NotNull DiscountType discountType,
    @NotNull @Positive BigDecimal discountValue,
    int priority,
    @NotNull CombinationMode combinationMode,
    UUID scopeProductId,
    String scopeCategory
) {
}
