package com.priceforge.catalog.domain.exception;

/**
 * Thrown when a product is created with a SKU that already exists in the catalog.
 */
public class DuplicateSkuException extends RuntimeException {

    public DuplicateSkuException(String sku) {
        super("A product with SKU '" + sku + "' already exists");
    }
}
