package com.priceforge.catalog.domain.model;

import com.priceforge.sharedkernel.DomainEvent;
import com.priceforge.sharedkernel.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductPriceBaseUpdated(
    UUID eventId,
    Instant occurredAt,
    String aggregateId,
    BigDecimal previousBasePriceAmount,
    BigDecimal newBasePriceAmount,
    String currency,
    String changedBy
) implements DomainEvent {

    public static ProductPriceBaseUpdated of(Product product, Money previousBasePrice, String changedBy) {
        return new ProductPriceBaseUpdated(
            UUID.randomUUID(),
            Instant.now(),
            product.id().toString(),
            previousBasePrice.amount(),
            product.basePrice().amount(),
            product.basePrice().currency().getCurrencyCode(),
            changedBy);
    }
}
