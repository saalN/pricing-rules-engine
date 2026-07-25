package com.priceforge.promotions.application.service.support;

import com.priceforge.pricing.application.dto.CalculateWorstCasePricingDiscountQuery;
import com.priceforge.pricing.application.dto.CalculateWorstCasePricingDiscountResult;
import com.priceforge.pricing.application.port.in.CalculateWorstCasePricingDiscountUseCase;
import java.math.BigDecimal;

/** In-memory stand-in for Pricing's UC-13 cross-context query, for Promotions' tests. */
public final class FakeCalculateWorstCasePricingDiscountUseCase implements CalculateWorstCasePricingDiscountUseCase {

    private BigDecimal worstCaseDiscountAmount = BigDecimal.ZERO;

    public void setWorstCaseDiscountAmount(BigDecimal amount) {
        this.worstCaseDiscountAmount = amount;
    }

    @Override
    public CalculateWorstCasePricingDiscountResult execute(CalculateWorstCasePricingDiscountQuery query) {
        return new CalculateWorstCasePricingDiscountResult(worstCaseDiscountAmount);
    }
}
