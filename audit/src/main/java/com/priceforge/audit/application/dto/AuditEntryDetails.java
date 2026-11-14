package com.priceforge.audit.application.dto;

import com.priceforge.audit.domain.model.AuditEntityType;
import com.priceforge.audit.domain.model.AuditEntry;
import java.time.Instant;
import java.util.UUID;

/**
 * Read model of an {@link AuditEntry} for the outside world. {@code payload} is the source
 * event's full JSON, which for a {@code PriceCalculated} entry includes its
 * {@code contextSnapshot} (UC-14).
 */
public record AuditEntryDetails(
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
    Instant occurredAt
) {

    public static AuditEntryDetails from(AuditEntry entry) {
        return new AuditEntryDetails(
            entry.eventId(), entry.eventType(), entry.entityType(), entry.entityId(), entry.productId(),
            entry.userId(), entry.changedBy(), entry.previousState(), entry.newState(), entry.reason(),
            entry.payload(), entry.occurredAt());
    }
}
