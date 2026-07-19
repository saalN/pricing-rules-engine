package com.priceforge.pricing.infrastructure.in.http.dto;

import com.priceforge.pricing.domain.model.CustomerSegment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.UUID;

/** {@code requestedAt} defaults to now, {@code zoneId} defaults to UTC, when absent. */
public record CalculatePriceRequest(
    @NotNull UUID productId,
    @NotNull UUID userId,
    @NotNull CustomerSegment userSegment,
    @Positive int quantity,
    Instant requestedAt,
    @NotBlank String location,
    @NotBlank String currency,
    String zoneId
) {
}
