package com.priceforge.catalog.application.dto;

import com.priceforge.sharedkernel.ProductId;

public record ChangeProductCategoryResult(ProductId productId, String previousCategory, String newCategory) {
}
