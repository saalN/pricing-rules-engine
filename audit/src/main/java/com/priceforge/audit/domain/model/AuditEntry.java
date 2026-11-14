package com.priceforge.audit.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One immutable fact recorded from another context's domain event. Never modified or deleted once
 * stored (UC-14) — there are deliberately no "with..." copy methods. {@code eventId} is the
 * source event's id, which makes recording idempotent under the outbox's at-least-once delivery.
 *
 * <p>{@code priceSnapshot} is only present for {@code PRICE_CALCULATION} entries; the
 * {@code previousState}/{@code newState}/{@code reason}/{@code changedBy} fields are whatever the
 * source event carried (UC-15) and are null when it carried none.
 */
public record AuditEntry(
    UUID eventId,
    String eventType,
    AuditEntityType entityType,
    String entityId,
    String productId,
    String userId,
    String changedBy,
    String previousState,
    String newState,
    String reason,
    String payload,
    Instant occurredAt,
    PriceCalculationSnapshot priceSnapshot
) {

    public AuditEntry {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(entityType, "entityType must not be null");
        Objects.requireNonNull(entityId, "entityId must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (priceSnapshot != null && entityType != AuditEntityType.PRICE_CALCULATION) {
            throw new IllegalArgumentException("only PRICE_CALCULATION entries can carry a price snapshot");
        }
    }
}
