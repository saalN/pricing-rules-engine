package com.priceforge.audit.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * The {@code contextSnapshot} of a {@code PriceCalculated} event plus the prices it produced:
 * everything needed to re-run the same calculation later (UC-14) and compare the outcome.
 * {@code userId} is nullable (anonymous calculations), as in Pricing's own context.
 */
public record PriceCalculationSnapshot(
    String productId,
    String userId,
    String userSegment,
    int quantity,
    Instant requestedAt,
    String location,
    String currency,
    String zoneId,
    BigDecimal basePriceAmount,
    BigDecimal finalPriceAmount
) {

    public PriceCalculationSnapshot {
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(userSegment, "userSegment must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(zoneId, "zoneId must not be null");
        Objects.requireNonNull(basePriceAmount, "basePriceAmount must not be null");
        Objects.requireNonNull(finalPriceAmount, "finalPriceAmount must not be null");
    }
}
