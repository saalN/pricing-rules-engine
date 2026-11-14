package com.priceforge.audit.application.dto;

import java.util.List;

public record AuditEntryPageResult(List<AuditEntryDetails> items, long totalElements, int page, int size) {
}
