package com.priceforge.pricing.domain.model;

import com.priceforge.sharedkernel.ProductId;
import com.priceforge.sharedkernel.UserId;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Input to a price calculation (UC-06). Stored verbatim as {@code PriceQuote.contextSnapshot} so
 * the exact same calculation can be reproduced later (UC-14) — {@code zoneId} is captured
 * explicitly rather than resolved from the JVM's default at calculation time, since that default
 * could differ when the calculation is replayed.
 */
public record PricingContext(
    ProductId productId,
    UserId userId,
    CustomerSegment userSegment,
    int quantity,
    Instant requestedAt,
    String location,
    String currency,
    ZoneId zoneId
) {

    public PricingContext {
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(userSegment, "userSegment must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        zoneId = Objects.requireNonNullElse(zoneId, ZoneId.of("UTC"));
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive, but was " + quantity);
        }
    }
}
