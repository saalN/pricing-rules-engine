package com.priceforge.pricing.domain.model;

import com.priceforge.sharedkernel.ProductId;

/** A rule with no explicit scope applies to every product (UC-05: "una regla sin ámbito... es global"). */
public record GlobalScope() implements RuleScope {

    @Override
    public boolean appliesTo(ProductId productId, String category) {
        return true;
    }
}
