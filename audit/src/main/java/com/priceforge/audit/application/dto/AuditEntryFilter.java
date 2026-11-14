package com.priceforge.audit.application.dto;

import com.priceforge.audit.domain.model.AuditEntityType;
import java.time.Instant;

/** Every field is optional; a null field does not restrict the search. */
public record AuditEntryFilter(
    AuditEntityType entityType,
    String entityId,
    String productId,
    String userId,
    Instant from,
    Instant to
) {
}
