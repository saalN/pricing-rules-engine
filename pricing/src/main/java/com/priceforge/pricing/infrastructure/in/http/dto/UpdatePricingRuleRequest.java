package com.priceforge.pricing.infrastructure.in.http.dto;

import com.priceforge.sharedkernel.CombinationMode;
import com.priceforge.sharedkernel.CustomerSegment;
import com.priceforge.pricing.domain.model.DiscountType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalTime;

/** No {@code type} field — a rule's type is fixed at creation (see {@code PricingRule.update}). */
public record UpdatePricingRuleRequest(
    LocalTime timeWindowStart,
    LocalTime timeWindowEnd,
    CustomerSegment segment,
    Integer minimumQuantity,
    String zone,
    @NotNull DiscountType discountType,
    @NotNull @Positive BigDecimal discountValue,
    int priority,
    @NotNull CombinationMode combinationMode
) {
}
