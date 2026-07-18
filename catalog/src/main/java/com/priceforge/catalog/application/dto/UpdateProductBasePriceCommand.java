package com.priceforge.catalog.application.dto;

import com.priceforge.sharedkernel.ProductId;
import java.math.BigDecimal;

public record UpdateProductBasePriceCommand(
    ProductId productId,
    BigDecimal newBasePriceAmount,
    String changedBy
) {
}
