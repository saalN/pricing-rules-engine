package com.priceforge.audit.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.priceforge.audit.application.dto.AuditEntryDetails;
import com.priceforge.audit.application.dto.EntityChangeHistoryQuery;
import com.priceforge.audit.application.service.support.AuditEntries;
import com.priceforge.audit.application.service.support.InMemoryAuditEntryRepositoryPort;
import com.priceforge.audit.domain.model.AuditEntityType;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class QueryEntityChangeHistoryServiceTest {

    private static final Instant T1 = Instant.parse("2026-07-20T10:00:00Z");
    private static final Instant T2 = Instant.parse("2026-07-21T10:00:00Z");
    private static final Instant T3 = Instant.parse("2026-07-22T10:00:00Z");

    private final InMemoryAuditEntryRepositoryPort repository = new InMemoryAuditEntryRepositoryPort();
    private final QueryEntityChangeHistoryService service = new QueryEntityChangeHistoryService(repository);

    @Test
    void returnsTheWholeLifecycleOfOneRuleOldestFirst() {
        repository.saveIfAbsent(AuditEntries.change(
            AuditEntityType.PRICING_RULE, "rule-1", "PricingRuleDeactivated", T3, "ACTIVE", "INACTIVE", "admin"));
        repository.saveIfAbsent(AuditEntries.change(
            AuditEntityType.PRICING_RULE, "rule-1", "PricingRuleCreated", T1, null, "type=SEGMENT_BASED", null));
        repository.saveIfAbsent(AuditEntries.change(
            AuditEntityType.PRICING_RULE, "rule-1", "PricingRuleUpdated", T2, "10%", "15%", "revenue"));
        repository.saveIfAbsent(AuditEntries.change(
            AuditEntityType.PRICING_RULE, "rule-2", "PricingRuleCreated", T1, null, "other rule", null));

        List<AuditEntryDetails> history =
            service.execute(new EntityChangeHistoryQuery(AuditEntityType.PRICING_RULE, "rule-1"));

        assertThat(history).extracting(AuditEntryDetails::eventType)
            .containsExactly("PricingRuleCreated", "PricingRuleUpdated", "PricingRuleDeactivated");
        assertThat(history.get(1).changedBy()).isEqualTo("revenue");
        assertThat(history.get(1).previousState()).isEqualTo("10%");
        assertThat(history.get(1).newState()).isEqualTo("15%");
    }

    @Test
    void doesNotMixEntityTypesThatShareAnId() {
        repository.saveIfAbsent(AuditEntries.change(
            AuditEntityType.PROMOTION, "same-id", "PromotionActivated", T1, null, "ACTIVE", null));
        repository.saveIfAbsent(AuditEntries.change(
            AuditEntityType.PRODUCT, "same-id", "ProductDeactivated", T2, "ACTIVE", "INACTIVE", null));

        assertThat(service.execute(new EntityChangeHistoryQuery(AuditEntityType.PROMOTION, "same-id"))).hasSize(1);
    }

    @Test
    void returnsAnEmptyHistoryForAnUnknownEntity() {
        assertThat(service.execute(new EntityChangeHistoryQuery(AuditEntityType.PROMOTION, "nope"))).isEmpty();
    }

    @Test
    void rejectsEntityTypesThatAreNotAuditableEntities() {
        assertThatThrownBy(() -> service.execute(new EntityChangeHistoryQuery(AuditEntityType.PRICE_CALCULATION, "x")))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.execute(new EntityChangeHistoryQuery(AuditEntityType.RULE_CONFLICT, "x")))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
