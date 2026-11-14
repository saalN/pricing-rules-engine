package com.priceforge.audit.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.priceforge.audit.application.dto.AuditEntryPageResult;
import com.priceforge.audit.application.dto.PriceCalculationHistoryQuery;
import com.priceforge.audit.application.service.support.AuditEntries;
import com.priceforge.audit.application.service.support.InMemoryAuditEntryRepositoryPort;
import com.priceforge.audit.domain.model.AuditEntityType;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class QueryPriceCalculationHistoryServiceTest {

    private static final Instant T1 = Instant.parse("2026-07-20T10:00:00Z");
    private static final Instant T2 = Instant.parse("2026-07-21T10:00:00Z");
    private static final Instant T3 = Instant.parse("2026-07-22T10:00:00Z");

    private final InMemoryAuditEntryRepositoryPort repository = new InMemoryAuditEntryRepositoryPort();
    private final QueryPriceCalculationHistoryService service = new QueryPriceCalculationHistoryService(repository);

    @Test
    void returnsOnlyPriceCalculationsNewestFirst() {
        repository.saveIfAbsent(AuditEntries.priceCalculation("p-1", "u-1", T1, BigDecimal.TEN, BigDecimal.ONE));
        repository.saveIfAbsent(AuditEntries.priceCalculation("p-1", "u-1", T3, BigDecimal.TEN, BigDecimal.ONE));
        repository.saveIfAbsent(AuditEntries.change(
            AuditEntityType.PRODUCT, "p-1", "ProductDeactivated", T2, "ACTIVE", "INACTIVE", null));

        AuditEntryPageResult result = service.execute(new PriceCalculationHistoryQuery(null, null, null, null, 0, 50));

        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.items()).extracting(item -> item.occurredAt()).containsExactly(T3, T1);
    }

    @Test
    void filtersByProductUserAndDateRange() {
        repository.saveIfAbsent(AuditEntries.priceCalculation("p-1", "u-1", T1, BigDecimal.TEN, BigDecimal.ONE));
        repository.saveIfAbsent(AuditEntries.priceCalculation("p-1", "u-2", T2, BigDecimal.TEN, BigDecimal.ONE));
        repository.saveIfAbsent(AuditEntries.priceCalculation("p-2", "u-1", T2, BigDecimal.TEN, BigDecimal.ONE));
        repository.saveIfAbsent(AuditEntries.priceCalculation("p-1", "u-1", T3, BigDecimal.TEN, BigDecimal.ONE));

        AuditEntryPageResult byProduct = service.execute(new PriceCalculationHistoryQuery("p-1", null, null, null, 0, 50));
        AuditEntryPageResult byUser = service.execute(new PriceCalculationHistoryQuery(null, "u-2", null, null, 0, 50));
        AuditEntryPageResult byRange = service.execute(new PriceCalculationHistoryQuery(null, null, T2, T2, 0, 50));
        AuditEntryPageResult combined = service.execute(new PriceCalculationHistoryQuery("p-1", "u-1", T2, null, 0, 50));

        assertThat(byProduct.totalElements()).isEqualTo(3);
        assertThat(byUser.totalElements()).isEqualTo(1);
        assertThat(byRange.totalElements()).isEqualTo(2);
        assertThat(combined.totalElements()).isEqualTo(1);
    }

    @Test
    void paginatesAndReportsTheTotal() {
        repository.saveIfAbsent(AuditEntries.priceCalculation("p-1", "u-1", T1, BigDecimal.TEN, BigDecimal.ONE));
        repository.saveIfAbsent(AuditEntries.priceCalculation("p-1", "u-1", T2, BigDecimal.TEN, BigDecimal.ONE));
        repository.saveIfAbsent(AuditEntries.priceCalculation("p-1", "u-1", T3, BigDecimal.TEN, BigDecimal.ONE));

        AuditEntryPageResult secondPage = service.execute(new PriceCalculationHistoryQuery(null, null, null, null, 1, 2));

        assertThat(secondPage.totalElements()).isEqualTo(3);
        assertThat(secondPage.items()).hasSize(1);
        assertThat(secondPage.items().get(0).occurredAt()).isEqualTo(T1);
    }

    @Test
    void rejectsAnInvalidPageOrSizeOrDateRange() {
        assertThatThrownBy(() -> service.execute(new PriceCalculationHistoryQuery(null, null, null, null, -1, 10)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.execute(new PriceCalculationHistoryQuery(null, null, null, null, 0, 0)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.execute(new PriceCalculationHistoryQuery(null, null, null, null, 0, 201)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.execute(new PriceCalculationHistoryQuery(null, null, T3, T1, 0, 10)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("from");
    }
}
