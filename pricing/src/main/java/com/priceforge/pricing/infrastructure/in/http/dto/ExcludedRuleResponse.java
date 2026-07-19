package com.priceforge.pricing.infrastructure.in.http.dto;

import java.util.UUID;

public record ExcludedRuleResponse(UUID ruleId, String reason) {
}
