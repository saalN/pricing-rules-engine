package com.priceforge.audit.application.service;

import com.priceforge.audit.application.dto.AuditEntrySearchResult;
import com.priceforge.audit.application.dto.ExportAuditTrailQuery;
import com.priceforge.audit.application.dto.ExportedAuditTrail;
import com.priceforge.audit.application.port.in.ExportAuditTrailUseCase;
import com.priceforge.audit.application.port.out.AuditEntryRepositoryPort;
import com.priceforge.audit.application.port.out.AuditExportWriterPort;
import com.priceforge.audit.domain.exception.ExportTooLargeException;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compliance export (UC-15) of any slice of the audit trail, oldest first. Refuses rather than
 * truncates when the slice is too large: a silently incomplete compliance report is worse than an
 * error that asks for a narrower filter.
 */
@Service
public class ExportAuditTrailService implements ExportAuditTrailUseCase {

    static final int EXPORT_ROW_LIMIT = 10_000;

    private final AuditEntryRepositoryPort auditEntryRepository;
    private final List<AuditExportWriterPort> writers;

    public ExportAuditTrailService(AuditEntryRepositoryPort auditEntryRepository, List<AuditExportWriterPort> writers) {
        this.auditEntryRepository = auditEntryRepository;
        this.writers = writers;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR')")
    public ExportedAuditTrail execute(ExportAuditTrailQuery query) {
        AuditExportWriterPort writer = writers.stream()
            .filter(candidate -> candidate.format() == query.format())
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported export format " + query.format()));

        AuditEntrySearchResult result = auditEntryRepository.search(query.filter(), 0, EXPORT_ROW_LIMIT, false);
        if (result.totalElements() > EXPORT_ROW_LIMIT) {
            throw new ExportTooLargeException(result.totalElements(), EXPORT_ROW_LIMIT);
        }

        return new ExportedAuditTrail(
            writer.write(result.entries()), writer.contentType(), "audit-trail." + writer.fileExtension());
    }
}
