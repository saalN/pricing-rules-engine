package com.priceforge.audit.application.dto;

import com.priceforge.audit.domain.model.AuditEntityType;

/** UC-15: the full, chronological history of one {@code PRICING_RULE}, {@code PROMOTION} or {@code PRODUCT}. */
public record EntityChangeHistoryQuery(AuditEntityType entityType, String entityId) {
}
