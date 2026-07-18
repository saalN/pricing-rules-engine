package com.priceforge.catalog.infrastructure.in.http.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangeProductCategoryRequest(@NotBlank String newCategory) {
}
