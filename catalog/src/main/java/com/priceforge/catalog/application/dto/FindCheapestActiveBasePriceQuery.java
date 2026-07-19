package com.priceforge.catalog.application.dto;

/**
 * {@code category} null means "global" — across every active product, not scoped to one category.
 */
public record FindCheapestActiveBasePriceQuery(String category) {
}
