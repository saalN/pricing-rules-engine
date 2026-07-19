package com.priceforge.pricing.domain.model;

import com.priceforge.sharedkernel.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record PricingRuleDeactivated(
    UUID eventId,
    Instant occurredAt,
    String aggregateId,
    String changedBy
) implements DomainEvent {

    public static PricingRuleDeactivated of(PricingRule rule, String changedBy) {
        return new PricingRuleDeactivated(UUID.randomUUID(), Instant.now(), rule.id().toString(), changedBy);
    }
}
