package com.priceforge.pricing.application.dto;

import com.priceforge.pricing.domain.model.CustomerSegment;
import com.priceforge.sharedkernel.ProductId;
import com.priceforge.sharedkernel.UserId;
import java.time.Instant;
import java.time.ZoneId;

public record CalculatePriceCommand(
    ProductId productId,
    UserId userId,
    CustomerSegment userSegment,
    int quantity,
    Instant requestedAt,
    String location,
    String currency,
    ZoneId zoneId
) {
}
