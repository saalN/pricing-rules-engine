package com.priceforge.promotions.domain.model;

import com.priceforge.sharedkernel.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record PromotionExpired(
    UUID eventId,
    Instant occurredAt,
    String aggregateId
) implements DomainEvent {

    public static PromotionExpired of(Promotion promotion) {
        return new PromotionExpired(UUID.randomUUID(), Instant.now(), promotion.id().toString());
    }
}
