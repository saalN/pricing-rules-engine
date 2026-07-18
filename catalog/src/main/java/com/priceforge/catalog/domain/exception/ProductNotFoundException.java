package com.priceforge.catalog.domain.exception;

import com.priceforge.sharedkernel.ProductId;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(ProductId productId) {
        super("Product '" + productId + "' does not exist");
    }
}
