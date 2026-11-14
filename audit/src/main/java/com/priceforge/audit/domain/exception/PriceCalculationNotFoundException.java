package com.priceforge.audit.domain.exception;

import java.util.UUID;

public class PriceCalculationNotFoundException extends RuntimeException {

    public PriceCalculationNotFoundException(UUID eventId) {
        super("No stored price calculation with event id " + eventId);
    }
}
