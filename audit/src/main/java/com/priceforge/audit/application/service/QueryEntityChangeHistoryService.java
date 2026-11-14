package com.priceforge.audit.application.service;

import com.priceforge.audit.application.dto.AuditEntryDetails;
import com.priceforge.audit.application.dto.AuditEntryFilter;
import com.priceforge.audit.application.dto.EntityChangeHistoryQuery;
import com.priceforge.audit.application.port.in.QueryEntityChangeHistoryUseCase;
import com.priceforge.audit.application.port.out.AuditEntryRepositoryPort;
import com.priceforge.audit.domain.model.AuditEntityType;
import java.util.List;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-15: the whole lifecycle of one auditable entity, oldest first. A single entity's history is
 * short (creation, a handful of edits, deactivation), so it is returned unpaginated.
 */
@Service
public class QueryEntityChangeHistoryService implements QueryEntityChangeHistoryUseCase {

    private static final Set<AuditEntityType> AUDITABLE_ENTITIES =
        Set.of(AuditEntityType.PRODUCT, AuditEntityType.PRICING_RULE, AuditEntityType.PROMOTION);

    private static final int ENTITY_HISTORY_LIMIT = 1_000;

    private final AuditEntryRepositoryPort auditEntryRepository;

    public QueryEntityChangeHistoryService(AuditEntryRepositoryPort auditEntryRepository) {
        this.auditEntryRepository = auditEntryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'REVENUE_MANAGER', 'MARKETING', 'AUDITOR')")
    public List<AuditEntryDetails> execute(EntityChangeHistoryQuery query) {
        if (!AUDITABLE_ENTITIES.contains(query.entityType())) {
            throw new IllegalArgumentException(
                "entityType must be one of " + AUDITABLE_ENTITIES + ", but was " + query.entityType());
        }

        AuditEntryFilter filter = new AuditEntryFilter(
            query.entityType(), query.entityId(), null, null, null, null);

        return auditEntryRepository.search(filter, 0, ENTITY_HISTORY_LIMIT, false).entries().stream()
            .map(AuditEntryDetails::from)
            .toList();
    }
}
