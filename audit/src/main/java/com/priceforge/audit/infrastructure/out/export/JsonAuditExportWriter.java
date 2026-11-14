package com.priceforge.audit.infrastructure.out.export;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.priceforge.audit.application.dto.AuditExportFormat;
import com.priceforge.audit.application.port.out.AuditExportWriterPort;
import com.priceforge.audit.domain.model.AuditEntry;
import java.util.List;
import org.springframework.stereotype.Component;

/** The source event's payload is embedded as a JSON object, not as an escaped string. */
@Component
public class JsonAuditExportWriter implements AuditExportWriterPort {

    private final ObjectMapper objectMapper;

    public JsonAuditExportWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public AuditExportFormat format() {
        return AuditExportFormat.JSON;
    }

    @Override
    public String contentType() {
        return "application/json";
    }

    @Override
    public String fileExtension() {
        return "json";
    }

    @Override
    public String write(List<AuditEntry> entries) {
        ArrayNode array = objectMapper.createArrayNode();
        for (AuditEntry entry : entries) {
            ObjectNode node = array.addObject();
            node.put("eventId", entry.eventId().toString());
            node.put("eventType", entry.eventType());
            node.put("entityType", entry.entityType().name());
            node.put("entityId", entry.entityId());
            node.put("productId", entry.productId());
            node.put("userId", entry.userId());
            node.put("changedBy", entry.changedBy());
            node.put("occurredAt", entry.occurredAt().toString());
            node.put("previousState", entry.previousState());
            node.put("newState", entry.newState());
            node.put("reason", entry.reason());
            node.set("payload", parse(entry.payload()));
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(array);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to write the audit export as JSON", e);
        }
    }

    private JsonNode parse(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored audit payload is not valid JSON", e);
        }
    }
}
