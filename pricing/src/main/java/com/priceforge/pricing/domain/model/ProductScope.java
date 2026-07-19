package com.priceforge.pricing.domain.model;

import com.priceforge.sharedkernel.ProductId;
import java.util.Objects;

public record ProductScope(ProductId productId) implements RuleScope {

    public ProductScope {
        Objects.requireNonNull(productId, "productId must not be null");
    }

    @Override
    public boolean appliesTo(ProductId productId, String category) {
        return this.productId.equals(productId);
    }
}
