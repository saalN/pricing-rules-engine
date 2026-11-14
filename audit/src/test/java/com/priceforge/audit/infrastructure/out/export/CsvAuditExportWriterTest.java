package com.priceforge.audit.infrastructure.out.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.priceforge.audit.domain.model.AuditEntityType;
import com.priceforge.audit.domain.model.AuditEntry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CsvAuditExportWriterTest {

    private static final Instant T1 = Instant.parse("2026-07-20T10:00:00Z");

    private final CsvAuditExportWriter writer = new CsvAuditExportWriter();

    @Test
    void writesAHeaderAndOneRowPerEntryWithCrLfLineEndings() {
        UUID id = UUID.randomUUID();
        AuditEntry entry = new AuditEntry(
            id, "PricingRuleUpdated", AuditEntityType.PRICING_RULE, "rule-1", null, null, "admin",
            "10%", "15%", null, "{}", T1, null);

        String csv = writer.write(List.of(entry));

        assertThat(csv).isEqualTo(
            "eventId,eventType,entityType,entityId,productId,userId,changedBy,occurredAt,"
                + "previousState,newState,reason,payload\r\n"
                + id + ",PricingRuleUpdated,PRICING_RULE,rule-1,,,admin,2026-07-20T10:00:00Z,10%,15%,,{}\r\n");
    }

    @Test
    void quotesCellsContainingCommasQuotesOrNewlines() {
        AuditEntry entry = new AuditEntry(
            UUID.randomUUID(), "PricingRuleUpdated", AuditEntityType.PRICING_RULE, "rule-1", null, null, "admin",
            "a, b", "say \"hi\"", "line1\nline2", "{\"k\":\"v\"}", T1, null);

        String csv = writer.write(List.of(entry));

        assertThat(csv).contains("\"a, b\"", "\"say \"\"hi\"\"\"", "\"line1\nline2\"", "\"{\"\"k\"\":\"\"v\"\"}\"");
    }

    @Test
    void neutralisesSpreadsheetFormulaTriggers() {
        AuditEntry entry = new AuditEntry(
            UUID.randomUUID(), "ProductCreated", AuditEntityType.PRODUCT, "p-1", null, null, "=HYPERLINK(\"x\")",
            "+1", "-2", "@SUM(A1)", "{}", T1, null);

        String csv = writer.write(List.of(entry));

        assertThat(csv).contains("\"'=HYPERLINK(\"\"x\"\")\"", ",'+1,", ",'-2,", ",'@SUM(A1),");
    }

    @Test
    void anEmptyTrailIsJustTheHeader() {
        assertThat(writer.write(List.of())).endsWith("payload\r\n").doesNotContain("\r\n2");
    }
}
