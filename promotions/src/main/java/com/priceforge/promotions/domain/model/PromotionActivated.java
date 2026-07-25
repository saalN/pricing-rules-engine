package com.priceforge.promotions.domain.model;

import com.priceforge.sharedkernel.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record PromotionActivated(
    UUID eventId,
    Instant occurredAt,
    String aggregateId
) implements DomainEvent {

    public static PromotionActivated of(Promotion promotion) {
        return new PromotionActivated(UUID.randomUUID(), Instant.now(), promotion.id().toString());
    }
}
