package com.priceforge.catalog.application.dto;

import com.priceforge.sharedkernel.ProductId;

public record DeactivateProductCommand(ProductId productId) {
}
