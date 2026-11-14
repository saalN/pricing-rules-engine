package com.priceforge.audit.application.service.support;

import com.priceforge.audit.domain.model.AuditEntityType;
import com.priceforge.audit.domain.model.AuditEntry;
import com.priceforge.audit.domain.model.PriceCalculationSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Test data builders for {@link AuditEntry}. */
public final class AuditEntries {

    private AuditEntries() {
    }

    public static AuditEntry priceCalculation(
            String productId, String userId, Instant occurredAt, BigDecimal basePrice, BigDecimal finalPrice) {
        PriceCalculationSnapshot snapshot = new PriceCalculationSnapshot(
            productId, userId, "VIP", 2, occurredAt, "URBAN", "EUR", "UTC", basePrice, finalPrice);
        return new AuditEntry(
            UUID.randomUUID(), "PriceCalculated", AuditEntityType.PRICE_CALCULATION, productId, productId,
            userId, null, basePrice + " EUR", finalPrice + " EUR", null,
            "{\"finalPriceAmount\":" + finalPrice + "}", occurredAt, snapshot);
    }

    public static AuditEntry change(
            AuditEntityType entityType, String entityId, String eventType, Instant occurredAt,
            String previousState, String newState, String changedBy) {
        return new AuditEntry(
            UUID.randomUUID(), eventType, entityType, entityId, null, null, changedBy,
            previousState, newState, null, "{\"aggregateId\":\"" + entityId + "\"}", occurredAt, null);
    }
}
