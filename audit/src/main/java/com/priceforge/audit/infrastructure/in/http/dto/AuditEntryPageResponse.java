package com.priceforge.audit.infrastructure.in.http.dto;

import java.util.List;

public record AuditEntryPageResponse(List<AuditEntryResponse> items, long totalElements, int page, int size) {
}
