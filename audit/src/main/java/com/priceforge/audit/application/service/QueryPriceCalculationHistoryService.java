package com.priceforge.audit.application.service;

import com.priceforge.audit.application.dto.AuditEntryDetails;
import com.priceforge.audit.application.dto.AuditEntryFilter;
import com.priceforge.audit.application.dto.AuditEntryPageResult;
import com.priceforge.audit.application.dto.AuditEntrySearchResult;
import com.priceforge.audit.application.dto.PriceCalculationHistoryQuery;
import com.priceforge.audit.application.port.in.QueryPriceCalculationHistoryUseCase;
import com.priceforge.audit.application.port.out.AuditEntryRepositoryPort;
import com.priceforge.audit.domain.model.AuditEntityType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-14: newest calculations first, so the most recent activity is on the first page. Every role
 * has read access to Audit (per the functional spec's permissions table); only exporting and
 * replaying are restricted.
 */
@Service
public class QueryPriceCalculationHistoryService implements QueryPriceCalculationHistoryUseCase {

    static final int MAX_PAGE_SIZE = 200;

    private final AuditEntryRepositoryPort auditEntryRepository;

    public QueryPriceCalculationHistoryService(AuditEntryRepositoryPort auditEntryRepository) {
        this.auditEntryRepository = auditEntryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'REVENUE_MANAGER', 'MARKETING', 'AUDITOR')")
    public AuditEntryPageResult execute(PriceCalculationHistoryQuery query) {
        if (query.page() < 0) {
            throw new IllegalArgumentException("page must not be negative, but was " + query.page());
        }
        if (query.size() < 1 || query.size() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                "size must be between 1 and " + MAX_PAGE_SIZE + ", but was " + query.size());
        }
        if (query.from() != null && query.to() != null && query.from().isAfter(query.to())) {
            throw new IllegalArgumentException("from must not be after to");
        }

        AuditEntryFilter filter = new AuditEntryFilter(
            AuditEntityType.PRICE_CALCULATION, null, query.productId(), query.userId(), query.from(), query.to());
        AuditEntrySearchResult result = auditEntryRepository.search(filter, query.page(), query.size(), true);

        return new AuditEntryPageResult(
            result.entries().stream().map(AuditEntryDetails::from).toList(),
            result.totalElements(), query.page(), query.size());
    }
}
