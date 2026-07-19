package com.priceforge.pricing.domain.exception;

import com.priceforge.sharedkernel.ProductId;

public class ProductNotActiveException extends RuntimeException {

    public ProductNotActiveException(ProductId productId) {
        super("Product '" + productId + "' is not ACTIVE");
    }
}
