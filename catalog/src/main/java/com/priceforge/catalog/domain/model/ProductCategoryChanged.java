package com.priceforge.catalog.domain.model;

import com.priceforge.sharedkernel.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record ProductCategoryChanged(
    UUID eventId,
    Instant occurredAt,
    String aggregateId,
    String previousCategory,
    String newCategory
) implements DomainEvent {

    public static ProductCategoryChanged of(Product product, String previousCategory) {
        return new ProductCategoryChanged(
            UUID.randomUUID(), Instant.now(), product.id().toString(), previousCategory, product.category());
    }
}
