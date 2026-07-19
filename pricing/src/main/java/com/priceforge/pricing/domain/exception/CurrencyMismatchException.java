package com.priceforge.pricing.domain.exception;

/** UC-06: the platform never converts currencies — the request must match the product's own. */
public class CurrencyMismatchException extends RuntimeException {

    public CurrencyMismatchException(String requestedCurrency, String productCurrency) {
        super("Requested currency '" + requestedCurrency + "' does not match the product's currency '"
            + productCurrency + "'");
    }
}
