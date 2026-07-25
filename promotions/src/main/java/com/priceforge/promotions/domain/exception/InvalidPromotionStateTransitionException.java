package com.priceforge.promotions.domain.exception;

import com.priceforge.promotions.domain.model.PromotionStatus;
import com.priceforge.sharedkernel.PromotionId;

public class InvalidPromotionStateTransitionException extends RuntimeException {

    public InvalidPromotionStateTransitionException(PromotionId id, PromotionStatus actual, String attemptedTransition) {
        super("Promotion '" + id + "' cannot " + attemptedTransition + " from status " + actual);
    }
}
