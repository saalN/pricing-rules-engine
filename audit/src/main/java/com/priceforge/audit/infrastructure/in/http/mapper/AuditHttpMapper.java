package com.priceforge.audit.infrastructure.in.http.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.priceforge.audit.application.dto.AuditEntryDetails;
import com.priceforge.audit.application.dto.AuditEntryFilter;
import com.priceforge.audit.application.dto.AuditEntryPageResult;
import com.priceforge.audit.application.dto.AuditExportFormat;
import com.priceforge.audit.application.dto.EntityChangeHistoryQuery;
import com.priceforge.audit.application.dto.ExportAuditTrailQuery;
import com.priceforge.audit.application.dto.PriceCalculationHistoryQuery;
import com.priceforge.audit.application.dto.ReplayStoredPriceCalculationCommand;
import com.priceforge.audit.application.dto.ReplayStoredPriceCalculationResult;
import com.priceforge.audit.domain.model.AuditEntityType;
import com.priceforge.audit.infrastructure.in.http.dto.AuditEntryPageResponse;
import com.priceforge.audit.infrastructure.in.http.dto.AuditEntryResponse;
import com.priceforge.audit.infrastructure.in.http.dto.ReplayPriceCalculationResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AuditHttpMapper {

    private final ObjectMapper objectMapper;

    public AuditHttpMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PriceCalculationHistoryQuery toQuery(
            String productId, String userId, Instant from, Instant to, int page, int size) {
        return new PriceCalculationHistoryQuery(productId, userId, from, to, page, size);
    }

    public EntityChangeHistoryQuery toQuery(AuditEntityType entityType, String entityId) {
        return new EntityChangeHistoryQuery(entityType, entityId);
    }

    public ReplayStoredPriceCalculationCommand toReplayCommand(UUID eventId) {
        return new ReplayStoredPriceCalculationCommand(eventId);
    }

    public ExportAuditTrailQuery toExportQuery(
            AuditExportFormat format, AuditEntityType entityType, String entityId, String productId,
            String userId, Instant from, Instant to) {
        return new ExportAuditTrailQuery(
            new AuditEntryFilter(entityType, entityId, productId, userId, from, to), format);
    }

    public AuditEntryPageResponse toResponse(AuditEntryPageResult result) {
        return new AuditEntryPageResponse(
            toResponse(result.items()), result.totalElements(), result.page(), result.size());
    }

    public List<AuditEntryResponse> toResponse(List<AuditEntryDetails> entries) {
        return entries.stream().map(this::toResponse).toList();
    }

    public ReplayPriceCalculationResponse toResponse(ReplayStoredPriceCalculationResult result) {
        return new ReplayPriceCalculationResponse(
            result.eventId(), result.originalCalculatedAt(), result.currency(),
            result.originalFinalPriceAmount(), result.replayedFinalPriceAmount(), result.matches());
    }

    private AuditEntryResponse toResponse(AuditEntryDetails details) {
        try {
            return new AuditEntryResponse(
                details.eventId(), details.eventType(), details.entityType(), details.entityId(),
                details.productId(), details.userId(), details.changedBy(), details.previousState(),
                details.newState(), details.reason(), details.occurredAt(),
                objectMapper.readTree(details.payload()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored audit payload is not valid JSON", e);
        }
    }
}
