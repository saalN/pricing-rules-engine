package com.priceforge.pricing.domain.model;

import com.priceforge.sharedkernel.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record PricingRuleUpdated(
    UUID eventId,
    Instant occurredAt,
    String aggregateId,
    String previousState,
    String newState,
    String changedBy
) implements DomainEvent {

    public static PricingRuleUpdated of(String previousState, PricingRule updatedRule, String changedBy) {
        return new PricingRuleUpdated(
            UUID.randomUUID(), Instant.now(), updatedRule.id().toString(),
            previousState, updatedRule.describeState(), changedBy);
    }
}
