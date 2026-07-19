package com.priceforge.pricing.infrastructure.in.http.dto;

import java.util.UUID;

public record DeactivatePricingRuleResponse(UUID ruleId, String status) {
}
