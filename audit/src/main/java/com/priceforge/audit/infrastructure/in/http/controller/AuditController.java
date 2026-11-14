package com.priceforge.audit.infrastructure.in.http.controller;

import com.priceforge.audit.application.dto.AuditExportFormat;
import com.priceforge.audit.application.dto.ExportedAuditTrail;
import com.priceforge.audit.application.port.in.ExportAuditTrailUseCase;
import com.priceforge.audit.application.port.in.QueryEntityChangeHistoryUseCase;
import com.priceforge.audit.application.port.in.QueryPriceCalculationHistoryUseCase;
import com.priceforge.audit.application.port.in.ReplayStoredPriceCalculationUseCase;
import com.priceforge.audit.domain.model.AuditEntityType;
import com.priceforge.audit.infrastructure.in.http.dto.AuditEntryPageResponse;
import com.priceforge.audit.infrastructure.in.http.dto.AuditEntryResponse;
import com.priceforge.audit.infrastructure.in.http.dto.ReplayPriceCalculationResponse;
import com.priceforge.audit.infrastructure.in.http.mapper.AuditHttpMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only by design: every endpoint is a GET, so no write path ever exists in Audit. */
@RestController
@RequestMapping("/audit")
@Tag(name = "Audit", description = "Price calculation and rule change history (UC-14, UC-15)")
public class AuditController {

    private final QueryPriceCalculationHistoryUseCase queryPriceCalculationHistoryUseCase;
    private final ReplayStoredPriceCalculationUseCase replayStoredPriceCalculationUseCase;
    private final QueryEntityChangeHistoryUseCase queryEntityChangeHistoryUseCase;
    private final ExportAuditTrailUseCase exportAuditTrailUseCase;
    private final AuditHttpMapper mapper;

    public AuditController(
            QueryPriceCalculationHistoryUseCase queryPriceCalculationHistoryUseCase,
            ReplayStoredPriceCalculationUseCase replayStoredPriceCalculationUseCase,
            QueryEntityChangeHistoryUseCase queryEntityChangeHistoryUseCase,
            ExportAuditTrailUseCase exportAuditTrailUseCase,
            AuditHttpMapper mapper) {
        this.queryPriceCalculationHistoryUseCase = queryPriceCalculationHistoryUseCase;
        this.replayStoredPriceCalculationUseCase = replayStoredPriceCalculationUseCase;
        this.queryEntityChangeHistoryUseCase = queryEntityChangeHistoryUseCase;
        this.exportAuditTrailUseCase = exportAuditTrailUseCase;
        this.mapper = mapper;
    }

    @Operation(summary = "UC-14: Query price calculation history",
        description = "Stored PriceCalculated events, newest first, each with its full contextSnapshot in `payload`.")
    @GetMapping("/price-calculations")
    public AuditEntryPageResponse priceCalculations(
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return mapper.toResponse(queryPriceCalculationHistoryUseCase.execute(
            mapper.toQuery(productId, userId, from, to, page, size)));
    }

    @Operation(summary = "UC-14: Replay a stored price calculation",
        description = "Re-runs the stored contextSnapshot against the pricing rules active now and compares the "
            + "final price. Promotions are not re-applied, so a calculation that originally used one may differ.")
    @GetMapping("/price-calculations/{eventId}/replay")
    public ReplayPriceCalculationResponse replay(@PathVariable UUID eventId) {
        return mapper.toResponse(replayStoredPriceCalculationUseCase.execute(mapper.toReplayCommand(eventId)));
    }

    @Operation(summary = "UC-15: Query the change history of a rule, promotion or product",
        description = "Chronological entries with changedBy, occurredAt, previousState, newState and reason "
            + "where the source event carried them.")
    @GetMapping("/history/{entityType}/{entityId}")
    public List<AuditEntryResponse> history(@PathVariable AuditEntityType entityType, @PathVariable String entityId) {
        return mapper.toResponse(queryEntityChangeHistoryUseCase.execute(mapper.toQuery(entityType, entityId)));
    }

    @Operation(summary = "UC-14/UC-15: Export the audit trail as JSON or CSV",
        description = "Oldest first. Fails with 422 instead of truncating when more than 10000 entries match.")
    @GetMapping("/export")
    public ResponseEntity<String> export(
            @RequestParam AuditExportFormat format,
            @RequestParam(required = false) AuditEntityType entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        ExportedAuditTrail export = exportAuditTrailUseCase.execute(
            mapper.toExportQuery(format, entityType, entityId, productId, userId, from, to));

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(export.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(export.fileName()).build().toString())
            .body(export.content());
    }
}
