package com.priceforge.promotions.domain.model;

import com.priceforge.sharedkernel.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record PromotionExhausted(
    UUID eventId,
    Instant occurredAt,
    String aggregateId
) implements DomainEvent {

    public static PromotionExhausted of(Promotion promotion) {
        return new PromotionExhausted(UUID.randomUUID(), Instant.now(), promotion.id().toString());
    }
}
