package com.priceforge.pricing.infrastructure.in.outbox.event;

import com.priceforge.sharedkernel.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Pricing's own local shape for the Catalog product events it reacts to
 * (ProductCreated/ProductPriceBaseUpdated/ProductDeactivated/ProductCategoryChanged) — only
 * {@code aggregateId} (the product id) is needed to invalidate the cache, so this deliberately
 * does not mirror Catalog's actual event classes field-for-field. Pricing must never depend on
 * Catalog's domain package; extra fields present in the real payload are ignored (see
 * {@code spring.jackson.deserialization.fail-on-unknown-properties=false} in app's config).
 */
public record CatalogProductChangedEvent(UUID eventId, Instant occurredAt, String aggregateId) implements DomainEvent {
}
