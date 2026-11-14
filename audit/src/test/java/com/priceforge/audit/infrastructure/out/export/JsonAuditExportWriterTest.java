package com.priceforge.audit.infrastructure.out.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.priceforge.audit.domain.model.AuditEntityType;
import com.priceforge.audit.domain.model.AuditEntry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JsonAuditExportWriterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonAuditExportWriter writer = new JsonAuditExportWriter(objectMapper);

    @Test
    void writesAJsonArrayEmbeddingThePayloadAsAnObject() throws Exception {
        UUID id = UUID.randomUUID();
        AuditEntry entry = new AuditEntry(
            id, "PricingRuleUpdated", AuditEntityType.PRICING_RULE, "rule-1", null, null, "admin",
            "10%", "15%", null, "{\"newState\":\"15%\",\"n\":1.50}", Instant.parse("2026-07-20T10:00:00Z"), null);

        JsonNode exported = objectMapper.readTree(writer.write(List.of(entry)));

        assertThat(exported.isArray()).isTrue();
        assertThat(exported).hasSize(1);
        JsonNode first = exported.get(0);
        assertThat(first.get("eventId").asText()).isEqualTo(id.toString());
        assertThat(first.get("entityType").asText()).isEqualTo("PRICING_RULE");
        assertThat(first.get("changedBy").asText()).isEqualTo("admin");
        assertThat(first.get("previousState").asText()).isEqualTo("10%");
        assertThat(first.get("occurredAt").asText()).isEqualTo("2026-07-20T10:00:00Z");
        assertThat(first.get("reason").isNull()).isTrue();
        assertThat(first.get("payload").get("newState").asText()).isEqualTo("15%");
    }

    @Test
    void anEmptyTrailIsAnEmptyArray() throws Exception {
        assertThat(objectMapper.readTree(writer.write(List.of()))).isEmpty();
    }
}
