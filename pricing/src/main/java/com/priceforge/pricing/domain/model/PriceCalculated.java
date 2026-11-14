package com.priceforge.pricing.domain.model;

import com.priceforge.sharedkernel.PricingContext;
import com.priceforge.sharedkernel.DomainEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Deliberately carries only {@code contextSnapshot}, not the applied/excluded rule breakdown —
 * UC-14's reproduction flow re-runs the calculation against current rules using this same
 * context, rather than replaying a stored breakdown.
 */
public record PriceCalculated(
    UUID eventId,
    Instant occurredAt,
    String aggregateId,
    BigDecimal basePriceAmount,
    BigDecimal finalPriceAmount,
    String currency,
    PricingContext contextSnapshot
) implements DomainEvent {

    public static PriceCalculated of(PriceQuote quote) {
        return new PriceCalculated(
            UUID.randomUUID(),
            quote.calculatedAt(),
            quote.contextSnapshot().productId().toString(),
            quote.basePrice().amount(),
            quote.finalPrice().amount(),
            quote.finalPrice().currency().getCurrencyCode(),
            quote.contextSnapshot());
    }
}
