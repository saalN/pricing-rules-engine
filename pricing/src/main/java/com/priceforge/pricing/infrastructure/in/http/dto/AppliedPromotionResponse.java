package com.priceforge.pricing.infrastructure.in.http.dto;

import java.math.BigDecimal;

public record AppliedPromotionResponse(String sourceId, String description, BigDecimal discountApplied, String reason) {
}
