package com.priceforge.pricing.domain.model;

import com.priceforge.sharedkernel.DomainEvent;
import com.priceforge.sharedkernel.ProductId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RuleConflictDetected(
    UUID eventId,
    Instant occurredAt,
    String aggregateId,
    ConflictReason reason,
    List<String> involvedRuleIds
) implements DomainEvent {

    public static RuleConflictDetected of(ProductId productId, ConflictReason reason, List<String> involvedRuleIds) {
        return new RuleConflictDetected(
            UUID.randomUUID(), Instant.now(), productId.toString(), reason, List.copyOf(involvedRuleIds));
    }
}
