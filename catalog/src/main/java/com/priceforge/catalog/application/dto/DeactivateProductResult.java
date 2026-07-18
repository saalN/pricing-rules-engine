package com.priceforge.catalog.application.dto;

import com.priceforge.catalog.domain.model.ProductStatus;
import com.priceforge.sharedkernel.ProductId;

public record DeactivateProductResult(ProductId productId, ProductStatus status) {
}
