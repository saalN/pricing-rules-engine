package com.priceforge.audit.infrastructure.in.http.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReplayPriceCalculationResponse(
    UUID eventId,
    Instant originalCalculatedAt,
    String currency,
    BigDecimal originalFinalPriceAmount,
    BigDecimal replayedFinalPriceAmount,
    boolean matches
) {
}
