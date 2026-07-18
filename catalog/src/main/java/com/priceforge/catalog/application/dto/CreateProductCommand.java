package com.priceforge.catalog.application.dto;

import java.math.BigDecimal;
import java.util.Map;

public record CreateProductCommand(
    String sku,
    String name,
    String description,
    BigDecimal basePriceAmount,
    String currencyCode,
    String category,
    Map<String, Object> customAttributes
) {
}
