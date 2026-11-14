package com.priceforge.audit.infrastructure.in.outbox.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.priceforge.sharedkernel.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Audit's local shape for every domain event it records: the three {@link DomainEvent} fields
 * plus the full payload tree. Audit consumes events from three other contexts and must never
 * depend on their domain packages, and it has to keep each event's complete payload (UC-14), so
 * unlike Pricing's narrow {@code CatalogProductChangedEvent} this deliberately mirrors nothing
 * field-for-field and reads whatever it needs out of {@code payload}.
 */
@JsonDeserialize(using = RawDomainEventDeserializer.class)
public record RawDomainEvent(UUID eventId, Instant occurredAt, String aggregateId, JsonNode payload)
    implements DomainEvent {

    /** The field as text; decimals in plain notation (never {@code 1E+2}), null when absent or null. */
    public String text(String field) {
        JsonNode node = payload.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        return node.isBigDecimal() ? node.decimalValue().toPlainString() : node.asText();
    }

    public JsonNode node(String field) {
        JsonNode node = payload.get(field);
        return node == null || node.isNull() ? null : node;
    }
}
