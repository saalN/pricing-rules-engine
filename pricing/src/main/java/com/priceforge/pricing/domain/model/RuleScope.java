package com.priceforge.pricing.domain.model;

import com.priceforge.sharedkernel.ProductId;

public sealed interface RuleScope permits ProductScope, CategoryScope, GlobalScope {

    /** {@code category} may be null — a product with no category never matches a CategoryScope. */
    boolean appliesTo(ProductId productId, String category);
}
