package com.priceforge.audit.infrastructure.in.http.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.priceforge.audit.domain.model.AuditEntityType;
import java.time.Instant;
import java.util.UUID;

/** {@code payload} is the source event's full JSON; for a PriceCalculated entry it holds the contextSnapshot. */
public record AuditEntryResponse(
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
    Instant occurredAt,
    JsonNode payload
) {
}
