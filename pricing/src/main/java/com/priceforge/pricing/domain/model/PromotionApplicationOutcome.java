package com.priceforge.pricing.domain.model;

import com.priceforge.sharedkernel.Money;
import java.util.List;

public record PromotionApplicationOutcome(
    Money newPrice,
    List<AppliedPromotionDetail> applied,
    List<ExcludedPromotionDetail> excluded,
    boolean priceFloorApplied
) {

    public static PromotionApplicationOutcome none(Money currentPrice) {
        return new PromotionApplicationOutcome(currentPrice, List.of(), List.of(), false);
    }
}
