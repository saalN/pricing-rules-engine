package com.priceforge.catalog.application.dto;

import com.priceforge.sharedkernel.ProductId;
import java.math.BigDecimal;

public record UpdateProductBasePriceResult(
    ProductId productId,
    BigDecimal previousBasePriceAmount,
    BigDecimal newBasePriceAmount,
    String currency
) {
}
