package com.priceforge.promotions.domain.model;

import com.priceforge.sharedkernel.DomainEvent;
import com.priceforge.sharedkernel.Money;
import com.priceforge.sharedkernel.PromotionId;
import com.priceforge.sharedkernel.UserId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PromotionApplied(
    UUID eventId,
    Instant occurredAt,
    String aggregateId,
    String userId,
    BigDecimal discountAppliedAmount,
    String currency
) implements DomainEvent {

    public static PromotionApplied of(PromotionId promotionId, UserId userId, Money discountApplied) {
        return new PromotionApplied(
            UUID.randomUUID(), Instant.now(), promotionId.toString(),
            userId.toString(), discountApplied.amount(), discountApplied.currency().getCurrencyCode());
    }
}
