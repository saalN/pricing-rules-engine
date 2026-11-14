package com.priceforge.audit.infrastructure.in.http.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.priceforge.audit.application.dto.AuditEntryDetails;
import com.priceforge.audit.application.dto.AuditEntryPageResult;
import com.priceforge.audit.application.dto.AuditExportFormat;
import com.priceforge.audit.application.dto.EntityChangeHistoryQuery;
import com.priceforge.audit.application.dto.ExportAuditTrailQuery;
import com.priceforge.audit.application.dto.ExportedAuditTrail;
import com.priceforge.audit.application.dto.PriceCalculationHistoryQuery;
import com.priceforge.audit.application.dto.ReplayStoredPriceCalculationResult;
import com.priceforge.audit.application.port.in.ExportAuditTrailUseCase;
import com.priceforge.audit.application.port.in.QueryEntityChangeHistoryUseCase;
import com.priceforge.audit.application.port.in.QueryPriceCalculationHistoryUseCase;
import com.priceforge.audit.application.port.in.ReplayStoredPriceCalculationUseCase;
import com.priceforge.audit.domain.exception.ExportTooLargeException;
import com.priceforge.audit.domain.exception.PriceCalculationNotFoundException;
import com.priceforge.audit.domain.model.AuditEntityType;
import com.priceforge.audit.infrastructure.in.http.exception.AuditExceptionHandler;
import com.priceforge.audit.infrastructure.in.http.mapper.AuditHttpMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuditController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({AuditHttpMapper.class, AuditExceptionHandler.class})
class AuditControllerTest {

    private static final Instant T1 = Instant.parse("2026-07-20T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QueryPriceCalculationHistoryUseCase queryPriceCalculationHistoryUseCase;

    @MockitoBean
    private ReplayStoredPriceCalculationUseCase replayStoredPriceCalculationUseCase;

    @MockitoBean
    private QueryEntityChangeHistoryUseCase queryEntityChangeHistoryUseCase;

    @MockitoBean
    private ExportAuditTrailUseCase exportAuditTrailUseCase;

    @Test
    void priceCalculationsPassesTheFiltersAndReturnsAPageWithTheParsedPayload() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(queryPriceCalculationHistoryUseCase.execute(any())).thenReturn(new AuditEntryPageResult(
            List.of(new AuditEntryDetails(
                eventId, "PriceCalculated", AuditEntityType.PRICE_CALCULATION, "p-1", "p-1", "u-1", null,
                "100 EUR", "85 EUR", null, "{\"contextSnapshot\":{\"quantity\":2}}", T1)),
            7, 1, 5));

        mockMvc.perform(get("/audit/price-calculations")
                .param("productId", "p-1")
                .param("userId", "u-1")
                .param("from", "2026-07-01T00:00:00Z")
                .param("to", "2026-07-31T00:00:00Z")
                .param("page", "1")
                .param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(7))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.items[0].eventId").value(eventId.toString()))
            .andExpect(jsonPath("$.items[0].payload.contextSnapshot.quantity").value(2));

        ArgumentCaptor<PriceCalculationHistoryQuery> query = ArgumentCaptor.forClass(PriceCalculationHistoryQuery.class);
        verify(queryPriceCalculationHistoryUseCase).execute(query.capture());
        assertThat(query.getValue()).isEqualTo(new PriceCalculationHistoryQuery(
            "p-1", "u-1", Instant.parse("2026-07-01T00:00:00Z"), Instant.parse("2026-07-31T00:00:00Z"), 1, 5));
    }

    @Test
    void priceCalculationsDefaultsToTheFirstPageOfFifty() throws Exception {
        when(queryPriceCalculationHistoryUseCase.execute(any())).thenReturn(new AuditEntryPageResult(List.of(), 0, 0, 50));

        mockMvc.perform(get("/audit/price-calculations")).andExpect(status().isOk());

        ArgumentCaptor<PriceCalculationHistoryQuery> query = ArgumentCaptor.forClass(PriceCalculationHistoryQuery.class);
        verify(queryPriceCalculationHistoryUseCase).execute(query.capture());
        assertThat(query.getValue()).isEqualTo(new PriceCalculationHistoryQuery(null, null, null, null, 0, 50));
    }

    @Test
    void replayReturnsTheComparison() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(replayStoredPriceCalculationUseCase.execute(any())).thenReturn(new ReplayStoredPriceCalculationResult(
            eventId, T1, "EUR", BigDecimal.valueOf(85), BigDecimal.valueOf(100), false));

        mockMvc.perform(get("/audit/price-calculations/{eventId}/replay", eventId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.eventId").value(eventId.toString()))
            .andExpect(jsonPath("$.originalFinalPriceAmount").value(85))
            .andExpect(jsonPath("$.replayedFinalPriceAmount").value(100))
            .andExpect(jsonPath("$.matches").value(false));
    }

    @Test
    void replayOfAnUnknownCalculationIs404() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(replayStoredPriceCalculationUseCase.execute(any())).thenThrow(new PriceCalculationNotFoundException(eventId));

        mockMvc.perform(get("/audit/price-calculations/{eventId}/replay", eventId))
            .andExpect(status().isNotFound());
    }

    @Test
    void historyReturnsTheChronologicalEntriesOfOneEntity() throws Exception {
        when(queryEntityChangeHistoryUseCase.execute(any())).thenReturn(List.of(
            new AuditEntryDetails(
                UUID.randomUUID(), "PricingRuleUpdated", AuditEntityType.PRICING_RULE, "rule-1", null, null,
                "revenue@priceforge.com", "10%", "15%", null, "{}", T1)));

        mockMvc.perform(get("/audit/history/{entityType}/{entityId}", "PRICING_RULE", "rule-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].changedBy").value("revenue@priceforge.com"))
            .andExpect(jsonPath("$[0].previousState").value("10%"))
            .andExpect(jsonPath("$[0].newState").value("15%"))
            .andExpect(jsonPath("$[0].occurredAt").value("2026-07-20T10:00:00Z"));

        verify(queryEntityChangeHistoryUseCase).execute(new EntityChangeHistoryQuery(AuditEntityType.PRICING_RULE, "rule-1"));
    }

    @Test
    void historyOfAnUnknownEntityTypeIs400() throws Exception {
        mockMvc.perform(get("/audit/history/{entityType}/{entityId}", "NOT_A_TYPE", "x"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void historyForANonAuditableEntityTypeIs400() throws Exception {
        when(queryEntityChangeHistoryUseCase.execute(any())).thenThrow(new IllegalArgumentException("not auditable"));

        mockMvc.perform(get("/audit/history/{entityType}/{entityId}", "RULE_CONFLICT", "x"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void exportReturnsTheFileAsAnAttachmentWithItsContentType() throws Exception {
        when(exportAuditTrailUseCase.execute(any()))
            .thenReturn(new ExportedAuditTrail("a,b\r\n", "text/csv", "audit-trail.csv"));

        mockMvc.perform(get("/audit/export")
                .param("format", "CSV")
                .param("entityType", "PROMOTION")
                .param("entityId", "promo-1"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/csv"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"audit-trail.csv\""))
            .andExpect(content().string("a,b\r\n"));

        ArgumentCaptor<ExportAuditTrailQuery> query = ArgumentCaptor.forClass(ExportAuditTrailQuery.class);
        verify(exportAuditTrailUseCase).execute(query.capture());
        assertThat(query.getValue().format()).isEqualTo(AuditExportFormat.CSV);
        assertThat(query.getValue().filter().entityType()).isEqualTo(AuditEntityType.PROMOTION);
        assertThat(query.getValue().filter().entityId()).isEqualTo("promo-1");
    }

    @Test
    void exportRequiresAFormat() throws Exception {
        mockMvc.perform(get("/audit/export")).andExpect(status().isBadRequest());
    }

    @Test
    void anExportThatIsTooLargeIs422() throws Exception {
        when(exportAuditTrailUseCase.execute(any())).thenThrow(new ExportTooLargeException(20_000, 10_000));

        mockMvc.perform(get("/audit/export").param("format", "JSON"))
            .andExpect(status().isUnprocessableEntity());
    }
}
