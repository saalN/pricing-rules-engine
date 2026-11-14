package com.priceforge.audit.application.dto;

import java.time.Instant;

/** UC-14. All filters are optional and combine with AND. */
public record PriceCalculationHistoryQuery(
    String productId,
    String userId,
    Instant from,
    Instant to,
    int page,
    int size
) {
}
