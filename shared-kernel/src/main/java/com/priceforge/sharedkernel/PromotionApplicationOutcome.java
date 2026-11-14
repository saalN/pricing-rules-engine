package com.priceforge.sharedkernel;

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
