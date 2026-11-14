package com.priceforge.audit.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.priceforge.audit.application.dto.AuditEntryFilter;
import com.priceforge.audit.application.dto.AuditEntrySearchResult;
import com.priceforge.audit.application.dto.AuditExportFormat;
import com.priceforge.audit.application.dto.ExportAuditTrailQuery;
import com.priceforge.audit.application.dto.ExportedAuditTrail;
import com.priceforge.audit.application.port.out.AuditEntryRepositoryPort;
import com.priceforge.audit.application.port.out.AuditExportWriterPort;
import com.priceforge.audit.application.service.support.AuditEntries;
import com.priceforge.audit.application.service.support.InMemoryAuditEntryRepositoryPort;
import com.priceforge.audit.domain.exception.ExportTooLargeException;
import com.priceforge.audit.domain.model.AuditEntityType;
import com.priceforge.audit.domain.model.AuditEntry;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExportAuditTrailServiceTest {

    private static final Instant T1 = Instant.parse("2026-07-20T10:00:00Z");
    private static final Instant T2 = Instant.parse("2026-07-21T10:00:00Z");

    private final InMemoryAuditEntryRepositoryPort repository = new InMemoryAuditEntryRepositoryPort();

    @Test
    void exportsTheMatchingEntriesOldestFirstUsingTheRequestedFormat() {
        repository.saveIfAbsent(AuditEntries.change(
            AuditEntityType.PROMOTION, "promo-1", "PromotionExpired", T2, null, "EXPIRED", null));
        repository.saveIfAbsent(AuditEntries.change(
            AuditEntityType.PROMOTION, "promo-1", "PromotionCreated", T1, null, "name=x", null));
        repository.saveIfAbsent(AuditEntries.change(
            AuditEntityType.PROMOTION, "promo-2", "PromotionCreated", T1, null, "name=y", null));
        ExportAuditTrailService service = new ExportAuditTrailService(repository, List.of(csvWriter(), jsonWriter()));

        ExportedAuditTrail export = service.execute(new ExportAuditTrailQuery(
            new AuditEntryFilter(AuditEntityType.PROMOTION, "promo-1", null, null, null, null),
            AuditExportFormat.CSV));

        assertThat(export.content()).isEqualTo("PromotionCreated,PromotionExpired");
        assertThat(export.contentType()).isEqualTo("text/csv");
        assertThat(export.fileName()).isEqualTo("audit-trail.csv");
    }

    @Test
    void picksTheWriterForTheRequestedFormat() {
        ExportAuditTrailService service = new ExportAuditTrailService(repository, List.of(csvWriter(), jsonWriter()));

        ExportedAuditTrail export = service.execute(new ExportAuditTrailQuery(
            new AuditEntryFilter(null, null, null, null, null, null), AuditExportFormat.JSON));

        assertThat(export.contentType()).isEqualTo("application/json");
        assertThat(export.fileName()).isEqualTo("audit-trail.json");
    }

    @Test
    void refusesToTruncateAnExportThatIsTooLarge() {
        AuditEntryRepositoryPort huge = new AuditEntryRepositoryPort() {
            @Override
            public void saveIfAbsent(AuditEntry entry) {
            }

            @Override
            public Optional<AuditEntry> findById(UUID eventId) {
                return Optional.empty();
            }

            @Override
            public AuditEntrySearchResult search(AuditEntryFilter filter, int page, int size, boolean newestFirst) {
                return new AuditEntrySearchResult(List.of(), ExportAuditTrailService.EXPORT_ROW_LIMIT + 1L);
            }
        };
        ExportAuditTrailService service = new ExportAuditTrailService(huge, List.of(csvWriter()));

        assertThatThrownBy(() -> service.execute(new ExportAuditTrailQuery(
                new AuditEntryFilter(null, null, null, null, null, null), AuditExportFormat.CSV)))
            .isInstanceOf(ExportTooLargeException.class);
    }

    @Test
    void rejectsAFormatNoWriterSupports() {
        ExportAuditTrailService service = new ExportAuditTrailService(repository, List.of(csvWriter()));

        assertThatThrownBy(() -> service.execute(new ExportAuditTrailQuery(
                new AuditEntryFilter(null, null, null, null, null, null), AuditExportFormat.JSON)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static AuditExportWriterPort csvWriter() {
        return writer(AuditExportFormat.CSV, "text/csv", "csv");
    }

    private static AuditExportWriterPort jsonWriter() {
        return writer(AuditExportFormat.JSON, "application/json", "json");
    }

    /** Writes just the event types, comma-separated: enough to assert what was exported and in what order. */
    private static AuditExportWriterPort writer(AuditExportFormat format, String contentType, String extension) {
        return new AuditExportWriterPort() {
            @Override
            public AuditExportFormat format() {
                return format;
            }

            @Override
            public String contentType() {
                return contentType;
            }

            @Override
            public String fileExtension() {
                return extension;
            }

            @Override
            public String write(List<AuditEntry> entries) {
                return String.join(",", entries.stream().map(AuditEntry::eventType).toList());
            }
        };
    }
}
