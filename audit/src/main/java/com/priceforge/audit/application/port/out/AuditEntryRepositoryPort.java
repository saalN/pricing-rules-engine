package com.priceforge.audit.application.port.out;

import com.priceforge.audit.application.dto.AuditEntryFilter;
import com.priceforge.audit.application.dto.AuditEntrySearchResult;
import com.priceforge.audit.domain.model.AuditEntry;
import java.util.Optional;
import java.util.UUID;

/** Append-only: there is intentionally no update or delete operation (UC-14). */
public interface AuditEntryRepositoryPort {

    /** Idempotent on {@code eventId}: a redelivered event is silently ignored, not duplicated. */
    void saveIfAbsent(AuditEntry entry);

    Optional<AuditEntry> findById(UUID eventId);

    AuditEntrySearchResult search(AuditEntryFilter filter, int page, int size, boolean newestFirst);
}
