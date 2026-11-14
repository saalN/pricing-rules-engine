package com.priceforge.pricing.application.dto;

import java.math.BigDecimal;

public record ReplayPriceCalculationResult(BigDecimal basePriceAmount, BigDecimal finalPriceAmount, String currency) {
}
