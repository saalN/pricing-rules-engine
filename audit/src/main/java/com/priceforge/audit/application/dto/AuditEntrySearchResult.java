package com.priceforge.audit.application.dto;

import com.priceforge.audit.domain.model.AuditEntry;
import java.util.List;

public record AuditEntrySearchResult(List<AuditEntry> entries, long totalElements) {
}
