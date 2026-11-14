package com.priceforge.audit.domain.model;

/**
 * What kind of thing an {@link AuditEntry} is about. {@code PRODUCT}, {@code PRICING_RULE} and
 * {@code PROMOTION} are the auditable entities of UC-15; the other two classify the pricing-side
 * events that are keyed by the product they were computed for rather than by an entity's own id.
 */
public enum AuditEntityType {
    PRODUCT,
    PRICING_RULE,
    PROMOTION,
    PRICE_CALCULATION,
    RULE_CONFLICT
}
