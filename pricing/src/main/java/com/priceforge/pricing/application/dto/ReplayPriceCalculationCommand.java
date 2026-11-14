package com.priceforge.pricing.application.dto;

import com.priceforge.sharedkernel.ProductId;
import com.priceforge.sharedkernel.UserId;
import java.time.Instant;
import java.time.ZoneId;

/**
 * The context of a past calculation (UC-14), as stored by Audit. {@code userSegment} is the
 * segment's name rather than the enum so a consuming context needs nothing from Pricing's domain
 * package to build this; {@code userId} is null for anonymous calculations.
 */
public record ReplayPriceCalculationCommand(
    ProductId productId,
    UserId userId,
    String userSegment,
    int quantity,
    Instant requestedAt,
    String location,
    String currency,
    ZoneId zoneId
) {
}
