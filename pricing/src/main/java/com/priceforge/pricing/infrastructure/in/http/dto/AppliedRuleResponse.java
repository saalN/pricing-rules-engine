package com.priceforge.pricing.infrastructure.in.http.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AppliedRuleResponse(
    UUID ruleId, String ruleType, String description, BigDecimal discountApplied, String reason
) {
}
