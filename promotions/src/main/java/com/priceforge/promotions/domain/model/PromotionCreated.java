package com.priceforge.promotions.domain.model;

import com.priceforge.sharedkernel.CombinationMode;
import com.priceforge.sharedkernel.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record PromotionCreated(
    UUID eventId,
    Instant occurredAt,
    String aggregateId,
    String name,
    int priority,
    CombinationMode combinationMode
) implements DomainEvent {

    public static PromotionCreated of(Promotion promotion) {
        return new PromotionCreated(
            UUID.randomUUID(), Instant.now(), promotion.id().toString(),
            promotion.name(), promotion.priority(), promotion.combinationMode());
    }
}
