package com.priceforge.catalog.domain.model;

import com.priceforge.sharedkernel.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record ProductDeactivated(
    UUID eventId,
    Instant occurredAt,
    String aggregateId
) implements DomainEvent {

    public static ProductDeactivated of(Product product) {
        return new ProductDeactivated(UUID.randomUUID(), Instant.now(), product.id().toString());
    }
}
