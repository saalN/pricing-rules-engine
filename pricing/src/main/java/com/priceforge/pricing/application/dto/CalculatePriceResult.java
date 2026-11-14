package com.priceforge.pricing.application.dto;

import com.priceforge.sharedkernel.AppliedPromotionDetail;
import com.priceforge.pricing.domain.model.AppliedRuleDetail;
import com.priceforge.sharedkernel.ExcludedPromotionDetail;
import com.priceforge.pricing.domain.model.ExcludedRuleDetail;
import com.priceforge.pricing.domain.model.PriceQuote;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Shared by UC-06 (real calculation) and UC-08 (simulation) — both produce the same shape. */
public record CalculatePriceResult(
    BigDecimal basePriceAmount,
    BigDecimal finalPriceAmount,
    String currency,
    List<AppliedRuleDetail> appliedRules,
    List<ExcludedRuleDetail> excludedRules,
    List<AppliedPromotionDetail> appliedPromotions,
    List<ExcludedPromotionDetail> excludedPromotions,
    Instant calculatedAt,
    boolean simulated
) {

    public static CalculatePriceResult from(PriceQuote quote) {
        return new CalculatePriceResult(
            quote.basePrice().amount(),
            quote.finalPrice().amount(),
            quote.finalPrice().currency().getCurrencyCode(),
            quote.appliedRules(),
            quote.excludedRules(),
            quote.appliedPromotions(),
            quote.excludedPromotions(),
            quote.calculatedAt(),
            quote.simulated());
    }
}
