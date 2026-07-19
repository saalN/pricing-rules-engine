package com.priceforge.catalog.application.dto;

import com.priceforge.sharedkernel.ProductId;
import java.math.BigDecimal;

/**
 * {@code status} is a plain String ("ACTIVE"/"INACTIVE"), not the domain {@code ProductStatus}
 * enum — this DTO is consumed cross-context (e.g. by Pricing), which must never depend on
 * Catalog's domain package.
 */
public record GetProductResult(
    ProductId productId,
    String sku,
    BigDecimal basePriceAmount,
    String currency,
    String category,
    String status
) {
}
