package com.priceforge.catalog.infrastructure.in.http.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductBasePriceResponse(
    UUID productId,
    BigDecimal previousBasePriceAmount,
    BigDecimal newBasePriceAmount,
    String currency
) {
}
