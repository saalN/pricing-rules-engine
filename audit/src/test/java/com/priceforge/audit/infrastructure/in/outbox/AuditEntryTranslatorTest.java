package com.priceforge.audit.infrastructure.in.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.priceforge.audit.domain.model.AuditEntityType;
import com.priceforge.audit.domain.model.AuditEntry;
import com.priceforge.audit.infrastructure.in.outbox.event.RawDomainEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Payloads here are written the way each emitting context's event actually serializes (typed ids
 * as {@code {"value": ...}}, enums by name) — Audit may not depend on those contexts' classes.
 */
class AuditEntryTranslatorTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String PRODUCT_ID = "22222222-2222-2222-2222-222222222222";
    private static final String USER_ID = "33333333-3333-3333-3333-333333333333";

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final AuditEntryTranslator translator = new AuditEntryTranslator();

    @Test
    void translatesAPriceCalculationKeepingItsContextSnapshotReplayable() throws Exception {
        AuditEntry entry = translate("PriceCalculated", PRODUCT_ID, """
            "basePriceAmount": 100.0000, "finalPriceAmount": 85.0000, "currency": "EUR",
            "contextSnapshot": {
              "productId": {"value": "%s"}, "userId": {"value": "%s"}, "userSegment": "VIP",
              "quantity": 3, "requestedAt": "2026-07-20T09:30:00Z", "location": "URBAN",
              "currency": "EUR", "zoneId": "Europe/Madrid"}
            """.formatted(PRODUCT_ID, USER_ID));

        assertThat(entry.entityType()).isEqualTo(AuditEntityType.PRICE_CALCULATION);
        assertThat(entry.entityId()).isEqualTo(PRODUCT_ID);
        assertThat(entry.productId()).isEqualTo(PRODUCT_ID);
        assertThat(entry.userId()).isEqualTo(USER_ID);
        assertThat(entry.priceSnapshot().userSegment()).isEqualTo("VIP");
        assertThat(entry.priceSnapshot().quantity()).isEqualTo(3);
        assertThat(entry.priceSnapshot().requestedAt()).isEqualTo(Instant.parse("2026-07-20T09:30:00Z"));
        assertThat(entry.priceSnapshot().zoneId()).isEqualTo("Europe/Madrid");
        assertThat(entry.priceSnapshot().basePriceAmount()).isEqualByComparingTo("100");
        assertThat(entry.priceSnapshot().finalPriceAmount()).isEqualByComparingTo("85");
        assertThat(entry.payload()).contains("contextSnapshot");
    }

    @Test
    void translatesAnAnonymousPriceCalculationWithoutAUser() throws Exception {
        AuditEntry entry = translate("PriceCalculated", PRODUCT_ID, """
            "basePriceAmount": 10, "finalPriceAmount": 10, "currency": "EUR",
            "contextSnapshot": {
              "productId": {"value": "%s"}, "userId": null, "userSegment": "STANDARD",
              "quantity": 1, "requestedAt": "2026-07-20T09:30:00Z", "location": null,
              "currency": "EUR", "zoneId": "UTC"}
            """.formatted(PRODUCT_ID));

        assertThat(entry.userId()).isNull();
        assertThat(entry.priceSnapshot().userId()).isNull();
        assertThat(entry.priceSnapshot().location()).isNull();
    }

    @Test
    void translatesAPricingRuleUpdateWithWhoWhatAndWhen() throws Exception {
        AuditEntry entry = translate("PricingRuleUpdated", "rule-1", """
            "previousState": "discount=10%", "newState": "discount=15%", "changedBy": "revenue@priceforge.com"
            """);

        assertThat(entry.entityType()).isEqualTo(AuditEntityType.PRICING_RULE);
        assertThat(entry.entityId()).isEqualTo("rule-1");
        assertThat(entry.changedBy()).isEqualTo("revenue@priceforge.com");
        assertThat(entry.previousState()).isEqualTo("discount=10%");
        assertThat(entry.newState()).isEqualTo("discount=15%");
        assertThat(entry.occurredAt()).isEqualTo(Instant.parse("2026-07-20T10:00:00Z"));
        assertThat(entry.eventId()).isEqualTo(EVENT_ID);
    }

    @Test
    void translatesAPricingRuleDeactivation() throws Exception {
        AuditEntry entry = translate("PricingRuleDeactivated", "rule-1", "\"changedBy\": \"admin@priceforge.com\"");

        assertThat(entry.entityType()).isEqualTo(AuditEntityType.PRICING_RULE);
        assertThat(entry.changedBy()).isEqualTo("admin@priceforge.com");
        assertThat(entry.previousState()).isEqualTo("ACTIVE");
        assertThat(entry.newState()).isEqualTo("INACTIVE");
    }

    @Test
    void translatesAPricingRuleCreationWithNoActorBecauseTheEventCarriesNone() throws Exception {
        AuditEntry entry = translate("PricingRuleCreated", "rule-1", """
            "ruleType": "SEGMENT_BASED", "priority": 10, "combinationMode": "STACKABLE"
            """);

        assertThat(entry.changedBy()).isNull();
        assertThat(entry.newState()).isEqualTo("type=SEGMENT_BASED, priority=10, combinationMode=STACKABLE");
    }

    @Test
    void translatesARuleConflictAsAProductScopedEntryWithItsReason() throws Exception {
        AuditEntry entry = translate("RuleConflictDetected", PRODUCT_ID, """
            "reason": "TIEBREAK", "involvedRuleIds": ["r-1", "r-2"]
            """);

        assertThat(entry.entityType()).isEqualTo(AuditEntityType.RULE_CONFLICT);
        assertThat(entry.productId()).isEqualTo(PRODUCT_ID);
        assertThat(entry.reason()).isEqualTo("TIEBREAK");
        assertThat(entry.newState()).isEqualTo("involvedRuleIds=[r-1, r-2]");
    }

    @Test
    void translatesTheProductEvents() throws Exception {
        AuditEntry created = translate("ProductCreated", PRODUCT_ID, """
            "sku": "SKU-1", "basePriceAmount": 10.5, "currency": "EUR", "category": "URBAN"
            """);
        AuditEntry priceUpdated = translate("ProductPriceBaseUpdated", PRODUCT_ID, """
            "previousBasePriceAmount": 10, "newBasePriceAmount": 12, "currency": "EUR", "changedBy": "admin"
            """);
        AuditEntry deactivated = translate("ProductDeactivated", PRODUCT_ID, "\"x\": 1");
        AuditEntry categoryChanged = translate("ProductCategoryChanged", PRODUCT_ID, """
            "previousCategory": "URBAN", "newCategory": "RURAL"
            """);

        assertThat(created.entityType()).isEqualTo(AuditEntityType.PRODUCT);
        assertThat(created.newState()).isEqualTo("sku=SKU-1, basePrice=10.5 EUR, category=URBAN");
        assertThat(priceUpdated.changedBy()).isEqualTo("admin");
        assertThat(priceUpdated.previousState()).isEqualTo("10 EUR");
        assertThat(priceUpdated.newState()).isEqualTo("12 EUR");
        assertThat(deactivated.newState()).isEqualTo("INACTIVE");
        assertThat(categoryChanged.previousState()).isEqualTo("URBAN");
        assertThat(categoryChanged.newState()).isEqualTo("RURAL");
        assertThat(created.productId()).isEqualTo(PRODUCT_ID);
    }

    @Test
    void translatesThePromotionEvents() throws Exception {
        AuditEntry created = translate("PromotionCreated", "promo-1", """
            "name": "Summer", "priority": 5, "combinationMode": "EXCLUSIVE"
            """);
        AuditEntry applied = translate("PromotionApplied", "promo-1", """
            "userId": "%s", "discountAppliedAmount": 5.25, "currency": "EUR"
            """.formatted(USER_ID));
        AuditEntry activated = translate("PromotionActivated", "promo-1", "\"x\": 1");
        AuditEntry expired = translate("PromotionExpired", "promo-1", "\"x\": 1");
        AuditEntry exhausted = translate("PromotionExhausted", "promo-1", "\"x\": 1");

        assertThat(created.entityType()).isEqualTo(AuditEntityType.PROMOTION);
        assertThat(created.newState()).isEqualTo("name=Summer, priority=5, combinationMode=EXCLUSIVE");
        assertThat(applied.userId()).isEqualTo(USER_ID);
        assertThat(applied.newState()).isEqualTo("discountApplied=5.25 EUR");
        assertThat(activated.newState()).isEqualTo("ACTIVE");
        assertThat(expired.newState()).isEqualTo("EXPIRED");
        assertThat(exhausted.newState()).isEqualTo("EXHAUSTED");
    }

    @Test
    void everyAuditedEventTypeIsTranslatable() throws Exception {
        Map<String, String> minimalBodies = Map.ofEntries(
            Map.entry("PriceCalculated", """
                "basePriceAmount": 1, "finalPriceAmount": 1, "currency": "EUR",
                "contextSnapshot": {"productId": {"value": "p"}, "userId": null, "userSegment": "VIP",
                  "quantity": 1, "requestedAt": "2026-07-20T09:30:00Z", "currency": "EUR", "zoneId": "UTC"}
                """));

        for (String eventType : AuditEntryTranslator.AUDITED_EVENT_TYPES) {
            AuditEntry entry = translate(eventType, "agg-1", minimalBodies.getOrDefault(eventType, "\"x\": 1"));
            assertThat(entry.eventType()).isEqualTo(eventType);
        }
    }

    @Test
    void rejectsAnEventTypeAuditDoesNotRecord() throws Exception {
        assertThatThrownBy(() -> translate("SomethingElse", "agg-1", "\"x\": 1"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAPriceCalculationWithoutAContextSnapshot() {
        assertThatThrownBy(() -> translate("PriceCalculated", PRODUCT_ID, "\"finalPriceAmount\": 1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("contextSnapshot");
    }

    private AuditEntry translate(String eventType, String aggregateId, String bodyFields) throws Exception {
        String json = """
            {"eventId": "%s", "occurredAt": "2026-07-20T10:00:00Z", "aggregateId": "%s", %s}
            """.formatted(EVENT_ID, aggregateId, bodyFields);
        RawDomainEvent event = objectMapper.readValue(json, RawDomainEvent.class);
        return translator.translate(eventType, event);
    }
}
