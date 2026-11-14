package com.priceforge.audit.application.dto;

import com.priceforge.audit.domain.model.AuditEntry;

/** Raised by the inbound event adapters, one per consumed domain event. */
public record RecordAuditEntryCommand(AuditEntry entry) {
}
