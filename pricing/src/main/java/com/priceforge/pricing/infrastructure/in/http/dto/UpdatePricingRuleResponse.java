package com.priceforge.pricing.infrastructure.in.http.dto;

import java.util.UUID;

public record UpdatePricingRuleResponse(UUID ruleId, String previousState, String newState) {
}
