package com.priceforge.audit.application.dto;

public record ExportAuditTrailQuery(AuditEntryFilter filter, AuditExportFormat format) {
}
