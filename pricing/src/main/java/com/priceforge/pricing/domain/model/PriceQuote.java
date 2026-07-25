package com.priceforge.pricing.domain.model;

import com.priceforge.sharedkernel.Money;
import java.time.Instant;
import java.util.List;

/**
 * {@code currency} isn't a separate field — it's always {@code finalPrice.currency()}, since the
 * platform never converts currencies mid-calculation. {@code appliedPromotions}/
 * {@code excludedPromotions} are always empty for UC-08's simulator, which only ever considers
 * pricing rules — applying a real promotion requires an atomic usage-limit increment, which has
 * no sensible meaning in a simulation that persists nothing.
 */
public record PriceQuote(
    Money basePrice,
    Money finalPrice,
    List<AppliedRuleDetail> appliedRules,
    List<ExcludedRuleDetail> excludedRules,
    List<AppliedPromotionDetail> appliedPromotions,
    List<ExcludedPromotionDetail> excludedPromotions,
    Instant calculatedAt,
    PricingContext contextSnapshot,
    boolean priceFloorApplied,
    boolean simulated
) {
}
