package com.priceforge.pricing.domain.model;

import com.priceforge.sharedkernel.CombinationMode;
import com.priceforge.sharedkernel.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record PricingRuleCreated(
    UUID eventId,
    Instant occurredAt,
    String aggregateId,
    PricingRuleType ruleType,
    int priority,
    CombinationMode combinationMode
) implements DomainEvent {

    public static PricingRuleCreated of(PricingRule rule) {
        return new PricingRuleCreated(
            UUID.randomUUID(), Instant.now(), rule.id().toString(),
            rule.type(), rule.priority(), rule.combinationMode());
    }
}
