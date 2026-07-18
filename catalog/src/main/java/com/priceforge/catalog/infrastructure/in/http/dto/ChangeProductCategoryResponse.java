package com.priceforge.catalog.infrastructure.in.http.dto;

import java.util.UUID;

public record ChangeProductCategoryResponse(UUID productId, String previousCategory, String newCategory) {
}
