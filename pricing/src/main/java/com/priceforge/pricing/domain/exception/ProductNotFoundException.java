package com.priceforge.pricing.domain.exception;

import com.priceforge.sharedkernel.ProductId;

/**
 * Pricing's own view of "product not found" — distinct from Catalog's exception of the same name
 * (a different package, a different context), since Pricing must never depend on Catalog's
 * domain package.
 */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(ProductId productId) {
        super("Product '" + productId + "' does not exist");
    }
}
