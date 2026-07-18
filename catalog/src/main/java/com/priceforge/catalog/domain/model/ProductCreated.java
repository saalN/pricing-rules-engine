package com.priceforge.catalog.domain.model;

import com.priceforge.sharedkernel.DomainEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductCreated(
    UUID eventId,
    Instant occurredAt,
    String aggregateId,
    String sku,
    BigDecimal basePriceAmount,
    String currency,
    String category
) implements DomainEvent {

    public static ProductCreated of(Product product) {
        return new ProductCreated(
            UUID.randomUUID(),
            Instant.now(),
            product.id().toString(),
            product.sku(),
            product.basePrice().amount(),
            product.basePrice().currency().getCurrencyCode(),
            product.category());
    }
}
