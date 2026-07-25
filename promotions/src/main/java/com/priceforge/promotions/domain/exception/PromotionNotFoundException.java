package com.priceforge.promotions.domain.exception;

import com.priceforge.sharedkernel.PromotionId;

public class PromotionNotFoundException extends RuntimeException {

    public PromotionNotFoundException(PromotionId id) {
        super("Promotion '" + id + "' does not exist");
    }
}
