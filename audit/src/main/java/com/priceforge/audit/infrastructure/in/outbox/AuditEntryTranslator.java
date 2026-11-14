package com.priceforge.audit.infrastructure.in.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.priceforge.audit.domain.model.AuditEntityType;
import com.priceforge.audit.domain.model.AuditEntry;
import com.priceforge.audit.domain.model.PriceCalculationSnapshot;
import com.priceforge.audit.infrastructure.in.outbox.event.RawDomainEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Turns another context's domain event into an {@link AuditEntry}: classifies it, and lifts the
 * UC-14/UC-15 fields (who, what it was before and after, why) out of the payload where the source
 * event carries them. Creation events carry no actor, so {@code changedBy} is null for those.
 */
@Component
public class AuditEntryTranslator {

    /** Every event type Audit records; one outbox handler is registered per entry. */
    public static final Set<String> AUDITED_EVENT_TYPES = Set.of(
        "ProductCreated", "ProductPriceBaseUpdated", "ProductDeactivated", "ProductCategoryChanged",
        "PricingRuleCreated", "PricingRuleUpdated", "PricingRuleDeactivated",
        "PriceCalculated", "RuleConflictDetected",
        "PromotionCreated", "PromotionApplied", "PromotionActivated", "PromotionExpired", "PromotionExhausted");

    public AuditEntry translate(String eventType, RawDomainEvent event) {
        String payload = event.payload().toString();
        String id = event.aggregateId();

        return switch (eventType) {
            case "ProductCreated" -> entry(eventType, AuditEntityType.PRODUCT, event, payload)
                .productId(id)
                .newState("sku=" + event.text("sku") + ", basePrice=" + event.text("basePriceAmount") + " "
                    + event.text("currency") + ", category=" + event.text("category"))
                .build();
            case "ProductPriceBaseUpdated" -> entry(eventType, AuditEntityType.PRODUCT, event, payload)
                .productId(id)
                .changedBy(event.text("changedBy"))
                .previousState(event.text("previousBasePriceAmount") + " " + event.text("currency"))
                .newState(event.text("newBasePriceAmount") + " " + event.text("currency"))
                .build();
            case "ProductDeactivated" -> entry(eventType, AuditEntityType.PRODUCT, event, payload)
                .productId(id)
                .previousState("ACTIVE")
                .newState("INACTIVE")
                .build();
            case "ProductCategoryChanged" -> entry(eventType, AuditEntityType.PRODUCT, event, payload)
                .productId(id)
                .previousState(event.text("previousCategory"))
                .newState(event.text("newCategory"))
                .build();
            case "PricingRuleCreated" -> entry(eventType, AuditEntityType.PRICING_RULE, event, payload)
                .newState("type=" + event.text("ruleType") + ", priority=" + event.text("priority")
                    + ", combinationMode=" + event.text("combinationMode"))
                .build();
            case "PricingRuleUpdated" -> entry(eventType, AuditEntityType.PRICING_RULE, event, payload)
                .changedBy(event.text("changedBy"))
                .previousState(event.text("previousState"))
                .newState(event.text("newState"))
                .build();
            case "PricingRuleDeactivated" -> entry(eventType, AuditEntityType.PRICING_RULE, event, payload)
                .changedBy(event.text("changedBy"))
                .previousState("ACTIVE")
                .newState("INACTIVE")
                .build();
            case "RuleConflictDetected" -> entry(eventType, AuditEntityType.RULE_CONFLICT, event, payload)
                .productId(id)
                .reason(event.text("reason"))
                .newState("involvedRuleIds=" + textValues(event.node("involvedRuleIds")))
                .build();
            case "PriceCalculated" -> priceCalculated(eventType, event, payload);
            case "PromotionCreated" -> entry(eventType, AuditEntityType.PROMOTION, event, payload)
                .newState("name=" + event.text("name") + ", priority=" + event.text("priority")
                    + ", combinationMode=" + event.text("combinationMode"))
                .build();
            case "PromotionApplied" -> entry(eventType, AuditEntityType.PROMOTION, event, payload)
                .userId(event.text("userId"))
                .newState("discountApplied=" + event.text("discountAppliedAmount") + " " + event.text("currency"))
                .build();
            case "PromotionActivated" -> entry(eventType, AuditEntityType.PROMOTION, event, payload)
                .newState("ACTIVE")
                .build();
            case "PromotionExpired" -> entry(eventType, AuditEntityType.PROMOTION, event, payload)
                .newState("EXPIRED")
                .build();
            case "PromotionExhausted" -> entry(eventType, AuditEntityType.PROMOTION, event, payload)
                .newState("EXHAUSTED")
                .build();
            default -> throw new IllegalArgumentException("Audit does not record events of type " + eventType);
        };
    }

    private AuditEntry priceCalculated(String eventType, RawDomainEvent event, String payload) {
        JsonNode context = event.node("contextSnapshot");
        if (context == null) {
            throw new IllegalArgumentException("PriceCalculated event " + event.eventId() + " has no contextSnapshot");
        }

        String productId = typedId(context.get("productId"));
        String userId = typedId(context.get("userId"));

        PriceCalculationSnapshot snapshot = new PriceCalculationSnapshot(
            productId,
            userId,
            context.get("userSegment").asText(),
            context.get("quantity").asInt(),
            java.time.Instant.parse(context.get("requestedAt").asText()),
            context.hasNonNull("location") ? context.get("location").asText() : null,
            context.get("currency").asText(),
            context.get("zoneId").asText(),
            new BigDecimal(event.text("basePriceAmount")),
            new BigDecimal(event.text("finalPriceAmount")));

        return entry(eventType, AuditEntityType.PRICE_CALCULATION, event, payload)
            .productId(productId)
            .userId(userId)
            .previousState(snapshot.basePriceAmount() + " " + snapshot.currency())
            .newState(snapshot.finalPriceAmount() + " " + snapshot.currency())
            .snapshot(snapshot)
            .build();
    }

    /** A typed id ({@code ProductId}, {@code UserId}) serializes as {@code {"value": "<uuid>"}}. */
    private static String typedId(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.isObject() ? node.get("value").asText() : node.asText();
    }

    private static List<String> textValues(JsonNode array) {
        List<String> values = new ArrayList<>();
        if (array != null) {
            array.forEach(item -> values.add(item.asText()));
        }
        return values;
    }

    private static Builder entry(String eventType, AuditEntityType entityType, RawDomainEvent event, String payload) {
        return new Builder(eventType, entityType, event, payload);
    }

    private static final class Builder {

        private final String eventType;
        private final AuditEntityType entityType;
        private final RawDomainEvent event;
        private final String payload;
        private String productId;
        private String userId;
        private String changedBy;
        private String previousState;
        private String newState;
        private String reason;
        private PriceCalculationSnapshot snapshot;

        private Builder(String eventType, AuditEntityType entityType, RawDomainEvent event, String payload) {
            this.eventType = eventType;
            this.entityType = entityType;
            this.event = event;
            this.payload = payload;
        }

        Builder productId(String value) {
            this.productId = value;
            return this;
        }

        Builder userId(String value) {
            this.userId = value;
            return this;
        }

        Builder changedBy(String value) {
            this.changedBy = value;
            return this;
        }

        Builder previousState(String value) {
            this.previousState = value;
            return this;
        }

        Builder newState(String value) {
            this.newState = value;
            return this;
        }

        Builder reason(String value) {
            this.reason = value;
            return this;
        }

        Builder snapshot(PriceCalculationSnapshot value) {
            this.snapshot = value;
            return this;
        }

        AuditEntry build() {
            return new AuditEntry(
                event.eventId(), eventType, entityType, event.aggregateId(), productId, userId, changedBy,
                previousState, newState, reason, payload, event.occurredAt(), snapshot);
        }
    }
}
