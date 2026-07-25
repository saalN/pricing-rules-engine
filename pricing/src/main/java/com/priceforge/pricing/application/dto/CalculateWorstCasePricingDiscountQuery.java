package com.priceforge.pricing.application.dto;

import java.math.BigDecimal;

/**
 * UC-13's input: the cheapest active base price in the promotion's target scope, and that scope
 * itself. {@code category} null means the promotion (and therefore this query) is global.
 */
public record CalculateWorstCasePricingDiscountQuery(BigDecimal basePriceAmount, String currency, String category) {
}
