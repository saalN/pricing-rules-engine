package com.priceforge.pricing.domain.model;

public enum ConflictReason {
    /** Applying a discount would have driven the price to zero or below; it was floored instead. */
    PRICE_FLOOR_APPLIED,
    /** Two same-priority rules were incompatible; UC-07's tie-break rules picked a winner. */
    TIEBREAK
}
