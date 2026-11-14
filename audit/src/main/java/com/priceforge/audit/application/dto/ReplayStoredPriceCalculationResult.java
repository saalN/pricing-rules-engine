package com.priceforge.audit.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * {@code matches} compares the stored final price with the replayed one by numeric value (scale
 * ignored). The replay re-runs the pricing rules only, so a calculation that originally had a
 * promotion applied can legitimately report {@code matches = false}.
 */
public record ReplayStoredPriceCalculationResult(
    UUID eventId,
    Instant originalCalculatedAt,
    String currency,
    BigDecimal originalFinalPriceAmount,
    BigDecimal replayedFinalPriceAmount,
    boolean matches
) {
}
