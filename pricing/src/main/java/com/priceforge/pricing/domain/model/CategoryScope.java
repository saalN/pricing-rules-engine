package com.priceforge.pricing.domain.model;

import com.priceforge.sharedkernel.ProductId;
import java.util.Objects;

public record CategoryScope(String category) implements RuleScope {

    public CategoryScope {
        Objects.requireNonNull(category, "category must not be null");
    }

    @Override
    public boolean appliesTo(ProductId productId, String category) {
        return this.category.equals(category);
    }
}
