package com.priceforge.pricing.infrastructure.in.http.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Shared by UC-06 (calculate) and UC-08 (simulate) — both produce the same response shape. */
public record PriceQuoteResponse(
    BigDecimal basePriceAmount,
    BigDecimal finalPriceAmount,
    String currency,
    List<AppliedRuleResponse> appliedRules,
    List<ExcludedRuleResponse> excludedRules,
    Instant calculatedAt,
    boolean simulated
) {
}
