package com.priceforge.catalog.domain.exception;

/**
 * Thrown when a product's base price is zero or negative — the base price is the starting point
 * for the pricing engine and can never itself be invalid.
 */
public class InvalidBasePriceException extends RuntimeException {

    public InvalidBasePriceException(String message) {
        super(message);
    }
}
